@file:JvmName("RouteOptionsExtensions")

package com.mapbox.navigation.base.extensions

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import java.util.Locale

/**
 * Applies the [RouteOptions] that are required for the route request for the selected profile to execute
 * or otherwise recommended for the Navigation SDK and all of its features to provide the best car navigation experience.
 * See also [https://docs.mapbox.com/api/navigation/directions/](https://docs.mapbox.com/api/navigation/directions/).
 *
 * Default profile is [DirectionsCriteria.PROFILE_DRIVING_TRAFFIC].
 *
 * @see DirectionsCriteria.ProfileCriteria
 */
@JvmOverloads
fun RouteOptions.Builder.applyDefaultNavigationOptions(
    @DirectionsCriteria.ProfileCriteria profile: String =
        DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
): RouteOptions.Builder = apply {
    val defaultAnnotations = listOf(
        DirectionsCriteria.ANNOTATION_SPEED,
        DirectionsCriteria.ANNOTATION_DURATION,
        DirectionsCriteria.ANNOTATION_DISTANCE,
    )

    when (profile) {
        DirectionsCriteria.PROFILE_DRIVING_TRAFFIC -> {
            annotationsList(
                mutableListOf(
                    DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                    DirectionsCriteria.ANNOTATION_MAXSPEED,
                    DirectionsCriteria.ANNOTATION_CLOSURE,
                ).apply { addAll(defaultAnnotations) },
            )
            continueStraight(true)
            enableRefresh(true)
        }
        DirectionsCriteria.PROFILE_DRIVING -> {
            annotationsList(
                mutableListOf(
                    DirectionsCriteria.ANNOTATION_MAXSPEED,
                ).apply { addAll(defaultAnnotations) },
            )
            continueStraight(true)
            enableRefresh(false)
        }
        DirectionsCriteria.PROFILE_CYCLING,
        DirectionsCriteria.PROFILE_WALKING,
        -> {
            annotationsList(defaultAnnotations)
            continueStraight(false)
            enableRefresh(false)
        }
        else -> throw IllegalArgumentException(
            "Unknown profile [$profile]. It must be one [DirectionsCriteria.ProfileCriteria]",
        )
    }

    profile(profile)
    overview(DirectionsCriteria.OVERVIEW_FULL)
    steps(true)
    roundaboutExits(true)
    voiceInstructions(true)
    bannerInstructions(true)
}

/**
 * Applies the [RouteOptions] that adapt the returned instructions' language and voice unit based on the device's [Locale].
 */
fun RouteOptions.Builder.applyLanguageAndVoiceUnitOptions(context: Context): RouteOptions.Builder =
    apply {
        language(context.inferDeviceLocale().language)
        voiceUnits(context.inferDeviceLocale().getUnitTypeForLocale().value)
    }

/**
 * Takes a list of [Point]s and correctly adds them as waypoints in the correct order.
 *
 * @receiver RouteOptions.Builder
 * @param origin Point
 * @param waypoints List<Point?>?
 * @param destination Point
 * @return RouteOptions.Builder
 */
@JvmOverloads
fun RouteOptions.Builder.coordinates(
    origin: Point,
    waypoints: List<Point>? = null,
    destination: Point,
): RouteOptions.Builder {
    val coordinates = mutableListOf<Point>().apply {
        add(origin)
        waypoints?.forEach { add(it) }
        add(destination)
    }

    coordinatesList(coordinates)

    return this
}
