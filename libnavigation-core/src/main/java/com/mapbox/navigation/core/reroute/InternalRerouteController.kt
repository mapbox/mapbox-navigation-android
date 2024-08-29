package com.mapbox.navigation.core.reroute

import androidx.annotation.UiThread
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin

@UiThread
internal abstract class InternalRerouteController : RerouteController() {

    /**
     * Invoked when re-route is not needed anymore (for instance when driver returns to previous route).
     * Might be ignored depending on [RerouteState] e.g. if a route has been fetched it does not make sense to interrupt re-routing
     */
    abstract fun interrupt()

    abstract fun rerouteOnDeviation(callback: RoutesCallback)
    abstract fun rerouteOnParametersChange(callback: RoutesCallback)

    @UiThread
    fun interface RoutesCallback {

        fun onNewRoutes(rerouteResult: RerouteResult)
    }
}

internal class RerouteResult internal constructor(
    val routes: List<NavigationRoute>,
    val initialLegIndex: Int,
    @RouterOrigin
    val origin: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RerouteResult

        if (routes != other.routes) return false
        if (initialLegIndex != other.initialLegIndex) return false
        return origin == other.origin
    }

    override fun hashCode(): Int {
        var result = routes.hashCode()
        result = 31 * result + initialLegIndex
        result = 31 * result + origin.hashCode()
        return result
    }

    override fun toString(): String {
        return "RerouteResult(routes=$routes, initialLegIndex=$initialLegIndex, origin=$origin)"
    }
}
