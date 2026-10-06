package com.mapbox.navigation.ui.androidauto.feedback.core

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.telemetry.UserFeedback
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        val userFeedback = slot<UserFeedback>()

        val sent = sender.send(
            CarFeedbackItem(
                carFeedbackTitle = "Routing error",
                navigationFeedbackType = FeedbackEvent.ROUTING_ERROR,
                navigationFeedbackSubType = listOf("sub-type"),
            ),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        assertTrue(sent)
        verify(exactly = 1) { mapboxNavigation.postUserFeedback(capture(userFeedback)) }
        with(userFeedback.captured) {
            assertEquals(FeedbackEvent.ROUTING_ERROR, feedbackType)
            assertEquals("Android Auto selection: Routing error", description)
            assertEquals(listOf("sub-type"), feedbackSubTypes)
            assertEquals("snapshot", screenshot)
            assertEquals(feedbackMetadata, this.feedbackMetadata)
        }
    }

    @Test
    fun `missing snapshot is posted without a screenshot`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        val userFeedback = slot<UserFeedback>()

        sender.send(
            CarFeedbackItem("Routing error", FeedbackEvent.ROUTING_ERROR),
            encodedSnapshot = null,
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        verify(exactly = 1) { mapboxNavigation.postUserFeedback(capture(userFeedback)) }
        assertNull(userFeedback.captured.screenshot)
        assertEquals(emptyList<String>(), userFeedback.captured.feedbackSubTypes)
    }

    @Test
    fun `unavailable feedback metadata is not posted and is reported as not sent`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        every { mapboxNavigation.provideFeedbackMetadataWrapper() } throws
            IllegalStateException("trip session is stopped")

        val sent = sender.send(
            CarFeedbackItem("Routing error", FeedbackEvent.ROUTING_ERROR),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        assertFalse(sent)
        verify(exactly = 0) { mapboxNavigation.postUserFeedback(any<UserFeedback>()) }
        verify(exactly = 1) { historyRecorder.pushHistory(HISTORY_EVENT_TYPE, any()) }
    }

    @Test
    fun `item without a feedback type is sent without feedback metadata`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        every { mapboxNavigation.provideFeedbackMetadataWrapper() } throws
            IllegalStateException("trip session is stopped")

        val sent = sender.send(
            CarFeedbackItem(carFeedbackTitle = "Incorrect address"),
            encodedSnapshot = null,
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        assertTrue(sent)
        verify(exactly = 1) { historyRecorder.pushHistory(HISTORY_EVENT_TYPE, any()) }
    }

    @Test
    fun `item without a feedback type is not sent when history can't be recorded`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        every { historyRecorder.pushHistory(any(), any()) } throws IllegalStateException("failed")

        val sent = sender.send(
            CarFeedbackItem(carFeedbackTitle = "Incorrect address"),
            encodedSnapshot = null,
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        assertFalse(sent)
    }

    @Test
    fun `failed post is reported as not sent`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        every { mapboxNavigation.postUserFeedback(any<UserFeedback>()) } throws
            IllegalStateException("destroyed")

        val sent = sender.send(
            CarFeedbackItem("Routing error", FeedbackEvent.ROUTING_ERROR),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        assertFalse(sent)
    }

    @Test
    fun `item without a feedback type is not posted but is recorded in history`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation

        val sent = sender.send(
            CarFeedbackItem(carFeedbackTitle = "Incorrect address"),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        assertTrue(sent)
        verify(exactly = 0) { mapboxNavigation.postUserFeedback(any<UserFeedback>()) }
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
        assertEquals("Routing error", item.string("carFeedbackTitle"))
        assertEquals(FeedbackEvent.ROUTING_ERROR, item.string("navigationFeedbackType"))
        assertEquals(FeedbackReason.INCORRECT_NAME, item.string("searchFeedbackReason"))
        assertEquals("wrong-favorite", item.string("favoritesFeedbackReason"))
    }

    @Test
    fun `history records whether a screenshot was attached, not the screenshot itself`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        val eventJson = slot<String>()
        every { historyRecorder.pushHistory(HISTORY_EVENT_TYPE, capture(eventJson)) } returns Unit

        sender.send(
            CarFeedbackItem("Routing error", FeedbackEvent.ROUTING_ERROR),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        val event = JsonParser.parseString(eventJson.captured).asJsonObject
        assertTrue(event.get("screenshotAttached").asBoolean)
        assertFalse(event.has("encodedSnapshot"))
        assertFalse(eventJson.captured.contains("snapshot\""))
    }

    @Test
    fun `failure to record history does not affect a posted item`() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        every { historyRecorder.pushHistory(any(), any()) } throws IllegalStateException("failed")

        val sent = sender.send(
            CarFeedbackItem("Routing error", FeedbackEvent.ROUTING_ERROR),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        assertTrue(sent)
    }

    @Test
    fun `sending without an attached MapboxNavigation does nothing and reports not sent`() {
        every { MapboxNavigationApp.current() } returns null

        val sent = sender.send(
            CarFeedbackItem("Routing error", FeedbackEvent.ROUTING_ERROR),
            encodedSnapshot = "snapshot",
            sourceScreenSimpleName = SOURCE_SCREEN,
        )

        assertFalse(sent)
        verify(exactly = 0) { historyRecorder.pushHistory(any(), any()) }
    }

    private fun JsonObject.string(name: String): String = get(name).asString

    private companion object {
        private const val SOURCE_SCREEN = "ROUTE_PREVIEW"
        private const val HISTORY_EVENT_TYPE = "car_feedback_sent"
    }
}
