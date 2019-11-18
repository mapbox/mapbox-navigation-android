package com.mapbox.navigation.route.onboard.model

import com.mapbox.navigation.navigator.model.RouterConfig

data class Config(
    val tilePath: String,
    val inMemoryTileCache: Int? = null,
    val mapMatchingSpatialCache: Int? = null,
    val threadsCount: Int? = null,
    val endpoint: Endpoint? = null
)

fun Config.mapToRouteConfig(): RouterConfig =
    RouterConfig(
        tilePath = tilePath,
        inMemoryTileCache = inMemoryTileCache,
        mapMatchingSpatialCache = mapMatchingSpatialCache,
        threadsCount = threadsCount,
        endpointConfig = endpoint?.mapToEndpointConfig()
    )
