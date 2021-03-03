package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BannerInstructionEventTest {

    @Test
    fun `isOccurring update current non-null bannerInstructions`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val mockedRouteProgress: RouteProgress = mockk()
        val anyBannerInstructions: BannerInstructions = mockk()
        every { mockedRouteProgress.bannerInstructions } returns anyBannerInstructions

        bannerInstructionEvent.isOccurring(mockedRouteProgress)

        assertEquals(anyBannerInstructions, bannerInstructionEvent.bannerInstructions)
    }

    @Test
    fun `isOccurring update current null bannerInstructions`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val mockedRouteProgress: RouteProgress = mockk()
        val nullBannerInstructions: BannerInstructions? = null
        every { mockedRouteProgress.bannerInstructions } returns nullBannerInstructions

        bannerInstructionEvent.isOccurring(mockedRouteProgress)

        assertNull(bannerInstructionEvent.bannerInstructions)
    }

    @Test
    fun `isOccurring update current latestBannerInstructions if instructions not null`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val mockedRouteProgress: RouteProgress = mockk()
        val anyBannerInstructions: BannerInstructions = mockk()
        every { mockedRouteProgress.bannerInstructions } returns anyBannerInstructions

        bannerInstructionEvent.isOccurring(mockedRouteProgress)

        assertEquals(anyBannerInstructions, bannerInstructionEvent.latestBannerInstructions)
    }

    @Test
    fun `isOccurring doesn't update current null latestBannerInstructions if instructions null`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val mockedRouteProgress: RouteProgress = mockk()
        val nullBannerInstructions: BannerInstructions? = null
        every { mockedRouteProgress.bannerInstructions } returns nullBannerInstructions

        bannerInstructionEvent.isOccurring(mockedRouteProgress)

        assertNull(bannerInstructionEvent.latestBannerInstructions)
    }

    @Test
    fun `isOccurring doesn't update current latestBannerInstructions if instructions null`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val mockedNonNullBannerInstructionsRouteProgress: RouteProgress = mockk()
        val nonNullBannerInstructions: BannerInstructions = mockk()
        val nullBannerInstructions: BannerInstructions? = null
        every {
            mockedNonNullBannerInstructionsRouteProgress.bannerInstructions
        } returns nonNullBannerInstructions andThen nullBannerInstructions

        bannerInstructionEvent.isOccurring(mockedNonNullBannerInstructionsRouteProgress)
        bannerInstructionEvent.isOccurring(mockedNonNullBannerInstructionsRouteProgress)

        assertEquals(nonNullBannerInstructions, bannerInstructionEvent.latestBannerInstructions)
    }

    @Test
    fun invalidateLatestBannerInstructions() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val mockedRouteProgress: RouteProgress = mockk()
        val anyBannerInstructions: BannerInstructions = mockk()
        every { mockedRouteProgress.bannerInstructions } returns anyBannerInstructions
        bannerInstructionEvent.isOccurring(mockedRouteProgress)

        bannerInstructionEvent.invalidateLatestBannerInstructions()

        assertNull(bannerInstructionEvent.latestBannerInstructions)
    }
}
