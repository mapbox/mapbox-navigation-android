package com.mapbox.navigation.core

import androidx.annotation.StringDef
import androidx.annotation.UiThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin

/**
 * Callback for replanning routes request result.
 */
@UiThread
@ExperimentalPreviewMapboxNavigationAPI
interface ReplanRoutesCallback {
    /**
     * Called when new routes have been fetched and set.
     *
     * @param routes list of new routes received
     * @param routerOrigin origin of the route request matching values from [RouterOrigin]
     */
    fun onNewRoutes(routes: List<NavigationRoute>, @RouterOrigin routerOrigin: String)

    /**
     * Called when the route replan request fails or gets interrupted.
     *
     * @param failure details about the failure
     */
    fun onFailure(failure: ReplanRouteError)
}

/**
 * Represents a failure to replan the route.
 *
 * @param errorType type of the error, defined by [ReplanRouteError.ReplanRouteErrorType].
 * @param message error message describing the failure.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ReplanRouteError internal constructor(
    @ReplanRouteErrorType val errorType: String,
    val message: String,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplanRouteError

        if (errorType != other.errorType) return false
        if (message != other.message) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = errorType.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ReplanRouteError(" +
            "errorType='$errorType', " +
            "message=$message)"
    }

    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        REPLAN_ROUTE_ERROR,
        REPLAN_ROUTE_INTERRUPTED,
    )
    annotation class ReplanRouteErrorType

    companion object {
        const val REPLAN_ROUTE_ERROR: String = "REPLAN_ROUTE_INTERNAL_ERROR"
        const val REPLAN_ROUTE_INTERRUPTED: String = "REPLAN_ROUTE_INTERRUPTED"
    }
}
