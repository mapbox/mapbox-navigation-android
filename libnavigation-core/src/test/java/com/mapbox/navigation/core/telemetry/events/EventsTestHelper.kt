package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

internal object EventsTestHelper {

    // PhoneState
    const val VOLUME_LEVEL = 10
    const val BATTERY_LEVEL = 11
    const val SCREEN_BRIGHTNESS = 12
    const val IS_BATTERY_PLUGGEDIN = false
    const val CONNECTIVITY = "connectivity_0"
    const val AUDIO_TYPE = "audioType_0"
    const val APPLICATION_STATE = "applicationState_0"
    const val CREATED = "created_0"
    const val FEEDBACK_ID = "feedbackId_0"
    const val USER_ID = "userId_0"

    // NavigationEvent
    const val NAVIGATOR_SESSION_IDENTIFIER = "navigatorSessionIdentifier_0"
    const val START_TIMESTAMP = "123456789"
    const val DRIVER_MODE = "driverMode"
    const val SESSION_IDENTIFIER = "sessionIdentifier_0"
    const val GEOMETRY = "geometry_0"
    const val PROFILE = "profile_0"
    const val REQUEST_IDENTIFIER = "requestIdentifier_0"
    const val ORIGINAL_GEOMETRY = "originalGeometry_0"
    const val LOCATION_ENGINE = "locationEngine_0"
    const val TRIP_IDENTIFIER = "tripIdentifier_0"
    const val LAT = 1.1
    const val LNG = 2.2
    const val SIMULATION = true
    const val ABSOLUTE_DISTANCE_TO_DESTINATION = 100
    const val PERCENT_TIME_IN_PORTRAIT = 20
    const val PERCENT_TIME_IN_FOREGROUND = 40
    const val DISTANCE_COMPLETED = 1000
    const val DISTANCE_REMAINING = 1001
    const val EVENT_VERSION = 1002
    const val ESTIMATED_DISTANCE = 1003
    const val ESTIMATED_DURATION = 1004
    const val REROUTE_COUNT = 1005
    const val ORIGINAL_ESTIMATED_DISTANCE = 1006
    const val ORIGINAL_ESTIMATED_DURATION = 1007
    const val STEP_COUNT = 1008
    const val ORIGINAL_STEP_COUNT = 1009
    const val LEG_INDEX = 1010
    const val LEG_COUNT = 1011
    const val STEP_INDEX = 1012
    const val VOICE_INDEX = 1013
    const val BANNER_INDEX = 1014
    const val TOTAL_STEP_COUNT = 1015
    val APP_METADATA = AppMetadata(
        name = "app_metadata_name",
        version = "app_metadata_version",
        userId = "app_metadata_userId",
        sessionId = "app_metadata_sessionId",
    )

    fun mockPhoneState(
        volumeLevel: Int = VOLUME_LEVEL,
        batteryLevel: Int = BATTERY_LEVEL,
        screenBrightness: Int = SCREEN_BRIGHTNESS,
        isBatteryPluggedIn: Boolean = IS_BATTERY_PLUGGEDIN,
        connectivity: String = CONNECTIVITY,
        audioType: String = AUDIO_TYPE,
        applicationState: String = APPLICATION_STATE,
        created: String = CREATED,
        feedbackId: String = FEEDBACK_ID,
        userId: String = USER_ID,
    ): PhoneState =
        mockk {
            every { this@mockk.volumeLevel } returns volumeLevel
            every { this@mockk.batteryLevel } returns batteryLevel
            every { this@mockk.screenBrightness } returns screenBrightness
            every { this@mockk.isBatteryPluggedIn } returns isBatteryPluggedIn
            every { this@mockk.connectivity } returns connectivity
            every { this@mockk.audioType } returns audioType
            every { this@mockk.applicationState } returns applicationState
            every { this@mockk.created } returns created
            every { this@mockk.feedbackId } returns feedbackId
            every { this@mockk.userId } returns userId
        }

