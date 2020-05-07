package com.mapbox.navigation.base.options

/**
 * Defines endpoint's properties to retrieve tiles
 *
 * @param host tiles endpoint
 * @param version version of tiles
 * @param userAgent HttpClient UserAgent
 */
data class Endpoint(
    val host: String,
    val version: String,
    val userAgent: String
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = Builder(
        host, version, userAgent
    )

    /**
     * Build a new [Endpoint]
     *
     * @param host tile endpoint
     * @param version version of tiles
     * @param userAgent HttpClient UserAgent
     */
    data class Builder(
        private var host: String,
        private var version: String,
        private var userAgent: String
    ) {
        /**
         * Tiles endpoint
         */
        fun host(host: String) =
            apply { this.host = host }

        /**
         * Version of tiles
         */
        fun version(version: String) =
            apply { this.version = version }

        /**
         * HttpClient UserAgent
         */
        fun userAgent(userAgent: String) =
            apply { this.userAgent = userAgent }

        /**
         * Build the [Endpoint]
         */
        fun build() = Endpoint(host, version, userAgent)
    }
}
