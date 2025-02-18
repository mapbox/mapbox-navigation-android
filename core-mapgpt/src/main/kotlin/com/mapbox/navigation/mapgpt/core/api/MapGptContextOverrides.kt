package com.mapbox.navigation.mapgpt.core.api

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Function that can override the context values.
 */
typealias MapGptOverrideFunction<T> = (T) -> T

/**
 * Gives an ability to override the context.
 */
class MapGptContextOverrides {
    val appContext = MutableStateFlow<MapGptOverrideFunction<MapGptAppContextDTO>?>(null)
    val userContext = MutableStateFlow<MapGptOverrideFunction<MapGptUserContextDTO>?>(null)
    val vehicleContext = MutableStateFlow<MapGptOverrideFunction<MapGptVehicleContextDTO>?>(null)
    val routeContext = MutableStateFlow<MapGptOverrideFunction<MapGptRouteContextDTO>?>(null)
    val evSearchContext = MutableStateFlow<MapGptOverrideFunction<MapGptEVContextDTO>?>(null)
}
