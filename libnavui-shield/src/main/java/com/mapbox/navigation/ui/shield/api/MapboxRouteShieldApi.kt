package com.mapbox.navigation.ui.shield.api

import android.graphics.drawable.VectorDrawable
import com.mapbox.api.directions.v5.models.*
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.shield.*
import com.mapbox.navigation.ui.shield.RoadShieldDownloader
import com.mapbox.navigation.ui.shield.RouteShieldAction
import com.mapbox.navigation.ui.shield.RouteShieldProcessor
import com.mapbox.navigation.ui.shield.RouteShieldResult
import com.mapbox.navigation.ui.shield.model.*
import com.mapbox.navigation.ui.shield.model.RouteSprite
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.LoggerProvider
import kotlinx.coroutines.launch

/**
 * A class that can be used to request route shields using either [BannerComponents.imageBaseUrl]
 * or [BannerComponents.mapboxShield]. The class exposes three different API(s) that allows you to
 * either always use [BannerComponents.imageBaseUrl] or use [BannerComponents.mapboxShield] or allow
 * SDK to decide smartly to use one or the other.
 *
 * [MapboxRouteShieldApi] returns route shield in the form of a SVG wrapped in a [ByteArray].
 * Since this SVG would later have to be converted to vector drawable at runtime for rendering
 * purposes, it is important to note that Android platform supports all [VectorDrawable] features
 * from [Tiny SVG 1.2](https://www.w3.org/TR/SVGTiny12/) except for text.
 * More can be read [here](https://developer.android.com/studio/write/vector-asset-studio#svg-support)
 * about support and restrictions for SVG files.
 *
 * @property accessToken
 * @property options MapboxRouteShieldOptions used to specify the default shields in case shield
 * url is unavailable or failure to download the shields.
 */
