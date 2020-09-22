package com.mapbox.navigation.route.offboard.router

import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.extensions.supportsRefresh
import java.util.Locale

private val EVENT_LISTENER = NavigationRouteEventListener()

/**
 * Apply [RouteOptions] to [MapboxDirections.Builder]
 *
 * @receiver MapboxDirections.Builder
 * @param options RouteOptions
 * @return MapboxDirections.Builder
 */
internal fun MapboxDirections.Builder.routeOptions(
    options: RouteOptions
): MapboxDirections.Builder {
    check(options.coordinates().size >= 2) { "At least 2 coordinates should be provided." }

    baseUrl(options.baseUrl())
    user(options.user())
    profile(options.profile())

    options.coordinates().forEachIndexed { index, point ->
        when (index) {
            0 -> origin(point)
            options.coordinates().size - 1 -> destination(point)
            else -> addWaypoint(point)
        }
    }

    alternatives(options.alternatives() ?: false)

    options.language()?.let {
        language(Locale(it))
    }
    options.radiusesList()?.let {
        radiuses(it)
    }

    options.bearingsList()?.let { it ->
        bearings(it)
    }

    continueStraight(options.continueStraight() ?: false)
    roundaboutExits(options.roundaboutExits() ?: false)

    options.geometries()?.let {
        geometries(options.geometries())
    }

    options.overview()?.let {
        overview(it)
    }

    steps(options.steps() ?: true)

    options.annotationsList()?.let {
        annotations(it)
    }

    voiceInstructions(options.voiceInstructions() ?: true)
    bannerInstructions(options.bannerInstructions() ?: true)

    options.voiceUnits()?.let {
        voiceUnits(it)
    }

    accessToken(options.accessToken())

    options.requestUuid().let {
        // TODO Check if needed as it is only set at response time
    }

    options.exclude()?.let {
        exclude(it)
    }

    options.approachesList()?.let { it ->
        approaches(it)
    }

    options.waypointIndicesList()?.let { it ->
        waypointIndices(it)
    }

    options.waypointNamesList()?.let { it ->
        waypointNames(it)
    }

    options.waypointTargetsList()?.let { it ->
        waypointTargets(it)
    }

    options.walkingOptions()?.let {
        walkingOptions(it)
    }

    enableRefresh(options.supportsRefresh())

    departAt(options.departAt())
    arriveBy(options.arriveBy())

    eventListener(EVENT_LISTENER)

    return this
}
