package com.mapbox.navigation.ui.androidauto.feedback.core

import android.graphics.Bitmap
import com.mapbox.maps.MapSurface
import com.mapbox.maps.MapView
import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions
import com.mapbox.navigation.core.telemetry.events.FeedbackHelper
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class CarFeedbackScreenshotTest {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val options: BitmapEncodeOptions = mockk()
    private val bitmap: Bitmap = mockk(relaxUnitFun = true)
    private var snapshotListener: MapView.OnSnapshotReady? = null
    private val mapSurface: MapSurface = mockk {
        every { snapshot(any<MapView.OnSnapshotReady>()) } answers {
            snapshotListener = firstArg()
        }
    }

    @Before
    fun setup() {
        mockkObject(FeedbackHelper)
        mockkStatic(FeedbackHelper::class)
        every { FeedbackHelper.encodeScreenshot(bitmap, options) } returns ENCODED
    }

    @Test
    fun `snapshot is encoded and recycled`() = runTest {
        val result = capture()

        snapshotListener!!.onSnapshotReady(bitmap)
        runCurrent()

        assertEquals(ENCODED, result.await())
        verify(exactly = 1) { bitmap.recycle() }
    }

    @Test
    fun `missing snapshot gives no screenshot`() = runTest {
        val result = capture()

        snapshotListener!!.onSnapshotReady(null)
        runCurrent()

        assertNull(result.await())
        verify(exactly = 0) { FeedbackHelper.encodeScreenshot(any(), any()) }
    }

    @Test
    fun `snapshot that never arrives gives no screenshot after the timeout`() = runTest {
        val result = capture()

        advanceTimeBy(TIMEOUT_MS + 1)

        assertNull(result.await())
        verify(exactly = 0) { FeedbackHelper.encodeScreenshot(any(), any()) }
    }

    @Test
    fun `snapshot that arrives after the timeout is recycled`() = runTest {
        val result = capture()
        advanceTimeBy(TIMEOUT_MS + 1)
        assertNull(result.await())

        snapshotListener!!.onSnapshotReady(bitmap)

        verify(exactly = 1) { bitmap.recycle() }
        verify(exactly = 0) { FeedbackHelper.encodeScreenshot(any(), any()) }
    }

    @Test
    fun `encoding failure gives no screenshot and still recycles`() = runTest {
        every { FeedbackHelper.encodeScreenshot(bitmap, options) } throws
            IllegalStateException("encoding failed")
        val result = capture()

        snapshotListener!!.onSnapshotReady(bitmap)
        runCurrent()

        assertNull(result.await())
        verify(exactly = 1) { bitmap.recycle() }
    }

    @Test
    fun `snapshot that arrives after the caller is cancelled is recycled`() = runTest {
        val result = capture()

        result.cancel()
        snapshotListener!!.onSnapshotReady(bitmap)
        runCurrent()

        verify(exactly = 1) { bitmap.recycle() }
        verify(exactly = 0) { FeedbackHelper.encodeScreenshot(any(), any()) }
    }

    @Test
    fun `snapshot that arrives while the caller is being cancelled is recycled`() = runTest {
        val result = capture()
        snapshotListener!!.onSnapshotReady(bitmap)

        result.cancel()
        runCurrent()

        verify(exactly = 1) { bitmap.recycle() }
        verify(exactly = 0) { FeedbackHelper.encodeScreenshot(any(), any()) }
    }

    @Test
    fun `snapshot is recycled when the caller is cancelled before encoding starts`() = runTest {
        val encodeDispatcher = QueueingDispatcher()
        val result = capture(encodeDispatcher)
        snapshotListener!!.onSnapshotReady(bitmap)
        runCurrent()

        result.cancel()
        encodeDispatcher.runQueued()
        runCurrent()

        assertTrue(result.isCancelled)
        verify(exactly = 1) { bitmap.recycle() }
        verify(exactly = 0) { FeedbackHelper.encodeScreenshot(any(), any()) }
    }

    @Test
    fun `snapshot is requested before the caller resumes`() = runTest {
        capture()

        assertNotNull(snapshotListener)
    }

    private fun TestScope.capture(
        encodeDispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
    ) = async(start = CoroutineStart.UNDISPATCHED) {
        mapSurface.captureEncodedScreenshot(
            options,
            timeoutMs = TIMEOUT_MS,
            encodeDispatcher = encodeDispatcher,
        )
    }

    // Holds dispatched tasks until the test runs them.
    private class QueueingDispatcher : CoroutineDispatcher() {
        private val queued = mutableListOf<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            queued += block
        }

        fun runQueued() {
            while (queued.isNotEmpty()) queued.removeAt(0).run()
        }
    }

    private companion object {
        private const val ENCODED = "encoded"
        private const val TIMEOUT_MS = 1000L
    }
}
