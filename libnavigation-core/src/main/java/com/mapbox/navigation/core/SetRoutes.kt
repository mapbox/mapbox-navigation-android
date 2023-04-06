package com.mapbox.navigation.core

import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.reroute.NavigationRerouteController
import com.mapbox.navigation.core.routerefresh.RouteRefreshController

internal sealed class SetRoutes {

    /**
     * Triggered when all routes are cleared.
     */
    internal object CleanUp : SetRoutes()

    /**
     * Triggered when the **primary route changes** via the call to [MapboxNavigation.setNavigationRoutes].
     * Alternatives might've changed as well (and typically do).
     */
    internal data class NewRoutes(
        val legIndex: Int,
    ) : SetRoutes()

    /**
     * Triggered when the **primary route changes** due to a reroute.
     * Alternatives might've changed as well (and typically do).
     *
     * Currently this can only be trigger internally by a response to [NavigationRerouteController.RoutesCallback].
     */
    internal data class Reroute(
        val legIndex: Int,
    ) : SetRoutes()

    /**
     * Triggered when the **alternative routes change** via the call to [MapboxNavigation.setNavigationRoutes]
     * but the **primary route has remained the same**.
     */
    internal data class Alternatives(
        val legIndex: Int,
    ) : SetRoutes()

    /**
     * Triggered when the **routes do not change but are refreshed**.
     *
     * Currently this can only be trigger internally by a response to [RouteRefreshController.refresh].
     */
    internal data class RefreshRoutes(
        val routeProgressData: RouteProgressData
    ) : SetRoutes()
}
