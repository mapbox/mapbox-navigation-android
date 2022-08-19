package com.mapbox.navigation.ui.app.internal.routefetch

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.app.internal.Action

/**
 * Defines actions responsible to mutate the [RoutePreviewState].
 */
sealed class RoutesAction : Action {

    /**
     * The action is used to directly set the [NavigationRoute] supplied to NavigationView.
     * @param routes list of [NavigationRoute]
     */
    data class SetRoutes(val routes: List<NavigationRoute>) : RoutesAction()

    /**
     * The action is used to directly set the [NavigationRoute] supplied to NavigationView.
     * @param routes list of [NavigationRoute]
     * @param legIndex index of route leg
     */
    data class SetRoutesWithIndex(
        val routes: List<NavigationRoute>,
        val legIndex: Int
    ) : RoutesAction()
}
