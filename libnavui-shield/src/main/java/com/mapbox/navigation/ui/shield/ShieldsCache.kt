package com.mapbox.navigation.ui.shield

import android.util.LruCache
import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.api.directions.v5.models.ShieldSvg
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.ui.shield.internal.model.generateSpriteSheetUrl
import com.mapbox.navigation.ui.shield.internal.model.getSpriteFrom
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal abstract class ResourceCache<Argument, Value>(cacheSize: Int) {

    internal companion object {
        internal const val CANCELED_MESSAGE = "canceled"
    }

    private val cache = LruCache<Argument, Expected<String, Value>>(cacheSize)
    private val ongoingRequest = mutableSetOf<Argument>()
    private val awaitingCallbacks = mutableListOf<() -> Boolean>()

    /**
     * Returns an [Expected] with value or error depending on whether the resource was generated
     * successfully or not.
     *
     * The provided **[argument] is considered as a unique key** for the cache entry.
     * Whenever there's a successful result generated for a given [argument],
     * that result is saved to the cache and returned immediately upon calls to [getOrRequest] with the same [argument] again.
     *
     * If a successful result for given [argument] is not available in the cache, the result will be attempted to be generated.
     * If the generation is asynchronous, any [getOrRequest] calls with the same [argument] will suspend
     * until the already ongoing request returns to avoid duplicating work.
     *
     * If the generation fails, the erroneous result is returned to original and all awaiting callers.
     * Once [getOrRequest] with the same [argument] is called again, the generation will be re-attempted.
     *
     * Calls to this function should be executed from a single thread, it's not thread safe.
     */
    suspend fun getOrRequest(argument: Argument): Expected<String, Value> {
        return cache.get(argument)?.value?.let { ExpectedFactory.createValue(it) } ?: run {
            if (ongoingRequest.contains(argument)) {
                suspendCancellableCoroutine { continuation ->
                    /**
                     * The callback checks if the result for this argument is available.
                     * Returns if true or keeps waiting if false.
                     */
                    val callback = {
                        ifNonNull(cache.get(argument)) { expected ->
                            if (!continuation.isCancelled) {
                                continuation.resume(expected)
                            }
                            true
                        } ?: false
                    }

                    // immediately verify if the result is available in case it was generated
                    // while we were initializing the coroutine
                    if (callback()) {
                        return@suspendCancellableCoroutine
                    }

                    // if the result is not available, wait
                    awaitingCallbacks.add(callback)
                    continuation.invokeOnCancellation {
                        awaitingCallbacks.remove(callback)
                    }
                }
            } else {
                try {
                    ongoingRequest.add(argument)
                    val result = obtainResource(argument)
                    cache.put(argument, result)
                    ongoingRequest.remove(argument)
                    invalidate()
                    result
                } catch (ex: CancellationException) {
                    ongoingRequest.remove(argument)
                    val result = ExpectedFactory.createError<String, Value>(CANCELED_MESSAGE)
                    cache.put(argument, result)
                    invalidate()
                    result
                }
            }
        }
    }

    /**
     * Notifies all awaiting callbacks that the result might be available.
     */
    private fun invalidate() {
        val iterator = awaitingCallbacks.iterator()
        while (iterator.hasNext()) {
            val remove = iterator.next().invoke()
            if (remove) {
                iterator.remove()
            }
        }
    }

    /**
     * Produces a result for a given [argument] if it was not found in the cache.
     */
    protected abstract suspend fun obtainResource(argument: Argument): Expected<String, Value>
}

internal class ShieldResultCache(
    private val shieldSpritesCache: ShieldSpritesCache = ShieldSpritesCache(),
    private val shieldByteArrayCache: ShieldByteArrayCache = ShieldByteArrayCache(),
) : ResourceCache<RouteShieldToDownload, RouteShield>(40) {
    override suspend fun obtainResource(
        argument: RouteShieldToDownload
    ): Expected<String, RouteShield> {
        return when (argument) {
            is RouteShieldToDownload.MapboxDesign -> prepareMapboxDesignShield(argument)
            is RouteShieldToDownload.MapboxLegacy -> prepareMapboxLegacyShield(argument)
        }
    }

    private suspend fun prepareMapboxDesignShield(
        toDownload: RouteShieldToDownload.MapboxDesign
    ): Expected<String, RouteShield> {
        val spriteUrl = toDownload.generateSpriteSheetUrl()
        val shieldSpritesResult = shieldSpritesCache.getOrRequest(spriteUrl)
        val shieldSprites = if (shieldSpritesResult.isValue) {
            shieldSpritesResult.value!!
        } else {
            return ExpectedFactory.createError(
                """
                    Error when downloading image sprite.
                    url: $spriteUrl
                    result: ${shieldSpritesResult.error!!}
                """.trimIndent()
            )
        }
        val sprite = toDownload.getSpriteFrom(shieldSprites)
            ?: return ExpectedFactory.createError(
                "Sprite not found for ${toDownload.mapboxShield.name()} in $shieldSprites."
            )
        val placeholder = sprite.spriteAttributes().placeholder()
        if (placeholder.isNullOrEmpty()) {
            return ExpectedFactory.createError(
                """
                    Mapbox shield sprite placeholder was null or empty in: $sprite
                """.trimIndent()
            )
        }

        val mapboxShieldUrl = toDownload.url
        return shieldByteArrayCache.getOrRequest(mapboxShieldUrl).fold(
            { throwable ->
                ExpectedFactory.createError(throwable)
            },
            { shieldByteArray ->
                val svgJson = String(shieldByteArray)
                val shieldSvg: Expected<Throwable, String> = try {
                    ExpectedFactory.createValue(ShieldSvg.fromJson(svgJson).svg())
                } catch (ex: Throwable) {
                    ExpectedFactory.createError(ex)
                }
                shieldSvg.fold(
                    { svgError ->
                        ExpectedFactory.createError(
                            "Error parsing shield svg: ${svgError.message}"
                        )
                    },
                    { value ->
                        val svg = appendTextToShield(
                            text = toDownload.mapboxShield.displayRef(),
                            shieldSvg = value,
                            textColor = toDownload.mapboxShield.textColor(),
                            placeholder = placeholder
                        ).toByteArray()
                        ExpectedFactory.createValue(
                            RouteShield.MapboxDesignedShield(
                                mapboxShieldUrl,
                                svg,
                                toDownload.mapboxShield,
                                sprite
                            )
                        )
                    }
                )
            }
        )
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

    private suspend fun prepareMapboxLegacyShield(
        toDownload: RouteShieldToDownload.MapboxLegacy
    ): Expected<String, RouteShield> {
        val shieldUrl = toDownload.url
        return shieldByteArrayCache.getOrRequest(shieldUrl).mapValue { byteArray ->
            RouteShield.MapboxLegacyShield(
                toDownload.url,
                byteArray,
                toDownload.initialUrl
            )
        }
    }
}

internal class ShieldSpritesCache : ResourceCache<String, ShieldSprites>(8) {
    override suspend fun obtainResource(argument: String): Expected<String, ShieldSprites> {
        val result = RoadShieldDownloader.download(argument)
        return try {
            result.mapValue { data ->
                val spriteJson = String(data)
                val shieldSprites = ShieldSprites.fromJson(spriteJson)
                shieldSprites
            }
        } catch (exception: Throwable) {
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

internal class ShieldByteArrayCache : ResourceCache<String, ByteArray>(15) {
    override suspend fun obtainResource(argument: String): Expected<String, ByteArray> {
        return RoadShieldDownloader.download(argument)
    }
}
