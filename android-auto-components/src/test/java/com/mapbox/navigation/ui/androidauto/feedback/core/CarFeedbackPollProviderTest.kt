package com.mapbox.navigation.ui.androidauto.feedback.core

import android.content.Context
import androidx.car.app.CarContext
import androidx.car.app.model.CarIcon
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackIcon
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackPoll
import com.mapbox.navigation.ui.androidauto.internal.getCarIcon
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.search.analytics.FeedbackEvent.FeedbackReason
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("DEPRECATION")
class CarFeedbackPollProviderTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val carContext: CarContext = mockk {
        every { getString(any()) } answers { context.getString(firstArg()) }
    }
    private val provider = CarFeedbackPollProvider()

    @Before
    fun setup() {
        mockkStatic("com.mapbox.navigation.ui.androidauto.internal.IconUtilsKt")
        every { carContext.getCarIcon(any()) } returns CarFeedbackIcon.Local(CarIcon.APP_ICON)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `route preview poll only offers options that are sent as navigation feedback`() {
        val poll = provider.getRoutePreviewFeedbackPoll(carContext)

        assertEquals(
            listOf(
                FeedbackEvent.ROAD_CLOSED,
                FeedbackEvent.POSITIONING_ISSUE,
                FeedbackEvent.ROUTING_ERROR,
                FeedbackEvent.ROUTE_NOT_ALLOWED,
                FeedbackEvent.OTHER_ISSUE,
            ),
            poll.options.map { it.type },
        )
        assertTrue(poll.options.all { it.searchFeedbackReason == null })
    }

    @Test
    fun `route preview poll does not offer incorrect location`() {
        val incorrectLocation = context.getString(R.string.car_feedback_search_incorrect_location)

        val poll = provider.getRoutePreviewFeedbackPoll(carContext)

        assertFalse(poll.options.any { it.title == incorrectLocation })
    }

    @Test
    fun `navigation polls only contain options with a feedback type`() {
        val polls = listOf(
            provider.getFreeDriveFeedbackPoll(carContext),
            provider.getActiveGuidanceFeedbackPoll(carContext),
            provider.getRoutePreviewFeedbackPoll(carContext),
            provider.getArrivalFeedbackPoll(carContext),
            provider.getPlaceFeedbackPoll(carContext),
        )

        polls.forEach { poll ->
            assertPollIsTitled(poll)
            assertTrue(poll.options.isNotEmpty())
            assertTrue(poll.options.all { it.type != null && it.nextPoll == null })
        }
    }

    @Test
    fun `place poll sends each search reason as navigation feedback`() {
        val poll = provider.getPlaceFeedbackPoll(carContext)

        assertPollIsTitled(poll)
        assertEquals(
            listOf(
                FeedbackEvent.OTHER_ISSUE to FeedbackReason.INCORRECT_ADDRESS,
                FeedbackEvent.OTHER_ISSUE to FeedbackReason.INCORRECT_LOCATION,
                FeedbackEvent.OTHER_ISSUE to FeedbackReason.INCORRECT_NAME,
                FeedbackEvent.OTHER_ISSUE to FeedbackReason.OTHER,
            ),
            poll.options.map { it.type to it.searchFeedbackReason },
        )
    }

    @Test
    fun `default polls only use navigation feedback types`() {
        val polls = listOf(
            provider.getFreeDriveFeedbackPoll(carContext),
            provider.getActiveGuidanceFeedbackPoll(carContext),
            provider.getRoutePreviewFeedbackPoll(carContext),
            provider.getArrivalFeedbackPoll(carContext),
            provider.getPlaceFeedbackPoll(carContext),
        )

        polls.flatMap { it.options }.forEach { option ->
            assertTrue(
                "${option.title} has type ${option.type}",
                option.type in NAVIGATION_FEEDBACK_TYPES,
            )
        }
    }

    @Test
    fun `place poll keeps an existing search poll override`() {
        val customPoll = CarFeedbackPoll("Custom", emptyList())
        val provider = object : CarFeedbackPollProvider() {
            @Deprecated("Override getPlaceFeedbackPoll() instead.")
            override fun getSearchFeedbackPoll(carContext: CarContext) = customPoll
        }

        assertSame(customPoll, provider.getPlaceFeedbackPoll(carContext))
    }

    @Test
    fun `place poll override does not consult the deprecated search poll`() {
        val customPoll = CarFeedbackPoll("Custom", emptyList())
        var searchPollCalls = 0
        val provider = object : CarFeedbackPollProvider() {
            override fun getPlaceFeedbackPoll(carContext: CarContext) = customPoll

            @Deprecated("Override getPlaceFeedbackPoll() instead.")
            override fun getSearchFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
                searchPollCalls++
                return super.getSearchFeedbackPoll(carContext)
            }
        }

        assertSame(customPoll, provider.getPlaceFeedbackPoll(carContext))
        assertEquals(0, searchPollCalls)
    }

    @Test
    fun `search poll override that calls the place poll does not recurse`() {
        val provider = object : CarFeedbackPollProvider() {
            @Deprecated("Override getPlaceFeedbackPoll() instead.")
            override fun getSearchFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
                val poll = getPlaceFeedbackPoll(carContext)
                return poll.copy(title = "Custom ${poll.title}")
            }
        }

        val poll = provider.getPlaceFeedbackPoll(carContext)

        assertEquals("Custom ${context.getString(R.string.car_feedback_title)}", poll.title)
        assertEquals(DEFAULT_PLACE_REASONS, poll.options.map { it.searchFeedbackReason })
    }

    @Test
    fun `place poll can be requested again after a search poll override throws`() {
        var shouldThrow = true
        val provider = object : CarFeedbackPollProvider() {
            @Deprecated("Override getPlaceFeedbackPoll() instead.")
            override fun getSearchFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
                if (shouldThrow) throw IllegalStateException("failed to build poll")
                return super.getSearchFeedbackPoll(carContext)
            }
        }
        runCatching { provider.getPlaceFeedbackPoll(carContext) }
        shouldThrow = false

        val poll = provider.getPlaceFeedbackPoll(carContext)

        assertEquals(DEFAULT_PLACE_REASONS, poll.options.map { it.searchFeedbackReason })
    }

    private fun assertPollIsTitled(poll: CarFeedbackPoll) {
        assertEquals(context.getString(R.string.car_feedback_title), poll.title)
    }

    private companion object {
        // Mirrors the values allowed by FeedbackEvent.Type.
        private val NAVIGATION_FEEDBACK_TYPES = setOf(
            FeedbackEvent.INCORRECT_VISUAL,
            FeedbackEvent.ROAD_ISSUE,
            FeedbackEvent.TRAFFIC_ISSUE,
            FeedbackEvent.OTHER_ISSUE,
            FeedbackEvent.ROAD_CLOSED,
            FeedbackEvent.ROUTING_ERROR,
            FeedbackEvent.ROUTE_NOT_ALLOWED,
            FeedbackEvent.INCORRECT_VISUAL_GUIDANCE,
            FeedbackEvent.INCORRECT_AUDIO_GUIDANCE,
            FeedbackEvent.POSITIONING_ISSUE,
            FeedbackEvent.ARRIVAL_FEEDBACK_GOOD,
            FeedbackEvent.ARRIVAL_FEEDBACK_NOT_GOOD,
        )
        private val DEFAULT_PLACE_REASONS = listOf(
            FeedbackReason.INCORRECT_ADDRESS,
            FeedbackReason.INCORRECT_LOCATION,
            FeedbackReason.INCORRECT_NAME,
            FeedbackReason.OTHER,
        )
    }
}
