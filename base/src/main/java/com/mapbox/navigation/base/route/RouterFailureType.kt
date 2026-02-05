package com.mapbox.navigation.base.route

import androidx.annotation.StringDef

/**
 * Describes possible [RouterFailure] types.
 */
@Retention(AnnotationRetention.BINARY)
@StringDef(
    RouterFailureType.THROTTLING_ERROR,
    RouterFailureType.INPUT_ERROR,
    RouterFailureType.NETWORK_ERROR,
    RouterFailureType.AUTHENTICATION_ERROR,
    RouterFailureType.ROUTE_CREATION_ERROR,
    RouterFailureType.ROUTE_EXPIRY_ERROR,
    RouterFailureType.RESPONSE_PARSING_ERROR,
    RouterFailureType.ROUTER_RECREATION_ERROR,
    RouterFailureType.MISSING_TILES_ERROR,
    RouterFailureType.UNKNOWN_ERROR,
)
annotation class RouterFailureType {

    /**
     * Object which contains failure type constants
     */
    companion object {

        /**
         * Error caused by too many requests to router
         */
        const val THROTTLING_ERROR = "THROTTLING_ERROR"

        /**
         * Unsupported request arguments or URL parsing error
         */
        const val INPUT_ERROR = "INPUT_ERROR"

        /**
         * Error which happened on network transport side, for example,
         * Connection error, SSL error, Time out, etc.
         */
        const val NETWORK_ERROR = "NETWORK_ERROR"

        /**
         * Authentication error, check the access token / account
         */
        const val AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR"

        /**
         * Critical errors from Router, for example,
         * NoRoute, NoSegment, NoChargersNearby. Fallback to onboard router unsupported
         */
        const val ROUTE_CREATION_ERROR = "ROUTE_CREATION_ERROR"

        /**
         * Route refresh update failed due to server losing the route cache due to expiry.
         */
        const val ROUTE_EXPIRY_ERROR = "ROUTE_EXPIRY_ERROR"

        /**
         * Server returned response with a route which could not be parsed.
         */
        const val RESPONSE_PARSING_ERROR = "RESPONSE_PARSING_ERROR"

        /**
         * Indicates that router was recreated for some internal reason.
         */
        const val ROUTER_RECREATION_ERROR = "ROUTER_RECREATION_ERROR"

        /**
         * Error caused by missing tiles required for route calculation
         */
        const val MISSING_TILES_ERROR = "MISSING_TILES_ERROR"

        /**
         * Error has an unknown type
         */
        const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
    }
}
