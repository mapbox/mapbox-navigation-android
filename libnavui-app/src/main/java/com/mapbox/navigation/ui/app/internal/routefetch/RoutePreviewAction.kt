package com.mapbox.navigation.ui.app.internal.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.ui.app.internal.Action

sealed class RoutePreviewAction : Action {
    /**
     * The action is used to fetch route based on the list of [points].
     * @param points list of points
     */
    data class FetchPoints(val points: List<Point>) : RoutePreviewAction()

    /**
     * The action is used to request and set routes based on [RouteOptions].
     * @param options
     */
    data class FetchOptions(val options: RouteOptions) : RoutePreviewAction()

    /**
     * The action informs that route request was successful and routes are ready to be used.
     * @param routes list of [NavigationRoute]
     */
    data class Ready(val routes: List<NavigationRoute>) : RoutePreviewAction()

    /**
     * The action informs that the route request failed.
     * @param reasons for why the request failed
     * @param routeOptions used to fetch the route
     */
    data class Failed(
        val reasons: List<RouterFailure>,
        val routeOptions: RouteOptions
    ) : RoutePreviewAction()

    /**
     * The action informs that the route request was canceled.
     * @param routeOptions used to fetch the route
     * @param routerOrigin origin of the route request
     */
    data class Canceled(
        val routeOptions: RouteOptions,
        val routerOrigin: RouterOrigin
    ) : RoutePreviewAction()

    /**
     * The actions informs that fetch route request has started.
     * @param requestId of the route requested
     */
    data class StartedFetchRequest(val requestId: Long) : RoutePreviewAction()
}