    fun NavigationEvent.fillValues(
        navigatorSessionIdentifier: String = NAVIGATOR_SESSION_IDENTIFIER,
        startTimestamp: String = START_TIMESTAMP,
        driverMode: String = DRIVER_MODE,
        sessionIdentifier: String = SESSION_IDENTIFIER,
        geometry: String = GEOMETRY,
        profile: String = PROFILE,
        requestIdentifier: String = REQUEST_IDENTIFIER,
        originalGeometry: String = ORIGINAL_GEOMETRY,
        locationEngine: String = LOCATION_ENGINE,
        tripIdentifier: String = TRIP_IDENTIFIER,
        lat: Double = LAT,
        lng: Double = LNG,
        simulation: Boolean = SIMULATION,
        absoluteDistanceToDestination: Int = ABSOLUTE_DISTANCE_TO_DESTINATION,
        percentTimeInPortrait: Int = PERCENT_TIME_IN_PORTRAIT,
        percentTimeInForeground: Int = PERCENT_TIME_IN_FOREGROUND,
        distanceCompleted: Int = DISTANCE_COMPLETED,
        distanceRemaining: Int = DISTANCE_REMAINING,
        eventVersion: Int = EVENT_VERSION,
        estimatedDistance: Int = ESTIMATED_DISTANCE,
        estimatedDuration: Int = ESTIMATED_DURATION,
        rerouteCount: Int = REROUTE_COUNT,
        originalEstimatedDistance: Int = ORIGINAL_ESTIMATED_DISTANCE,
        originalEstimatedDuration: Int = ORIGINAL_ESTIMATED_DURATION,
        stepCount: Int = STEP_COUNT,
        originalStepCount: Int = ORIGINAL_STEP_COUNT,
        legIndex: Int = LEG_INDEX,
        legCount: Int = LEG_COUNT,
        stepIndex: Int = STEP_INDEX,
        voiceIndex: Int = VOICE_INDEX,
        bannerIndex: Int = BANNER_INDEX,
        totalStepCount: Int = TOTAL_STEP_COUNT,
        appMetadata: AppMetadata = APP_METADATA,
    ) {
        this.navigatorSessionIdentifier = navigatorSessionIdentifier
        this.startTimestamp = startTimestamp
        this.driverMode = driverMode
        this.sessionIdentifier = sessionIdentifier
        this.geometry = geometry
        this.profile = profile
        this.requestIdentifier = requestIdentifier
        this.originalGeometry = originalGeometry
        this.locationEngine = locationEngine
        this.tripIdentifier = tripIdentifier
        this.lat = lat
        this.lng = lng
        this.simulation = simulation
        this.absoluteDistanceToDestination = absoluteDistanceToDestination
        this.percentTimeInPortrait = percentTimeInPortrait
        this.percentTimeInForeground = percentTimeInForeground
        this.distanceCompleted = distanceCompleted
        this.distanceRemaining = distanceRemaining
        this.eventVersion = eventVersion
        this.estimatedDistance = estimatedDistance
        this.estimatedDuration = estimatedDuration
        this.rerouteCount = rerouteCount
        this.originalEstimatedDistance = originalEstimatedDistance
        this.originalEstimatedDuration = originalEstimatedDuration
        this.stepCount = stepCount
        this.originalStepCount = originalStepCount
        this.legIndex = legIndex
        this.legCount = legCount
        this.stepIndex = stepIndex
        this.voiceIndex = voiceIndex
        this.bannerIndex = bannerIndex
        this.totalStepCount = totalStepCount
        this.appMetadata = appMetadata
    }

