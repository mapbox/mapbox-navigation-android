package com.mapbox.navigation.copilot

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceInterface
import com.mapbox.common.TransferError
import com.mapbox.common.TransferErrorCode
import com.mapbox.common.TransferState
import com.mapbox.common.UploadOptions
import com.mapbox.common.UploadStatus
import com.mapbox.common.UploadStatusCallback
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.delete
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.rename
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.internal.PushStatus
import com.mapbox.navigation.copilot.internal.PushStatusObserver
import com.mapbox.navigation.copilot.work.HistoryUploadWorker
import com.mapbox.navigation.copilot.work.HistoryUploadWorker.Companion.putCopilotSession
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
    private val uploadUrl = "https://example.com/uploads"
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
            }
        }
        every { delete(any()) } returns false

        mockedContext = mockk<Context>(relaxed = true)
        mockedUploadServiceInterface = mockHttpService()

        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
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
        assertEquals("uploadOptions.url", uploadUrl, uploadOptions.url)
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
            assertNotNull(deletedFiles.first { it.name.endsWith("pbf.gz") })
            assertNotNull(deletedFiles.first { it.name.endsWith("metadata.json") })
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
            assertNotNull(deletedFiles.first { it.name.endsWith("pbf.gz") })
            assertNotNull(deletedFiles.first { it.name.endsWith("metadata.json") })
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

    private fun workerParams(
        session: CopilotSession,
        runAttemptCount: Int = 1,
    ): WorkerParameters {
        return mockk<WorkerParameters>(relaxed = true) {
            every { inputData } returns Data.Builder()
                .putCopilotSession(session)
                .putString("upload_url", uploadUrl)
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
