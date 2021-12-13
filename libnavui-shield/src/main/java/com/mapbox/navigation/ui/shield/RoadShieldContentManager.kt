package com.mapbox.navigation.ui.shield

import com.google.gson.JsonSyntaxException
import com.mapbox.api.directions.v5.models.*
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.shield.model.*
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.LoggerProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

/**
 * Sample instruction
 *
 * bannerInstructions: {
 *    primary: {
 *      components: [
 *          {
 *              type: text
 *              text: Fremont Street
 *          },
 *          {
 *              imageBaseUrl: https://mapbox.navigation.shields.s3.....
 *              mapbox_shield: {
 *                  base_url: https://api.mapbox.com/styles/v1
 *                  text_color: black
 *                  display_ref: 880
 *                  shieldName: us-interstate
 *              }
 *              type: icon
 *              text: I880
 *          }
 *      ]
 *    }
 * }
 *
 * 1. If imageBaseUrl, mapbox shield is null return default
 * 2. If imageBaseUrl is non null, mapbox shield is null:
 *     - Request shield using imageBaseUrl:
 *       - If request success: return generic shield
 *       - If request fails: return default shield
 * 3. If mapbox shield is non null, imageBaseUrl is null
 *     - If any of userId, styleId or accessToken is null: do nothing
 *     - If all of userId, styleId or accessToken are non null:
 *       - If sprite does not exist for shield url: return default shield
 *       - If sprite exists:
 *         - If placeholder does not exist: return default shield
 *         - If placeholder exists:
 *           - Request shield using mapbox shield:
 *             - If request success: return mapbox shield
 *             - If request fails: return default
 * 4. If mapbox shield & imageBaseUrl is non null
 *     - If any of userId, styleId or accessToken is null: do nothing
 *     - If all of userId, styleId or accessToken are non null:
 *       - If sprite does not exist for mapbox shield url: follow 2
 *       - If sprite exists:
 *         - If placeholder does not exist: follow 2
 *         - If placeholder exists:
 *           - Request shield using mapbox shield url:
 *             - If request success: return mapbox shield
 *             - If request fails: follow 2
 */
class RoadShieldContentManager {
    companion object {
        private const val CANCELED_MESSAGE = "canceled"
        private val TAG = Tag("MbxRoadShieldContentManager")
    }

    private val shieldByteArrayCache = hashMapOf<String, ByteArray>()
    private val shieldSpritesCache: HashMap<String, ShieldSprites> = hashMapOf()
    private val resultMap = hashMapOf<String, Expected<RouteShieldError, RouteShieldResult>>()
    private val ongoingRequestSet = mutableSetOf<String>()

    private val mainJob = InternalJobControlFactory.createMainScopeJobControl()
    private val awaitingCallbacks = mutableListOf<() -> Boolean>()

    internal suspend fun getShields(
        fallbackToLegacy: Boolean = true,
        shieldsToDownload: List<RouteShieldToDownload>
    ): List<Expected<RouteShieldError, RouteShieldResult>> {
        val requestIds = prepareShields(
            shieldsToDownload = shieldsToDownload,
            fallbackToLegacy = fallbackToLegacy,
        )

        return try {
            waitForShields(requestIds)
        } catch (ex: CancellationException) {
            listOf()
        }
    }

    private suspend fun getOrRequestShieldSprites(url: String): ShieldSprites {
        return shieldSpritesCache[url] ?: run {
            val spriteJob = mainJob.scope.async {
                RoadShieldDownloader.download(url).fold(
                    { error ->
                        LoggerProvider.logger.e(TAG, Message(error))
                        ShieldSprites.builder().sprites(emptyList()).build()
                    },
                    { data ->
                        val spriteJson = String(data)
                        try {
                            val shieldSprites = ShieldSprites.fromJson(spriteJson)
                            this@RoadShieldContentManager.shieldSpritesCache[url] = shieldSprites
                            shieldSprites
                        } catch (exception: JsonSyntaxException) {
                            LoggerProvider.logger.e(
                                TAG,
                                Message("Error in parsing json: $spriteJson")
                            )
                            ShieldSprites.builder().sprites(emptyList()).build()
                        }
                    }
                )
            }
            spriteJob.await()
        }
    }

