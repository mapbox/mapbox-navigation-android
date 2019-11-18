package com.mapbox.navigation.route.onboard.model

import com.mapbox.navigation.navigator.model.EndpointConfig

data class Endpoint(
    val host: String,
    val version: String,
    val token: String,
    val userAgent: String
)

fun Endpoint.mapToEndpointConfig() =
    EndpointConfig(
        host = host,
        version = version,
        token = token,
        userAgent = userAgent
    )
