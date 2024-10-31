package com.mapbox.navigation.core.reroute

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.ResponseOriginAPI.Companion.DIRECTIONS_API
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE

internal typealias RouteProgressProvider = () -> RouteProgress?

private const val ROUTES_HISTORY_QUERY_PARAM = "routes_history"
private const val MAX_ROUTE_HISTORY_SIZE = 10
private const val LOG_TAG = "RouteHistoryOptionsAdapter"

internal class RouteHistoryOptionsAdapter(
    private val latestRouteProgressProvider: RouteProgressProvider,
) : InternalRerouteOptionsAdapter {
    override fun onRouteOptions(
        routeOptions: RouteOptions,
        params: RouteOptionsAdapterParams,
    ): RouteOptions {
        return try {
            return onRouteOptionsInternal(routeOptions)
        } catch (t: Throwable) {
            logE(LOG_TAG) {
                "Unhandled error: $t. Leaving original route options as is"
            }
            routeOptions
        }
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun onRouteOptionsInternal(
        routeOptions: RouteOptions,
    ): RouteOptions {
        val routeProgress = latestRouteProgressProvider()
        return if (routeProgress != null &&
            routeProgress.navigationRoute.origin == RouterOrigin.ONLINE &&
            routeProgress.navigationRoute.responseOriginAPI == DIRECTIONS_API &&
            routeOptions.profile() == PROFILE_DRIVING_TRAFFIC
        ) {
            addCurrentRouteToHistory(routeProgress, routeOptions)
        } else {
            routeOptions
        }
    }

    private fun addCurrentRouteToHistory(
        routeProgress: RouteProgress,
        routeOptions: RouteOptions,
    ): RouteOptions {
        val uuid = routeProgress.navigationRoute.responseUUID
        val routeIndex = routeProgress.navigationRoute.routeIndex
        val geometryIndex = routeProgress.currentRouteGeometryIndex
        val existingProperties = routeOptions.unrecognizedJsonProperties.orEmpty()
        val previousHistory = existingProperties[ROUTES_HISTORY_QUERY_PARAM]
            ?.let {
                it.asString.split(";").take(MAX_ROUTE_HISTORY_SIZE - 1)
                    .joinToString(separator = ";") { it }
            }
            ?.let { ";$it" }.orEmpty()
        logD(
            LOG_TAG,
            "Adding uuid=[$uuid], " +
                "routeIndex=[$routeIndex], " +
                "geometryIndex=[$geometryIndex] to history",
        )
        return routeOptions.toBuilder()
            .unrecognizedJsonProperties(
                existingProperties +
                    mapOf(
                        ROUTES_HISTORY_QUERY_PARAM
                            to
                                JsonPrimitive(
                                    "$uuid,$routeIndex,$geometryIndex$previousHistory",
                                ),
                    ),
            )
            .build()
    }
}

internal fun tripSessionRouteProgressProvider(tripSession: TripSession): RouteProgressProvider = {
    tripSession.getRouteProgress()
}
