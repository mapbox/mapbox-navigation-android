package com.mapbox.navigation.ui.shield.model

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite

internal data class RouteShieldToDownload(
    val text: String,
    val legacyShieldUrl: String?,
    val mapboxShieldUrl: String?,
    val shieldSprite: ShieldSprite?,
    val mapboxShield: MapboxShield?
)
