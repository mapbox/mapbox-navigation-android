package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.fillValues
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyNavigationEventFields
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyTelemetryLocations
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class NavigationRerouteEventTest {

    @Test
    fun testValue() {
        val rerouteEvent = NavigationRerouteEvent(
            EventsTestHelper.mockPhoneState(),
            MetricsRouteProgress(null)
        ).apply {
            fillValues()
            newDistanceRemaining = 50
            newDurationRemaining = 51
            newGeometry = "newGeometry_0"
            secondsSinceLastReroute = 52
            locationsBefore = arrayOf(
                TelemetryLocation(
                    2.2,
                    3.3,
                    10.0f,
                    11.0f,
                    12.0,
                    "timestamp_0",
                    13.0f,
                    15.0f,
                )
            )
            locationsAfter = arrayOf(
                TelemetryLocation(
                    12.2,
                    13.3,
                    110.0f,
                    111.0f,
                    112.0,
                    "timestamp_1",
                    113.0f,
                    115.0f,
                )
            )
            screenshot = "screenshot_0"
        }

        val toValue = rerouteEvent.toValue()

        toValue.verifyNavigationEventFields(rerouteEvent.event)
        (toValue.contents as Map<String, Value>).let { content ->
            assertEquals(rerouteEvent.feedbackId, content["feedbackId"]!!.contents)
            assertEquals(
                rerouteEvent.newDistanceRemaining.toLong(),
                content["newDistanceRemaining"]!!.contents
            )
            assertEquals(
                rerouteEvent.newDurationRemaining.toLong(),
                content["newDurationRemaining"]!!.contents
            )
            assertEquals(rerouteEvent.newGeometry, content["newGeometry"]!!.contents)
            assertEquals(
                rerouteEvent.secondsSinceLastReroute.toLong(),
                content["secondsSinceLastReroute"]!!.contents
            )
            assertEquals(rerouteEvent.screenshot, content["screenshot"]!!.contents)
            assertTrue(content.containsKey("locationsBefore"))
            (content["locationsBefore"] as Value)
                .verifyTelemetryLocations(rerouteEvent.locationsBefore!!)
            assertTrue(content.containsKey("locationsAfter"))
            (content["locationsAfter"] as Value)
                .verifyTelemetryLocations(rerouteEvent.locationsAfter!!)
        }
    }
}
