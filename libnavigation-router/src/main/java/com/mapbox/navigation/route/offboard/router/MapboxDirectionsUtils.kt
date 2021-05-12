package com.mapbox.navigation.route.offboard.router

import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.RouteOptions
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
    options: RouteOptions,
    refreshEnabled: Boolean
): MapboxDirections.Builder {

    enableRefresh(refreshEnabled)

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

    options.alternatives()?.let {
        alternatives(it)
    }

    options.language()?.let {
        language(Locale(it))
    }
    options.radiusesList()?.let {
        radiuses(it)
    }

    options.bearingsList()?.let { it ->
        bearings(it)
    }

    options.continueStraight()?.let {
        continueStraight(it)
    }

    options.roundaboutExits()?.let {
        roundaboutExits(it)
    }

    options.geometries()?.let {
        geometries(it)
    }

    options.overview()?.let {
        overview(it)
    }

    options.steps()?.let {
        steps(it)
    }

    options.annotationsList()?.let {
        annotations(it)
    }

    options.voiceInstructions()?.let {
        voiceInstructions(it)
    }

    options.voiceInstructions()?.let {
        voiceInstructions(it)
    }

    options.bannerInstructions()?.let {
        bannerInstructions(it)
    }

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

    options.snappingClosuresList()?.let {
        snappingClosures(it)
    }

    eventListener(EVENT_LISTENER)

    return this
}
