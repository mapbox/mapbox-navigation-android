package com.mapbox.navigation.ui.shield

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.api.directions.v5.models.ShieldSpriteAttribute
import com.mapbox.api.directions.v5.models.ShieldSvg
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.ui.shield.model.MapboxRouteShieldOptions
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import com.mapbox.navigation.ui.shield.model.RouteShieldToDownload
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.launch
import java.util.*

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
class RoadShieldContentManager(val options: MapboxRouteShieldOptions) {
    companion object {
        private const val SVG_EXTENSION = ".svg"
        private const val CANCELED_MESSAGE = "canceled"
        private val TAG = Tag("MbxRoadShieldContentManager")
        private const val REQUEST_ACCESS_TOKEN = "?access_token="
    }

    private val shieldDownloadSuccess = hashMapOf<String, RouteShield.Success>()
    private val shieldDownloadFailure = hashMapOf<String, RouteShield.Failure>()
    private val requestedShields = mutableListOf<String>()

    private val mainJob = InternalJobControlFactory.createMainScopeJobControl()
    private val awaitingCallbacks = mutableListOf<() -> Boolean>()

    internal suspend fun getShields(
        accessToken: String,
        fallbackToLegacy: Boolean = true,
        fallbackToGeneric: Boolean = true,
        shieldsToDownload: List<RouteShieldToDownload>
    ): RouteShieldResult {
        clearFailuresFor(shieldsToDownload)
        mainJob.scope.launch {
            prepareShields(
                accessToken = accessToken,
                fallbackToLegacy = fallbackToLegacy,
                fallbackToGeneric = fallbackToGeneric,
                shieldsToDownload = shieldsToDownload
            )
        }

        return RouteShieldResult(shieldDownloadSuccess, shieldDownloadFailure)
        /*return try {
            waitForShields(shieldsToDownload)
        } catch (ex: CancellationException) {
            RouteShieldResult(shieldDownloadSuccess, shieldDownloadFailure)
        }*/
    }

    internal fun cancelAll() {
        requestedShields.clear()
        mainJob.job.children.forEach { it.cancel() }
    }

    private fun clearFailuresFor(list: List<RouteShieldToDownload>) {
        /*list.forEach { shieldToDownload ->
            val routeShield = shieldDownloadFailure.find { failed ->
                failed.url == shieldToDownload.mapboxShieldUrl
                    || failed.url == shieldToDownload.legacyShieldUrl
            }
            shieldDownloadFailure.remove(routeShield)
        }*/
    }

    private fun prepareShields(
        accessToken: String,
        fallbackToLegacy: Boolean = true,
        fallbackToGeneric: Boolean = true,
        shieldsToDownload: List<RouteShieldToDownload>
    ) {
        shieldsToDownload.forEach { toDownload ->
            val legacyUrl = toDownload.legacyShieldUrl
            val mapboxShield = toDownload.mapboxShield
            val mapboxShieldUrl = toDownload.mapboxShieldUrl
            when {
                /*mapboxShield != null -> {
                    if (!requestedShields.contains(mapboxShieldUrl)) {
                        val isUnavailable = shieldDownloadSuccess.find {
                            if (it is RouteShield.Success.MapboxDesignedShield) {
                                it.url == mapboxShieldUrl
                            } else {
                                false
                            }
                        } == null
                        if (isUnavailable) {
                            requestedShields.add(mapboxShieldUrl!!)
                            val shieldSprite = toDownload.shieldSprite
                            if (shieldSprite != null) {
                                val placeholder = shieldSprite.spriteAttributes().placeholder()
                                if (!placeholder.isNullOrEmpty()) {
                                    uponAbleToMakeRequest(
                                        legacyUrl = legacyUrl,
                                        accessToken = accessToken,
                                        mapboxShield = mapboxShield,
                                        shieldSprite = shieldSprite,
                                        mapboxShieldUrl = mapboxShieldUrl,
                                        fallbackToLegacy = fallbackToLegacy,
                                        fallbackToGeneric = fallbackToGeneric
                                    )
                                } else {
                                    uponNoSpritePlaceholder(
                                        legacyUrl = legacyUrl,
                                        mapboxShield = mapboxShield,
                                        shieldSprite = shieldSprite,
                                        mapboxShieldUrl = mapboxShieldUrl,
                                        fallbackToLegacy = fallbackToLegacy,
                                        fallbackToGeneric = fallbackToGeneric
                                    )
                                }
                            } else {
                                uponNoShieldSprite(
                                    legacyUrl = legacyUrl,
                                    mapboxShield = mapboxShield,
                                    mapboxShieldUrl = mapboxShieldUrl,
                                    fallbackToLegacy = fallbackToLegacy,
                                    fallbackToGeneric = fallbackToGeneric
                                )
                            }
                            requestedShields.remove(mapboxShieldUrl)
                            invalidate()
                        }
                    }
                }*/
                legacyUrl != null -> {
                    val isAvailableInFailure = shieldDownloadFailure.entries.find {
                        it.value.url == toDownload.legacyShieldUrl
                    }
                    val isAvailableInSuccess = shieldDownloadSuccess.entries.find {
                        if (it.value is RouteShield.Success.MapboxLegacyShield) {
                            (it.value as RouteShield.Success.MapboxLegacyShield).url == toDownload.legacyShieldUrl
                        } else {
                            false
                        }
                    }
                    if (isAvailableInSuccess != null) {
                        val isFallback =
                            (isAvailableInSuccess.value as RouteShield.Success.MapboxLegacyShield).isFromFallback
                        if (isFallback) {
                            requestLegacyOrFallback(
                                text = mapboxShield?.displayRef(),
                                legacyUrl,
                                fallbackToGeneric,
                                isAvailableInSuccess,
                                isAvailableInFailure
                            )
                        } else {
                            requestLegacyOrFallback(legacyUrl, fallbackToGeneric, null, isAvailableInFailure)
                        }
                    }
                }
            }
        }
    }

