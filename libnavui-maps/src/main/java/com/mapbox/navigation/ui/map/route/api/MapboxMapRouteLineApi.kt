package com.mapbox.navigation.ui.map.route.api

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.navigation.ui.base.map.route.api.MapRouteLineApi
import com.mapbox.navigation.ui.base.map.route.model.RouteLineOptions
import com.mapbox.navigation.ui.base.map.route.model.RouteLineState
import com.mapbox.navigation.ui.maps.R

object MapboxMapRouteLineApi : MapRouteLineApi {

    override fun createDefaultOptions(context: Context) = RouteLineOptions.Builder()
        .primaryRouteColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_primary_route_color
            )
        )
        .primaryShieldColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_primary_route_shield_color
            )
        )
        .primaryCongestionLightColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_primary_route_color
            )
        )
        .primaryCongestionModerateColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_primary_route_congestion_yellow
            )
        )
        .primaryCongestionHeavyColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_primary_route_congestion_red
            )
        )
        .primaryCongestionSevereColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_primary_route_congestion_red
            )
        )
        .alternativeRouteColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_alternative_route_color
            )
        )
        .alternativeShieldColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_alternative_route_shield_color
            )
        )
        .alternativeCongestionLightColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_alternative_route_color
            )
        )
        .alternativeCongestionModerateColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_alternative_route_congestion_yellow
            )
        )
        .alternativeCongestionHeavyColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_alternative_route_congestion_red
            )
        )
        .alternativeCongestionSevereColor(
            ContextCompat.getColor(
                context,
                R.color.mapbox_navigation_alternative_route_congestion_red
            )
        )
        .build()

    override fun getState(context: Context, options: RouteLineOptions?): RouteLineState =
        RouteLineState(
            options = options ?: createDefaultOptions(context),
            features = emptyList()
        )

    override fun getState(
        previousState: RouteLineState,
        options: RouteLineOptions
    ): RouteLineState = previousState.copy(options = options)

    @WorkerThread
    override fun getState(previousState: RouteLineState, directionsRoute: DirectionsRoute) =
        getState(previousState, listOf(directionsRoute))

    @WorkerThread
    override fun getState(
        previousState: RouteLineState,
        directionsRoutes: List<DirectionsRoute>
    ) = previousState.copy(features = generateFeatures(directionsRoutes))

    private fun generateFeatures(routes: List<DirectionsRoute>): List<Feature> {
        return routes.map {
            Feature.fromGeometry(
                LineString.fromPolyline(
                    it.geometry() ?: "",
                    6 // todo hardcoded
                )
            )
        }
    }
}

@WorkerThread
fun RouteLineState.withNewRoute(directionsRoute: DirectionsRoute): RouteLineState =
    MapboxMapRouteLineApi.getState(this, directionsRoute)

@WorkerThread
fun RouteLineState.withNewRoutes(directionsRoutes: List<DirectionsRoute>): RouteLineState =
    MapboxMapRouteLineApi.getState(this, directionsRoutes)

fun RouteLineState.withNewOptions(
    block: RouteLineOptions.Builder.() -> Unit
): RouteLineState = MapboxMapRouteLineApi.getState(
        this,
        options.toBuilder().apply(block).build()
    )
