package com.mapbox.navigation.ui.shield.internal.loader

import com.google.gson.JsonSyntaxException
import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.shield.internal.RoadShieldDownloader

internal class ShieldSpritesDownloader : ResourceDownloader<String, ShieldSprites>() {
    override suspend fun download(argument: String): Expected<String, ShieldSprites> {
        val result = RoadShieldDownloader.download(argument)
        return try {
            result.mapValue { data ->
                val spriteJson = String(data)
                val shieldSprites = ShieldSprites.fromJson(spriteJson)
                shieldSprites
            }
        } catch (exception: JsonSyntaxException) {
            val json = result.value?.let { String(it) } ?: "null"
            ExpectedFactory.createError(
                """
                    |Error parsing shield sprites:
                    |exception: $exception
                    |json: $json
                """.trimMargin()
            )
        }
    }
}