    private fun requestGeneric(text: String): RouteShield.Success {
        val sprite = ShieldSprite
            .builder()
            .spriteName("default-shield")
            .spriteAttributes(options.spriteAttributes)
            .build()
        return getGenericShield(text = text, sprite = sprite)
    }

    private fun requestLegacyOrFallback(
        text: String,
        legacyUrl: String,
        fallbackToGeneric: Boolean,
        wasAvailableAsFallback: Map.Entry<String,RouteShield.Success>?,
        isAvailableInFailure: Map.Entry<String,RouteShield.Failure>?
    ) {
        val requestId = UUID.randomUUID().toString()
        mainJob.scope.launch {
            val urlWithSvgExtension = legacyUrl.plus(SVG_EXTENSION)
            RoadShieldDownloader.downloadImage(urlWithSvgExtension).fold(
                {
                    when (fallbackToGeneric) {
                        true -> {
                            val generic = requestGeneric(text = text) as RouteShield.Success.GenericShield
                            val shield = RouteShield.Success.MapboxLegacyShield(
                                legacyUrl,
                                generic.shield,
                                true,
                                generic.sprite
                            )
                            if (isAvailableInFailure != null) {
                                shieldDownloadFailure.remove(isAvailableInFailure.key)
                            }
                            if (wasAvailableAsFallback != null) {
                                shieldDownloadSuccess.remove(wasAvailableAsFallback.key)
                            }
                            shieldDownloadSuccess[requestId] = shield
                        }
                        false -> {
                            if ((isAvailableInFailure != null &&
                                    isAvailableInFailure.value.message != it) ||
                                isAvailableInFailure == null) {
                                shieldDownloadFailure[requestId] = it
                            }
                        }
                    }
                },
                {
                    if (isAvailableInFailure != null) {
                        shieldDownloadFailure.remove(isAvailableInFailure.key)
                    }
                    if (wasAvailableAsFallback != null) {
                        shieldDownloadSuccess.remove(wasAvailableAsFallback.key)
                    }
                    shieldDownloadSuccess[requestId] = RouteShield.Success.MapboxLegacyShield(
                        legacyUrl, it, false, null
                    )
                }
            )
        }
    }

