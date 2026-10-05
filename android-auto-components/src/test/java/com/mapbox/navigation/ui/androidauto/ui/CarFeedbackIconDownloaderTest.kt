package com.mapbox.navigation.ui.androidauto.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
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
    private val carContext = mockk<CarContext>()
    private val screen = mockk<Screen>(relaxUnitFun = true) {
        every { carContext } returns this@CarFeedbackIconDownloaderTest.carContext
        every { lifecycle } returns LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.STARTED
        }
    }
    private val downloader = CarFeedbackIconDownloader(screen)
    private val uri = mockk<Uri>()
    private val requestBuilder = mockk<RequestBuilder<Drawable>>()
    private val requestManager = mockk<RequestManager>(relaxUnitFun = true) {
        every { load(uri) } returns requestBuilder
    }
    private val carIcon = mockk<CarIcon>()
    private val localIcon = CarFeedbackIcon.Local(carIcon)
    private val remoteIcon = CarFeedbackIcon.Remote(uri)
    private val downloadedBitmap = mockk<Bitmap> {
        every { width } returns WIDTH
        every { height } returns HEIGHT
    }
    private val downloadedDrawable = mockk<BitmapDrawable> {
        every { intrinsicWidth } returns WIDTH
        every { intrinsicHeight } returns HEIGHT
        every { bitmap } returns downloadedBitmap
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
        verify(exactly = 0) { requestManager.load(any<Uri>()) }
        verify(exactly = 0) { screen.invalidate() }
    }

    @Test
    fun `remote icon starts downloading if retrieved for the first time`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(SHORT_DELAY)

            val actualIcon = downloader.getOrDownload(remoteIcon)

            assertNull(actualIcon)
            verify(exactly = 1) { requestManager.load(uri) }
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
            verify(exactly = 1) { requestManager.load(uri) }
            verify(exactly = 0) { screen.invalidate() }
        }
    }

    @Test
    fun `screen is invalidated, after icon is downloaded`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(SHORT_DELAY)

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)

            verify(exactly = 1) { requestManager.load(uri) }
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
            verify(exactly = 1) { requestManager.load(uri) }
            verify(exactly = 1) { screen.invalidate() }
        }
    }

    @Test
    fun `error icon is returned, if download took too much time`() {
        coroutineRule.runBlockingTest {
            mockSuccessfulRequest(LONG_DELAY)

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)
            val actualIcon = downloader.getOrDownload(remoteIcon)

            assertEquals(CarIcon.ERROR, actualIcon)
            verify(exactly = 1) { requestManager.load(uri) }
            verify(exactly = 1) { screen.invalidate() }
        }
    }

    @Test
    fun `error icon is returned, if download failed`() {
        coroutineRule.runBlockingTest {
            mockFailedRequest()

            downloader.getOrDownload(remoteIcon)
            testScheduler.advanceTimeBy(LONG_DELAY)
            val actualIcon = downloader.getOrDownload(remoteIcon)

            assertEquals(CarIcon.ERROR, actualIcon)
            verify(exactly = 1) { requestManager.load(uri) }
            verify(exactly = 1) { screen.invalidate() }
        }
    }

    private fun mockSuccessfulRequest(time: Long) {
        mockRequest(time) { onResourceReady(downloadedDrawable, null) }
    }

    private fun mockFailedRequest() {
        mockRequest(SHORT_DELAY) { onLoadFailed(null) }
    }

    private fun mockRequest(time: Long, block: Target<Drawable>.() -> Unit) {
        every { requestBuilder.into(any<Target<Drawable>>()) } answers {
            val target = firstArg<Target<Drawable>>()
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
        private const val SHORT_DELAY = 2000L
        private const val LONG_DELAY = 4000L
    }
}
