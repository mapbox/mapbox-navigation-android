package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Wrapper of Route refresh state result. See [RouteRefreshStatesObserver]
 * @param state route refresh state. See [RouteRefreshExtra.RouteRefreshState]
 * @param message string represented message (optional)
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteRefreshStateResult internal constructor(
    @RouteRefreshExtra.RouteRefreshState val state: String,
    val message: String? = null,
) {
    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteRefreshStateResult

        if (state != other.state) return false
        if (message != other.message) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteRefreshStateResult(state='$state', message=$message)"
    }
}
