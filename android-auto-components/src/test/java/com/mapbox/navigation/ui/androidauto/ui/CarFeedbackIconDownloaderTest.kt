package com.mapbox.navigation.ui.androidauto.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.util.DisplayMetrics
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.Target
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackIcon
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackIconDownloader
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
@SuppressLint("CheckResult")
class CarFeedbackIconDownloaderTest {

    @get:Rule val coroutineRule = MainCoroutineRule()
    private val carContext = mockk<CarContext> {
        every { resources } returns mockk {
            every { displayMetrics } returns DisplayMetrics().apply { density = DENSITY }
        }
    }
    private val screen = mockk<Screen>(relaxUnitFun = true) {
        every { carContext } returns this@CarFeedbackIconDownloaderTest.carContext
        every { lifecycle } returns LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.STARTED
        }
    }
    private val downloader = CarFeedbackIconDownloader(screen)
    private val uri = mockk<Uri>()
    private val requestBuilder = mockk<RequestBuilder<Bitmap>> {
        every { load(uri) } returns this
        every { override(any<Int>()) } returns this
    }
    private val requestManager = mockk<RequestManager>(relaxUnitFun = true) {
        every { asBitmap() } returns requestBuilder
    }
    private val carIcon = mockk<CarIcon>()
    private val localIcon = CarFeedbackIcon.Local(carIcon)
    private val remoteIcon = CarFeedbackIcon.Remote(uri)
    private val downloadedBitmap = mockk<Bitmap> {
        every { width } returns WIDTH
        every { height } returns HEIGHT
    }

    @Before
    fun `set up`() {
        mockkStatic(Glide::class)
        every { Glide.with(carContext) } returns requestManager
    }

    @After
    fun `tear down`() {
        unmockkStatic(Glide::class)
    }

    @Test
    fun `local icon is returned without downloading`() {
        val actualIcon = downloader.getOrDownload(localIcon)

        assertEquals(carIcon, actualIcon)
        verify(exactly = 0) { requestManager.asBitmap() }
        verify(exactly = 0) { screen.invalidate() }
    }

    @Test
    fun `remote icon starts downloading if retrieved for the first time`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(SHORT_DELAY)

            val actualIcon = downloader.getOrDownload(remoteIcon)

            assertNull(actualIcon)
            verify(exactly = 1) { requestBuilder.load(uri) }
            verify(exactly = 0) { screen.invalidate() }
        }
    }

    @Test
    fun `null is returned, while icon is being downloaded`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(LONG_DELAY)

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(SHORT_DELAY)
            val actualIcon = downloader.getOrDownload(remoteIcon)

            assertNull(actualIcon)
            verify(exactly = 1) { requestBuilder.load(uri) }
            verify(exactly = 0) { screen.invalidate() }
        }
    }

    @Test
    fun `screen is invalidated, after icon is downloaded`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(SHORT_DELAY)

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)

            verify(exactly = 1) { requestBuilder.load(uri) }
            verify(exactly = 1) { screen.invalidate() }
        }
    }

    @Test
    fun `actual icon is returned, after icon is downloaded`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(SHORT_DELAY)

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)
            val actualIcon = downloader.getOrDownload(remoteIcon)

            val expectedIcon = IconCompat.createWithBitmap(downloadedBitmap)
            assertEquals(CarIcon.Builder(expectedIcon).build(), actualIcon)
            verify(exactly = 1) { requestBuilder.load(uri) }
            verify(exactly = 1) { screen.invalidate() }
        }
    }

    @Test
    fun `remote icon is decoded at the grid icon size`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(SHORT_DELAY)

            downloader.getOrDownload(remoteIcon)

            verify(exactly = 1) { requestBuilder.override(GRID_ICON_SIZE_PX) }
        }
    }

    @Test
    fun `cached icon does not start another download`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(SHORT_DELAY)

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)
            downloader.getOrDownload(remoteIcon)
            downloader.getOrDownload(remoteIcon)

            verify(exactly = 1) { requestBuilder.load(uri) }
        }
    }

    @Test
    fun `download that took too much time is retried`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(LONG_DELAY)

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)
            val actualIcon = downloader.getOrDownload(remoteIcon)

            assertNull(actualIcon)
            verify(exactly = 2) { requestBuilder.load(uri) }
            verify(exactly = 1) { screen.invalidate() }
        }
    }

    @Test
    fun `failed download is retried`() {
        coroutineRule.runBlockingTest {
            mockFailedRequest()

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)
            val actualIcon = downloader.getOrDownload(remoteIcon)

            assertNull(actualIcon)
            verify(exactly = 2) { requestBuilder.load(uri) }
            verify(exactly = 1) { screen.invalidate() }
        }
    }

    @Test
    fun `error icon is returned once every attempt failed`() {
        coroutineRule.runBlockingTest {
            mockFailedRequest()

            repeat(MAX_ATTEMPTS) {
                downloader.getOrDownload(remoteIcon)
                testScheduler.advanceTimeBy(LONG_DELAY)
            }
            val actualIcon = downloader.getOrDownload(remoteIcon)

            assertEquals(CarIcon.ERROR, actualIcon)
            verify(exactly = MAX_ATTEMPTS) { requestBuilder.load(uri) }
            verify(exactly = MAX_ATTEMPTS) { screen.invalidate() }
        }
    }

    @Test
    fun `download that took too much time is cleared from glide`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(LONG_DELAY)

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)

            verify(exactly = 1) { requestManager.clear(any<Target<Bitmap>>()) }
        }
    }

    @Test
    fun `late result after a timeout is ignored`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(LONG_DELAY)

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY + SHORT_DELAY)

            verify(exactly = 1) { screen.invalidate() }
        }
    }

    @Test
    fun `failed request restarted by glide does not resume the download twice`() {
        coroutineRule.runBlockingTest {
            // Glide restarts failed requests into the same target when connectivity returns.
            mockRequest(SHORT_DELAY) {
                onLoadFailed(null)
                onLoadFailed(null)
                onResourceReady(downloadedBitmap, null)
            }

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)
            val actualIcon = downloader.getOrDownload(remoteIcon)

            assertNull(actualIcon)
            verify(exactly = 1) { screen.invalidate() }
        }
    }

    private fun mockSuccessfulRequest(time: Long) {
        mockRequest(time) { onResourceReady(downloadedBitmap, null) }
    }

    private fun mockFailedRequest() {
        mockRequest(SHORT_DELAY) { onLoadFailed(null) }
    }

    private fun mockRequest(time: Long, block: Target<Bitmap>.() -> Unit) {
        every { requestBuilder.into(any<Target<Bitmap>>()) } answers {
            val target = firstArg<Target<Bitmap>>()
            coroutineRule.coroutineScope.launch {
                delay(time)
                target.block()
            }
            target
        }
    }

    private companion object {
        private const val WIDTH = 16
        private const val HEIGHT = 10
        private const val DENSITY = 2f
        private const val GRID_ICON_SIZE_PX = 128
        private const val MAX_ATTEMPTS = 3
        private const val SHORT_DELAY = 2000L
        private const val LONG_DELAY = 4000L
    }
}
