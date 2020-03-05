package com.mapbox.navigation.core.telemetry

import android.content.Context
import android.location.Location
import android.media.AudioManager
import android.provider.Settings
import android.text.TextUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.core.telemetry.events.TelemetryStep
import com.mapbox.navigation.utils.audio.AudioTypeChain
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlin.math.floor

private const val PRECISION_6 = 6
private const val PRECISION_5 = 5
private const val PERCENT_NORMALIZER = 100.0
private const val SCREEN_BRIGHTNESS_MAX = 255.0
private const val BRIGHTNESS_EXCEPTION_VALUE = -1

fun populateTelemetryStep(legProgress: RouteLegProgress): TelemetryStep {
    return TelemetryStep(
        legProgress.upcomingStep()?.maneuver()?.instruction() ?: "",
        legProgress.upcomingStep()?.maneuver()?.type() ?: "",
        legProgress.upcomingStep()?.maneuver()?.type() ?: "",
        legProgress.upcomingStep()?.name() ?: "",

        legProgress.currentStepProgress()?.step()?.maneuver()?.instruction() ?: "",
        legProgress.currentStepProgress()?.step()?.maneuver()?.type() ?: "",
        legProgress.currentStepProgress()?.step()?.maneuver()?.type() ?: "",
        legProgress.currentStepProgress()?.step()?.name() ?: "",

        legProgress.currentStepProgress()?.distanceTraveled()?.toInt() ?: 0,
        legProgress.currentStepProgress()?.durationRemaining()?.toInt() ?: 0,
        legProgress.upcomingStep()?.distance()?.toInt() ?: 0,
        legProgress.upcomingStep()?.duration()?.toInt() ?: 0
    )
}

fun obtainGeometry(directionsRoute: DirectionsRoute?): String =
    ifNonNull(directionsRoute, directionsRoute?.geometry()) { _, geometry ->
        if (TextUtils.isEmpty(geometry)) {
            return@ifNonNull ""
        }
        val positions = PolylineUtils.decode(geometry, PRECISION_6)
        return@ifNonNull PolylineUtils.encode(positions, PRECISION_5)
    } ?: ""

fun obtainStepCount(directionsRoute: DirectionsRoute?): Int =
    ifNonNull(directionsRoute, directionsRoute?.legs()) { _, legs ->
        var stepCount = 0
        for (leg in legs) {
            stepCount += leg.steps()?.size ?: 0
        }
        return@ifNonNull stepCount
    } ?: 0

fun obtainAbsoluteDistance(
    currentLocation: Location,
    finalPoint: Point
): Int {
    val currentPoint = Point.fromLngLat(currentLocation.longitude, currentLocation.latitude)
    return TurfMeasurement.distance(currentPoint, finalPoint, TurfConstants.UNIT_METERS)
        .toInt()
}

fun obtainRouteDestination(route: DirectionsRoute?): Point =
    route?.legs()?.lastOrNull()?.steps()?.lastOrNull()?.maneuver()?.location()
        ?: Point.fromLngLat(0.0, 0.0)

fun obtainVolumeLevel(context: Context): Int {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return floor(
        PERCENT_NORMALIZER * audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) /
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    ).toInt()
}

fun obtainScreenBrightness(context: Context): Int =
    try {
        val systemScreenBrightness = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        calculateScreenBrightnessPercentage(systemScreenBrightness)
    } catch (exception: Settings.SettingNotFoundException) {
        BRIGHTNESS_EXCEPTION_VALUE
    }

fun obtainAudioType(context: Context): String =
    AudioTypeChain().setup().obtainAudioType(context)

private fun calculateScreenBrightnessPercentage(screenBrightness: Int): Int =
    floor(PERCENT_NORMALIZER * screenBrightness / SCREEN_BRIGHTNESS_MAX).toInt()
