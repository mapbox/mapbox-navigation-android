package com.mapbox.navigation.ui.shield

internal sealed class RouteShieldAction {

    data class GenerateSpriteUrl(
        val userId: String,
        val styleId: String,
        val accessToken: String
    ) : RouteShieldAction()

    data class SpritesAvailable(
        val url: String,
    ): RouteShieldAction()

    data class ParseSprite(
        val url: String,
        val json: String
    ) : RouteShieldAction()

    data class GetSprite(
        val spriteUrl: String,
        val shieldName: String,
        val displayRef: String
    ) : RouteShieldAction()
}
