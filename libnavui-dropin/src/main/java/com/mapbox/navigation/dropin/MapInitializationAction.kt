package com.mapbox.navigation.dropin

import com.mapbox.maps.MapboxMap

sealed class MapInitializationAction: Action {
    data class OnMapViewInitialized(val mapboxMap: MapboxMap): MapInitializationAction()
}
