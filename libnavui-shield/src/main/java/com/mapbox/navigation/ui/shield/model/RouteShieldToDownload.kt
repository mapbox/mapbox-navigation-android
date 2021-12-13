package com.mapbox.navigation.ui.shield.model

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite

sealed class RouteShieldToDownload {
    class MapboxDesign(
        val mapboxShield: MapboxShield?,
        val legacy: MapboxLegacy? = null
    ) : RouteShieldToDownload()

    class MapboxLegacy(
        val url: String?
    ) : RouteShieldToDownload()
}
