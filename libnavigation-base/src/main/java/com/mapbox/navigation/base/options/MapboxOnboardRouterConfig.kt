package com.mapbox.navigation.base.options

/**
 * @param tilePath Path where tiles will be stored / Path where tiles will be fetched
 * @param inMemoryTileCache Int?
 * @param mapMatchingSpatialCache Int?
 * @param threadsCount Max count of native threads(optional)
 * @param endpoint Endpoint config
 */
data class MapboxOnboardRouterConfig(
    val tilePath: String,
    val inMemoryTileCache: Int? = null,
    val mapMatchingSpatialCache: Int? = null,
    val threadsCount: Int? = null,
    val endpoint: Endpoint? = null
)
