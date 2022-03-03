package com.mapbox.navigation.dropin.lifecycle

import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap

sealed class UICommand {

    sealed class MapCommand: UICommand() {
        data class OnMapClicked(
            val point: Point,
            val map: MapboxMap,
            val padding: Float
        ): MapCommand()
        data class OnMapLongClicked(
            val point: Point,
            val map: MapboxMap,
            val padding: Float
        ): MapCommand()
    }

    sealed class RoutesCommand: UICommand() {
        data class FetchRoute(
            val origin: Point,
            val destination: Point,
            val waypoint: List<Point>? = null,
        ): RoutesCommand()
        data class FetchRouteFromCurrentLocation(
            val destination: Point,
            val waypoint: List<Point>? = null,
        ): RoutesCommand()
    }

    sealed class RouteLineCommand: UICommand() {
        data class SelectRoute(
            val point: Point,
            val map: MapboxMap,
            val clickPadding: Float
        ): RouteLineCommand()
    }
}
