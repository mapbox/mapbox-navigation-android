package com.mapbox.navigation.base.options

/**
 * Defines endpoint's properties to retrieve tiles
 *
 * @param host Tiles endpoint
 * @param version Version of tiles
 * @param token Token for tiles retrieving (in most cases mapbox accessToken)
 * @param userAgent HttpClient UserAgent
 */
data class Endpoint(
    val host: String,
    val version: String,
    val token: String,
    val userAgent: String
)
