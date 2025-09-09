package com.mapbox.navigation.tripdata.shield

import android.util.LruCache
import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.ShieldSprites
import com.mapbox.api.directions.v5.models.ShieldSvg
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.tripdata.shield.api.ShieldFontConfig
import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.internal.model.SizeSpecificSpriteInfo
import com.mapbox.navigation.tripdata.shield.internal.model.generateSpriteSheetUrl
import com.mapbox.navigation.tripdata.shield.internal.model.getSpriteFrom
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal abstract class ResourceCache<Argument, Value>(cacheSize: Int) {

    data class SuccessfulResponse<Value>(
        val response: Value,
        val requestUrl: String,
    )

    data class RequestError(
        val error: String,
        val requestUrl: String?,
    )

    internal companion object {
        internal const val CANCELED_MESSAGE = "canceled"
    }

    private val cache =
        LruCache<Argument, Expected<RequestError, SuccessfulResponse<Value>>>(cacheSize)
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
    suspend fun getOrRequest(
        argument: Argument,
    ): Expected<RequestError, SuccessfulResponse<Value>> {
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
                    val result =
                        ExpectedFactory.createError<RequestError, SuccessfulResponse<Value>>(
                            RequestError(CANCELED_MESSAGE, null),
                        )
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
    protected abstract suspend fun obtainResource(
        argument: Argument,
    ): Expected<RequestError, SuccessfulResponse<Value>>
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ShieldResultCache(
    private val shieldSpritesCache: ShieldSpritesCache = ShieldSpritesCache(),
    private val shieldByteArrayCache: ShieldByteArrayCache = ShieldByteArrayCache(),
) : ResourceCache<RouteShieldToDownload, RouteShield>(40) {
    override suspend fun obtainResource(
        argument: RouteShieldToDownload,
    ): Expected<RequestError, SuccessfulResponse<RouteShield>> {
        return when (argument) {
            is RouteShieldToDownload.MapboxDesign -> prepareMapboxDesignShield(argument)
            is RouteShieldToDownload.MapboxLegacy -> prepareMapboxLegacyShield(argument)
        }
    }

    private suspend fun prepareMapboxDesignShield(
        toDownload: RouteShieldToDownload.MapboxDesign,
    ): Expected<RequestError, SuccessfulResponse<RouteShield>> {
        val spriteUrl = toDownload.generateSpriteSheetUrl()
        val shieldSpritesResult = shieldSpritesCache.getOrRequest(spriteUrl)
        val shieldSprites = if (shieldSpritesResult.isValue) {
            shieldSpritesResult.value!!.response
        } else {
            return ExpectedFactory.createError(
                RequestError(
                    """
                        Error when downloading image sprite.
                        url: $spriteUrl
                        result: ${shieldSpritesResult.error!!.error}
                    """.trimIndent(),
                    null,
                ),
            )
        }
        val sprite = toDownload.getSpriteFrom(shieldSprites.toSizeSpecificSpriteInfos())
            ?: return ExpectedFactory.createError(
                RequestError(
                    "Sprite not found for ${toDownload.mapboxShield.name()} in $shieldSprites.",
                    spriteUrl,
                ),
            )
        val placeholder = sprite.spriteAttributes().placeholder()
        if (placeholder.isNullOrEmpty()) {
            return ExpectedFactory.createError(
                RequestError(
                    """
                    Mapbox shield sprite placeholder was null or empty in: $sprite
                    """.trimIndent(),
                    spriteUrl,
                ),
            )
        }

        val mapboxShieldUrl = toDownload.generateUrl(sprite)
        return shieldByteArrayCache.getOrRequest(mapboxShieldUrl).fold(
            { throwable ->
                ExpectedFactory.createError(throwable)
            },
            { shieldResponse ->
                val svgJson = String(shieldResponse.response)
                val shieldSvg: Expected<Throwable, String> = try {
                    ExpectedFactory.createValue(ShieldSvg.fromJson(svgJson).svg())
                } catch (ex: Throwable) {
                    ExpectedFactory.createError(ex)
                }
                shieldSvg.fold(
                    { svgError ->
                        ExpectedFactory.createError(
                            RequestError(
                                "Error parsing shield svg: ${svgError.message}",
                                shieldResponse.requestUrl,
                            ),
                        )
                    },
                    { value ->
                        val svg = appendTextToShield(
                            text = toDownload.mapboxShield.displayRef(),
                            shieldSvg = value,
                            textColor = toDownload.mapboxShield.textColor(),
                            placeholder = placeholder,
                            fontConfig = toDownload.shieldSpriteToDownload.fontConfig,
                        ).toByteArray()
                        ExpectedFactory.createValue(
                            SuccessfulResponse(
                                RouteShield.MapboxDesignedShield(
                                    mapboxShieldUrl,
                                    svg,
                                    toDownload.mapboxShield,
                                    sprite,
                                ),
                                shieldResponse.requestUrl,
                            ),
                        )
                    },
                )
            },
        )
    }

    private fun appendTextToShield(
        text: String,
        shieldSvg: String,
        textColor: String,
        placeholder: List<Double>,
        fontConfig: ShieldFontConfig? = null,
    ): String {
        val textTagX = placeholder[0] + placeholder[2] / 2
        val textTagY = placeholder[3]
        val textSize = placeholder[3] - placeholder[1] + 3

        val fontAttributes = if (fontConfig != null) {
            "font-family=\"${fontConfig.fontFamily}\"" +
                " font-weight=\"${fontConfig.fontWeight}\"" +
                " font-style=\"${fontConfig.fontStyle}\" "
        } else {
            "font-family=\"Arial, Helvetica, sans-serif\" " +
                "font-weight=\"bold\" "
        }

        val shieldText = "\t<text x=\"$textTagX\" y=\"$textTagY\" " +
            fontAttributes +
            "text-anchor=\"middle\" font-size=\"$textSize\" fill=\"$textColor\">$text</text>"

        return shieldSvg.replace("</svg>", shieldText.plus("</svg>"))
    }

    private suspend fun prepareMapboxLegacyShield(
        toDownload: RouteShieldToDownload.MapboxLegacy,
    ): Expected<RequestError, SuccessfulResponse<RouteShield>> {
        val shieldUrl = toDownload.url
        return shieldByteArrayCache.getOrRequest(shieldUrl).mapValue { response ->
            SuccessfulResponse(
                RouteShield.MapboxLegacyShield(
                    toDownload.url,
                    response.response,
                    toDownload.initialUrl,
                ),
                response.requestUrl,
            )
        }
    }
}

internal class ShieldSpritesCache : ResourceCache<String, ShieldSprites>(8) {
    override suspend fun obtainResource(
        argument: String,
    ): Expected<RequestError, SuccessfulResponse<ShieldSprites>> {
        val result = RoadShieldDownloader.download(argument)
        return try {
            result.fold(
                { error -> ExpectedFactory.createError(RequestError(error, argument)) },
                { data ->
                    val spriteJson = String(data)
                    val shieldSprites = ShieldSprites.fromJson(spriteJson)
                    ExpectedFactory.createValue(SuccessfulResponse(shieldSprites, argument))
                },
            )
        } catch (exception: Throwable) {
            val json = result.value?.let { String(it) } ?: "null"
            ExpectedFactory.createError(
                RequestError(
                    """
                    |Error parsing shield sprites:
                    |exception: $exception
                    |json: $json
                    """.trimMargin(),
                    argument,
                ),
            )
        }
    }
}

internal class ShieldByteArrayCache : ResourceCache<String, ByteArray>(15) {
    override suspend fun obtainResource(
        argument: String,
    ): Expected<RequestError, SuccessfulResponse<ByteArray>> {
        return RoadShieldDownloader.download(argument).fold(
            { error -> ExpectedFactory.createError(RequestError(error, argument)) },
            { value -> ExpectedFactory.createValue(SuccessfulResponse(value, argument)) },
        )
    }
}

@VisibleForTesting
internal fun ShieldSprites.toSizeSpecificSpriteInfos(): List<SizeSpecificSpriteInfo> {
    return sprites().mapNotNull { original ->
        if (original.spriteName().contains("-")) {
            val length = original.spriteName().substringAfterLast("-").toIntOrNull()
            length?.let { length ->
                val name = original.spriteName().substringBeforeLast("-")
                SizeSpecificSpriteInfo(name, length, original)
            }
        } else {
            null
        }
    }
}
