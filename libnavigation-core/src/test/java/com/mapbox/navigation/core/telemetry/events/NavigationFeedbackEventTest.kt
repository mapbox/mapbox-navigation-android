package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.fillValues
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyNavigationEventFields
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyTelemetryLocations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationFeedbackEventTest {

    @Test
    fun testValue() {
        val feedbackEvent = NavigationFeedbackEvent(
            EventsTestHelper.mockPhoneState(),
            NavigationStepData(MetricsRouteProgress(null)),
        ).apply {
            fillValues()
            feedbackType = "feedbackType_0"
            source = "source_0"
            description = "description_0"
            locationsBefore = arrayOf(
                TelemetryLocation(
                    2.2,
                    3.3,
                    10.0,
                    11.0,
                    12.0,
                    "timestamp_0",
                    13.0,
                    15.0,
                ),
            )
            locationsAfter = arrayOf(
                TelemetryLocation(
                    12.2,
                    13.3,
                    110.0,
                    111.0,
                    112.0,
                    "timestamp_1",
                    113.0,
                    115.0,
                ),
            )
            screenshot = "screenshot_0"
            feedbackSubType = arrayOf("feedbackSubType_0", "feedbackSubType_1")
        }

        val toValue = feedbackEvent.toValue()

        toValue.verifyNavigationEventFields(feedbackEvent.event)
        (toValue.contents as Map<String, Value>).let { content ->
            assertEquals(feedbackEvent.userId, content["userId"]!!.contents)
            assertEquals(feedbackEvent.feedbackId, content["feedbackId"]!!.contents)
            assertEquals(feedbackEvent.feedbackType, content["feedbackType"]!!.contents)
            assertEquals(feedbackEvent.source, content["source"]!!.contents)
            assertEquals(feedbackEvent.description, content["description"]!!.contents)
            assertEquals(feedbackEvent.screenshot, content["screenshot"]!!.contents)
            assertTrue(content.containsKey("locationsBefore"))
            (content["locationsBefore"] as Value)
                .verifyTelemetryLocations(feedbackEvent.locationsBefore!!)
            assertTrue(content.containsKey("locationsAfter"))
            (content["locationsAfter"] as Value)
                .verifyTelemetryLocations(feedbackEvent.locationsAfter!!)
        }
    }
}
