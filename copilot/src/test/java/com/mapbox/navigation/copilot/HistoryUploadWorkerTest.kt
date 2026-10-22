package com.mapbox.navigation.copilot

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceInterface
import com.mapbox.common.MapboxOptions
import com.mapbox.common.TransferError
import com.mapbox.common.TransferErrorCode
import com.mapbox.common.TransferState
import com.mapbox.common.UploadOptions
import com.mapbox.common.UploadStatus
import com.mapbox.common.UploadStatusCallback
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.create
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.delete
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.rename
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.internal.PushStatus
import com.mapbox.navigation.copilot.internal.PushStatusObserver
import com.mapbox.navigation.copilot.work.HistoryUploadWorker
import com.mapbox.navigation.copilot.work.HistoryUploadWorker.Companion.cancelScheduledUploading
import com.mapbox.navigation.copilot.work.HistoryUploadWorker.Companion.putCopilotSession
import com.mapbox.navigation.core.internal.telemetry.standalone.StandaloneNavigationTelemetry
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/* ktlint-disable max-line-length */
@Suppress("MaximumLineLength", "MaxLineLength")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class HistoryUploadWorkerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var mockedContext: Context
    private lateinit var mockedUploadServiceInterface: HttpServiceInterface
    private val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)

    private val stubCopilotSession = CopilotSession(
        appMode = "mbx-debug",
        driveMode = "free-drive",
        driveId = "1e59e8e7-618b-4670-b6d6-a2cb5cf2ed98",
        startedAt = "2024-08-15T16:31:01.801Z",
        endedAt = "2024-08-15T17:00:00.801Z",
        navSdkVersion = "3.3.0-rc.1",
        navNativeSdkVersion = "316.0.0",
        appVersion = "0.17.0.local",
        appUserId = "user-id",
        appSessionId = "session-id",
        recording = "/tmp/2024-08-15T16-31-01Z_9398d18c-439e-49b7-a889-d08ebab828b2.pbf.gz",
    )
    private val expectedAttachmentFilename =
        "2024-08-15T16:31:01.801Z__2024-08-15T17:00:00.801Z__android__3.3.0-rc.1__316.0.0_____0.17.0.local__user-id__session-id.pbf.gz"
    private val expectedAttachmentFilepath = "/tmp/$expectedAttachmentFilename"
    private val accessToken = "pk.my-token"
    private val expectedUploadUrl = "https://events.mapbox.com/attachments/v1?access_token=$accessToken"
    private val uploadSessionId = "my-session-id"

    @Before
    fun setup() {
        // Mock File operations
        mockkObject(HistoryAttachmentsUtils)
        coEvery { rename(any(), any()) } coAnswers {
            val originalFile = firstArg<File>()
            val newFilename = secondArg<String>()
            mockk<File>(relaxed = true) {
                every { parent } returns originalFile.parent
                every { absolutePath } returns originalFile.parent.orEmpty() + "/" + newFilename
                every { name } returns newFilename
                every { exists() } returns true
            }
        }
        every { create(any(), any()) } answers {
            val parent = firstArg<String?>()
            val child = secondArg<String>()
            mockk<File>(relaxed = true) {
                every { this@mockk.parent } returns parent
                every { absolutePath } returns parent.orEmpty() + "/" + child
                every { name } returns child
                every { exists() } returns true
            }
        }
        every { delete(any()) } returns false

        mockkStatic(MapboxOptions::class)
        every { MapboxOptions.accessToken } returns accessToken

        mockedContext = mockk<Context>(relaxed = true)
        mockedUploadServiceInterface = mockHttpService()

        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)

        mockkObject(StandaloneNavigationTelemetry.Companion)
        every { StandaloneNavigationTelemetry.getOrCreate() } returns mockk(relaxed = true)

        mockkObject(HistoryUploadWorker.Companion)
        every { cancelScheduledUploading(any(), any()) } returns Unit
    }

    @After
    fun teardown() {
        MapboxCopilot.pushStatusObservers.clear()
        unmockkAll()
    }

    @Test
    fun `onPushStatusChanged Failed when FAILED`() = runBlocking {
        givenUploadServiceAnswer(
            uploadStatus(
                state = TransferState.FAILED,
                error = mockk<TransferError>(relaxed = true) {
                    every { code } returns mockk<TransferErrorCode>()
                },
            ),
        )

        HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession))
            .doWork()

        verify(exactly = 1) {
            mockedPushStatusObserver.onPushStatusChanged(ofType<PushStatus.Failed>())
        }
    }

    @Test
    fun `onPushStatusChanged Success when FINISHED - 204`() = runBlocking {
        givenUploadServiceAnswer(
            uploadStatus(
                state = TransferState.FINISHED,
                error = null,
                httpResult = httpResult(204),
            ),
        )

        HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession))
            .doWork()

        verify(exactly = 1) {
            mockedPushStatusObserver.onPushStatusChanged(ofType<PushStatus.Success>())
        }
    }

    @Test
    fun `onPushStatusChanged Failed when FINISHED - 401`() = runBlocking {
        val uploadOptionsSlot = slot<UploadOptions>()
        givenUploadServiceAnswer(
            uploadStatus(
                state = TransferState.FINISHED,
                error = null,
                httpResult = httpResult(401),
            ),
            uploadOptionsCapture = uploadOptionsSlot,
        )

        HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession))
            .doWork()

        verify(exactly = 1) {
            mockedPushStatusObserver.onPushStatusChanged(ofType<PushStatus.Failed>())
        }
    }

    @Test
    fun `AttachmentMetadata is correctly set`() = runBlocking {
        val uploadOptionsCapture = slot<UploadOptions>()
        givenUploadServiceAnswer(
            uploadStatus(
                state = TransferState.FINISHED,
                error = null,
                httpResult = httpResult(204),
            ),
            uploadOptionsCapture = uploadOptionsCapture,
        )

        HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession))
            .doWork()

        val uploadOptions = uploadOptionsCapture.captured
        val attachment = uploadOptions.getAttachments().first()
        assertEquals("attachment.created", stubCopilotSession.startedAt, attachment.created)
        assertEquals("attachment.name", expectedAttachmentFilename, attachment.name)
        assertEquals("uploadOptions.filePath", expectedAttachmentFilepath, uploadOptions.filePath)
        assertEquals("attachment.sessionId", uploadSessionId, attachment.sessionId)
        assertEquals("uploadOptions.url", expectedUploadUrl, uploadOptions.url)
        assertEquals(
            "uploadOptions.sdkInformation",
            MapboxCopilot.sdkInformation,
            uploadOptions.sdkInformation,
        )
    }

    @Test
    fun `Result retry - FAILED`() = runBlocking {
        givenUploadServiceAnswer(
            uploadStatus(
                state = TransferState.FAILED,
                error = mockk<TransferError>(relaxed = true) {
                    every { code } returns mockk<TransferErrorCode>()
                },
            ),
        )

        val sut = HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession))
        val result = sut.doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun `remove recording and metadata file - FINISHED 204`() = runBlocking {
        givenUploadServiceAnswer(
            uploadStatus(
                state = TransferState.FINISHED,
                error = null,
                httpResult = httpResult(204),
            ),
        )

        HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession))
            .doWork()

        val deletedFiles = mutableListOf<File>()
        verify(exactly = 2) { delete(capture(deletedFiles)) }
        assertEquals(2, deletedFiles.size)
        assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("pbf.gz") })
        assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("metadata.json") })
        verify(exactly = 1) {
            cancelScheduledUploading(any(), any())
        }
    }

    @Test
    fun `Result success - FINISHED 204`() = runBlocking {
        givenUploadServiceAnswer(
            uploadStatus(
                state = TransferState.FINISHED,
                error = null,
                httpResult = httpResult(204),
            ),
        )

        val result = HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession))
            .doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun `Result retry - FINISHED 401`() = runBlocking {
        givenUploadServiceAnswer(
            uploadStatus(
                state = TransferState.FINISHED,
                error = null,
                httpResult = httpResult(401),
            ),
        )

        val result = HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession))
            .doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun `remove history file not called - FAILED, runAttemptCount = 1`() = runBlocking {
        givenUploadServiceAnswer(
            uploadStatus(
                state = TransferState.FAILED,
                error = mockk<TransferError>(relaxed = true) {
                    every { code } returns mockk<TransferErrorCode>()
                },
            ),
        )

        HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession, runAttemptCount = 1))
            .doWork()

        verify(exactly = 0) {
            delete(any())
        }

        verify(exactly = 0) {
            cancelScheduledUploading(any(), any())
        }
    }

    @Test
    fun `remove history file - FAILED, runAttemptCount = MAX_RUN_ATTEMPT_COUNT`() =
        runBlocking {
            givenUploadServiceAnswer(
                uploadStatus(
                    state = TransferState.FAILED,
                    error = mockk<TransferError>(relaxed = true) {
                        every { code } returns mockk<TransferErrorCode>()
                    },
                ),
            )

            HistoryUploadWorker(
                mockedContext,
                workerParams(
                    stubCopilotSession,
                    runAttemptCount = HistoryUploadWorker.MAX_RUN_ATTEMPT_COUNT,
                ),
            ).doWork()

            val deletedFiles = mutableListOf<File>()
            verify(exactly = 2) {
                delete(capture(deletedFiles))
            }
            assertEquals(2, deletedFiles.size)
            assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("pbf.gz") })
            assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("metadata.json") })
            verify(exactly = 1) {
                cancelScheduledUploading(any(), any())
            }
        }

    @Test
    fun `remove history file - FINISHED 204`() =
        runBlocking {
            givenUploadServiceAnswer(
                uploadStatus(
                    state = TransferState.FINISHED,
                    error = null,
                    httpResult = httpResult(204),
                ),
            )

            HistoryUploadWorker(mockedContext, workerParams(stubCopilotSession))
                .doWork()

            val deletedFiles = mutableListOf<File>()
            verify(exactly = 2) {
                delete(capture(deletedFiles))
            }
            assertEquals(2, deletedFiles.size)
            assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("pbf.gz") })
            assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("metadata.json") })
            verify(exactly = 1) {
                cancelScheduledUploading(any(), any())
            }
        }

    @Test
    fun `Result failure - runAttemptCount greater or equal than MAX_RUN_ATTEMPT_COUNT`() =
        runBlocking {
            givenUploadServiceAnswer(
                uploadStatus(
                    state = TransferState.FAILED,
                    error = mockk<TransferError>(relaxed = true) {
                        every { code } returns mockk<TransferErrorCode>()
                    },
                ),
            )

            val result = HistoryUploadWorker(
                mockedContext,
                workerParams(stubCopilotSession, HistoryUploadWorker.MAX_RUN_ATTEMPT_COUNT),
            ).doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
        }

    @Test
    fun `upload prefers the current token over the scheduled one`() = runBlocking {
        val url = uploadUrlWith(
            currentToken = "pk.rotated",
            scheduledToken = "pk.scheduled",
            legacyUrl = uploadUrl("pk.legacy"),
        )

        assertEquals(uploadUrl("pk.rotated"), url)
    }

    @Test
    fun `upload falls back to the scheduled token when the current one is empty`() = runBlocking {
        val url = uploadUrlWith(
            currentToken = "",
            scheduledToken = "pk.scheduled",
            legacyUrl = uploadUrl("pk.legacy"),
        )

        assertEquals(uploadUrl("pk.scheduled"), url)
    }

    @Test
    fun `upload falls back to the token in a legacy url when no other token available`() = runBlocking {
        val url = uploadUrlWith(
            currentToken = "",
            scheduledToken = null,
            legacyUrl = uploadUrl("pk.legacy"),
        )

        assertEquals(uploadUrl("pk.legacy"), url)
    }

    @Test
    fun `upload uses the current token for a legacy job with an empty token`() = runBlocking {
        val url = uploadUrlWith(
            currentToken = "pk.rotated",
            scheduledToken = null,
            legacyUrl = uploadUrl(""),
        )

        assertEquals(uploadUrl("pk.rotated"), url)
    }

    @Test
    fun `Result retry - legacy job with an empty token and no current token`() = runBlocking {
        every { MapboxOptions.accessToken } returns ""

        val result = HistoryUploadWorker(
            mockedContext,
            workerParams(stubCopilotSession, scheduledToken = null, legacyUrl = uploadUrl("")),
        ).doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        verify(exactly = 0) { mockedUploadServiceInterface.upload(any(), any()) }
        verify(exactly = 0) { delete(any()) }
    }

    @Test
    fun `Result retry and nothing deleted - access token is blank`() = runBlocking {
        every { MapboxOptions.accessToken } returns ""

        val result = HistoryUploadWorker(
            mockedContext,
            workerParams(stubCopilotSession, runAttemptCount = 1, scheduledToken = ""),
        ).doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        verify(exactly = 0) { mockedUploadServiceInterface.upload(any(), any()) }
        verify(exactly = 0) { delete(any()) }
    }

    @Test
    fun `Result failure and files deleted - access token is blank at MAX_RUN_ATTEMPT_COUNT`() =
        runBlocking {
            every { MapboxOptions.accessToken } returns ""

            val result = HistoryUploadWorker(
                mockedContext,
                workerParams(
                    stubCopilotSession,
                    HistoryUploadWorker.MAX_RUN_ATTEMPT_COUNT,
                    scheduledToken = "",
                ),
            ).doWork()

            assertTrue(result is ListenableWorker.Result.Failure)
            verify(exactly = 0) { mockedUploadServiceInterface.upload(any(), any()) }
            verifyRecordingAndSessionFilesDeleted()
        }

    @Test
    fun `Result retry and nothing deleted - 401 below MAX_RUN_ATTEMPT_COUNT`() = runBlocking {
        givenUploadServiceAnswer(
            uploadStatus(TransferState.FINISHED, httpResult = httpResult(401)),
        )

        val result = HistoryUploadWorker(
            mockedContext,
            workerParams(stubCopilotSession, HistoryUploadWorker.MAX_RUN_ATTEMPT_COUNT - 1),
        ).doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        verify(exactly = 0) { delete(any()) }
    }

    @Test
    fun `Result failure and files deleted - 401 at MAX_RUN_ATTEMPT_COUNT`() = runBlocking {
        givenUploadServiceAnswer(
            uploadStatus(TransferState.FINISHED, httpResult = httpResult(401)),
        )

        val result = HistoryUploadWorker(
            mockedContext,
            workerParams(stubCopilotSession, HistoryUploadWorker.MAX_RUN_ATTEMPT_COUNT),
        ).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verifyRecordingAndSessionFilesDeleted()
    }

    @Test
    fun `scheduled retries keep the recording for less than 24 hours`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        val request = slot<OneTimeWorkRequest>()

        HistoryUploadWorker.uploadHistory(mockedContext, stubCopilotSession)

        verify(exactly = 1) { workManager.enqueueUniqueWork(any(), any(), capture(request)) }
        val workSpec = request.captured.workSpec
        assertEquals(BackoffPolicy.EXPONENTIAL, workSpec.backoffPolicy)

        // Attempt 1 runs at once; the delay before attempt n is initial * 2^(n-2), capped by
        // WorkManager. The worker gives up at MAX_RUN_ATTEMPT_COUNT, so this is the longest a
        // recording can stay on the device before it is uploaded or deleted.
        val totalRetryDelayMillis = (2..HistoryUploadWorker.MAX_RUN_ATTEMPT_COUNT).sumOf { attempt ->
            minOf(
                workSpec.backoffDelayDuration shl (attempt - 2),
                WorkRequest.MAX_BACKOFF_MILLIS,
            )
        }

        assertTrue(
            "retries span ${TimeUnit.MILLISECONDS.toHours(totalRetryDelayMillis)} h",
            totalRetryDelayMillis < TimeUnit.HOURS.toMillis(24),
        )
    }

    @Test
    fun `uploadHistory stores the token used at scheduling time`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        val request = slot<OneTimeWorkRequest>()

        HistoryUploadWorker.uploadHistory(mockedContext, stubCopilotSession)

        verify(exactly = 1) { workManager.enqueueUniqueWork(any(), any(), capture(request)) }
        assertEquals(accessToken, request.captured.workSpec.input.getString("access_token"))
    }

    @Test
    fun `uploadHistory still schedules work when the token is not set yet`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        every { MapboxOptions.accessToken } returns ""

        HistoryUploadWorker.uploadHistory(mockedContext, stubCopilotSession)

        verify(exactly = 1) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    private fun verifyRecordingAndSessionFilesDeleted() {
        val deletedFiles = mutableListOf<File>()
        verify(exactly = 2) { delete(capture(deletedFiles)) }
        assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("pbf.gz") })
        assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("metadata.json") })
        verify(exactly = 1) { cancelScheduledUploading(any(), any()) }
    }

    private fun uploadUrl(token: String) = "https://events.mapbox.com/attachments/v1?access_token=$token"

    private suspend fun uploadUrlWith(
        currentToken: String,
        scheduledToken: String?,
        legacyUrl: String?,
    ): String {
        val uploadOptions = slot<UploadOptions>()
        givenUploadServiceAnswer(
            uploadStatus(TransferState.FINISHED, httpResult = httpResult(204)),
            uploadOptionsCapture = uploadOptions,
        )
        every { MapboxOptions.accessToken } returns currentToken

        HistoryUploadWorker(
            mockedContext,
            workerParams(stubCopilotSession, scheduledToken = scheduledToken, legacyUrl = legacyUrl),
        ).doWork()

        return uploadOptions.captured.url
    }

    private fun workerParams(
        session: CopilotSession,
        runAttemptCount: Int = 1,
        scheduledToken: String? = accessToken,
        legacyUrl: String? = null,
    ): WorkerParameters {
        return mockk<WorkerParameters>(relaxed = true) {
            every { inputData } returns Data.Builder()
                .putCopilotSession(session)
                .putString("access_token", scheduledToken)
                .putString("upload_url", legacyUrl)
                .putString("upload_session_id", uploadSessionId)
                .build()
            every { this@mockk.runAttemptCount } returns runAttemptCount
        }
    }

    private fun uploadStatus(
        state: TransferState,
        error: TransferError? = null,
        httpResult: Expected<HttpRequestError, HttpResponseData>? = null,
    ): UploadStatus = mockk<UploadStatus>(relaxed = true) {
        every { this@mockk.state } returns state
        every { this@mockk.error } returns error
        every { this@mockk.httpResult } returns httpResult
    }

    private fun givenUploadServiceAnswer(
        uploadStatus: UploadStatus,
        uploadOptionsCapture: CapturingSlot<UploadOptions> = slot<UploadOptions>(),
    ) {
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        every {
            mockedUploadServiceInterface.upload(
                capture(uploadOptionsCapture),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(uploadStatus)
            1L
        }
    }

    private fun httpResult(httpCode: Int) = createValue<HttpRequestError, HttpResponseData>(
        mockk<HttpResponseData>(relaxed = true) {
            every { code } returns httpCode
        },
    )

    private fun mockHttpService(): HttpServiceInterface {
        val mockedUploadServiceInterface = mockk<HttpServiceInterface>(relaxed = true)
        mockkObject(HttpServiceProvider)
        every {
            HttpServiceProvider.getInstance()
        } returns mockedUploadServiceInterface
        return mockedUploadServiceInterface
    }

    private fun UploadOptions.getAttachments(): List<AttachmentMetadata> {
        val listType = TypeToken.getParameterized(List::class.java, AttachmentMetadata::class.java)
        return Gson().fromJson(metadata, listType.type)
    }
}
