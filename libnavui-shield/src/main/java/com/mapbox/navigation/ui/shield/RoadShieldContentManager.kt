package com.mapbox.navigation.ui.shield

import com.mapbox.api.directions.v5.models.ShieldSpriteAttribute
import com.mapbox.api.directions.v5.models.ShieldSvg
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldOrigin
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import com.mapbox.navigation.ui.shield.model.RouteShieldToDownload
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.CancellationException
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
// TODO: Change the Route shield object to sealed class
// TODO: use url for comparison
// TODO: Migrate to the existing content manager
// TODO: Introduce a wrapper for maneuver
// TODO: Before falling back to legacy check if it already exists
// TODO: Introduce a RouteShieldError. Treat fallback as success but remove it from success before you download it next time
class RoadShieldContentManager {
    companion object {
        private const val SVG_EXTENSION = ".svg"
        private const val CANCELED_MESSAGE = "canceled"
        private val TAG = Tag("MbxRoadShieldContentManager")
        private const val REQUEST_ACCESS_TOKEN = "?access_token="
    }

    private val byteArrayCache = hashMapOf<String, ByteArray>()
    private val resultMap = hashMapOf<String, Expected<RouteShieldError, RouteShieldResult>>()
    private val ongoingRequestList = mutableSetOf<String>()

    private val mainJob = InternalJobControlFactory.createMainScopeJobControl()
    private val awaitingCallbacks = mutableListOf<() -> Boolean>()

    internal suspend fun getShields(
        accessToken: String,
        fallbackToLegacy: Boolean = true,
        shieldsToDownload: List<RouteShieldToDownload>
    ): List<Expected<RouteShieldError, RouteShieldResult>> {
        val requestIds = prepareShields(
            accessToken = accessToken,
            fallbackToLegacy = fallbackToLegacy,
            shieldsToDownload = shieldsToDownload
        )

        return try {
            waitForShields(requestIds)
        } catch (ex: CancellationException) {
            listOf()
        }
    }

    internal fun cancelAll() {
        resultMap.clear()
        mainJob.job.children.forEach { it.cancel() }
    }