    internal fun cancelAll() {
        resultMap.clear()
        mainJob.job.children.forEach { it.cancel() }
    }

    private suspend fun getOrRequestShieldByteArray(
        url: String
    ): Expected<String, ByteArray> {
        return shieldByteArrayCache[url]?.let { ExpectedFactory.createValue(it) } ?: run {
            if (ongoingRequestSet.contains(url)) {
                suspendCancellableCoroutine { continuation ->
                    val callback = {
                        check(!continuation.isCancelled)
                        shieldByteArrayCache[url]?.let {
                            continuation.resume(ExpectedFactory.createValue(it))
                            true
                        } ?: false
                    }
                    if (callback()) {
                        return@suspendCancellableCoroutine
                    }
                    awaitingCallbacks.add(callback)
                    continuation.invokeOnCancellation {
                        awaitingCallbacks.remove(callback)
                    }
                }
            } else {
                ongoingRequestSet.add(url)
                val result = RoadShieldDownloader.download(url)
                ongoingRequestSet.remove(url)
                result
            }
        }
    }

    private fun prepareShields(
        shieldsToDownload: List<RouteShieldToDownload>,
        fallbackToLegacy: Boolean,
    ): Set<String> {
        return shieldsToDownload.map { toDownload ->
            val requestId = UUID.randomUUID().toString()
            mainJob.scope.launch {
                when (toDownload) {
                    is RouteShieldToDownload.MapboxDesign -> {
                        val mapboxDesignShieldResult = prepareMapboxDesignShield(toDownload)
                        if (mapboxDesignShieldResult.isError) {
                            if (fallbackToLegacy) {
                                val legacyShieldResult =
                                    prepareMapboxLegacyShield(toDownload.legacy)
                                legacyShieldResult.fold(
                                    { error ->
                                        resultMap[requestId] = ExpectedFactory.createError(
                                            RouteShieldError(
                                                url = toDownload.legacy.url,
                                                errorMessage = error
                                            )
                                        )
                                    },
                                    { legacyShield ->
                                        val result = RouteShieldResult(
                                            legacyShield,
                                            RouteShieldOrigin(
                                                isFallback = true,
                                                originalUrl = toDownload.generateShieldUrl(),
                                                mapboxDesignShieldResult.error!!
                                            )
                                        )
                                        resultMap[requestId] = ExpectedFactory.createValue(result)
                                    }
                                )
                            } else {
                                resultMap[requestId] = ExpectedFactory.createError(
                                    RouteShieldError(
                                        url = toDownload.generateShieldUrl(),
                                        errorMessage = mapboxDesignShieldResult.error!!
                                    )
                                )
                            }
                        } else {
                            val result = RouteShieldResult(
                                mapboxDesignShieldResult.value!!,
                                RouteShieldOrigin(
                                    isFallback = false,
                                    mapboxDesignShieldResult.value!!.url,
                                    ""
                                )
                            )
                            resultMap[requestId] = ExpectedFactory.createValue(result)
                        }
                    }
                    is RouteShieldToDownload.MapboxLegacy -> {
                        val legacyShieldResult = prepareMapboxLegacyShield(toDownload)
                        legacyShieldResult.fold(
                            { error ->
                                resultMap[requestId] = ExpectedFactory.createError(
                                    RouteShieldError(
                                        url = toDownload.url,
                                        errorMessage = error
                                    )
                                )
                            },
                            { legacyShield ->
                                val result = RouteShieldResult(
                                    legacyShield,
                                    RouteShieldOrigin(
                                        isFallback = false,
                                        originalUrl = toDownload.url,
                                        originalErrorMessage = ""
                                    )
                                )
                                resultMap[requestId] = ExpectedFactory.createValue(result)
                            }
                        )
                    }
                }
                invalidate()
            }
            requestId
        }.toSet()
    }

