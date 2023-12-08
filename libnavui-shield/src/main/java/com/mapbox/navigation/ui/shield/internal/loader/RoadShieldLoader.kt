package com.mapbox.navigation.ui.shield.internal.loader

import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.api.directions.v5.models.ShieldSvg
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.ui.shield.internal.model.generateSpriteSheetUrl
import com.mapbox.navigation.ui.shield.internal.model.getSpriteFrom
import com.mapbox.navigation.ui.shield.model.RouteShield

internal class RoadShieldLoader(
    private val spritesLoader: Loader<String, ShieldSprites>,
    private val imageLoader: Loader<String, ByteArray>
) : Loader<RouteShieldToDownload, RouteShield> {

    override suspend fun load(input: RouteShieldToDownload): Expected<Error, RouteShield> {
        return when (input) {
            is RouteShieldToDownload.MapboxDesign -> loadMapboxDesignShield(input)
            is RouteShieldToDownload.MapboxLegacy -> loadMapboxLegacyShield(input)
        }
    }

    private suspend fun loadMapboxDesignShield(
        toDownload: RouteShieldToDownload.MapboxDesign
    ): Expected<Error, RouteShield> {
        val spriteUrl = toDownload.generateSpriteSheetUrl()
        val shieldSpritesResult = spritesLoader.load(spriteUrl)
        val shieldSprites = if (shieldSpritesResult.isValue) {
            shieldSpritesResult.value!!
        } else {
            return createError(
                """
                    Error when downloading image sprite.
                    url: $spriteUrl
                    result: ${shieldSpritesResult.error?.message}
                """.trimIndent()
            )
        }
        val sprite = toDownload.getSpriteFrom(shieldSprites)
            ?: return createError(
                "Sprite not found for ${toDownload.mapboxShield.name()} in $shieldSprites."
            )
        val placeholder = sprite.spriteAttributes().placeholder()
        if (placeholder.isNullOrEmpty()) {
            return createError(
                """
                    Mapbox shield sprite placeholder was null or empty in: $sprite
                """.trimIndent()
            )
        }

        val mapboxShieldUrl = toDownload.url
        return imageLoader.load(mapboxShieldUrl).mapValue { shieldByteArray ->
            val svgJson = String(shieldByteArray)
            val svg = appendTextToShield(
                text = toDownload.mapboxShield.displayRef(),
                shieldSvg = ShieldSvg.fromJson(svgJson).svg(),
                textColor = toDownload.mapboxShield.textColor(),
                placeholder = placeholder
            ).toByteArray()
            RouteShield.MapboxDesignedShield(
                mapboxShieldUrl,
                svg,
                toDownload.mapboxShield,
                sprite
            )
        }
    }

    private fun appendTextToShield(
        text: String,
        shieldSvg: String,
        textColor: String,
        placeholder: List<Double>
    ): String {
        val textTagX = placeholder[0] + placeholder[2] / 2
        val textTagY = placeholder[3]
        val textSize = placeholder[3] - placeholder[1] + 3
        val shieldText = "\t<text x=\"$textTagX\" y=\"$textTagY\" " +
            "font-family=\"Arial, Helvetica, sans-serif\" font-weight=\"bold\" " +
            "text-anchor=\"middle\" font-size=\"$textSize\" fill=\"$textColor\">$text</text>"
        return shieldSvg.replace("</svg>", shieldText.plus("</svg>"))
    }

    private suspend fun loadMapboxLegacyShield(
        toDownload: RouteShieldToDownload.MapboxLegacy
    ): Expected<Error, RouteShield> {
        val shieldUrl = toDownload.url
        return imageLoader.load(shieldUrl).mapValue { byteArray ->
            RouteShield.MapboxLegacyShield(
                toDownload.url,
                byteArray,
                toDownload.initialUrl
            )
        }
    }

    private fun <V> createError(errorMessage: String): Expected<Error, V> =
        ExpectedFactory.createError(Error(errorMessage))
}
