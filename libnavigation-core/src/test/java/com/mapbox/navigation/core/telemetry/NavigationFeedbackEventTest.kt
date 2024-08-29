package com.mapbox.navigation.core.telemetry

import com.google.gson.Gson
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.MetricsRouteProgress
import com.mapbox.navigation.core.telemetry.events.NavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.NavigationStepData
import com.mapbox.navigation.core.telemetry.events.PhoneState
import com.mapbox.navigation.core.telemetry.events.TelemetryLocation
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NavigationFeedbackEventTest {

    private val gson = Gson()
    private val phoneState = mockk<PhoneState>()
    private val metricsRouteProgress = mockk<MetricsRouteProgress>()
    private val metadata = AppMetadata(
        APP_METADATA_NAME,
        APP_METADATA_VERSION,
        APP_METADATA_USER_ID,
        APP_METADATA_SESSION_ID,
    )

    private val locationBefore = TelemetryLocation(
        LOCATION_BEFORE_LATITUDE,
        LOCATION_BEFORE_LONGITUDE,
        LOCATION_BEFORE_SPEED,
        LOCATION_BEFORE_BEARING,
        LOCATION_BEFORE_ALTITUDE,
        LOCATION_BEFORE_TIMESTAMP,
        LOCATION_BEFORE_HORIZONTAL_ACCURACY,
        LOCATION_BEFORE_VERTICAL_ACCURACY,
    )
    private val locationAfter = TelemetryLocation(
        LOCATION_AFTER_LATITUDE,
        LOCATION_AFTER_LONGITUDE,
        LOCATION_AFTER_SPEED,
        LOCATION_AFTER_BEARING,
        LOCATION_AFTER_ALTITUDE,
        LOCATION_AFTER_TIMESTAMP,
        LOCATION_AFTER_HORIZONTAL_ACCURACY,
        LOCATION_AFTER_VERTICAL_ACCURACY,
    )

    @Before
    fun setUp() {
        every { phoneState.volumeLevel } returns STATE_VOLUME_LEVEL
        every { phoneState.batteryLevel } returns STATE_BATTERY_LEVEL
        every { phoneState.screenBrightness } returns STATE_SCREEN_BRIGHTNESS
        every { phoneState.isBatteryPluggedIn } returns STATE_IS_BATTERY_PLUGGED_IN
        every { phoneState.connectivity } returns STATE_CONNECTIVITY
        every { phoneState.audioType } returns STATE_AUDIO_TYPE
        every { phoneState.applicationState } returns STATE_APPLICATION_STATE
        every { phoneState.created } returns STATE_CREATED
        every { phoneState.feedbackId } returns STATE_FEEDBACK_ID
        every { phoneState.userId } returns STATE_USER_ID

        every {
            metricsRouteProgress.directionsRouteDistance
        } returns PROGRESS_DIRECTIONS_ROUTE_DISTANCE
        every {
            metricsRouteProgress.directionsRouteDuration
        } returns PROGRESS_DIRECTIONS_ROUTE_DURATION
        every {
            metricsRouteProgress.directionsRouteProfile
        } returns PROGRESS_DIRECTIONS_ROUTE_PROFILE
        every { metricsRouteProgress.distanceRemaining } returns PROGRESS_DISTANCE_REMAINING
        every { metricsRouteProgress.durationRemaining } returns PROGRESS_DURATION_REMAINING
        every { metricsRouteProgress.distanceTraveled } returns PROGRESS_DISTANCE_TRAVELED
        every { metricsRouteProgress.currentStepDistance } returns PROGRESS_CURRENT_STEP_DISTANCE
        every { metricsRouteProgress.currentStepDuration } returns PROGRESS_CURRENT_STEP_DURATION
        every {
            metricsRouteProgress.currentStepDistanceRemaining
        } returns PROGRESS_CURRENT_STEP_DISTANCE_REMAINING
        every {
            metricsRouteProgress.currentStepDurationRemaining
        } returns PROGRESS_CURRENT_STEP_DURATION_REMAINING
        every {
            metricsRouteProgress.upcomingStepInstruction
        } returns PROGRESS_UPCOMING_STEP_INSTRUCTION
        every {
            metricsRouteProgress.upcomingStepModifier
        } returns PROGRESS_UPCOMING_STEP_MODIFIER
        every { metricsRouteProgress.upcomingStepType } returns PROGRESS_UPCOMING_STEP_TYPE
        every { metricsRouteProgress.upcomingStepName } returns PROGRESS_UPCOMING_STEP_NAME
        every {
            metricsRouteProgress.previousStepInstruction
        } returns PROGRESS_PREVIOUS_STEP_INSTRUCTION
        every { metricsRouteProgress.previousStepModifier } returns PROGRESS_PREVIOUS_STEP_MODIFIER
        every { metricsRouteProgress.previousStepType } returns PROGRESS_PREVIOUS_STEP_TYPE
        every { metricsRouteProgress.previousStepName } returns PROGRESS_PREVIOUS_STEP_NAME
        every { metricsRouteProgress.legIndex } returns PROGRESS_LEG_INDEX
        every { metricsRouteProgress.legCount } returns PROGRESS_LEG_COUNT
        every { metricsRouteProgress.stepIndex } returns PROGRESS_STEP_INDEX
        every { metricsRouteProgress.stepCount } returns PROGRESS_STEP_COUNT
    }

    @Test
    fun checkSerialization() {
        val feedbackEvent =
            NavigationFeedbackEvent(phoneState, NavigationStepData(metricsRouteProgress)).apply {
                feedbackType = EVENT_FEEDBACK_TYPE
                source = EVENT_SOURCE
                description = EVENT_DESCRIPTION
                screenshot = EVENT_SCREENSHOT
                appMetadata = metadata
                feedbackSubType = arrayOf(EVENT_FEEDBACK_SUB_TYPE)
                locationsBefore = arrayOf(locationBefore)
                locationsAfter = arrayOf(locationAfter)
            }

        val feedbackEventJson = gson.toJson(feedbackEvent)
        val deserializedFeedbackEvent =
            gson.fromJson(feedbackEventJson, NavigationFeedbackEvent::class.java)

        deserializedFeedbackEvent.run {
            assertEquals(feedbackEvent.version, version)
            assertEquals(feedbackEvent.userId, userId)
            assertEquals(feedbackEvent.feedbackId, feedbackId)
            assertEquals(feedbackEvent.step, step)
            assertEquals(feedbackEvent.feedbackType, feedbackType)
            assertEquals(feedbackEvent.source, source)
            assertEquals(feedbackEvent.description, description)
            assertEquals(feedbackEvent.screenshot, screenshot)
            assertEquals(feedbackEvent.appMetadata, appMetadata)
            assertArrayEquals(feedbackEvent.feedbackSubType, feedbackSubType)
            assertArrayEquals(feedbackEvent.locationsBefore, locationsBefore)
            assertArrayEquals(feedbackEvent.locationsAfter, locationsAfter)
        }
    }

    companion object {
        // PhoneState
        private const val STATE_VOLUME_LEVEL = 1
        private const val STATE_BATTERY_LEVEL = 2
        private const val STATE_SCREEN_BRIGHTNESS = 3
        private const val STATE_IS_BATTERY_PLUGGED_IN = true
        private const val STATE_CONNECTIVITY = "connectivity"
        private const val STATE_AUDIO_TYPE = "audioType"
        private const val STATE_APPLICATION_STATE = "applicationState"
        private const val STATE_CREATED = "created"
        private const val STATE_FEEDBACK_ID = "feedbackId"
        private const val STATE_USER_ID = "userId"

        // MetricsRouteProgress
        private const val PROGRESS_DIRECTIONS_ROUTE_DISTANCE = 11
        private const val PROGRESS_DIRECTIONS_ROUTE_DURATION = 12
        private const val PROGRESS_DIRECTIONS_ROUTE_PROFILE = "directionsRouteProfile"
        private const val PROGRESS_DISTANCE_REMAINING = 13
        private const val PROGRESS_DURATION_REMAINING = 14
        private const val PROGRESS_DISTANCE_TRAVELED = 15
        private const val PROGRESS_CURRENT_STEP_DISTANCE = 16
        private const val PROGRESS_CURRENT_STEP_DURATION = 17
        private const val PROGRESS_CURRENT_STEP_DISTANCE_REMAINING = 18
        private const val PROGRESS_CURRENT_STEP_DURATION_REMAINING = 19
        private const val PROGRESS_UPCOMING_STEP_INSTRUCTION = "upcomingStepInstruction"
        private const val PROGRESS_UPCOMING_STEP_MODIFIER = "upcomingStepModifier"
        private const val PROGRESS_UPCOMING_STEP_TYPE = "upcomingStepType"
        private const val PROGRESS_UPCOMING_STEP_NAME = "upcomingStepName"
        private const val PROGRESS_PREVIOUS_STEP_INSTRUCTION = "previousStepInstruction"
        private const val PROGRESS_PREVIOUS_STEP_MODIFIER = "previousStepModifier"
        private const val PROGRESS_PREVIOUS_STEP_TYPE = "previousStepType"
        private const val PROGRESS_PREVIOUS_STEP_NAME = "previousStepName"
        private const val PROGRESS_LEG_INDEX = 20
        private const val PROGRESS_LEG_COUNT = 21
        private const val PROGRESS_STEP_INDEX = 22
        private const val PROGRESS_STEP_COUNT = 23

        // NavigationFeedbackEvent
        private const val EVENT_VERSION = "version"
        private const val EVENT_FEEDBACK_TYPE = "feedbackType"
        private const val EVENT_SOURCE = "source"
        private const val EVENT_DESCRIPTION = "description"
        private const val EVENT_SCREENSHOT = "screenshot"
        private const val EVENT_FEEDBACK_SUB_TYPE = "feedbackSubType"

        // AppMetadata
        private const val APP_METADATA_NAME = "name"
        private const val APP_METADATA_VERSION = "version"
        private const val APP_METADATA_USER_ID = "userId"
        private const val APP_METADATA_SESSION_ID = "sessionId"

        // FeedbackLocation before
        private const val LOCATION_BEFORE_LATITUDE = 1.1
        private const val LOCATION_BEFORE_LONGITUDE = 2.2
        private const val LOCATION_BEFORE_SPEED = 30.0
        private const val LOCATION_BEFORE_BEARING = 200.0
        private const val LOCATION_BEFORE_ALTITUDE = 10.0
        private const val LOCATION_BEFORE_TIMESTAMP = "999999"
        private const val LOCATION_BEFORE_HORIZONTAL_ACCURACY = 1.0
        private const val LOCATION_BEFORE_VERTICAL_ACCURACY = 2.0

        // FeedbackLocation after
        private const val LOCATION_AFTER_LATITUDE = 22.1
        private const val LOCATION_AFTER_LONGITUDE = 33.2
        private const val LOCATION_AFTER_SPEED = 50.0
        private const val LOCATION_AFTER_BEARING = 330.0
        private const val LOCATION_AFTER_ALTITUDE = 17.0
        private const val LOCATION_AFTER_TIMESTAMP = "55555555"
        private const val LOCATION_AFTER_HORIZONTAL_ACCURACY = 55.0
        private const val LOCATION_AFTER_VERTICAL_ACCURACY = 44.0
    }
}
