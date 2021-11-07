package com.mapbox.navigation.ui.shield

import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.navigation.ui.shield.model.RouteSprite

internal sealed class RouteShieldResult {

    sealed class Sprites: RouteShieldResult() {
        data class Available(val sprite: RouteSprite): Sprites()
        object Unavailable: Sprites()
    }

    data class OnSpriteUrl(val url: String) : RouteShieldResult()

    sealed class GenerateSprite : RouteShieldResult() {
        data class Failure(val error: String) : GenerateSprite()
        data class Success(val sprite: RouteSprite) : GenerateSprite()
    }

    data class OnSprite(val sprite: ShieldSprite?) : RouteShieldResult()
}
