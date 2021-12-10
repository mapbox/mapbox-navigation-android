package com.mapbox.navigation.ui.shield.model

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite

internal sealed class RouteShieldToDownload(
    val url: String?
) {
    class MapboxDesign(
        url: String?,
        val shieldSprite: ShieldSprite?,
        val mapboxShield: MapboxShield?,
        val legacy: MapboxLegacy? = null
    ) : RouteShieldToDownload(url)

    class MapboxLegacy(
        url: String?
    ) : RouteShieldToDownload(url)
}
