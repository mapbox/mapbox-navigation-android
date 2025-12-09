package com.mapbox.navigation.copilot

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.attachmentFilename
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.work.HistoryUploadWorker
import com.mapbox.navigation.copilot.work.PeriodicHistoryCleanupWorker
import com.mapbox.navigation.core.internal.telemetry.standalone.EventsServiceProvider
import com.mapbox.navigation.core.internal.telemetry.standalone.StandaloneNavigationTelemetry
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.logD
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MaximumLineLength", "MaxLineLength")
class PeriodicHistoryCleanupWorkerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggingFrontendTestRule = LoggingFrontendTestRule()

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    // absolute metadata file path -> CopilotSession
    private lateinit var uploadableFilesMap: MutableMap<String, CopilotSession>
    private lateinit var residualRecordings: MutableList<File>
    private lateinit var nonResidualRecordings: MutableList<File>
    private lateinit var historyFilesDir: File

    private lateinit var mockedContext: Context

    @Before
    fun setup() {
        mockkObject(StandaloneNavigationTelemetry.Companion)
        every { StandaloneNavigationTelemetry.getOrCreate() } returns mockk(relaxed = true)

        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } answers {
            // // uncomment to verify log printout
            // println(secondArg<String?>() + " " + firstArg())
        }

        mockkObject(HistoryUploadWorker.Companion)
        every { HistoryUploadWorker.uploadHistory(any(), any()) } just Runs

        mockedContext = mockk<Context>(relaxed = true)
        setupStubHistoryFiles()
    }

    @After
    fun teardown() {
        unmockkAll()
        unmockkObject(EventsServiceProvider)
    }

    @Test
    fun `fixtures check`() {
        assertEquals(
            uploadableFilesMap.keys.map { it.extractSessionDate() }.sorted(),
            setOf(
                "2025-05-12T10-01-01Z",
                "2025-05-12T10:02:01.801Z",
            ).toList(),
        )
        assertEquals(
            residualRecordings.map { it.absolutePath.extractSessionDate() }.sorted(),
            listOf("2025-05-12T10-03-01Z", "2025-05-12T10-04-01Z").sorted(),
        )
        assertEquals(
            nonResidualRecordings.sorted().map { it.absolutePath.extractSessionDate() },
            listOf(
                "2025-05-12T10-01-01Z",
                "2025-05-12T10:02:01.801Z",
                "2025-05-12T10-05-01Z",
            ).sorted(),
        )
    }

    @Test
    fun `should delete all residual recordings`() = runTest {
        PeriodicHistoryCleanupWorker(mockedContext, workerParams())
            .doWork()

        val deletedFiles = residualRecordings.filter { !it.exists() }
        assertEquals(
            residualRecordings.map { it.name }.sorted(),
            deletedFiles.map { it.name }.sorted(),
        )
    }

    @Test
    fun `should not delete non-residual recordings`() = runTest {
        PeriodicHistoryCleanupWorker(mockedContext, workerParams())
            .doWork()

        val existingFiles = nonResidualRecordings.filter { it.exists() }
        assertEquals(
            nonResidualRecordings.map { it.name }.sorted(),
            existingFiles.map { it.name }.sorted(),
        )
    }

    @Test
    fun `should upload all recordings with metadata file except latest`() = runTest {
        PeriodicHistoryCleanupWorker(mockedContext, workerParams())
            .doWork()

        val expectedSessionUploads = uploadableFilesMap.entries
            .sortedBy { it.key }
            .map { it.value }

        verifyOrder {
            expectedSessionUploads.forEach {
                HistoryUploadWorker.uploadHistory(mockedContext, it)
            }
        }
    }

    private fun workerParams(): WorkerParameters {
        return mockk<WorkerParameters>(relaxed = true) {
            every { inputData } returns Data.Builder()
                .putString("history_files_dir", historyFilesDir.absolutePath)
                .build()
        }
    }

    private fun rename(from: File, filename: String): File =
        File(from.parent, filename).also { from.renameTo(it) }

    private fun createSessionFiles(
        currentSecond: Int,
        hasRecording: Boolean,
        isLatest: Boolean,
        recordingRenamed: Boolean = false,
    ) {
        val sessionName = generateSessionName(currentSecond)
        val originalRecording = File(historyFilesDir, "$sessionName.pbf.gz")
        val session = createCopilotSession(currentSecond, originalRecording.absolutePath)

        val recording = if (recordingRenamed) {
            rename(File(session.recording), attachmentFilename(session))
        } else {
            originalRecording
        }.also {
            it.createNewFile()
        }

        if (hasRecording) {
            val sessionFile = File(historyFilesDir, "$sessionName.metadata.json").apply {
                writeText(session.toJson())
            }

            if (!isLatest) {
                uploadableFilesMap[recording.absolutePath] = session
            }

            nonResidualRecordings.add(recording)
        } else {
            residualRecordings.add(recording)
        }
    }

    private fun setupStubHistoryFiles() {
        uploadableFilesMap = mutableMapOf()
        residualRecordings = mutableListOf()
        nonResidualRecordings = mutableListOf()
        historyFilesDir = folder.newFolder("copilot")

        createSessionFiles(currentSecond = 1, hasRecording = true, isLatest = false)
        createSessionFiles(
            currentSecond = 2,
            hasRecording = true,
            isLatest = false,
            recordingRenamed = true,
        )
        createSessionFiles(currentSecond = 3, hasRecording = false, isLatest = false)
        createSessionFiles(currentSecond = 4, hasRecording = false, isLatest = false)
        createSessionFiles(currentSecond = 5, hasRecording = true, isLatest = true)
    }

    private fun generateSessionName(currentMinute: Int) =
        "2025-05-12T10-0$currentMinute-01Z_${UUID.randomUUID()}"

    private fun createCopilotSession(currentMinute: Int, recordingFile: String) = CopilotSession(
        appMode = "mbx-debug",
        driveMode = "free-drive",
        driveId = "drive-$currentMinute",
        startedAt = "2025-05-12T10:0$currentMinute:01.801Z",
        endedAt = "2025-05-12T11:00:00.801Z",
        navSdkVersion = "3.3.0-rc.1",
        navNativeSdkVersion = "316.0.0",
        appVersion = "0.17.0.local",
        appUserId = "user-id-$currentMinute",
        appSessionId = "session-id-$currentMinute",
        recording = recordingFile,
    )

    private fun String.extractSessionDate() = substringAfterLast("/").substringBefore("_")
}
