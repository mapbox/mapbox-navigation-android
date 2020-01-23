@file:JvmName("MapboxDirectionsUtils")

package com.mapbox.navigation.route.offboard.router

import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.SEMICOLON
import com.mapbox.navigation.base.extensions.checkFields
import com.mapbox.navigation.base.extensions.convertToListOfDoubles
import com.mapbox.navigation.base.extensions.convertToListOfPairsOfDoubles
import com.mapbox.navigation.base.extensions.parseWaypointIndices
import com.mapbox.navigation.base.extensions.parseWaypointTargets
import java.util.Locale
import okhttp3.Interceptor

private val EVENT_LISTENER = NavigationRouteEventListener()

fun MapboxDirections.Builder.routeOptions(options: RouteOptions): MapboxDirections.Builder {
    options.checkFields()

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
    options.radiuses()?.let { radiuses ->
        if (radiuses.isNotEmpty()) {
            radiuses.convertToListOfDoubles(SEMICOLON[0])?.toDoubleArray()?.let { result ->
                radiuses(*result)
            }
        }
    }

    options.bearings()?.let { bearings ->
        if (bearings.isNotEmpty()) {
            bearings.convertToListOfPairsOfDoubles()
                ?.forEach { pair ->
                    if (pair != null) {
                        addBearing(pair.first, pair.second)
                    } else {
                        addBearing(null, null)
                    }
                }
        }
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

    options.annotations()?.let {
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

    options.approaches()?.let { approaches ->
        if (approaches.isNotEmpty()) {
            val result =
                approaches.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            addApproaches(*result)
        }
    }

    options.waypointIndices()?.let { waypointIndices ->
        if (waypointIndices.isNotEmpty()) {
            val splitWaypointIndices = parseWaypointIndices(waypointIndices)
            addWaypointIndices(*splitWaypointIndices)
        }
    }

    options.waypointNames()?.let { waypointNames ->
        if (waypointNames.isNotEmpty()) {
            val names =
                waypointNames.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            addWaypointNames(*names)
        }
    }

    options.waypointTargets()?.let { waypointTargets ->
        if (waypointTargets.isNotEmpty()) {
            val splitWaypointTargets = parseWaypointTargets(waypointTargets)
            addWaypointTargets(*splitWaypointTargets)
        }
    }

    options.walkingOptions()?.let {
        walkingOptions(it)
    }

    eventListener(EVENT_LISTENER)

    return this
}

fun MapboxDirections.Builder.addInterceptor(interceptor: Interceptor): MapboxDirections.Builder {
    addInterceptor(interceptor)
    return this
}