    fun Value.verifyNavigationEventFields(
        eventName: String,
        navigatorSessionIdentifier: String = NAVIGATOR_SESSION_IDENTIFIER,
        startTimestamp: String = START_TIMESTAMP,
        driverMode: String = DRIVER_MODE,
        sessionIdentifier: String = SESSION_IDENTIFIER,
        geometry: String = GEOMETRY,
        profile: String = PROFILE,
        requestIdentifier: String = REQUEST_IDENTIFIER,
        originalGeometry: String = ORIGINAL_GEOMETRY,
        locationEngine: String = LOCATION_ENGINE,
        tripIdentifier: String = TRIP_IDENTIFIER,
        lat: Double = LAT,
        lng: Double = LNG,
        simulation: Boolean = SIMULATION,
        absoluteDistanceToDestination: Int = ABSOLUTE_DISTANCE_TO_DESTINATION,
        percentTimeInPortrait: Int = PERCENT_TIME_IN_PORTRAIT,
        percentTimeInForeground: Int = PERCENT_TIME_IN_FOREGROUND,
        distanceCompleted: Int = DISTANCE_COMPLETED,
        distanceRemaining: Int = DISTANCE_REMAINING,
        eventVersion: Int = EVENT_VERSION,
        estimatedDistance: Int = ESTIMATED_DISTANCE,
        estimatedDuration: Int = ESTIMATED_DURATION,
        rerouteCount: Int = REROUTE_COUNT,
        originalEstimatedDistance: Int = ORIGINAL_ESTIMATED_DISTANCE,
        originalEstimatedDuration: Int = ORIGINAL_ESTIMATED_DURATION,
        stepCount: Int = STEP_COUNT,
        originalStepCount: Int = ORIGINAL_STEP_COUNT,
        legIndex: Int = LEG_INDEX,
        legCount: Int = LEG_COUNT,
        stepIndex: Int = STEP_INDEX,
        voiceIndex: Int = VOICE_INDEX,
        bannerIndex: Int = BANNER_INDEX,
        totalStepCount: Int = TOTAL_STEP_COUNT,
        appMetadata: AppMetadata = APP_METADATA,
    ) {
        (this.contents as Map<String, Value>).let { content ->
            assertEquals(
                "event value",
                eventName,
                content["event"]!!.contents,
            )
            assertEquals(
                "navigatorSessionIdentifier value",
                navigatorSessionIdentifier,
                content["navigatorSessionIdentifier"]!!.contents,
            )
            assertEquals(
                "startTimestamp value",
                startTimestamp,
                content["startTimestamp"]!!.contents,
            )
            assertEquals(
                "driverMode value",
                driverMode,
                content["driverMode"]!!.contents,
            )
            assertEquals(
                "sessionIdentifier value",
                sessionIdentifier,
                content["sessionIdentifier"]!!.contents,
            )
            assertEquals(
                "geometry value",
                geometry,
                content["geometry"]!!.contents,
            )
            assertEquals(
                "profile value",
                profile,
                content["profile"]!!.contents,
            )
            assertEquals(
                "requestIdentifier value",
                requestIdentifier,
                content["requestIdentifier"]!!.contents,
            )
            assertEquals(
                "originalGeometry value",
                originalGeometry,
                content["originalGeometry"]!!.contents,
            )
            assertEquals(
                "locationEngine value",
                locationEngine,
                content["locationEngine"]!!.contents,
            )
            assertEquals(
                "tripIdentifier value",
                tripIdentifier,
                content["tripIdentifier"]!!.contents,
            )
            assertEquals(
                "lat value",
                lat,
                content["lat"]!!.contents as Double,
                0.0,
            )
            assertEquals(
                "lng value",
                lng,
                content["lng"]!!.contents as Double,
                0.0,
            )
            assertEquals(
                "simulation value",
                simulation,
                content["simulation"]!!.contents,
            )
            assertEquals(
                "absoluteDistanceToDestination value",
                absoluteDistanceToDestination.toLong(),
                content["absoluteDistanceToDestination"]!!.contents,
            )
            assertEquals(
                "percentTimeInPortrait value",
                percentTimeInPortrait.toLong(),
                content["percentTimeInPortrait"]!!.contents,
            )
            assertEquals(
                "percentTimeInForeground value",
                percentTimeInForeground.toLong(),
                content["percentTimeInForeground"]!!.contents,
            )
            assertEquals(
                "distanceCompleted value",
                distanceCompleted.toLong(),
                content["distanceCompleted"]!!.contents,
            )
            assertEquals(
                "distanceRemaining value",
                distanceRemaining.toLong(),
                content["distanceRemaining"]!!.contents,
            )
            assertEquals(
                "eventVersion value",
                eventVersion.toLong(),
                content["eventVersion"]!!.contents,
            )
            assertEquals(
                "estimatedDistance value",
                estimatedDistance.toLong(),
                content["estimatedDistance"]!!.contents,
            )
            assertEquals(
                "estimatedDuration value",
                estimatedDuration.toLong(),
                content["estimatedDuration"]!!.contents,
            )
            assertEquals(
                "rerouteCount value",
                rerouteCount.toLong(),
                content["rerouteCount"]!!.contents,
            )
            assertEquals(
                "originalEstimatedDistance value",
                originalEstimatedDistance.toLong(),
                content["originalEstimatedDistance"]!!.contents,
            )
            assertEquals(
                "originalEstimatedDuration value",
                originalEstimatedDuration.toLong(),
                content["originalEstimatedDuration"]!!.contents,
            )
            assertEquals(
                "stepCount value",
                stepCount.toLong(),
                content["stepCount"]!!.contents,
            )
            assertEquals(
                "originalStepCount value",
                originalStepCount.toLong(),
                content["originalStepCount"]!!.contents,
            )
            assertEquals(
                "legIndex value",
                legIndex.toLong(),
                content["legIndex"]!!.contents,
            )
            assertEquals(
                "legCount value",
                legCount.toLong(),
                content["legCount"]!!.contents,
            )
            assertEquals(
                "stepIndex value",
                stepIndex.toLong(),
                content["stepIndex"]!!.contents,
            )
            assertEquals(
                "voiceIndex value",
                voiceIndex.toLong(),
                content["voiceIndex"]!!.contents,
            )
            assertEquals(
                "bannerIndex value",
                bannerIndex.toLong(),
                content["bannerIndex"]!!.contents,
            )
            assertEquals(
                "totalStepCount value",
                totalStepCount.toLong(),
                content["totalStepCount"]!!.contents,
            )
            assertTrue(content.containsKey("appMetadata"))
            content["appMetadata"]!!.verifyAppMetadata(appMetadata)
        }
    }

