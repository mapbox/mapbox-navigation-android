package com.mapbox.navigation.ui.shield.model

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.api.directions.v5.models.ShieldSprites

private const val MINIMUM_DISPLAY_REF_LENGTH = 2
private const val MAXIMUM_DISPLAY_REF_LENGTH = 6
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
        val legacy: MapboxLegacy? = null
    ) : RouteShieldToDownload()

    data class MapboxLegacy(
        val url: String,
        val accessToken: String,
    ) : RouteShieldToDownload()
}

internal fun RouteShieldToDownload.MapboxDesign.generateShieldUrl(): String {
    val refLen = getRefLen(this.mapboxShield.displayRef())
    return this.mapboxShield.baseUrl()
        .plus(this.shieldSpriteToDownload.userId)
        .plus("/${this.shieldSpriteToDownload.styleId}")
        .plus(SPRITE)
        .plus("/${this.mapboxShield.name()}")
        .plus("-$refLen")
        .plus(REQUEST_ACCESS_TOKEN)
        .plus(accessToken)
}

internal fun RouteShieldToDownload.MapboxDesign.generateSpriteSheetUrl(): String {
    return SPRITE_BASE_URL
        .plus("${this.shieldSpriteToDownload.userId}/")
        .plus("${this.shieldSpriteToDownload.styleId}/")
        .plus(SPRITE_JSON)
        .plus(REQUEST_ACCESS_TOKEN)
        .plus(accessToken)
}

internal fun RouteShieldToDownload.MapboxLegacy.generateShieldUrl(): String {
    return url
        .plus(SVG_EXTENSION)
        .plus(REQUEST_ACCESS_TOKEN)
        .plus(accessToken)
}

fun RouteShieldToDownload.MapboxDesign.getSpriteFrom(
    shieldSprites: ShieldSprites
): ShieldSprite? {
    val refLen = getRefLen(this.mapboxShield.displayRef())
    return shieldSprites.sprites().find { shieldSprite ->
        shieldSprite.spriteName() == this.mapboxShield.name().plus("-$refLen")
    }
}

private fun getRefLen(displayRef: String): Int {
    return when {
        displayRef.length <= 1 -> {
            MINIMUM_DISPLAY_REF_LENGTH
        }
        displayRef.length > 6 -> {
            MAXIMUM_DISPLAY_REF_LENGTH
        }
        else -> {
            displayRef.length
        }
    }
}

data class ShieldSpriteToDownload(
    val userId: String,
    val styleId: String,
)
