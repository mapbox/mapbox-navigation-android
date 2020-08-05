package com.mapbox.navigation.ui.instruction

import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class GuidanceViewImageProviderTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val callback: GuidanceViewImageProvider.OnGuidanceImageDownload = mockk(relaxed = true)
    private val guidanceViewImageProvider: GuidanceViewImageProvider = GuidanceViewImageProvider()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
    }

    @Test
    fun `when banner view is null`() {
        val bannerInstructions: BannerInstructions = mockk()
        every { bannerInstructions.view() } returns null

        guidanceViewImageProvider.renderGuidanceView(bannerInstructions, callback)

        verify(exactly = 1) { callback.onNoGuidanceImageUrl() }
    }

    @Test
    fun `when banner component list is null`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns null

        guidanceViewImageProvider.renderGuidanceView(bannerInstructions, callback)

        verify(exactly = 1) { callback.onNoGuidanceImageUrl() }
    }

    @Test
    fun `when guidance view url is null`() {
        val captureData = slot<String>()
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentList =
            mutableListOf<BannerComponents>(BannerComponentsFaker.bannerComponentsWithNullGuidanceUrl())
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentList

        guidanceViewImageProvider.renderGuidanceView(bannerInstructions, callback)

        verify(exactly = 1) { callback.onFailure(capture(captureData)) }
        assertEquals("Guidance View Image URL is null", captureData.captured)
    }

    @Test
    fun `when url has invalid access token`() = coroutineRule.runBlockingTest {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.start()

        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentList =
            mutableListOf<BannerComponents>(BannerComponentsFaker.bannerComponentsWithGuidanceUrlNoAccessToken(mockWebServer.url("guidance-views/v1/1580515200/jct/CA075101?arrow_ids=CA07510E&access_token=null").toString()))
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentList

        val latch = CountDownLatch(1)
        val bitmapRef = AtomicReference<Bitmap>()
        val failureRef = AtomicBoolean()
        val failureMessageRef = AtomicReference<String?>()
        val aCallback = provideACallback(latch, bitmapRef, failureRef, failureMessageRef)

        val job = launch {
            guidanceViewImageProvider.renderGuidanceView(bannerInstructions, aCallback)
        }
        job.join()

        latch.await()
        assertTrue(failureRef.get())
        assertEquals("Client Error", failureMessageRef.get())
        mockWebServer.shutdown()
    }

    private fun provideACallback(latch: CountDownLatch, bitmapRef: AtomicReference<Bitmap>, failureRef: AtomicBoolean, failureMessageRef: AtomicReference<String?>): GuidanceViewImageProvider.OnGuidanceImageDownload {
        return object : GuidanceViewImageProvider.OnGuidanceImageDownload {
            override fun onGuidanceImageReady(bitmap: Bitmap) {
                bitmapRef.set(bitmap)
                failureRef.set(true)
                latch.countDown()
            }

            override fun onNoGuidanceImageUrl() {
            }

            override fun onFailure(message: String?) {
                failureRef.set(true)
                failureMessageRef.set(message)
                latch.countDown()
            }
        }
    }
}
