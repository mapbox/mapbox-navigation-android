package com.mapbox.navigation.copilot

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.work.HistoryUploadWorker
import com.mapbox.navigation.copilot.work.PeriodicHistoryCleanupWorker
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
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

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
    private lateinit var historyFilesDir: File

    private lateinit var mockedContext: Context

    @Before
    fun setup() {
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
    }

    @Test
    fun `fixtures check`() {
        assertEquals("num of stub files", 14, historyFilesDir.listFiles()!!.size)
        assertEquals("num of uploadable recordings", 5, uploadableFilesMap.size)
        assertEquals("num of residual recordings", 4, residualRecordings.size)
    }

    @Test
    fun `should delete all residual recordings`() = runTest {
        PeriodicHistoryCleanupWorker(mockedContext, workerParams())
            .doWork()

        val deletedFiles = residualRecordings.filter { !it.exists() }
        assertEquals(residualRecordings.size, deletedFiles.size)
    }

    @Test
    fun `should upload all recordings with metadata file except latest`() = runTest {
        PeriodicHistoryCleanupWorker(mockedContext, workerParams())
            .doWork()

        val expectedSessionUploads = uploadableFilesMap.entries
            .sortedBy { it.key }
            .dropLast(1)
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

    // Creates a temporary 'copilot' folder with:
    // - 10 metadata.json + pbf.gz file pairs (5 json + 5 gz)
    // - 4 pbf.gz residual recording files
    private fun setupStubHistoryFiles() {
        uploadableFilesMap = mutableMapOf()
        residualRecordings = mutableListOf()
        historyFilesDir = folder.newFolder("copilot")

        repeat(9) { i ->
            val id = UUID.randomUUID().toString()
            val name = "2024-08-15T16-0$i-01Z_$id"
            val recordingFile = File(historyFilesDir, "$name.pbf.gz").apply { createNewFile() }
            // every second recording file won't have metadata.json file associated with it
            if (i % 2 == 0) {
                // recording with session
                val session = CopilotSession(
                    appMode = "mbx-debug",
                    driveMode = "free-drive",
                    driveId = "drive-$i",
                    startedAt = "2024-08-15T16:0$i:01.801Z",
                    endedAt = "2024-08-15T17:00:00.801Z",
                    navSdkVersion = "3.3.0-rc.1",
                    navNativeSdkVersion = "316.0.0",
                    appVersion = "0.17.0.local",
                    appUserId = "user-id-$i",
                    appSessionId = "session-id-$i",
                    recording = recordingFile.absolutePath,
                )
                val metadataFile = File(historyFilesDir, "$name.metadata.json")
                metadataFile.writeText(session.toJson())
                uploadableFilesMap[metadataFile.absolutePath] = session
            } else {
                // residual recording file
                residualRecordings.add(recordingFile)
            }
        }
    }
}
