package com.mapbox.navigation.core

import com.mapbox.navigation.core.internal.congestions.TrafficOverrideHandler
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.routerefresh.RouteRefreshController
import com.mapbox.navigation.core.routerefresh.RoutesRefresherResult

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
     * Currently this can only be trigger internally by a response to [RerouteController.RoutesCallback].
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
     * Triggered when primary route changed and previous primary became alternative.
     */
    internal data class Reorder(val legIndex: Int) : SetRoutes()

    /**
     * Triggered when the **routes do not change but are refreshed**.
     *
     * Currently this can only be trigger internally by a response to [RouteRefreshController.refresh]
     * or applying traffic filtration in [TrafficOverrideHandler]
     */
    internal sealed class RefreshRoutes : SetRoutes() {
        internal data class ExternalRefresh(
            val legIndex: Int,
            val isManual: Boolean,
        ) : RefreshRoutes()
        internal data class RefreshControllerRefresh(
            val routeRefreshResult: RoutesRefresherResult,
        ) : RefreshRoutes()
    }
}
