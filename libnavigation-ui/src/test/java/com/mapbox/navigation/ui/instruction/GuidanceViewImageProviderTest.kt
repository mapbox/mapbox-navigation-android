package com.mapbox.navigation.ui.instruction

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class GuidanceViewImageProviderTest {

    private val callback: GuidanceViewImageProvider.OnGuidanceImageDownload = mockk(relaxed = true)
    private val guidanceViewImageProvider: GuidanceViewImageProvider = GuidanceViewImageProvider()

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
    fun `when url has invalid access token`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.start()

        val captureData = slot<String>()
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponentList =
            mutableListOf<BannerComponents>(BannerComponentsFaker.bannerComponentsWithGuidanceUrlNoAccessToken(mockWebServer.url("guidance-views/v1/1580515200/jct/CA075101?arrow_ids=CA07510E&access_token=null").toString()))
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns bannerComponentList

        guidanceViewImageProvider.renderGuidanceView(bannerInstructions, callback)

        verify(exactly = 1) { callback.onFailure(capture(captureData)) }
        assertEquals("Client Error", captureData.captured)
        mockWebServer.shutdown()
    }
}
