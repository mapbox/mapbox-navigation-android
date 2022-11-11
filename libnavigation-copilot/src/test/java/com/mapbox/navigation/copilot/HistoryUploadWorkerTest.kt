package com.mapbox.navigation.copilot

import android.content.Context
import androidx.work.ListenableWorker.Result.Failure
import androidx.work.ListenableWorker.Result.Retry
import androidx.work.ListenableWorker.Result.Success
import androidx.work.WorkerParameters
import com.mapbox.common.UploadError
import com.mapbox.common.UploadOptions
import com.mapbox.common.UploadServiceInterface
import com.mapbox.common.UploadState
import com.mapbox.common.UploadStatus
import com.mapbox.common.UploadStatusCallback
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.copyToAndRemove
import com.mapbox.navigation.copilot.internal.PushStatus
import com.mapbox.navigation.copilot.internal.PushStatusObserver
import com.mapbox.navigation.utils.internal.logD
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class HistoryUploadWorkerTest {

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onPushStatusChanged Failed when FAILED`() = runBlocking {
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
        val mockedUploadServiceInterface = prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
        every { mockedUploadStatus.state } returns UploadState.FAILED
        val mockedUploadError = mockk<UploadError>(relaxed = true)
        every { mockedUploadError.code } returns mockk()
        every { mockedUploadStatus.error } returns mockedUploadError
        every { mockedUploadStatus.httpResult?.value } returns mockk()
        val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
        every {
            mockedUploadServiceInterface.upload(
                capture(mockedUploadOptions),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(mockedUploadStatus)
            1L
        }
        val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

        historyUploadWorker.doWork()

        verify(exactly = 1) {
            mockedPushStatusObserver.onPushStatusChanged(ofType<PushStatus.Failed>())
        }
    }

    @Test
    fun `onPushStatusChanged Success when FINISHED - 204`() = runBlocking {
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
        val mockedUploadServiceInterface = prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
        every { mockedUploadStatus.state } returns UploadState.FINISHED
        every { mockedUploadStatus.httpResult?.value?.code } returns 204L
        val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
        every {
            mockedUploadServiceInterface.upload(
                capture(mockedUploadOptions),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(mockedUploadStatus)
            1L
        }
        val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

        historyUploadWorker.doWork()

        verify(exactly = 1) {
            mockedPushStatusObserver.onPushStatusChanged(ofType<PushStatus.Success>())
        }
    }

    @Test
    fun `onPushStatusChanged Failed when FINISHED - 401`() = runBlocking {
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
        val mockedUploadServiceInterface = prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
        every { mockedUploadStatus.state } returns UploadState.FINISHED
        every { mockedUploadStatus.httpResult?.value?.code } returns 401
        val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
        every {
            mockedUploadServiceInterface.upload(
                capture(mockedUploadOptions),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(mockedUploadStatus)
            1L
        }
        val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

        historyUploadWorker.doWork()

        verify(exactly = 1) {
            mockedPushStatusObserver.onPushStatusChanged(ofType<PushStatus.Failed>())
        }
    }

    @Test
    fun `remove history file not called - FAILED`() = runBlocking {
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
        val mockedUploadServiceInterface = prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
        every { mockedUploadStatus.state } returns UploadState.FAILED
        val mockedUploadError = mockk<UploadError>(relaxed = true)
        every { mockedUploadError.code } returns mockk()
        every { mockedUploadStatus.error } returns mockedUploadError
        every { mockedUploadStatus.httpResult?.value } returns mockk()
        val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
        every {
            mockedUploadServiceInterface.upload(
                capture(mockedUploadOptions),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(mockedUploadStatus)
            1L
        }
        val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

        historyUploadWorker.doWork()

        verify(exactly = 0) {
            HistoryAttachmentsUtils.delete(any())
        }
    }

    @Test
    fun `Result retry - FAILED`() = runBlocking {
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
        val mockedUploadServiceInterface = prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
        every { mockedUploadStatus.state } returns UploadState.FAILED
        val mockedUploadError = mockk<UploadError>(relaxed = true)
        every { mockedUploadError.code } returns mockk()
        every { mockedUploadStatus.error } returns mockedUploadError
        every { mockedUploadStatus.httpResult?.value } returns mockk()
        val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
        every {
            mockedUploadServiceInterface.upload(
                capture(mockedUploadOptions),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(mockedUploadStatus)
            1L
        }
        val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

        val result = historyUploadWorker.doWork()

        assertTrue(result is Retry)
    }

    @Test
    fun `remove history file - FINISHED 204`() = runBlocking {
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
        val filePath = "path/to/history/file"
        every {
            mockedWorkerParams.inputData.getString("history_file_path")
        } returns filePath
        val mockedUploadServiceInterface = prepareUploadMockks()
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
        every { mockedUploadStatus.state } returns UploadState.FINISHED
        every { mockedUploadStatus.httpResult?.value?.code } returns 204L
        val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
        every {
            mockedUploadServiceInterface.upload(
                any(),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(mockedUploadStatus)
            1L
        }
        val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)
        val fileSlot = slot<File>()

        historyUploadWorker.doWork()

        verify(exactly = 1) {
            HistoryAttachmentsUtils.delete(capture(fileSlot))
        }
        assertEquals(filePath, fileSlot.captured.toString())
    }

    @Test
    fun `Result success - FINISHED 204`() = runBlocking {
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
        val filePath = "path/to/history/file"
        every {
            mockedWorkerParams.inputData.getString("history_file_path")
        } returns filePath
        val mockedUploadServiceInterface = prepareUploadMockks()
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
        every { mockedUploadStatus.state } returns UploadState.FINISHED
        every { mockedUploadStatus.httpResult?.value?.code } returns 204L
        val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
        every {
            mockedUploadServiceInterface.upload(
                any(),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(mockedUploadStatus)
            1L
        }
        val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

        val result = historyUploadWorker.doWork()

        assertTrue(result is Success)
    }

    @Test
    fun `remove history file not called - FINISHED 401`() = runBlocking {
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
        val mockedUploadServiceInterface = prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
        every { mockedUploadStatus.state } returns UploadState.FINISHED
        every { mockedUploadStatus.httpResult?.value?.code } returns 401
        val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
        every {
            mockedUploadServiceInterface.upload(
                capture(mockedUploadOptions),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(mockedUploadStatus)
            1L
        }
        val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

        historyUploadWorker.doWork()

        verify(exactly = 0) {
            HistoryAttachmentsUtils.delete(any())
        }
    }

    @Test
    fun `Result retry - FINISHED 401`() = runBlocking {
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
        val mockedUploadServiceInterface = prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        val mockedUploadStatusCallback = slot<UploadStatusCallback>()
        val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
        every { mockedUploadStatus.state } returns UploadState.FINISHED
        every { mockedUploadStatus.httpResult?.value?.code } returns 401
        val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
        MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
        every {
            mockedUploadServiceInterface.upload(
                capture(mockedUploadOptions),
                capture(mockedUploadStatusCallback),
            )
        } answers {
            mockedUploadStatusCallback.captured.run(mockedUploadStatus)
            1L
        }
        val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

        val result = historyUploadWorker.doWork()

        assertTrue(result is Retry)
    }

    @Test
    fun `remove history file - runAttemptCount greater or equal than MAX_RUN_ATTEMPT_COUNT`() =
        runBlocking {
            mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
            every { logD(msg = any(), category = any()) } just Runs
            val mockedContext = mockk<Context>(relaxed = true)
            val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
            every { mockedWorkerParams.runAttemptCount } returns 8
            val mockedUploadServiceInterface = prepareUploadMockks()
            val mockedUploadOptions = slot<UploadOptions>()
            val mockedUploadStatusCallback = slot<UploadStatusCallback>()
            val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
            every { mockedUploadStatus.state } returns UploadState.FINISHED
            every { mockedUploadStatus.httpResult?.value?.code } returns 401
            val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
            MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
            every {
                mockedUploadServiceInterface.upload(
                    capture(mockedUploadOptions),
                    capture(mockedUploadStatusCallback),
                )
            } answers {
                mockedUploadStatusCallback.captured.run(mockedUploadStatus)
                1L
            }
            val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

            historyUploadWorker.doWork()

            verify(exactly = 1) {
                HistoryAttachmentsUtils.delete(any())
            }
        }

    @Test
    fun `Result failure - runAttemptCount greater or equal than MAX_RUN_ATTEMPT_COUNT`() =
        runBlocking {
            mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
            every { logD(msg = any(), category = any()) } just Runs
            val mockedContext = mockk<Context>(relaxed = true)
            val mockedWorkerParams = mockk<WorkerParameters>(relaxed = true)
            every { mockedWorkerParams.runAttemptCount } returns 8
            val mockedUploadServiceInterface = prepareUploadMockks()
            val mockedUploadOptions = slot<UploadOptions>()
            val mockedUploadStatusCallback = slot<UploadStatusCallback>()
            val mockedUploadStatus = mockk<UploadStatus>(relaxed = true)
            every { mockedUploadStatus.state } returns UploadState.FINISHED
            every { mockedUploadStatus.httpResult?.value?.code } returns 401
            val mockedPushStatusObserver = mockk<PushStatusObserver>(relaxUnitFun = true)
            MapboxCopilot.pushStatusObservers.add(mockedPushStatusObserver)
            every {
                mockedUploadServiceInterface.upload(
                    capture(mockedUploadOptions),
                    capture(mockedUploadStatusCallback),
                )
            } answers {
                mockedUploadStatusCallback.captured.run(mockedUploadStatus)
                1L
            }
            val historyUploadWorker = HistoryUploadWorker(mockedContext, mockedWorkerParams)

            val result = historyUploadWorker.doWork()

            assertTrue(result is Failure)
        }

    private fun prepareUploadMockks(): UploadServiceInterface {
        mockkObject(HistoryAttachmentsUtils)
        val fileSlot = slot<File>()
        val mockedFile = mockk<File>(relaxed = true)
        every { copyToAndRemove(capture(fileSlot), any()) } answers {
            every { mockedFile.absolutePath } returns fileSlot.captured.toString()
            mockedFile
        }
        val mockedUploadServiceInterface = mockk<UploadServiceInterface>(relaxed = true)
        mockkObject(UploadServiceInterfaceFactory)
        every {
            UploadServiceInterfaceFactory.retrieveUploadServiceInterface()
        } returns mockedUploadServiceInterface
        return mockedUploadServiceInterface
    }
}