    /*private fun uponNoShieldSprite(
        legacyUrl: String?,
        mapboxShieldUrl: String,
        fallbackToLegacy: Boolean,
        fallbackToGeneric: Boolean,
        mapboxShield: MapboxShield
    ) {
        if (legacyUrl != null && fallbackToLegacy) {
            requestLegacyOrGeneric(
                text = mapboxShield.displayRef(),
                legacyUrl = legacyUrl,
                valueFun = { shield -> shieldDownloadFailure.add(shield) },
                errorFun = { shield -> shieldDownloadFailure.add(shield) },
                fallbackToGeneric = fallbackToGeneric
            )
        } else if (fallbackToGeneric) {
            val shield = requestGeneric(text = mapboxShield.displayRef())
            shieldDownloadSuccess.add(shield)
        } else {
            shieldDownloadFailure.add(
                RouteShield.Failure(
                    url = mapboxShieldUrl,
                    message = """
                        Shield sprite for $mapboxShield is null. Hence the request for 
                        $mapboxShieldUrl was not made.
                        """.trimIndent()
                )
            )
        }
    }

    private fun uponNoSpritePlaceholder(
        legacyUrl: String?,
        mapboxShieldUrl: String,
        fallbackToLegacy: Boolean,
        fallbackToGeneric: Boolean,
        mapboxShield: MapboxShield,
        shieldSprite: ShieldSprite
    ) {
        if (legacyUrl != null && fallbackToLegacy) {
            requestLegacyOrGeneric(
                text = mapboxShield.displayRef(),
                legacyUrl = legacyUrl,
                valueFun = { shield -> shieldDownloadFailure.add(shield) },
                errorFun = { shield -> shieldDownloadFailure.add(shield) },
                fallbackToGeneric = fallbackToGeneric
            )
        } else if (fallbackToGeneric) {
            val shield = requestGeneric(text = mapboxShield.displayRef())
            shieldDownloadSuccess.add(shield)
        } else {
            shieldDownloadFailure.add(
                RouteShield.Failure(
                    url = mapboxShieldUrl,
                    message = """
                        Mapbox shield($mapboxShield) referring to shield sprite($shieldSprite) 
                        has empty or null placeholder value. Hence the request for $mapboxShieldUrl 
                        was not made.
                        """.trimIndent()
                )
            )
        }
    }

    private fun uponAbleToMakeRequest(
        legacyUrl: String?,
        accessToken: String,
        mapboxShieldUrl: String,
        fallbackToLegacy: Boolean,
        fallbackToGeneric: Boolean,
        mapboxShield: MapboxShield,
        shieldSprite: ShieldSprite
    ) {
        if (legacyUrl != null && fallbackToLegacy) {
            requestMapboxShieldOrLegacy(
                shieldUrl = mapboxShieldUrl,
                legacyUrl = legacyUrl,
                sprite = shieldSprite,
                accessToken = accessToken,
                mapboxShield = mapboxShield,
                fallbackToLegacy = fallbackToLegacy,
                fallbackToGeneric = fallbackToGeneric,
                valueFun = { shield -> shieldDownloadSuccess.add(shield) },
                errorFun = { shield -> shieldDownloadFailure.add(shield) }
            )
        } else if (fallbackToGeneric) {
            requestMapboxShieldOrGeneric(
                url = mapboxShieldUrl,
                sprite = shieldSprite,
                accessToken = accessToken,
                mapboxShield = mapboxShield,
                fallbackToGeneric = fallbackToGeneric,
                valueFun = { shield -> shieldDownloadSuccess.add(shield) },
                errorFun = { shield -> shieldDownloadFailure.add(shield) }
            )
        }
        // what happens if legacy url is null and fallbackTogeneric + fallbacktolegacy are false
    }*/

    private fun requestMapboxShieldOrGeneric(
        url: String,
        accessToken: String,
        sprite: ShieldSprite,
        mapboxShield: MapboxShield,
        fallbackToGeneric: Boolean,
        valueFun: (RouteShield.Success) -> Unit,
        errorFun: (RouteShield.Failure) -> Unit
    ) {
        mainJob.scope.launch {
            val requestUrl = url.plus(REQUEST_ACCESS_TOKEN).plus(accessToken)
            RoadShieldDownloader.downloadImage(requestUrl).fold(
                {
                    when (fallbackToGeneric) {
                        true -> {
                            val generic = requestGeneric(text = mapboxShield.displayRef())
                            valueFun(
                                RouteShield.Success.MapboxDesignedShield(
                                    url = url,
                                    shield = generic.shield,
                                    isFromFallback = true,
                                    mapboxShield = mapboxShield,
                                    shieldSprite = generic.sprite
                                )
                            )
                        }
                        false -> {
                            errorFun(RouteShield.Failure(url = url, message = it))
                        }
                    }
                },
                {
                    val svgJson = String(it)
                    val svg = appendTextToShield(
                        text = mapboxShield.displayRef(),
                        shieldSvg = ShieldSvg.fromJson(svgJson).svg(),
                        spriteAttr = sprite.spriteAttributes()
                    ).toByteArray()
                    val shield = getMapboxShield(
                        shield = svg,
                        url = url,
                        sprite = sprite,
                        mapboxShield = mapboxShield,
                        isFromFallback = false
                    )
                    valueFun(shield)
                }
            )
        }
    }

