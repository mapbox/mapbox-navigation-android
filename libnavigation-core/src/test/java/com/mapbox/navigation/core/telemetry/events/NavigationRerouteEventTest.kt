package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.fillValues
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyNavigationEventFields
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyTelemetryLocations
import com.mapbox.navigation.core.testutil.EventsProvider
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Assert
import org.junit.Test

class NavigationRerouteEventTest {

    @Test
    fun testValue() {
        val rerouteEvent = NavigationRerouteEvent(
            EventsTestHelper.mockPhoneState(),
            EventsProvider.mockNavigationStepData(),
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
        }

        val toValue = rerouteEvent.toValue()

        toValue.verifyNavigationEventFields(rerouteEvent.event)
        (toValue.contents as Map<String, Value>).let { content ->
            assertEquals(rerouteEvent.feedbackId, content["feedbackId"]!!.contents)
            assertEquals(
                rerouteEvent.newDistanceRemaining.toLong(),
                content["newDistanceRemaining"]!!.contents,
            )
            assertEquals(
                rerouteEvent.newDurationRemaining.toLong(),
                content["newDurationRemaining"]!!.contents,
            )
            assertEquals(rerouteEvent.newGeometry, content["newGeometry"]!!.contents)
            assertEquals(
                rerouteEvent.secondsSinceLastReroute.toLong(),
                content["secondsSinceLastReroute"]!!.contents,
            )
            assertEquals(rerouteEvent.screenshot, content["screenshot"]!!.contents)
            assertTrue(content.containsKey("locationsBefore"))
            (content["locationsBefore"] as Value)
                .verifyTelemetryLocations(rerouteEvent.locationsBefore!!)
            assertTrue(content.containsKey("locationsAfter"))
            (content["locationsAfter"] as Value)
                .verifyTelemetryLocations(rerouteEvent.locationsAfter!!)
            // NavigationStepData verify
            assertTrue(content.containsKey("step"))
            (content["step"]!!.contents as HashMap<String, Value>).let { stepContent ->
                Assert.assertEquals(1L, stepContent["durationRemaining"]!!.contents)
                Assert.assertEquals(2L, stepContent["distance"]!!.contents)
                Assert.assertEquals(3L, stepContent["distanceRemaining"]!!.contents)
                Assert.assertEquals(4L, stepContent["duration"]!!.contents)
                Assert.assertEquals("upcomingName_0", stepContent["upcomingName"]!!.contents)
                Assert.assertEquals(
                    "upcomingModifier_0",
                    stepContent["upcomingModifier"]!!.contents,
                )
                Assert.assertEquals(
                    "previousInstruction_0",
                    stepContent["previousInstruction"]!!.contents,
                )
                Assert.assertEquals("previousName_0", stepContent["previousName"]!!.contents)
                Assert.assertEquals(
                    "upcomingInstruction_0",
                    stepContent["upcomingInstruction"]!!.contents,
                )
                Assert.assertEquals("previousType_0", stepContent["previousType"]!!.contents)
                Assert.assertEquals("upcomingType_0", stepContent["upcomingType"]!!.contents)
                Assert.assertEquals(
                    "previousModifier_0",
                    stepContent["previousModifier"]!!.contents,
                )
            }
        }
    }
}
