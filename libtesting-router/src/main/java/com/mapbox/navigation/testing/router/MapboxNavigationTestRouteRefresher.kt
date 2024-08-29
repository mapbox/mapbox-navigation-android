package com.mapbox.navigation.testing.router

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * Implement this interface to handle route refresh requests from the Nav SDK.
 */
@ExperimentalMapboxNavigationAPI
interface MapboxNavigationTestRouteRefresher {
    /**
     * This method is called when the Nav SDK makes a route refresh request.
     * @param options Options which indicate which route to refresh.
     * @param callback A callback which should be called when refreshed route is ready or in case
     * of error.
     */
    fun getRouteRefresh(
        options: RefreshOptions,
        callback: RouteRefreshCallback,
    )
}

/**
 * Options which indicates which route is being refreshed.
 * @property responseUUID is UUID of route response, see [DirectionsResponse.uuid].
 * @property routeIndex is an index of the route in [DirectionsResponse.routes] list.
 */
@ExperimentalMapboxNavigationAPI
class RefreshOptions internal constructor(
    val responseUUID: String,
    val routeIndex: Int,
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RefreshOptions

        if (responseUUID != other.responseUUID) return false
        if (routeIndex != other.routeIndex) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = responseUUID.hashCode()
        result = 31 * result + routeIndex.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RefreshOptions(responseUUID='$responseUUID', routeIndex=$routeIndex)"
    }
}

/**
 * A callback to call when a route is refreshed or an error is ready to be sent to the Nav SDK.
 */
@ExperimentalMapboxNavigationAPI
interface RouteRefreshCallback {
    /**
     * Call this method to return a refreshed route to the Nav SDK.
     *
     * The [directionsRoute] won't be return to the Nav SDK as is. The local web server calculates
     * data to send to the Nav SDK based on provided route and route request form the Nav SDK.
     * For example current position could affect which data will be refreshed on the route.
     * The specific implementation of route refresh could vary from version to version.
     * @param directionsRoute A refreshed route.
     */
    fun onRefresh(directionsRoute: DirectionsRoute)

    /**
     * Call this method to return an error on route refresh.
     * @param failure A description of the failure which happened during route refresh.
     */
    fun onFailure(failure: TestRefresherFailure)
}

/**
 * Represents an error which could happen during route refresh.
 * Use factory methods to create a specific failures.
 */
@ExperimentalMapboxNavigationAPI
class TestRefresherFailure internal constructor(
    internal val httpCode: Int,
    internal val errorBody: String,
) {
    companion object {
        /**
         * A generic server error.
         */
        fun serverError() = TestRefresherFailure(
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
internal class DefaultRefresher : MapboxNavigationTestRouteRefresher {
    override fun getRouteRefresh(options: RefreshOptions, callback: RouteRefreshCallback) {
        callback.onFailure(TestRefresherFailure.serverError())
    }
}
