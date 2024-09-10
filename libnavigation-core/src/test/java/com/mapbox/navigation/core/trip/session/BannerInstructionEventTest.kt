package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.BannerInstructions
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BannerInstructionEventTest {

    @Test
    fun `isOccurring update current non-null bannerInstructions`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val anyBannerInstructions: BannerInstructions = mockk()

        val isOccurring = bannerInstructionEvent.isOccurring(anyBannerInstructions, 0)

        assertTrue(isOccurring)
        assertEquals(anyBannerInstructions, bannerInstructionEvent.bannerInstructions)
    }

    @Test
    fun `isOccurring update current null bannerInstructions`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val nullBannerInstructions: BannerInstructions? = null

        val isOccurring = bannerInstructionEvent.isOccurring(nullBannerInstructions, 0)

        assertFalse(isOccurring)
        assertNull(bannerInstructionEvent.bannerInstructions)
    }

    @Test
    fun `isOccurring update current latestBannerInstructions if instructions not null`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val anyBannerInstructions: BannerInstructions = mockk()

        val isOccurring = bannerInstructionEvent.isOccurring(anyBannerInstructions, 0)

        assertTrue(isOccurring)
        assertEquals(anyBannerInstructions, bannerInstructionEvent.latestBannerInstructions)
        assertEquals(0, bannerInstructionEvent.latestInstructionIndex)
    }

    @Test
    fun `isOccurring doesn't update current null latestBannerInstructions if instructions null`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val nullBannerInstructions: BannerInstructions? = null

        val isOccurring = bannerInstructionEvent.isOccurring(nullBannerInstructions, null)

        assertFalse(isOccurring)
        assertNull(bannerInstructionEvent.latestBannerInstructions)
        assertNull(bannerInstructionEvent.latestInstructionIndex)
    }

    @Test
    fun `isOccurring doesn't update current latestBannerInstructions if instructions null`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val nonNullBannerInstructions: BannerInstructions = mockk()
        val nullBannerInstructions: BannerInstructions? = null

        val isOccurringNonNullBannerInstructions =
            bannerInstructionEvent.isOccurring(nonNullBannerInstructions, 1)

        val isOccurringNullBannerInstructions =
            bannerInstructionEvent.isOccurring(nullBannerInstructions, null)

        assertTrue(isOccurringNonNullBannerInstructions)
        assertFalse(isOccurringNullBannerInstructions)
        assertEquals(nonNullBannerInstructions, bannerInstructionEvent.latestBannerInstructions)
        assertEquals(1, bannerInstructionEvent.latestInstructionIndex)
    }

    @Test
    fun `isOccurring doesn't update current latestBannerInstructions if same instructions`() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val nonNullBannerInstructions: BannerInstructions = mockk()

        val isOccurringNonNullBannerInstructions =
            bannerInstructionEvent.isOccurring(nonNullBannerInstructions, 0)
        val isOccurringNullBannerInstructions =
            bannerInstructionEvent.isOccurring(nonNullBannerInstructions, 0)

        assertTrue(isOccurringNonNullBannerInstructions)
        assertFalse(isOccurringNullBannerInstructions)
        assertEquals(nonNullBannerInstructions, bannerInstructionEvent.latestBannerInstructions)
        assertEquals(0, bannerInstructionEvent.latestInstructionIndex)
    }

    @Test
    fun invalidateLatestBannerInstructions() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val anyBannerInstructions: BannerInstructions = mockk()
        bannerInstructionEvent.isOccurring(anyBannerInstructions, 0)

        bannerInstructionEvent
            .invalidateLatestBannerInstructions(bannerInstructionEvent.latestInstructionWrapper)

        assertNull(bannerInstructionEvent.latestBannerInstructions)
        assertNull(bannerInstructionEvent.latestInstructionIndex)
        assertNull(bannerInstructionEvent.latestInstructionWrapper)
    }

    @Test
    fun invalidateNonExistingLatestBannerInstructions() {
        val bannerInstructionEvent = BannerInstructionEvent()
        val anyBannerInstructions: BannerInstructions = mockk()
        bannerInstructionEvent.isOccurring(anyBannerInstructions, 0)

        bannerInstructionEvent
            .invalidateLatestBannerInstructions(mockk())

        assertEquals(
            BannerInstructionEvent.LatestInstructionWrapper(0, anyBannerInstructions),
            bannerInstructionEvent.latestInstructionWrapper,
        )
        assertEquals(
            anyBannerInstructions,
            bannerInstructionEvent.latestBannerInstructions,
        )
        assertEquals(0, bannerInstructionEvent.latestInstructionIndex)
    }
}
