package com.mapbox.services.android.navigation.v5.internal.navigation

import android.annotation.SuppressLint
import android.os.Parcelable

@SuppressLint("ParcelCreator")
internal data class RouteRetrievalEvent(
    private val elapsedTime: Double,
    private val routeUuid: String,
    private val sessionId: String,
    private val metadata: NavigationPerformanceMetadata?
) : NavigationPerformanceEvent(sessionId, ROUTE_RETRIEVAL_EVENT_NAME, metadata), Parcelable {

    companion object {
        private const val ELAPSED_TIME_NAME = "elapsed_time"
        private const val ROUTE_UUID_NAME = "route_uuid"
        private const val ROUTE_RETRIEVAL_EVENT_NAME = "route_retrieval_event"
    }

    init {
        addCounter(DoubleCounter(ELAPSED_TIME_NAME, elapsedTime))
        addAttribute(Attribute(ROUTE_UUID_NAME, routeUuid))
    }
}
