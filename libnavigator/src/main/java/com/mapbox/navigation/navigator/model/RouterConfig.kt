package com.mapbox.navigation.navigator.model

data class RouterConfig(
    val tilePath: String,
    val inMemoryTileCache: Int?,
    val mapMatchingSpatialCache: Int?,
    val threadsCount: Int?,
    val endpointConfig: EndpointConfig?
)
