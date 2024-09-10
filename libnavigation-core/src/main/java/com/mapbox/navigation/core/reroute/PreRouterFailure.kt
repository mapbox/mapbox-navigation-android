package com.mapbox.navigation.core.reroute

internal data class PreRouterFailure(
    val message: String,
    val isRetryable: Boolean,
)
