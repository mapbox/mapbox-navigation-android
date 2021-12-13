package com.mapbox.navigation.ui.shield

import com.google.gson.JsonSyntaxException
import com.mapbox.api.directions.v5.models.*
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.shield.model.*
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import com.mapbox.navigation.ui.shield.model.RouteShieldToDownload
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
        private const val SPRITE = "/sprite"
        private const val SVG_EXTENSION = ".svg"
        private const val SPRITE_JSON = "sprite.json"
        private const val CANCELED_MESSAGE = "canceled"
        private const val MINIMUM_DISPLAY_REF_LENGTH = 2
        private const val MAXIMUM_DISPLAY_REF_LENGTH = 6
        private val TAG = Tag("MbxRoadShieldContentManager")
        private const val REQUEST_ACCESS_TOKEN = "?access_token="
        private const val SPRITE_BASE_URL = "https://api.mapbox.com/styles/v1/"
    }

    private val byteArrayCache = hashMapOf<String, ByteArray>()
    private val sprites: HashMap<String, ShieldSprites> = hashMapOf()
    private val resultMap = hashMapOf<String, Expected<RouteShieldError, RouteShieldResult>>()
    private val ongoingRequestList = mutableSetOf<String>()

    private val mainJob = InternalJobControlFactory.createMainScopeJobControl()
    private val awaitingCallbacks = mutableListOf<() -> Boolean>()

    internal suspend fun getShields(
        userId: String?,
        styleId: String?,
        accessToken: String?,
        fallbackToLegacy: Boolean = true,
        shieldsToDownload: List<RouteShieldToDownload>
    ): List<Expected<RouteShieldError, RouteShieldResult>> {
        val shieldSprites = if (userId != null && styleId != null && accessToken != null) {
            requestSprites(userId, styleId, accessToken)
        } else {
            ShieldSprites.builder().sprites(emptyList()).build()
        }
        val requestIds = prepareShields(
            userId = userId,
            styleId = styleId,
            accessToken = accessToken,
            shieldSprites = shieldSprites,
            fallbackToLegacy = fallbackToLegacy,
            shieldsToDownload = shieldsToDownload
        )

        return try {
            waitForShields(requestIds)
        } catch (ex: CancellationException) {
            listOf()
        }
    }

    private suspend fun requestSprites(
        userId: String,
        styleId: String,
        accessToken: String
    ): ShieldSprites {
        val url = SPRITE_BASE_URL
            .plus("$userId/")
            .plus("$styleId/")
            .plus(SPRITE_JSON)
            .plus(REQUEST_ACCESS_TOKEN)
            .plus(accessToken)
        val spriteSheet = when (sprites[url]) {
            null -> {
                ShieldSprites.builder().sprites(emptyList()).build()
            }
            else -> {
                sprites[url]!!
            }
        }
        return if (spriteSheet.sprites().isEmpty()) {
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
                            this@RoadShieldContentManager.sprites[url] = shieldSprites
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
        } else {
            spriteSheet
        }
    }

    internal fun cancelAll() {
        resultMap.clear()
        mainJob.job.children.forEach { it.cancel() }
    }

    private suspend fun retrieveByteArrayOrWaitIfDownloading(url: String?): ByteArray? {
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
        userId: String?,
        styleId: String?,
        accessToken: String?,
        shieldSprites: ShieldSprites,
        fallbackToLegacy: Boolean = true,
        shieldsToDownload: List<RouteShieldToDownload>
    ): Set<String> {
        return shieldsToDownload.map { toDownload ->
            val requestId = UUID.randomUUID().toString()
            mainJob.scope.launch {
                when (toDownload) {
                    is RouteShieldToDownload.MapboxDesign -> {
                        val mapboxShieldUrl = generateShieldUrl(
                            userId,
                            styleId,
                            toDownload.mapboxShield
                        )
                        val sprite = getSpriteFrom(
                            toDownload.mapboxShield?.name(),
                            toDownload.mapboxShield?.displayRef(),
                            shieldSprites
                        )
                        requestMapboxDesignedShield(
                            requestId = requestId,
                            accessToken = accessToken,
                            sprite = sprite,
                            fallbackToLegacy = fallbackToLegacy,
                            mapboxShieldUrl = mapboxShieldUrl,
                            mapboxShield = toDownload.mapboxShield,
                            legacy = toDownload.legacy
                        )
                    }
                    is RouteShieldToDownload.MapboxLegacy -> {
                        requestMapboxLegacyShield(requestId = requestId, url = toDownload.url)
                    }
                }
            }
            requestId
        }.toSet()
    }

    private suspend fun requestMapboxDesignedShield(
        requestId: String,
        accessToken: String?,
        sprite: ShieldSprite?,
        mapboxShieldUrl: String?,
        fallbackToLegacy: Boolean,
        mapboxShield: MapboxShield?,
        legacy: RouteShieldToDownload.MapboxLegacy?
    ) {
        var message = ""
        val designByteArray = retrieveByteArrayOrWaitIfDownloading(mapboxShieldUrl) ?: run {
            val placeholder = sprite?.spriteAttributes()?.placeholder()
            if (mapboxShield != null && !accessToken.isNullOrEmpty() && !placeholder.isNullOrEmpty()
                && !mapboxShieldUrl.isNullOrEmpty()
            ) {
                val requestUrl = mapboxShieldUrl.plus(REQUEST_ACCESS_TOKEN).plus(accessToken)
                ongoingRequestList.add(requestUrl)
                val result = RoadShieldDownloader.download(requestUrl)
                invalidate()
                val array = if (result.isValue) {
                    val svgJson = String(result.value!!)
                    val svg = appendTextToShield(
                        text = mapboxShield.displayRef(),
                        shieldSvg = ShieldSvg.fromJson(svgJson).svg(),
                        spriteAttr = sprite.spriteAttributes()
                    ).toByteArray()
                    byteArrayCache[mapboxShieldUrl] = svg
                    svg
                } else {
                    message = """
                        For mapbox shield url: $mapboxShieldUrl an error was received with 
                        message: ${result.error}.
                    """.trimIndent()
                    null
                }
                array
            } else {
                message = """
                    For mapbox shield any of the following could have happened:
                    - access token was null or empty: $accessToken
                    - mapbox shield was null: $mapboxShield
                    - mapbox shield url was null or empty: $mapboxShieldUrl
                    - mapbox shield sprite was not found: $sprite
                    - mapbox shield sprite placeholder was null or empty
                """.trimIndent()
                null
            }
        }
        if (designByteArray != null) {
            val shieldResult = RouteShieldResult(
                RouteShield.MapboxDesignedShield(
                    mapboxShieldUrl!!, // byteArray could only be non null if this url was non null
                    designByteArray,
                    mapboxShield!!, // byteArray could only be non null if this mapboxShield was non null
                    sprite
                ),
                RouteShieldOrigin(
                    isFallback = false,
                    mapboxShieldUrl,
                    message
                )
            )
            resultMap[requestId] = ExpectedFactory.createValue(shieldResult)
            invalidate()
        } else if (fallbackToLegacy && legacy != null) {
            requestMapboxLegacyShield(requestId, legacy.url)
            val legacyByteArray = retrieveByteArrayOrWaitIfDownloading(legacy.url) ?: run {
                if (legacy.url != null) {
                    val requestUrl = legacy.url.plus(SVG_EXTENSION)
                    ongoingRequestList.add(requestUrl)
                    val result = RoadShieldDownloader.download(requestUrl)
                    invalidate()
                    val array = if (result.isValue) {
                        byteArrayCache[legacy.url] = result.value!!
                        result.value
                    } else {
                        val legacyMessage = """
                            For mapbox shield url: ${legacy.url} an error was received with 
                            message: ${result.error}.
                        """.trimIndent()
                        message += legacyMessage
                        null
                    }
                    array
                } else {
                    val legacyMessage = """
                        Could not fallback to legacy url because legacyShield url was null.
                    """.trimIndent()
                    message += legacyMessage
                    null
                }
            }
            if (legacyByteArray != null) {
                val shieldResult = RouteShieldResult(
                    RouteShield.MapboxLegacyShield(shield = legacyByteArray, url = legacy.url!!), // legacyByteArray could only be non null if this legacyShield was non null
                    RouteShieldOrigin(isFallback = true, mapboxShieldUrl, message)
                )
                resultMap[requestId] = ExpectedFactory.createValue(shieldResult)
                invalidate()
            } else {
                resultMap[requestId] = ExpectedFactory.createError(
                    RouteShieldError(mapboxShieldUrl, message)
                )
                invalidate()
            }
        } else {
            resultMap[requestId] = ExpectedFactory.createError(
                RouteShieldError(mapboxShieldUrl, message)
            )
            invalidate()
        }
    }

    private suspend fun requestMapboxLegacyShield(requestId: String, url: String?) {
        var message = ""
        val shield = retrieveByteArrayOrWaitIfDownloading(url) ?: run {
            if (url != null) {
                val requestUrl = url.plus(SVG_EXTENSION)
                ongoingRequestList.add(requestUrl)
                val result = RoadShieldDownloader.download(imageUrl = requestUrl)
                invalidate()
                val data = if (result.isValue) {
                    byteArrayCache[url] = result.value!!
                    result.value
                } else {
                    message = """
                        For legacy shield url: $url an error was received with message: 
                        ${result.error}.
                    """.trimIndent()
                    null
                }
                data
            } else {
                message = "Could not download shield because url because was null."
                null
            }
        }
        if (shield != null) {
            val shieldResult = RouteShieldResult(
                shield = RouteShield.MapboxLegacyShield(shield = shield, url = url!!), // shield could only be non null if this url was non null
                origin = RouteShieldOrigin(isFallback = false, originalUrl = url, errorMessage = message)
            )
            resultMap[requestId] = ExpectedFactory.createValue(shieldResult)
            invalidate()
        } else {
            resultMap[requestId] = ExpectedFactory.createError(RouteShieldError(url, message))
            invalidate()
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

    private fun generateShieldUrl(
        userId: String?,
        styleId: String?,
        mapboxShield: MapboxShield?
    ): String? {
        if (mapboxShield == null || userId == null || styleId == null) {
            return null
        }
        val refLen = getRefLen(mapboxShield.displayRef())
        return mapboxShield.baseUrl()
            .plus(userId)
            .plus("/$styleId")
            .plus(SPRITE)
            .plus("/${mapboxShield.name()}")
            .plus("-$refLen")
    }

    private fun getSpriteFrom(
        shieldName: String?,
        displayRef: String?,
        shieldSprites: ShieldSprites
    ): ShieldSprite? {
        if (shieldName == null || displayRef == null) {
            return null
        }
        val refLen = getRefLen(displayRef)
        return shieldSprites.sprites().find { shieldSprite ->
            shieldSprite.spriteName() == shieldName.plus("-$refLen")
        }
    }

    private fun getRefLen(displayRef: String): Int {
        return when  {
            displayRef.length <= 1 -> {
                MINIMUM_DISPLAY_REF_LENGTH
            }
            displayRef.length > 6 -> {
                MAXIMUM_DISPLAY_REF_LENGTH
            }
            else -> { displayRef.length }
        }
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
