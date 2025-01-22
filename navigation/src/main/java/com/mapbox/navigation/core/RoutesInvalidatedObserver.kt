package com.mapbox.navigation.core

import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Observer that is notified of routes being invalidated.
 */
fun interface RoutesInvalidatedObserver {

    /**
     * Invoked when routes are invalidated.
     * It means that specified routes won't be refreshed anymore:
     * you won't receive traffic updates, incident updates, etc.
     * It is recommended to rebuild a route once it becomes invalidated.
     * Note: if route A becomes invalidated and you continue using it instead of rebuilding it,
     * you won't be notified the second time that this route is no longer valid:
     * you'll only be notified once about a specific route being invalidated.
     *
     * @param params [RoutesInvalidatedParams]
     */
    fun onRoutesInvalidated(params: RoutesInvalidatedParams)
}

/**
 * Class containing information about invalidated routes.
 *
 * @param invalidatedRoutes list of routes that became invalidated
 */
class RoutesInvalidatedParams internal constructor(
    val invalidatedRoutes: List<NavigationRoute>,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesInvalidatedParams

        if (invalidatedRoutes != other.invalidatedRoutes) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return invalidatedRoutes.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RoutesInvalidatedParams(invalidatedRoutes=$invalidatedRoutes)"
    }
}
