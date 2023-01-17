package com.mapbox.navigation.ui.shield.internal.loader

import com.google.gson.JsonSyntaxException
import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.navigation.ui.shield.internal.RoadShieldDownloader

internal class ShieldSpritesDownloader : Downloader<String, ShieldSprites>() {
    override suspend fun download(input: String): Expected<Error, ShieldSprites> {
        val result = RoadShieldDownloader.download(input)
        return try {
            result.mapValue { data ->
                val spriteJson = String(data)
                val shieldSprites = ShieldSprites.fromJson(spriteJson)
                shieldSprites
            }
        } catch (exception: JsonSyntaxException) {
            val json = result.value?.let { String(it) } ?: "null"
            createError(
                Error(
                    """
                    |Error parsing shield sprites:
                    |exception: $exception
                    |json: $json
                    """.trimMargin(),
                    exception
                )
            )
        }
    }
}
