package com.mapbox.navigation.base.internal.route

import com.mapbox.navigation.base.route.NavigationRoute

data class RoutesResponse(
    val routes: List<NavigationRoute>,
    val meta: Metadata,
) {
    data class Metadata(
        /**
         * Time of parsing completion.
         * Required to calculate delays of data availability on the main thread.
         */
        val createdAtElapsedMillis: Long,
        /**
         * The waiting time in the scheduler between receiving a response from the server and
         * response parsing start.
         */
        val responseWaitMillis: Long,
        /**
         * Duration of response parsing.
         */
        val responseParseMillis: Long,
        /**
         * Thread name where response parsing took place.
         */
        val responseParseThread: String,
        /**
         * The waiting time in the scheduler between receiving a response from the server and
         * native parsing start.
         */
        val nativeWaitMillis: Long,
        /**
         * Duration of native parsing.
         */
        val nativeParseMillis: Long,
        /**
         * The waiting time in the scheduler between receiving a response from the server and
         * route options parsing start.
         */
        val routeOptionsWaitMillis: Long,
        /**
         * Duration of route options parsing.
         */
        val routeOptionsParseMillis: Long,
    )
}
