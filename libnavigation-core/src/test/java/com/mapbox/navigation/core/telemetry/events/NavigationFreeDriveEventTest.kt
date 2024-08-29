package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyAppMetadata
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyTelemetryLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationFreeDriveEventTest {

    @Test
    fun testValue() {
        val freeDriveEvent = NavigationFreeDriveEvent(EventsTestHelper.mockPhoneState()).apply {
            eventVersion = 100
            locationEngine = "locationEngine_0"
            percentTimeInPortrait = 101
            percentTimeInForeground = 102
            simulation = true
            navigatorSessionIdentifier = "navigatorSessionIdentifier_0"
            startTimestamp = "startTimestamp_0"
            sessionIdentifier = "sessionIdentifier_0"
            location = TelemetryLocation(
                2.2,
                3.3,
                10.0,
                11.0,
                12.0,
                "timestamp_0",
                13.0,
                15.0,
            )
            eventType = "eventType_0"
            appMetadata = AppMetadata(
                name = "APP_METADATA_NAME",
                version = "APP_METADATA_VERSION",
                userId = "APP_METADATA_USER_ID",
                sessionId = "APP_METADATA_SESSION_ID",
            )
        }

        val toValue = freeDriveEvent.toValue()

        (toValue.contents as Map<String, Value>).let { content ->
            assertEquals(freeDriveEvent.version, content["version"]!!.contents)
            assertEquals(freeDriveEvent.created, content["created"]!!.contents)
            assertEquals(freeDriveEvent.volumeLevel.toLong(), content["volumeLevel"]!!.contents)
            assertEquals(freeDriveEvent.batteryLevel.toLong(), content["batteryLevel"]!!.contents)
            assertEquals(
                freeDriveEvent.screenBrightness.toLong(),
                content["screenBrightness"]!!.contents,
            )
            assertEquals(freeDriveEvent.batteryPluggedIn, content["batteryPluggedIn"]!!.contents)
            assertEquals(freeDriveEvent.connectivity, content["connectivity"]!!.contents)
            assertEquals(freeDriveEvent.audioType, content["audioType"]!!.contents)
            assertEquals(freeDriveEvent.applicationState, content["applicationState"]!!.contents)
            assertEquals(freeDriveEvent.event, content["event"]!!.contents)
            assertEquals(freeDriveEvent.eventVersion.toLong(), content["eventVersion"]!!.contents)
            assertEquals(freeDriveEvent.locationEngine, content["locationEngine"]!!.contents)
            assertEquals(
                freeDriveEvent.percentTimeInPortrait.toLong(),
                content["percentTimeInPortrait"]!!.contents,
            )
            assertEquals(
                freeDriveEvent.percentTimeInForeground.toLong(),
                content["percentTimeInForeground"]!!.contents,
            )
            assertEquals(freeDriveEvent.simulation, content["simulation"]!!.contents)
            assertEquals(
                freeDriveEvent.navigatorSessionIdentifier,
                content["navigatorSessionIdentifier"]!!.contents,
            )
            assertEquals(freeDriveEvent.startTimestamp, content["startTimestamp"]!!.contents)
            assertEquals(freeDriveEvent.sessionIdentifier, content["sessionIdentifier"]!!.contents)
            assertEquals(freeDriveEvent.eventType, content["eventType"]!!.contents)
            content["location"]!!.verifyTelemetryLocation(freeDriveEvent.location!!)
            content["appMetadata"]!!.verifyAppMetadata(freeDriveEvent.appMetadata!!)
        }
    }
}
