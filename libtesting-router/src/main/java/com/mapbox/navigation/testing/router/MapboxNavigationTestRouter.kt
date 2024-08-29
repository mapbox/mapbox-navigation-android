package com.mapbox.navigation.testing.router

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Implement this interface to handle route requests from the Navigation SDK.
 */
@ExperimentalMapboxNavigationAPI
interface MapboxNavigationTestRouter {
    /**
     * This method is called when the Navigation SDK requests a route.
     * @param routeOptions Options of requested route
     * @param callback Callback which should be notified when the route is ready
     */
    fun getRoute(routeOptions: RouteOptions, callback: RouterCallback)
}

/**
 * A callback to call when a route or an error is ready to be sent to the Nav SDK.
 */
@ExperimentalMapboxNavigationAPI
interface RouterCallback {
    /**
     * Call this method to return calculated route to the Nav SDK.
     * @param response Model of response from Mapbox Directions API.
     */
    fun onRoutesReady(response: DirectionsResponse)

    /**
     * Call this method to return an error on route request to the Nav SDK.
     * @param failure A description of the failure which happen during route request.
     */
    fun onFailure(failure: TestRouterFailure)
}

/**
 * Represents an error which could happen during route calculation.
 * Use factory methods to create a specific failures.
 */
@ExperimentalMapboxNavigationAPI
class TestRouterFailure internal constructor(
    internal val httpCode: Int,
    internal val errorBody: String,
) {
    companion object {
        /**
         * A failure which Directions API returns when a route can't be calculated.
         */
        fun noRoutesFound() = TestRouterFailure(
            200,
            "{\"code\":\"NoRoute\",\"message\":\"No route found\",\"routes\":[]}",
        )

        /**
         * A generic server error.
         */
        fun serverError() = TestRouterFailure(
            500,
            "server error",
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestRefresherFailure

        if (httpCode != other.httpCode) return false
        if (errorBody != other.errorBody) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = httpCode.hashCode()
        result = 31 * result + errorBody.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TestRefresherFailure(httpCode=$httpCode, errorBody='$errorBody')"
    }
}

@ExperimentalMapboxNavigationAPI
internal class DefaultRouter : MapboxNavigationTestRouter {
    override fun getRoute(routeOptions: RouteOptions, callback: RouterCallback) {
        callback.onFailure(TestRouterFailure.noRoutesFound())
    }
}