    private suspend fun retrieveByteArrayOrWaitIfDownloading(url: String): ByteArray? {
        return byteArrayCache[url] ?: run {
            if (ongoingRequestList.contains(url)) {
                suspendCancellableCoroutine { continuation ->
                    val callback = {
                        check(!continuation.isCancelled)
                        byteArrayCache[url]?.let {
                            continuation.resume(it)
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
                null
            }
        }
    }

    private fun prepareShields(
        accessToken: String,
        fallbackToLegacy: Boolean = true,
        shieldsToDownload: List<RouteShieldToDownload>
    ): Set<String> {
        return shieldsToDownload.map { toDownload ->
            val requestId = UUID.randomUUID().toString()
            mainJob.scope.launch {
                when (toDownload) {
                    is RouteShieldToDownload.MapboxDesign -> {
                        var message: String? = null
                        val designByteArray = retrieveByteArrayOrWaitIfDownloading(toDownload.url)
                            ?: run {
                                val placeholder =
                                    toDownload.shieldSprite.spriteAttributes().placeholder()
                                if (!placeholder.isNullOrEmpty()) {
                                    val requestUrl =
                                        toDownload.url.plus(REQUEST_ACCESS_TOKEN)
                                            .plus(accessToken)
                                    ongoingRequestList.add(requestUrl)
                                    val result = RoadShieldDownloader.downloadImage(requestUrl)
                                    invalidate()
                                    val array = if (result.isValue) {
                                        val svgJson = String(result.value!!)
                                        val svg = appendTextToShield(
                                            text = toDownload.mapboxShield.displayRef(),
                                            shieldSvg = ShieldSvg.fromJson(svgJson).svg(),
                                            spriteAttr = toDownload.shieldSprite.spriteAttributes()
                                        ).toByteArray()
                                        byteArrayCache[toDownload.url] = svg
                                        svg
                                    } else {
                                        message = """
                                        For mapbox shield url: ${toDownload.url} an error was
                                        received with message: ${result.error}.
                                    """.trimIndent()
                                        null
                                    }
                                    array
                                } else {
                                    message = """
                                    For mapbox shield url: ${toDownload.url} an error was
                                    received: missing placeholder in ${toDownload.shieldSprite}.
                                """.trimIndent()
                                    null
                                }
                            }

                        if (designByteArray != null) {
                            val shieldResult = RouteShieldResult(
                                RouteShield.MapboxDesignedShield(
                                    toDownload.url,
                                    designByteArray,
                                    toDownload.mapboxShield,
                                    toDownload.shieldSprite
                                ),
                                RouteShieldOrigin(
                                    isFallback = false,
                                    toDownload.url,
                                    message
                                )
                            )
                            resultMap[requestId] = ExpectedFactory.createValue(shieldResult)
                            invalidate()
                        } else if (fallbackToLegacy) {
                            val legacyShield = toDownload.legacy
                            if (legacyShield != null) {
                                val legacyByteArray =
                                    retrieveByteArrayOrWaitIfDownloading(toDownload.url)
                                        ?: run {
                                            val requestUrl = legacyShield.url.plus(SVG_EXTENSION)
                                            ongoingRequestList.add(requestUrl)
                                            val result =
                                                RoadShieldDownloader.downloadImage(requestUrl)
                                            invalidate()
                                            val array = if (result.isValue) {
                                                byteArrayCache[legacyShield.url] =
                                                    result.value!!
                                                result.value
                                            } else {
                                                val legacyMessage = """
                                            For mapbox shield url: ${legacyShield.url} an error was
                                            received with message: ${result.error}.
                                        """.trimIndent()
                                                message += legacyMessage
                                                null
                                            }
                                            array
                                        }
                                if (legacyByteArray != null) {
                                    val shieldResult = RouteShieldResult(
                                        RouteShield.MapboxLegacyShield(
                                            legacyShield.url,
                                            legacyByteArray,
                                        ),
                                        RouteShieldOrigin(
                                            isFallback = true,
                                            toDownload.url,
                                            message
                                        )
                                    )
                                    resultMap[requestId] =
                                        ExpectedFactory.createValue(shieldResult)
                                    invalidate()
                                } else {
                                    resultMap[requestId] = ExpectedFactory.createError(
                                        RouteShieldError(
                                            toDownload.url,
                                            message
                                        )
                                    )
                                    invalidate()
                                }
                            }
                        } else {
                            resultMap[requestId] = ExpectedFactory.createError(
                                RouteShieldError(
                                    toDownload.url,
                                    message
                                )
                            )
                            invalidate()
                        }
                    }
                    is RouteShieldToDownload.MapboxLegacy -> {
                        var message: String? = null
                        val legacyByteArray = retrieveByteArrayOrWaitIfDownloading(toDownload.url)
                            ?: run {
                                val requestUrl = toDownload.url.plus(SVG_EXTENSION)
                                ongoingRequestList.add(requestUrl)
                                val result = RoadShieldDownloader.downloadImage(requestUrl)
                                invalidate()
                                val array = if (result.isValue) {
                                    byteArrayCache[toDownload.url] = result.value!!
                                    result.value
                                } else {
                                    val legacyMessage = """
                                            For mapbox shield url: ${toDownload.url} an error was
                                            received with message: ${result.error}.
                                        """.trimIndent()
                                    message += legacyMessage
                                    null
                                }
                                array
                            }
                        if (legacyByteArray != null) {
                            val shieldResult = RouteShieldResult(
                                RouteShield.MapboxLegacyShield(
                                    toDownload.url,
                                    legacyByteArray,
                                ),
                                RouteShieldOrigin(
                                    isFallback = false,
                                    toDownload.url,
                                    message
                                )
                            )
                            resultMap[requestId] =
                                ExpectedFactory.createValue(shieldResult)
                            invalidate()
                        } else {
                            resultMap[requestId] = ExpectedFactory.createError(
                                RouteShieldError(
                                    toDownload.url,
                                    message
                                )
                            )
                            invalidate()
                        }
                    }
                }
            }
            requestId
        }.toSet()
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
