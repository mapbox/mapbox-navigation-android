package com.mapbox.navigation.ui.instruction

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.instruction.BannerComponentsFaker.bannerComponentsWithGuidanceUrlNoAccessToken
import com.mapbox.navigation.ui.instruction.BannerComponentsFaker.bannerComponentsWithNullGuidanceUrl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

@ExperimentalCoroutinesApi
class GuidanceViewImageProviderTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val callback: GuidanceViewImageProvider.OnGuidanceImageDownload = mockk(relaxed = true)
    private val guidanceViewImageProvider: GuidanceViewImageProvider = GuidanceViewImageProvider()
    private val bannerInstructions: BannerInstructions = mockk()
    private val bannerView: BannerView = mockk()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
        every { bannerInstructions.view() } returns bannerView
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
        every { bannerView.components() } returns null

        guidanceViewImageProvider.renderGuidanceView(bannerInstructions, callback)

        verify(exactly = 1) { callback.onNoGuidanceImageUrl() }
    }

    @Test
    fun `when guidance view url is null`() {
        val messageSlot = slot<String>()
        every {
            bannerView.components()
        } returns mutableListOf(bannerComponentsWithNullGuidanceUrl())

        guidanceViewImageProvider.renderGuidanceView(bannerInstructions, callback)

        verify(exactly = 1) { callback.onFailure(capture(messageSlot)) }
        assertEquals("Guidance View Image URL is null", messageSlot.captured)
    }

    @Test
    fun `when url has invalid access token`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.start()
        every { bannerView.components() } returns mutableListOf(
            bannerComponentsWithGuidanceUrlNoAccessToken(
                mockWebServer.url(URL_WITH_NULL_TOKEN).toString()
            )
        )
        val messageSlot = slot<String>()
        val latch = CountDownLatch(1)
        every { callback.onFailure(capture(messageSlot)) } answers { latch.countDown() }

        guidanceViewImageProvider.renderGuidanceView(bannerInstructions, callback)

        latch.await()
        verify(exactly = 1) { callback.onFailure(any()) }
        assertEquals("Client Error", messageSlot.captured)
        mockWebServer.shutdown()
    }

    private companion object {
        private const val URL_WITH_NULL_TOKEN =
            "guidance-views/v1/1580515200/jct/CA075101?arrow_ids=CA07510E&access_token=null"
    }
}
