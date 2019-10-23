package com.mapbox.navigation.metrics

import androidx.annotation.StringDef

interface DirectionsMetrics {

    companion object {
        const val REROUTE = "navigation.reroute"
        const val ROUTE_RETRIEVAL = "route_retrieval_event"
    }

    @StringDef(
        REROUTE,
        ROUTE_RETRIEVAL
    )
    annotation class MetricName

    fun rerouteEvent(eventName: String, eventJsonString: String)

    fun routeRetrievalEvent(eventName: String, eventJsonString: String)
}