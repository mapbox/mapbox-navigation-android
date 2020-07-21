package com.mapbox.navigation.ui.instruction

import android.content.Context
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class GuidanceViewImageProviderTest {

    private val context: Context = mockk(relaxed = true)
    private val callback: GuidanceViewImageProvider.OnGuidanceImageDownload = mockk(relaxed = true)
    private val guidanceViewImageProvider: GuidanceViewImageProvider = GuidanceViewImageProvider()

    @Test
    fun `when banner view is null`() {
        val bannerInstructions: BannerInstructions = mockk()
        every { bannerInstructions.view() } returns null
        guidanceViewImageProvider.renderGuidanceView(bannerInstructions, context, callback)
        verify(exactly = 1) { callback.onNoGuidanceImageUrl() }
    }

    @Test
    fun `when banner component list is null`() {
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        every { bannerInstructions.view() } returns bannerView
        every { bannerView.components() } returns null

        guidanceViewImageProvider.renderGuidanceView(bannerInstructions, context, callback)
        verify(exactly = 1) { callback.onNoGuidanceImageUrl() }
    }
}
