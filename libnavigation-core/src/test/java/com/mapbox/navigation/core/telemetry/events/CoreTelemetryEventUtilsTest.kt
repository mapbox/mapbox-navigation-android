package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyTelemetryLocations
import com.mapbox.navigation.core.testutil.EventsProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoreTelemetryEventUtilsTest {

    @Test
    fun `filled TelemetryLocation to value`() {
        val telemetryLocation = TelemetryLocation(
            latitude = 1.1,
            longitude = 2.2,
            speed = 3.3,
            bearing = 4.4,
            altitude = 5.5,
            timestamp = "timestamp_0",
            horizontalAccuracy = 6.6,
            verticalAccuracy = 7.7,
        )

        val toValue = telemetryLocation.toValue()

        (toValue.contents as HashMap<String, Value>).let { content ->
            assertEquals(1.1, content["lat"]!!.contents as Double, 0.000001)
            assertEquals(2.2, content["lng"]!!.contents as Double, 0.000001)
            assertEquals(3.3, content["speed"]!!.contents as Double, 0.001)
            assertEquals(4.4, content["course"]!!.contents as Double, 0.001)
            assertEquals(5.5, content["altitude"]!!.contents as Double, 0.001)
            assertEquals("timestamp_0", content["timestamp"]!!.contents)
            assertEquals(6.6, content["horizontalAccuracy"]!!.contents as Double, 0.001)
            assertEquals(7.7, content["verticalAccuracy"]!!.contents as Double, 0.001)
        }
    }

    @Test
    fun `nullable TelemetryLocation to value`() {
        val telemetryLocation = TelemetryLocation(
            latitude = 1.1,
            longitude = 2.2,
            speed = null,
            bearing = null,
            altitude = null,
            timestamp = "timestamp_0",
            horizontalAccuracy = 6.6,
            verticalAccuracy = 7.7,
        )

        val toValue = telemetryLocation.toValue()

        (toValue.contents as HashMap<String, Value>).let { content ->
            assertEquals(1.1, content["lat"]!!.contents as Double, 0.000001)
            assertEquals(2.2, content["lng"]!!.contents as Double, 0.000001)
            assertNull(content["speed"])
            assertNull(content["course"])
            assertNull(content["altitude"])
            assertEquals("timestamp_0", content["timestamp"]!!.contents)
            assertEquals(6.6, content["horizontalAccuracy"]!!.contents as Double, 0.001)
            assertEquals(7.7, content["verticalAccuracy"]!!.contents as Double, 0.001)
        }
    }

    @Test
    fun `AppMetadata to value`() {
        val appMetadata = AppMetadata(
            name = "name_0",
            version = "version_0",
            userId = "userId_0",
            sessionId = "sessionId_0",
        )

        val toValue = appMetadata.toValue()

        (toValue.contents as HashMap<String, Value>).let { content ->
            assertEquals("name_0", content["name"]!!.contents)
            assertEquals("version_0", content["version"]!!.contents)
            assertEquals("userId_0", content["userId"]!!.contents)
            assertEquals("sessionId_0", content["sessionId"]!!.contents)
        }
    }

    @Test
    fun `NavigationStepData to value`() {
        val mockNavigationStepData = EventsProvider.mockNavigationStepData()

        val toValue = mockNavigationStepData.toValue()

        (toValue.contents as HashMap<String, Value>).let { content ->
            assertEquals(1L, content["durationRemaining"]!!.contents)
            assertEquals(2L, content["distance"]!!.contents)
            assertEquals(3L, content["distanceRemaining"]!!.contents)
            assertEquals(4L, content["duration"]!!.contents)
            assertEquals("upcomingName_0", content["upcomingName"]!!.contents)
            assertEquals("upcomingModifier_0", content["upcomingModifier"]!!.contents)
            assertEquals("previousInstruction_0", content["previousInstruction"]!!.contents)
            assertEquals("previousName_0", content["previousName"]!!.contents)
            assertEquals("upcomingInstruction_0", content["upcomingInstruction"]!!.contents)
            assertEquals("previousType_0", content["previousType"]!!.contents)
            assertEquals("upcomingType_0", content["upcomingType"]!!.contents)
            assertEquals("previousModifier_0", content["previousModifier"]!!.contents)
        }
    }

    @Test
    fun `Array of strings to value`() {
        val strings = arrayOf(
            "string_0",
            "string_1",
            "string_2",
        )

        val toValue = strings.toValue { toValue() }

        assertArrayEquals(
            strings,
            (toValue.contents!! as List<Value>).map { it.contents as String }.toTypedArray(),
        )
    }

    @Test
    fun `Array of TelemetryLocation to value`() {
        val telemetryLocations = EventsProvider.provideDefaultTelemetryLocationsArray()

        val toValue = telemetryLocations.toValue { toValue() }

        toValue.verifyTelemetryLocations(telemetryLocations)
    }
}
