package com.mapbox.navigation.route.onboard.model

data class Config(
    val tilePath: String,
    val inMemoryTileCache: Int? = null,
    val mapMatchingSpatialCache: Int? = null,
    val threadsCount: Int? = null,
    val endpoint: Endpoint? = null
)
