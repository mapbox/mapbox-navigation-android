package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationCustomEventTest {

    @Test
    fun testValue() {
        val cancelEvent = NavigationCustomEvent().apply {
            type = "type_0"
            payload = "payload_0"
            customEventVersion = "customEventVersion_0"

            createdMonotime = 123
            driverMode = "driverMode_0"
            driverModeStartTimestampMonotime = 124
            sdkIdentifier = "sdkIdentifier_0"
            eventVersion = 125
            simulation = true
            locationEngine = "locationEngine"
            lat = 10.1
            lng = 11.2
        }

        val toValue = cancelEvent.toValue()

        (toValue.contents as Map<String, Value>).let { content ->
            // val
            assertEquals(cancelEvent.version, content["version"]!!.contents)
            assertEquals(cancelEvent.driverModeId, content["driverModeId"]!!.contents)
            assertEquals(cancelEvent.event, content["event"]!!.contents)
            assertEquals(cancelEvent.created, content["created"]!!.contents)
            assertEquals(cancelEvent.operatingSystem, content["operatingSystem"]!!.contents)
            assertEquals(
                cancelEvent.driverModeStartTimestamp,
                content["driverModeStartTimestamp"]!!.contents,
            )

            // var
            assertEquals(cancelEvent.type, content["type"]!!.contents)
            assertEquals(cancelEvent.payload, content["payload"]!!.contents)
            assertEquals(cancelEvent.customEventVersion, content["customEventVersion"]!!.contents)
            assertEquals(
                cancelEvent.createdMonotime.toLong(),
                content["createdMonotime"]!!.contents,
            )
            assertEquals(cancelEvent.driverMode, content["driverMode"]!!.contents)
            assertEquals(
                cancelEvent.driverModeStartTimestampMonotime.toLong(),
                content["driverModeStartTimestampMonotime"]!!.contents,
            )
            assertEquals(cancelEvent.sdkIdentifier, content["sdkIdentifier"]!!.contents)
            assertEquals(cancelEvent.eventVersion.toLong(), content["eventVersion"]!!.contents)
            assertEquals(cancelEvent.simulation, content["simulation"]!!.contents)
            assertEquals(
                cancelEvent.locationEngine,
                content["locationEngine"]!!.contents,
            )
            assertEquals(cancelEvent.lat, content["lat"]!!.contents)
            assertEquals(cancelEvent.lng, content["lng"]!!.contents)
        }
    }
}
