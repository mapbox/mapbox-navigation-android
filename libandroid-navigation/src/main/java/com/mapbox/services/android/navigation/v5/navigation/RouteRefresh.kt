package com.mapbox.services.android.navigation.v5.navigation

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull

/**
 * This class allows the developer to interact with the Directions Refresh API, receiving updated
 * annotations for a route previously requested with the enableRefresh flag.
 *
 * Creates a [RouteRefresh] object.
 *
 * @param accessToken mapbox access token
 */
class RouteRefresh(private val accessToken: String, private val context: Context) {

    companion object {
        private const val INVALID_DIRECTIONS_ROUTE =
            "RouteProgress passed has invalid DirectionsRoute"
    }

    /**
     * Refreshes the [DirectionsRoute] included in the [RouteProgress] and returns it
     * to the callback that was originally passed in. The client will then have to update their
     * [DirectionsRoute] with the [com.mapbox.api.directions.v5.models.LegAnnotation]s
     * returned in this response. The leg annotations start at the current leg index of the
     * [RouteProgress]
     *
     * @param routeProgress to refresh via the route and current leg index
     * @param refreshCallback to call with updated routes
     */
    fun refresh(routeProgress: RouteProgress, refreshCallback: RefreshCallback) {
        ifNonNull(routeProgress.directionsRoute(), routeProgress.legIndex()) { directionsRoute, legIndex ->
            refresh(directionsRoute, legIndex, refreshCallback)
        }
    }

    private fun refresh(
        directionsRoute: DirectionsRoute,
        legIndex: Int,
        refreshCallback: RefreshCallback
    ) {
        if (isInvalid(directionsRoute, refreshCallback)) {
            return
        }
        val builder: MapboxDirectionsRefresh.Builder = MapboxDirectionsRefresh.builder()
            .requestId(directionsRoute.routeOptions()?.requestUuid())
        directionsRoute.routeIndex()?.let { routeIndex ->
            builder.routeIndex(routeIndex.toInt())
        }
        builder.legIndex(legIndex)
            .accessToken(accessToken)
            .build().enqueueCall(RouteRefreshCallback(directionsRoute, legIndex, refreshCallback))
    }

    private fun isInvalid(
        directionsRoute: DirectionsRoute,
        refreshCallback: RefreshCallback
    ): Boolean {
        val requestUuid = directionsRoute.routeOptions()?.requestUuid() ?: ""
        if (requestUuid.isEmpty() || directionsRoute.routeIndex() == null) {
            refreshCallback.onError(RefreshError(INVALID_DIRECTIONS_ROUTE))
            return true
        }
        return false
    }
}
