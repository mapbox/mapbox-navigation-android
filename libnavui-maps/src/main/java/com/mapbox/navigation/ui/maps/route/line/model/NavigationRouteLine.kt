@file:JvmName("NavigationRouteLineEx")

package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.Keep
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Represents a route and an optional identification of used for representing routes on the map.
 *
 * @param route a directions route
 * @param identifier an optional identifier for the directions route which can be used to
 * influence color of the route when it is an alternative route.
 */
@Keep
class NavigationRouteLine(val route: NavigationRoute, val identifier: String?) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationRouteLine

        if (route != other.route) return false
        return identifier == other.identifier
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + (identifier?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NavigationRouteLine(route=$route, identifier=$identifier)"
    }
}
