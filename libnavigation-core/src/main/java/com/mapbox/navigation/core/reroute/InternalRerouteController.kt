package com.mapbox.navigation.core.reroute

import androidx.annotation.UiThread
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin

@UiThread
internal interface InternalRerouteController : NavigationRerouteController {

    fun reroute(callback: RoutesCallback)

    @UiThread
    fun interface RoutesCallback {

        fun onNewRoutes(rerouteResult: RerouteResult)
    }
}

internal class RerouteResult internal constructor(
    val routes: List<NavigationRoute>,
    val initialLegIndex: Int,
    val origin: RouterOrigin,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RerouteResult

        if (routes != other.routes) return false
        if (initialLegIndex != other.initialLegIndex) return false
        if (origin != other.origin) return false

        return true
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