    fun Value.verifyAppMetadata(appMetadata: AppMetadata) {
        (contents as Map<String, Value>).let { appMetadataContent ->
            assertEquals(
                "appMetadata, name value",
                appMetadata.name,
                appMetadataContent["name"]!!.contents,
            )
            assertEquals(
                "appMetadata, version value",
                appMetadata.version,
                appMetadataContent["version"]!!.contents,
            )
            assertEquals(
                "appMetadata, sessionId value",
                appMetadata.sessionId,
                appMetadataContent["sessionId"]!!.contents,
            )
            assertEquals(
                "appMetadata, userId value",
                appMetadata.userId,
                appMetadataContent["userId"]!!.contents,
            )
        }
    }

    fun Value.verifyTelemetryLocation(location: TelemetryLocation) {
        (contents as Map<String, Value>).let { content ->
            assertEquals(
                "check TelemetryLocation: latitude value",
                location.latitude,
                content["lat"]!!.contents as Double,
                0.000001,
            )
            assertEquals(
                "check TelemetryLocation: longitude value",
                location.longitude,
                content["lng"]!!.contents as Double,
                0.000001,
            )
            assertEquals(
                "check TelemetryLocation: speed value",
                location.speed.toString().toDouble(),
                content["speed"]!!.contents as Double,
                0.001,
            )
            assertEquals(
                "check TelemetryLocation: bearing value",
                location.bearing.toString().toDouble(),
                content["course"]!!.contents as Double,
                0.001,
            )
            assertEquals(
                "check TelemetryLocation: altitude value",
                location.altitude,
                content["altitude"]!!.contents,
            )
            assertEquals(
                "check TelemetryLocation: timestamp value",
                location.timestamp,
                content["timestamp"]!!.contents,
            )
            assertEquals(
                "check TelemetryLocation: timestamp value",
                location.horizontalAccuracy.toString().toDouble(),
                content["horizontalAccuracy"]!!.contents as Double,
                0.001,
            )
            assertEquals(
                "check TelemetryLocation: verticalAccuracy value",
                location.verticalAccuracy.toString().toDouble(),
                content["verticalAccuracy"]!!.contents as Double,
                0.001,
            )
        }
    }

    fun Value.verifyTelemetryLocations(array: Array<TelemetryLocation>) {
        (contents!! as List<Value>).let { content ->
            assertEquals(array.size, content.size)
            content.forEachIndexed { index, value ->
                value.verifyTelemetryLocation(array[index])
            }
        }
    }
}
