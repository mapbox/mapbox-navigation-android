package com.mapbox.navigation.ui.shield.model

import com.mapbox.api.directions.v5.models.ShieldSprite

internal data class RouteSprite(
    val spriteUrl: String,
    val sprites: List<ShieldSprite>
)
