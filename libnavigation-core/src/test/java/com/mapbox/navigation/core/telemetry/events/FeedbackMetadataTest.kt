package com.mapbox.navigation.core.telemetry.events

import android.location.Location
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.utils.toTelemetryLocations
import com.mapbox.navigation.testing.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
class FeedbackMetadataTest {

    private val jsonFeedbackMetadata = FileUtils.loadJsonFixture("feedback_metadata.json")

    private companion object {
        const val SESSION_IDENTIFIER = "SESSION_IDENTIFIER"
        const val DRIVER_MODE_IDENTIFIER = "DRIVER_MODE_IDENTIFIER"
        const val DRIVER_MODE = FeedbackEvent.DRIVER_MODE_TRIP
        const val DRIVER_MODE_START_TIME = "DATE_TIME_FORMAT"
        const val REROUTE_COUNT = 1

        private val locationsBefore = listOf(
            Location("providername1").apply {
                latitude = 0.1
                longitude = 0.2
            },
            Location("providername2").apply {
                latitude = 1.1
                longitude = 1.2
            }
        )
        private val locationsAfter = listOf(
            Location("providername3").apply {
                latitude = 3.1
                longitude = 3.2
            },
            Location("providername4").apply {
                latitude = 4.1
                longitude = 4.2
            }
        )
    }

    @Test
    fun sanity() {
        assertNotNull(jsonFeedbackMetadata)
        assertTrue(jsonFeedbackMetadata.isNotBlank())
    }

    @Test
    fun feedbackMetadataToJson() {
        val feedbackMetadataOriginal = provideFeedbackMetadata()

        val json = feedbackMetadataOriginal.toJson(Gson())
        val feedbackMetadataFromJson = FeedbackMetadata.fromJson(json)

        assertNotNull(feedbackMetadataFromJson)
        assertEquals(feedbackMetadataOriginal, feedbackMetadataFromJson)
    }

    @Test
    fun feedbackMetadataFromJson() {
        val feedbackMetadataFromJson = FeedbackMetadata.fromJson(jsonFeedbackMetadata)

        val newJsonFeedbackMetadata = feedbackMetadataFromJson?.toJson(Gson())

        assertNotNull(feedbackMetadataFromJson)
        assertNotNull(newJsonFeedbackMetadata)
        assertEquals(
            JsonParser.parseString(jsonFeedbackMetadata),
            JsonParser.parseString(newJsonFeedbackMetadata),
        )
    }

    private fun provideFeedbackMetadata(): FeedbackMetadata =
        FeedbackMetadata(
            sessionIdentifier = SESSION_IDENTIFIER,
            driverModeStartTime = DRIVER_MODE_START_TIME,
            driverModeIdentifier = DRIVER_MODE_IDENTIFIER,
            driverMode = DRIVER_MODE,
            rerouteCount = REROUTE_COUNT,
            locationsBeforeEvent = locationsBefore.toTelemetryLocations(),
            locationsAfterEvent = locationsAfter.toTelemetryLocations(),
            locationEngineNameExternal = "LOCATION_ENGINE_NAME_EXTERNAL",
            percentTimeInPortrait = 50,
            percentTimeInForeground = 20,
            eventVersion = 100,
            lastLocation = Point.fromLngLat(30.0, 40.0),
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
                userId = "USER_ID"
            ),
            navigationStepData = NavigationStepData(MetricsRouteProgress(null)),
            appMetadata = AppMetadata(
                name = "APP_METADATA_NAME",
                version = "APP_METADATA_VERSION",
                userId = "APP_METADATA_USER_ID",
                sessionId = "APP_METADATA_SESSION_ID",
            )
        )
}
