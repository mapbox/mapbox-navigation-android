package com.mapbox.navigation.ui.base.map.route.api

import android.content.Context
import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.ui.base.map.route.model.RouteLineOptions
import com.mapbox.navigation.ui.base.map.route.model.RouteLineState

interface MapRouteLineApi {

    fun createDefaultOptions(context: Context): RouteLineOptions

    fun getState(context: Context, options: RouteLineOptions? = null): RouteLineState

    fun getState(previousState: RouteLineState, options: RouteLineOptions): RouteLineState

    @WorkerThread
    fun getState(previousState: RouteLineState, directionsRoute: DirectionsRoute): RouteLineState

    @WorkerThread
    fun getState(
        previousState: RouteLineState,
        directionsRoutes: List<DirectionsRoute>
    ): RouteLineState
}
