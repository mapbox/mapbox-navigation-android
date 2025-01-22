package com.mapbox.navigation.tripdata.shield.internal.model

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite

private const val SPRITE = "/sprite"
private const val SPRITE_BASE_URL = "https://api.mapbox.com/styles/v1/"
private const val SPRITE_JSON = "sprite.json"
private const val REQUEST_ACCESS_TOKEN = "?access_token="
private const val SVG_EXTENSION = ".svg"

sealed class RouteShieldToDownload {

    data class MapboxDesign(
        val shieldSpriteToDownload: ShieldSpriteToDownload,
        val accessToken: String,
        val mapboxShield: MapboxShield,
        val legacyFallback: MapboxLegacy? = null,
    ) : RouteShieldToDownload() {

        fun generateUrl(sprite: ShieldSprite): String = this.mapboxShield.baseUrl()
            .plus("/${this.shieldSpriteToDownload.userId}")
            .plus("/${this.shieldSpriteToDownload.styleId}")
            .plus(SPRITE)
            .plus("/${sprite.spriteName()}")
            .plus(REQUEST_ACCESS_TOKEN)
            .plus(accessToken)
    }

    /**
     * @param initialUrl url returned by the Navigation API which misses file extension
     */
    data class MapboxLegacy(
        val initialUrl: String,
    ) : RouteShieldToDownload() {

        val url: String = initialUrl.plus(SVG_EXTENSION)
    }
}

internal fun RouteShieldToDownload.MapboxDesign.generateSpriteSheetUrl(): String {
    return SPRITE_BASE_URL
        .plus("${this.shieldSpriteToDownload.userId}/")
        .plus("${this.shieldSpriteToDownload.styleId}/")
        .plus(SPRITE_JSON)
        .plus(REQUEST_ACCESS_TOKEN)
        .plus(accessToken)
}

internal data class SizeSpecificSpriteInfo(
    val name: String,
    val length: Int,
    val originalSprite: ShieldSprite,
)

internal fun RouteShieldToDownload.MapboxDesign.getSpriteFrom(
    spriteInfos: List<SizeSpecificSpriteInfo>,
): ShieldSprite? {
    val sortedSuitableInfos = spriteInfos
        .filter { it.name == this.mapboxShield.name() }
        .sortedBy { it.length }
    if (sortedSuitableInfos.isEmpty()) {
        return null
    }
    val result = sortedSuitableInfos.firstOrNull {
        this.mapboxShield.displayRef().length <= it.length
    } ?: sortedSuitableInfos.last()
    return result.originalSprite
}

data class ShieldSpriteToDownload(
    val userId: String,
    val styleId: String,
)