class MapboxRouteShieldApi @JvmOverloads constructor(
    private val accessToken: String,
    private val options: MapboxRouteShieldOptions = MapboxRouteShieldOptions.Builder().build()
) {

    internal companion object {
        private val TAG = Tag("MbxRouteShieldApi")
        private const val SPRITE = "/sprite"
        private const val MINIMUM_DISPLAY_REF_LENGTH = 2
        private const val MAXIMUM_DISPLAY_REF_LENGTH = 6
    }

    private val contentManager = RoadShieldContentManager()
    private val mainJob = InternalJobControlFactory.createMainScopeJobControl()

    fun getRouteShields(
        bannerInstructions: List<BannerInstructions>,
        callback: RouteShieldCallback
    ) {
        val routeShieldToDownload = mutableListOf<RouteShieldToDownload>()
        bannerInstructions.forEach { bannerInstruction ->
            val primaryBannerComponents = bannerInstruction.primary().components()
            val secondaryBannerComponents = bannerInstruction.secondary()?.components()
            val subBannerComponents = bannerInstruction.sub()?.components()
            primaryBannerComponents?.forEach { component ->
                if (component.type() == BannerComponents.ICON) {
                    val legacyShieldUrl = component.imageBaseUrl()
                    routeShieldToDownload.add(
                        RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                    )
                }
            }
            secondaryBannerComponents?.forEach { component ->
                if (component.type() == BannerComponents.ICON) {
                    val legacyShieldUrl = component.imageBaseUrl()
                    routeShieldToDownload.add(
                        RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                    )
                }
            }
            subBannerComponents?.forEach { component ->
                if (component.type() == BannerComponents.ICON) {
                    val legacyShieldUrl = component.imageBaseUrl()
                    routeShieldToDownload.add(
                        RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                    )
                }
            }
        }
        mainJob.scope.launch {
            if (routeShieldToDownload.isNotEmpty()) {
                val result = contentManager.getShields(
                    accessToken = accessToken,
                    fallbackToLegacy = false,
                    shieldsToDownload = routeShieldToDownload
                )
                callback.onRoadShields(shields = result)
            }
        }
    }

    fun getRouteShields(
        userId: String,
        styleId: String,
        bannerInstructions: List<BannerInstructions>,
        callback: RouteShieldCallback,
        fallbackToLegacy: Boolean
    ) {
        requestSprite(userId = userId, styleId = styleId) { routeSprite ->
            val routeShieldToDownload = mutableListOf<RouteShieldToDownload>()
            bannerInstructions.forEach { bannerInstruction ->
                val primaryBannerComponents = bannerInstruction.primary().components()
                val secondaryBannerComponents = bannerInstruction.secondary()?.components()
                val subBannerComponents = bannerInstruction.sub()?.components()
                primaryBannerComponents?.forEach { component ->
                    if (component.type() == BannerComponents.ICON) {
                        val mapboxShield = component.mapboxShield()
                        val mapboxShieldUrl =
                            generateShieldUrl(userId, styleId, mapboxShield)
                        val legacyShieldUrl = component.imageBaseUrl()
                        val shieldSprite = getShieldSprite(
                            spriteUrl = routeSprite.spriteUrl,
                            shieldName = mapboxShield?.name(),
                            displayRef = mapboxShield?.displayRef()
                        )
                        routeShieldToDownload.add(
                            RouteShieldToDownload.MapboxDesign(
                                shieldSprite = shieldSprite,
                                mapboxShield = mapboxShield,
                                url = mapboxShieldUrl,
                                legacy = RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                            )
                        )
                    }
                }
                secondaryBannerComponents?.forEach { component ->
                    if (component.type() == BannerComponents.ICON) {
                        val mapboxShield = component.mapboxShield()
                        val mapboxShieldUrl =
                            generateShieldUrl(userId, styleId, mapboxShield)
                        val legacyShieldUrl = component.imageBaseUrl()
                        val shieldSprite = getShieldSprite(
                            spriteUrl = routeSprite.spriteUrl,
                            shieldName = mapboxShield?.name(),
                            displayRef = mapboxShield?.displayRef()
                        )
                        routeShieldToDownload.add(
                            RouteShieldToDownload.MapboxDesign(
                                shieldSprite = shieldSprite,
                                mapboxShield = mapboxShield,
                                url = mapboxShieldUrl,
                                legacy = RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                            )
                        )
                    }
                }
                subBannerComponents?.forEach { component ->
                    if (component.type() == BannerComponents.ICON) {
                        val mapboxShield = component.mapboxShield()
                        val mapboxShieldUrl =
                            generateShieldUrl(userId, styleId, mapboxShield)
                        val legacyShieldUrl = component.imageBaseUrl()
                        val shieldSprite = getShieldSprite(
                            spriteUrl = routeSprite.spriteUrl,
                            shieldName = mapboxShield?.name(),
                            displayRef = mapboxShield?.displayRef()
                        )
                        routeShieldToDownload.add(
                            RouteShieldToDownload.MapboxDesign(
                                shieldSprite = shieldSprite,
                                mapboxShield = mapboxShield,
                                url = mapboxShieldUrl,
                                legacy = RouteShieldToDownload.MapboxLegacy(url = legacyShieldUrl)
                            )
                        )
                    }
                }
            }
            mainJob.scope.launch {
                if (routeShieldToDownload.isNotEmpty()) {
                    val result = contentManager.getShields(
                        accessToken = accessToken,
                        fallbackToLegacy = fallbackToLegacy,
                        shieldsToDownload = routeShieldToDownload
                    )
                    callback.onRoadShields(shields = result)
                }
            }
        }
    }

    private fun generateShieldUrl(
        userId: String,
        styleId: String,
        mapboxShield: MapboxShield?
    ): String? {
        if (mapboxShield == null) {
            return null
        }
        val refLen = when  {
            mapboxShield.displayRef().length <= 1 -> { MINIMUM_DISPLAY_REF_LENGTH }
            mapboxShield.displayRef().length > 6 -> { MAXIMUM_DISPLAY_REF_LENGTH }
            else -> { mapboxShield.displayRef().length }
        }
        return mapboxShield.baseUrl()
            .plus(userId)
            .plus("/$styleId")
            .plus(SPRITE)
            .plus("/${mapboxShield.name()}")
            .plus("-$refLen")
    }

    private fun getShieldSprite(
        spriteUrl: String,
        shieldName: String?,
        displayRef: String?
    ): ShieldSprite? {
        if (shieldName == null || displayRef == null) {
            return null
        }
        val action = RouteShieldAction.GetSprite(
            spriteUrl = spriteUrl,
            shieldName = shieldName,
            displayRef = displayRef
        )
        return when (val result = RouteShieldProcessor.process(action)) {
            is RouteShieldResult.OnSprite -> result.sprite
            else -> null
        }
    }

    private fun requestSprite(
        userId: String,
        styleId: String,
        consumer: MapboxNavigationConsumer<RouteSprite>
    ) {
        val action = RouteShieldAction.GenerateSpriteUrl(
            userId = userId,
            styleId = styleId,
            accessToken = accessToken
        )
        when (val result = RouteShieldProcessor.process(action)) {
            is RouteShieldResult.OnSpriteUrl -> {
                val spriteUrl = result.url
                val routeSprite = getSprites(spriteUrl)
                if (routeSprite.sprites.isEmpty()) {
                    mainJob.scope.launch {
                        RoadShieldDownloader.downloadImage(spriteUrl).fold(
                            { error ->
                                LoggerProvider.logger.e(TAG, Message(error))
                                consumer.accept(
                                    RouteSprite(spriteUrl = spriteUrl, sprites = emptyList())
                                )
                            },
                            { spriteJson ->
                                parseSprite(
                                    url = spriteUrl,
                                    data = spriteJson,
                                    errorFun = { error ->
                                        LoggerProvider.logger.e(TAG, Message(error))
                                        consumer.accept(
                                            RouteSprite(spriteUrl = spriteUrl, sprites = emptyList())
                                        )
                                    },
                                    valueFun = { routeSprite ->
                                        consumer.accept(routeSprite)
                                    }
                                )
                            }
                        )
                    }
                } else {
                    consumer.accept(routeSprite)
                }
            }
            else -> {
                LoggerProvider.logger.e(TAG, Message("Incorrect $result emitted for $action."))
                consumer.accept(RouteSprite(spriteUrl = "", sprites = emptyList()))
            }
        }
    }

    private fun getSprites(url: String): RouteSprite {
        val action = RouteShieldAction.SpritesAvailable(url)
        return when (val result = RouteShieldProcessor.process(action)) {
            is RouteShieldResult.Sprites.Available -> {
                result.sprite
            }
            else -> {
                RouteSprite(spriteUrl = url, sprites = emptyList())
            }
        }
    }

    private fun parseSprite(
        url: String,
        data: ByteArray,
        errorFun: (String) -> Unit,
        valueFun: (RouteSprite) -> Unit
    ) {
        val action = RouteShieldAction.ParseSprite(url, String(data))
        when (val result = RouteShieldProcessor.process(action)) {
            is RouteShieldResult.GenerateSprite.Success -> {
                valueFun(
                    result.sprite
                )
            }
            is RouteShieldResult.GenerateSprite.Failure -> {
                errorFun(
                    result.error
                )
            }
            else -> {
                errorFun(
                    "Inappropriate $result emitted for $action."
                )
            }
        }
    }
}
