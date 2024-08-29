package com.mapbox.navigation.core.telemetry.events

import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.telemetry.toTelemetryLocations
import com.mapbox.navigation.core.telemetry.LocationsCollector
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
class FeedbackMetadataWrapperTest {

    private lateinit var wrapper: FeedbackMetadataWrapper
    private lateinit var phoneState: PhoneState
    private lateinit var locationsCollector: LocationsCollector
    private val locationsCollectorListenerSlot =
        slot<LocationsCollector.LocationsCollectorListener>()

    private companion object {
        const val SESSION_IDENTIFIER = "SESSION_IDENTIFIER"
        const val DRIVER_MODE_IDENTIFIER = "DRIVER_MODE_IDENTIFIER"
        const val DRIVER_MODE = FeedbackEvent.DRIVER_MODE_TRIP
        const val DRIVER_MODE_START_TIME = "DATE_TIME"
        const val REROUTE_COUNT = 1

        private val locationsBefore = listOf(
            Location.Builder().latitude(0.1).longitude(0.2).build(),
            Location.Builder().latitude(1.1).longitude(1.2).build(),
        )
        private val locationsAfter = listOf(
            Location.Builder().latitude(3.1).longitude(3.2).build(),
            Location.Builder().latitude(4.1).longitude(4.2).build(),
        )
    }

    @Before
    fun setup() {
        locationsCollector = mockk(relaxUnitFun = true) {
            every { collectLocations(capture(locationsCollectorListenerSlot)) } just runs
        }

        phoneState = PhoneState(
            volumeLevel = 5,
            batteryLevel = 11,
            screenBrightness = 16,
            isBatteryPluggedIn = true,
            connectivity = "CONNECTIVITY_STATE",
            audioType = "AUDIO_TYPE",
            applicationState = "APP_STATE",
            created = "CREATED_DATA",
            feedbackId = "FEEDBACK_ID",
            userId = "USER_ID",
        )

        wrapper = FeedbackMetadataWrapper(
            sessionIdentifier = SESSION_IDENTIFIER,
            driverModeStartTime = DRIVER_MODE_START_TIME,
            driverModeIdentifier = DRIVER_MODE_IDENTIFIER,
            driverMode = DRIVER_MODE,
            rerouteCount = REROUTE_COUNT,
            locationEngineNameExternal = "LOCATION_ENGINE_NAME_EXTERNAL",
            simulation = true,
            percentTimeInPortrait = 50,
            percentTimeInForeground = 20,
            eventVersion = 100,
            lastLocation = Point.fromLngLat(30.0, 40.0),
            phoneState = phoneState,
            metricsDirectionsRoute = MetricsDirectionsRoute(route = null),
            metricsRouteProgress = MetricsRouteProgress(routeProgress = null),
            appMetadata = AppMetadata(
                name = "APP_METADATA_NAME",
                version = "APP_METADATA_VERSION",
                userId = "APP_METADATA_USER_ID",
                sessionId = "APP_METADATA_SESSION_ID",
            ),
            locationsCollector = locationsCollector,
        )
    }

    @Test
    fun `metadata without locations`() {
        val metadata = wrapper.get()

        checkMetadataFields(metadata)
        verify(exactly = 1) {
            locationsCollector.flushBufferFor(locationsCollectorListenerSlot.captured)
        }
    }

    @Test
    fun `metadata with locations when buffer full`() {
        locationsCollectorListenerSlot.captured.onBufferFull(locationsBefore, locationsAfter)

        val metadata = wrapper.get()

        checkMetadataFields(
            metadata,
            locationsBefore.toTelemetryLocations(),
            locationsAfter.toTelemetryLocations(),
        )
        verify(exactly = 0) {
            locationsCollector.flushBufferFor(any())
        }
    }

    @Test
    fun `metadata with locations forced to assemble`() {
        every {
            locationsCollector.flushBufferFor(locationsCollectorListenerSlot.captured)
        } answers {
            locationsCollectorListenerSlot.captured.onBufferFull(locationsBefore, locationsAfter)
        }

        val metadata = wrapper.get()

        checkMetadataFields(
            metadata,
            locationsBefore.toTelemetryLocations(),
            locationsAfter.toTelemetryLocations(),
        )
        verify(exactly = 1) {
            locationsCollector.flushBufferFor(locationsCollectorListenerSlot.captured)
        }
    }

    private fun checkMetadataFields(
        feedbackMetadata: FeedbackMetadata,
        locationsBefore: Array<TelemetryLocation> = arrayOf(),
        locationsAfter: Array<TelemetryLocation> = arrayOf(),
    ) {
        assertEquals(SESSION_IDENTIFIER, feedbackMetadata.sessionIdentifier)
        assertEquals(DRIVER_MODE_IDENTIFIER, feedbackMetadata.driverModeIdentifier)
        assertEquals(DRIVER_MODE, feedbackMetadata.driverMode)
        assertEquals(DRIVER_MODE_START_TIME, feedbackMetadata.driverModeStartTime)
        assertEquals(REROUTE_COUNT, feedbackMetadata.rerouteCount)
        assertEquals(phoneState, feedbackMetadata.phoneState)
        assertArrayEquals(locationsBefore, feedbackMetadata.locationsBeforeEvent)
        assertArrayEquals(locationsAfter, feedbackMetadata.locationsAfterEvent)
    }
}
