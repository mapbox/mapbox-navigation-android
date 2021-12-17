package com.mapbox.navigation.ui.shield.internal.model

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
    abstract val url: String

    data class MapboxDesign(
        val shieldSpriteToDownload: ShieldSpriteToDownload,
        val accessToken: String,
        val mapboxShield: MapboxShield,
        val legacyFallback: MapboxLegacy? = null
    ) : RouteShieldToDownload() {

        override val url: String = this.mapboxShield.baseUrl()
            .plus("/${this.shieldSpriteToDownload.userId}")
            .plus("/${this.shieldSpriteToDownload.styleId}")
            .plus(SPRITE)
            .plus("/${this.mapboxShield.name()}")
            .plus("-${getRefLen(this.mapboxShield.displayRef())}")
            .plus(REQUEST_ACCESS_TOKEN)
            .plus(accessToken)
    }

    /**
     * @param initialUrl url returned by the Navigation API which misses file extension
     */
    data class MapboxLegacy(
        private val initialUrl: String
    ) : RouteShieldToDownload() {

        override val url: String = initialUrl.plus(SVG_EXTENSION)
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

internal fun RouteShieldToDownload.MapboxDesign.getSpriteFrom(
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
