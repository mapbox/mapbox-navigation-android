package com.mapbox.navigation.ui.shield.model

data class RouteShieldOrigin(
    val isFallback: Boolean,
    val originalUrl: String?,
    val errorMessage: String? = null
)
