package com.mapbox.navigation.ui.maps.arrival.api

import com.mapbox.common.Logger
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver

/**
 * When approaching a waypoint or the final destination, this api is used to
 * highlight the building in 3D. The building will be highlighted if the final
 * destination coordinate is inside the building geometry.
 */
class MapboxBuildingArrivalApi {
    private var mapboxNavigation: MapboxNavigation? = null
    private var mapboxBuildingHighlightApi: MapboxBuildingHighlightApi? = null

    /**
     * Bind a [MapboxBuildingHighlightApi] to arrival.
     *   null will clear the previous highlighted building
     */
    fun buildingHighlightApi(mapboxBuildingHighlightApi: MapboxBuildingHighlightApi?) = apply {
        this.mapboxBuildingHighlightApi?.clear()
        this.mapboxBuildingHighlightApi = mapboxBuildingHighlightApi
    }

    /**
     * Enables the [MapboxBuildingHighlightApi] upon arrival
     */
    fun enable(mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigation?.unregisterArrivalObserver(arrivalObserver)
        this.mapboxNavigation = mapboxNavigation
        mapboxNavigation.registerArrivalObserver(arrivalObserver)
    }

    /**
     * Disables the [MapboxBuildingHighlightApi] upon arrival
     */
    fun disable() {
        mapboxNavigation?.unregisterArrivalObserver(arrivalObserver)
        mapboxBuildingHighlightApi?.clear()
    }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            checkMapStyle()
            mapboxBuildingHighlightApi?.highlightBuilding(null)
        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
            checkMapStyle()
            val routeOptions = routeProgress.route.routeOptions()
            val coordinateIndex = routeProgress.currentLegProgress?.legIndex!! + 1
            routeOptions?.coordinatesList()?.get(coordinateIndex)?.let { point ->
                mapboxBuildingHighlightApi?.highlightBuilding(point)
            }
        }

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            checkMapStyle()
            val finalDestination = routeProgress.route.routeOptions()
                ?.coordinatesList()
                ?.lastOrNull()
            mapboxBuildingHighlightApi?.highlightBuilding(finalDestination)
        }
    }

    private fun checkMapStyle() {
        mapboxBuildingHighlightApi ?: Logger.w(
            "BuildingArrivalApi",
            "Set the map style, see BuildingArrivalApi.onStyleLoaded"
        )
    }
}
