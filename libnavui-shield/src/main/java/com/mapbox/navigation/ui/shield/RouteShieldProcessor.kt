package com.mapbox.navigation.ui.shield

import com.google.gson.JsonSyntaxException
import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.navigation.ui.shield.model.RouteSprite

internal object RouteShieldProcessor {

    private val sprites: HashMap<String, List<ShieldSprite>> = hashMapOf()
    private const val MINIMUM_DISPLAY_REF_LENGTH = 2
    private const val SPRITE_JSON = "sprite.json"
    private const val REQUEST_ACCESS_TOKEN = "?access_token="
    private const val SPRITE_BASE_URL = "https://api.mapbox.com/styles/v1/"

    fun process(action: RouteShieldAction): RouteShieldResult {
        return when (action) {
            is RouteShieldAction.GenerateSpriteUrl -> {
                generateSpriteUrl(
                    userId = action.userId,
                    styleId = action.styleId,
                    accessToken = action.accessToken
                )
            }
            is RouteShieldAction.SpritesAvailable -> {
                isSpriteAvailable(url = action.url)
            }
            is RouteShieldAction.ParseSprite -> {
                generateSprite(url = action.url, json = action.json)
            }
            is RouteShieldAction.GetSprite -> {
                getSprite(
                    spriteUrl = action.spriteUrl,
                    shieldName = action.shieldName,
                    displayRef = action.displayRef)
            }
        }
    }

    private fun generateSpriteUrl(
        userId: String,
        styleId: String,
        accessToken: String
    ): RouteShieldResult.OnSpriteUrl {
        val url = SPRITE_BASE_URL
            .plus("$userId/")
            .plus("$styleId/")
            .plus(SPRITE_JSON)
            .plus(REQUEST_ACCESS_TOKEN)
            .plus(accessToken)
        return RouteShieldResult.OnSpriteUrl(url)
    }

    private fun isSpriteAvailable(url: String): RouteShieldResult.Sprites {
        return when (val shieldSprites = sprites[url]) {
            null -> { RouteShieldResult.Sprites.Unavailable }
            else -> {
                RouteShieldResult.Sprites.Available(
                    RouteSprite(spriteUrl = url, sprites = shieldSprites)
                )
            }
        }
    }

    private fun generateSprite(url: String, json: String): RouteShieldResult.GenerateSprite {
        return try {
            val shieldSprites = ShieldSprites.fromJson(json)
            sprites[url] = shieldSprites.sprites()
            RouteShieldResult.GenerateSprite.Success(
                sprite = RouteSprite(spriteUrl = url, sprites = shieldSprites.sprites())
            )
        } catch (exception: JsonSyntaxException) {
            RouteShieldResult.GenerateSprite.Failure(
                error = "Error in parsing json: $json"
            )
        }
    }

    private fun getSprite(
        spriteUrl: String,
        shieldName: String,
        displayRef: String
    ): RouteShieldResult.OnSprite {
        val refLen = getRefLen(displayRef)
        val shieldSprite = sprites[spriteUrl]?.find { shieldSprite ->
            shieldSprite.spriteName() == shieldName.plus("-$refLen")
        }
        return RouteShieldResult.OnSprite(shieldSprite)
    }

    private fun getRefLen(displayRef: String): Int {
        return if (displayRef.length <= 1) {
            MINIMUM_DISPLAY_REF_LENGTH
        } else {
            displayRef.length
        }
    }
}
