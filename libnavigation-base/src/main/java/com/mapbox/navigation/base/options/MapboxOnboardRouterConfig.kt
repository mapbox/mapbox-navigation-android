package com.mapbox.navigation.base.options

/**
 * Defines configuration for on-board router
 *
 * @param tilePath Path where tiles will be stored to / Path where tiles will be fetched from
 * @param inMemoryTileCache Max size of tiles cache (optional)
 * @param mapMatchingSpatialCache Max size of cache for map matching (optional)
 * @param threadsCount Max count of native threads (optional)
 * @param endpoint Endpoint config (optional)
 */
data class MapboxOnboardRouterConfig(
    val tilePath: String,
    val inMemoryTileCache: Int? = null,
    val mapMatchingSpatialCache: Int? = null,
    val threadsCount: Int? = null,
    val endpoint: Endpoint? = null
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = Builder(
        tilePath,
        inMemoryTileCache,
        mapMatchingSpatialCache,
        threadsCount,
        endpoint
    )

    /**
     * Builder for [MapboxOnboardRouterConfig].
     */
    data class Builder(
        private var tilePath: String,
        private var inMemoryTileCache: Int? = null,
        private var mapMatchingSpatialCache: Int? = null,
        private var threadsCount: Int? = null,
        private var endpoint: Endpoint? = null
    ) {
        /**
         * Path where tiles will be stored to / Path where tiles will be fetched from
         */
        fun tilePath(tilePath: String) =
            apply { this.tilePath = tilePath }

        fun inMemoryTileCache(inMemoryTileCache: Int?) =
            apply { this.inMemoryTileCache = inMemoryTileCache }

        fun mapMatchingSpatialCache(mapMatchingSpatialCache: Int?) =
            apply { this.mapMatchingSpatialCache = mapMatchingSpatialCache }

        /**
         * Max count of native threads
         */
        fun threadsCount(threadsCount: Int?) =
            apply { this.threadsCount = threadsCount }

        /**
         * Endpoint config
         */
        fun endpoint(endpoint: Endpoint?) =
            apply { this.endpoint = endpoint }

        /**
         * Build the [MapboxOnboardRouterConfig]
         */
        fun build() = MapboxOnboardRouterConfig(
            tilePath, inMemoryTileCache, mapMatchingSpatialCache, threadsCount, endpoint
        )
    }
}
