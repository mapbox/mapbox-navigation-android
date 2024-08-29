package com.mapbox.navigation.core.telemetry

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.text.TextUtils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.common.TelemetrySystemUtils
import com.mapbox.common.location.Location
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.core.telemetry.audio.AudioTypeChain
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlin.math.floor

private const val PERCENT_NORMALIZER = 100.0
private const val SCREEN_BRIGHTNESS_MAX = 255.0
private const val BRIGHTNESS_EXCEPTION_VALUE = -1

/**
 * Encode route geometry to *precision 5*
 */
internal fun obtainGeometry(directionsRoute: DirectionsRoute?): String =
    ifNonNull(directionsRoute, directionsRoute?.geometry()) { route, geometry ->
        if (TextUtils.isEmpty(geometry)) {
            return@ifNonNull ""
        }
        val positions = route.completeGeometryToPoints()
        return@ifNonNull PolylineUtils.encode(positions, Constants.PRECISION_5)
    } ?: ""

/**
 * Provide a count of the steps in a whole route, including the legs
 *
 * @see [com.mapbox.api.directions.v5.models.LegStep]
 */
internal fun obtainStepCount(directionsRoute: DirectionsRoute?): Int =
    ifNonNull(directionsRoute, directionsRoute?.legs()) { _, legs ->
        var stepCount = 0
        for (leg in legs) {
            stepCount += leg.steps()?.size ?: 0
        }
        return@ifNonNull stepCount
    } ?: 0

/**
 * Provide the absolute distance between 2 points, not including the geometry
 *
 * @return Int distance unit meter
 */
internal fun obtainAbsoluteDistance(
    currentLocation: Location?,
    finalPoint: Point,
): Int {
    val currentPoint = currentLocation?.let {
        Point.fromLngLat(currentLocation.longitude, currentLocation.latitude)
    }
    return obtainAbsoluteDistance(currentPoint, finalPoint)
}

/**
 * Provide the absolute distance between 2 points, not including the geometry
 *
 * @return Int distance unit meter
 */
internal fun obtainAbsoluteDistance(
    currentPoint: Point?,
    finalPoint: Point,
): Int {
    currentPoint?.let {
        return TurfMeasurement.distance(it, finalPoint, TurfConstants.UNIT_METERS).toInt()
    } ?: return 0
}

/**
 * Provide the last [Point] in a [DirectionsRoute]
 */
internal fun obtainRouteDestination(route: DirectionsRoute?): Point =
    route?.legs()?.lastOrNull()?.steps()?.lastOrNull()?.maneuver()?.location()
        ?: Point.fromLngLat(0.0, 0.0)

/**
 * Provide the volume level in the percentages(range is *0..100*)
 */
internal fun obtainVolumeLevel(context: Context): Int {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return floor(
        PERCENT_NORMALIZER * audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) /
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
    ).toInt()
}

/**
 * Provide screen brightness in range *0..100*
 */
internal fun obtainScreenBrightness(context: Context): Int =
    try {
        val systemScreenBrightness = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
        )
        calculateScreenBrightnessPercentage(systemScreenBrightness)
    } catch (exception: Settings.SettingNotFoundException) {
        BRIGHTNESS_EXCEPTION_VALUE
    }

/**
 * Provide audio type
 * @see [com.mapbox.navigation.core.telemetry.audio.AudioTypeResolver]
 */
internal fun obtainAudioType(context: Context): String =
    AudioTypeChain().setup().obtainAudioType(context)

private fun calculateScreenBrightnessPercentage(screenBrightness: Int): Int =
    floor(PERCENT_NORMALIZER * screenBrightness / SCREEN_BRIGHTNESS_MAX).toInt()

internal fun navObtainUniversalSessionId(): String =
    TelemetrySystemUtils.obtainUniversalUniqueIdentifier()

internal fun navObtainUniversalTelemetryNavigationSessionId(): String =
    TelemetrySystemUtils.obtainUniversalUniqueIdentifier()

internal fun navObtainUniversalTelemetryNavigationModeId(): String =
    TelemetrySystemUtils.obtainUniversalUniqueIdentifier()

internal fun navObtainUniversalTelemetryTripId(): String =
    TelemetrySystemUtils.obtainUniversalUniqueIdentifier()
