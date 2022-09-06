package com.mapbox.navigation.ui.app.internal.routefetch

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.app.internal.Action

/**
 * Defines actions responsible to mutate the [RoutePreviewState].
 */
sealed class RoutesAction : Action {

    /**
     * The action is used to directly set the [NavigationRoute] supplied to NavigationView.
     * @param routes list of [NavigationRoute]
     * @param legIndex optional index of [RouteLeg]
     */
    data class SetRoutes(val routes: List<NavigationRoute>, val legIndex: Int = 0) : RoutesAction()
}