    private suspend fun prepareMapboxDesignShield(
        toDownload: RouteShieldToDownload.MapboxDesign
    ): Expected<String, RouteShield.MapboxDesignedShield> {
        val spriteUrl = toDownload.generateSpriteSheetUrl()
        val shieldSprites = getOrRequestShieldSprites(spriteUrl)
        val sprite = toDownload.getSpriteFrom(shieldSprites)
            ?: return ExpectedFactory.createError(
                "Sprite not found for ${toDownload.mapboxShield.name()} in $shieldSprites."
            )

        val mapboxShieldUrl = toDownload.generateShieldUrl()
        val shieldByteArrayResult = getOrRequestShieldByteArray(mapboxShieldUrl)
        val shieldByteArray = if (shieldByteArrayResult.isValue) {
            shieldByteArrayResult.value!!
        } else {
            return ExpectedFactory.createError(shieldByteArrayResult.error!!)
        }

        val placeholder = sprite.spriteAttributes().placeholder()
        return if (!placeholder.isNullOrEmpty()) {
            val svgJson = String(shieldByteArray)
            val svg = appendTextToShield(
                text = toDownload.mapboxShield.displayRef(),
                shieldSvg = ShieldSvg.fromJson(svgJson).svg(),
                spriteAttr = sprite.spriteAttributes()
            ).toByteArray()
            shieldByteArrayCache[mapboxShieldUrl] = svg
            ExpectedFactory.createValue(
                RouteShield.MapboxDesignedShield(
                    mapboxShieldUrl,
                    svg,
                    toDownload.mapboxShield,
                    sprite
                )
            )
        } else {
            ExpectedFactory.createError(
                """
                    Mapbox shield sprite placeholder was null or empty in:
                    ${sprite.spriteAttributes().placeholder()}
                """.trimIndent()
            )
        }
    }

    private suspend fun prepareMapboxLegacyShield(
        toDownload: RouteShieldToDownload.MapboxLegacy
    ): Expected<String, RouteShield.MapboxLegacyShield> {
        val shieldUrl = toDownload.generateShieldUrl()
        val shieldByteArrayResult = getOrRequestShieldByteArray(shieldUrl)
        return shieldByteArrayResult.mapValue { byteArray ->
            shieldByteArrayCache[shieldUrl] = byteArray
            RouteShield.MapboxLegacyShield(
                toDownload.url,
                byteArray
            )
        }
    }

    private fun appendTextToShield(
        text: String,
        shieldSvg: String,
        spriteAttr: ShieldSpriteAttribute
    ): String {
        val textTagX = 0.5 + spriteAttr.placeholder()!![0] + spriteAttr.placeholder()!![2] / 2
        val textTagY = spriteAttr.placeholder()!![3]
        val textSize = spriteAttr.placeholder()!![3] - spriteAttr.placeholder()!![1] + 2
        val shieldText = "\t<text x=\"$textTagX\" y=\"$textTagY\" font-family=\"Arial\" " +
            "font-weight=\"bold\" text-anchor=\"middle\" font-size=\"$textSize\">$text</text>"
        return shieldSvg.replace("</svg>", shieldText.plus("\n</svg>"))
    }

    private fun invalidate() {
        val iterator = awaitingCallbacks.iterator()
        while (iterator.hasNext()) {
            val remove = iterator.next().invoke()
            if (remove) {
                iterator.remove()
            }
        }
    }

    private suspend fun waitForShields(
        requestIds: Set<String>
    ): List<Expected<RouteShieldError, RouteShieldResult>> {
        return suspendCancellableCoroutine { continuation ->
            val callback = {
                check(!continuation.isCancelled)
                if (requestIds.all { requestId -> resultMap.containsKey(requestId) }) {
                    val returnList = mutableListOf<Expected<RouteShieldError, RouteShieldResult>>()
                    requestIds.forEach {
                        returnList.add(resultMap.remove(it)!!)
                    }
                    continuation.resume(returnList)
                    true
                } else {
                    false
                }
            }
            if (callback()) {
                return@suspendCancellableCoroutine
            }
            awaitingCallbacks.add(callback)
            continuation.invokeOnCancellation {
                awaitingCallbacks.remove(callback)
            }
        }
    }
}
