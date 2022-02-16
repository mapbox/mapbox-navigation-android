package com.mapbox.navigation.dropin.component.routefetch

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

object MapboxDropInRouteRequester {
    private val routeRequestSink: MutableSharedFlow<List<Point>> =
        MutableSharedFlow(onBufferOverflow = BufferOverflow.DROP_OLDEST, extraBufferCapacity = 1)
    val routeRequests: Flow<List<Point>> = routeRequestSink

    private val routeRequestFromOptionsSink: MutableSharedFlow<RouteOptions> =
        MutableSharedFlow(onBufferOverflow = BufferOverflow.DROP_OLDEST, extraBufferCapacity = 1)
    val routeOptionsRequests: Flow<RouteOptions> = routeRequestFromOptionsSink

    private val routeSetRequestSink: MutableSharedFlow<List<DirectionsRoute>> =
        MutableSharedFlow(onBufferOverflow = BufferOverflow.DROP_OLDEST, extraBufferCapacity = 1)
    val setRouteRequests: Flow<List<DirectionsRoute>> = routeSetRequestSink

    fun setRoutes(routes: List<DirectionsRoute>) {
        routeSetRequestSink.tryEmit(routes)
    }

    fun fetchAndSetRoute(points: List<Point>) {
        routeRequestSink.tryEmit(points)
    }

    fun fetchAndSetRoute(routeOptions: RouteOptions) {
        routeRequestFromOptionsSink.tryEmit(routeOptions)
    }
}
