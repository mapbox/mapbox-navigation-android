package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.dropin.model.Action

/**
 * Defines actions responsible to mutate the [RoutesState].
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class RoutesAction : Action {
    /**
     * The action is used to fetch route based on the list of [points].
     * @param points list of points
     */
    data class FetchPoints(val points: List<Point>) : RoutesAction()

    /**
     * The action is used to fetch route based on [RouteOptions].
     * @param options
     */
    data class FetchOptions(val options: RouteOptions) : RoutesAction()

    /**
     * The action is used to directly set the [NavigationRoute] supplied to NavigationView.
     * @param routes list of [NavigationRoute]
     */
    data class SetRoutes(val routes: List<NavigationRoute>) : RoutesAction()

    /**
     * The action informs that route request was successful and routes are ready to be used.
     * @param routes list of [NavigationRoute]
     */
    data class Ready(val routes: List<NavigationRoute>) : RoutesAction()

    /**
     * The action informs that the route request failed.
     * @param reasons for why the request failed
     * @param routeOptions used to fetch the route
     */
    data class Failed(
        val reasons: List<RouterFailure>,
        val routeOptions: RouteOptions
    ) : RoutesAction()

    /**
     * The action informs that the route request was canceled.
     * @param routeOptions used to fetch the route
     * @param routerOrigin origin of the route request
     */
    data class Canceled(
        val routeOptions: RouteOptions,
        val routerOrigin: RouterOrigin
    ) : RoutesAction()
}
