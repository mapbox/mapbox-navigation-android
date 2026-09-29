package com.mapbox.navigation.ui.androidauto.feedback.core

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackItem
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.search.analytics.FeedbackEvent.FeedbackReason
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CarFeedbackSenderTest {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val historyRecorder: MapboxHistoryRecorder = mockk(relaxed = true)
    private val feedbackMetadata: FeedbackMetadata = mockk()
    private val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { historyRecorder } returns this@CarFeedbackSenderTest.historyRecorder
        every { provideFeedbackMetadataWrapper() } returns mockk {
            every { get() } returns feedbackMetadata
        }
    }
    private val sender = CarFeedbackSender()

    @Test
    fun `item with a feedback type is posted as user feedback`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation

        sender.send(
            CarFeedbackItem(
                carFeedbackTitle = "Routing error",
                navigationFeedbackType = FeedbackEvent.ROUTING_ERROR,
                navigationFeedbackSubType = listOf("sub-type"),
            ),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        verify(exactly = 1) {
            mapboxNavigation.postUserFeedback(
                feedbackType = FeedbackEvent.ROUTING_ERROR,
                description = "Android Auto selection: Routing error",
                feedbackSource = FeedbackEvent.UI,
                screenshot = "snapshot",
                feedbackSubType = arrayOf("sub-type"),
                feedbackMetadata = feedbackMetadata,
            )
        }
    }

    @Test
    fun `missing snapshot is posted as an empty screenshot`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation

        sender.send(
            CarFeedbackItem("Routing error", FeedbackEvent.ROUTING_ERROR),
            encodedSnapshot = null,
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        verify(exactly = 1) {
            mapboxNavigation.postUserFeedback(
                feedbackType = FeedbackEvent.ROUTING_ERROR,
                description = any(),
                feedbackSource = FeedbackEvent.UI,
                screenshot = "",
                feedbackSubType = null,
                feedbackMetadata = feedbackMetadata,
            )
        }
    }

    @Test
    fun `item without a feedback type is not posted but is recorded in history`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation

        sender.send(
            CarFeedbackItem(carFeedbackTitle = "Incorrect address"),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        verify(exactly = 0) {
            mapboxNavigation.postUserFeedback(any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 1) { historyRecorder.pushHistory(HISTORY_EVENT_TYPE, any()) }
    }

    @Test
    fun `selection is recorded in history with its title, type and reasons`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        val eventJson = slot<String>()
        every { historyRecorder.pushHistory(HISTORY_EVENT_TYPE, capture(eventJson)) } returns Unit

        sender.send(
            CarFeedbackItem(
                carFeedbackTitle = "Routing error",
                navigationFeedbackType = FeedbackEvent.ROUTING_ERROR,
                searchFeedbackReason = FeedbackReason.INCORRECT_NAME,
                favoritesFeedbackReason = "wrong-favorite",
            ),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        val event = JsonParser.parseString(eventJson.captured).asJsonObject
        val item = event.getAsJsonObject("carFeedbackItem")
        assertEquals(SOURCE_SCREEN, event.string("sourceScreen"))
        assertEquals("snapshot", event.string("encodedSnapshot"))
        assertEquals("Routing error", item.string("carFeedbackTitle"))
        assertEquals(FeedbackEvent.ROUTING_ERROR, item.string("navigationFeedbackType"))
        assertEquals(FeedbackReason.INCORRECT_NAME, item.string("searchFeedbackReason"))
        assertEquals("wrong-favorite", item.string("favoritesFeedbackReason"))
    }

    @Test
    fun `sending without an attached MapboxNavigation does nothing`() {
        every { MapboxNavigationApp.current() } returns null

        sender.send(
            CarFeedbackItem("Routing error", FeedbackEvent.ROUTING_ERROR),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        verify(exactly = 0) { historyRecorder.pushHistory(any(), any()) }
    }

    private fun JsonObject.string(name: String): String = get(name).asString

    private companion object {
        private const val SOURCE_SCREEN = "ROUTE_PREVIEW"
        private const val HISTORY_EVENT_TYPE = "car_feedback_sent"
    }
}