    private fun requestMapboxShieldOrLegacy(
        shieldUrl: String,
        legacyUrl: String,
        accessToken: String,
        sprite: ShieldSprite,
        fallbackToLegacy: Boolean,
        fallbackToGeneric: Boolean,
        mapboxShield: MapboxShield,
        valueFun: (RouteShield.Success) -> Unit,
        errorFun: (RouteShield.Failure) -> Unit
    ) {
        mainJob.scope.launch {
            val requestUrl = shieldUrl.plus(REQUEST_ACCESS_TOKEN).plus(accessToken)
            RoadShieldDownloader.downloadImage(requestUrl).fold(
                {
                    when (fallbackToLegacy) {
                        true -> {
                            val message = """
                                For mapbox shield url: $shieldUrl an error was received with 
                                message: $it Invoking a network request for legacy url :$legacyUrl 
                                as fallback. 
                            """.trimIndent()
                            requestLegacyOrGeneric(
                                text = mapboxShield.displayRef(),
                                legacyUrl = legacyUrl,
                                valueFun = valueFun,
                                errorFun = errorFun,
                                fallbackToGeneric = fallbackToGeneric
                            )
                        }
                        false -> {
                            errorFun(RouteShield.Failure(shieldUrl, it))
                        }
                    }
                },
                {
                    val svgJson = String(it)
                    val svg = appendTextToShield(
                        text = mapboxShield.displayRef(),
                        shieldSvg = ShieldSvg.fromJson(svgJson).svg(),
                        spriteAttr = sprite.spriteAttributes()
                    ).toByteArray()
                    val shield = getMapboxShield(
                        shield = svg,
                        url = shieldUrl,
                        sprite = sprite,
                        mapboxShield = mapboxShield,
                        isFromFallback = false
                    )
                    valueFun(shield)
                }
            )
        }
    }

    private fun getGenericShield(
        text: String,
        sprite: ShieldSprite? = null,
    ): RouteShield.Success.GenericShield {
        return RouteShield.Success.GenericShield(
            shield = appendTextToShield(
                text, options.shieldSvg, sprite!!.spriteAttributes()
            ).toByteArray(),
            sprite = sprite,
        )
    }

    private fun getLegacyShield(
        url: String,
        shield: ByteArray,
        isFromFallback: Boolean,
        sprite: ShieldSprite?
    ): RouteShield.Success.MapboxLegacyShield {
        return RouteShield.Success.MapboxLegacyShield(
            url = url,
            shield = shield,
            isFromFallback = isFromFallback,
            shieldSprite = sprite
        )
    }

    private fun getMapboxShield(
        shield: ByteArray,
        url: String,
        sprite: ShieldSprite?,
        isFromFallback: Boolean,
        mapboxShield: MapboxShield
    ): RouteShield.Success.MapboxDesignedShield {
        return RouteShield.Success.MapboxDesignedShield(
            url = url,
            shield = shield,
            shieldSprite = sprite,
            mapboxShield = mapboxShield,
            isFromFallback = isFromFallback
        )
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
        shieldsToDownload: List<RouteShieldToDownload>
    )/*: RouteShieldResult*/ {
        /*return suspendCancellableCoroutine { continuation ->
            val callback = {
                check(!continuation.isCancelled)
                if (shieldsToDownload.all { toDownload ->
                        shieldDownloadSuccess.any {
                            it.url == toDownload.legacyShieldUrl ||
                                it.url == toDownload.mapboxShieldUrl
                        } ||
                            shieldDownloadFailure.any {
                                it.url == toDownload.legacyShieldUrl ||
                                    it.url == toDownload.mapboxShieldUrl
                            }
                    }
                ) {
                    continuation.resume(
                        RouteShieldResult(shieldDownloadSuccess, shieldDownloadFailure)
                    )
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
        }*/
    }
}
