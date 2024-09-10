package com.mapbox.navigation.tripdata.shield.api

import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.common.MapboxOptions
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.tripdata.shield.RoadShieldContentManagerContainer
import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.internal.model.ShieldSpriteToDownload
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.tripdata.shield.model.RouteShieldCallback
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.launch

/**
 * A class that can be used to request route shields using either [BannerComponents.imageBaseUrl]
 * or [BannerComponents.mapboxShield]. The class exposes API(s) that one can invoke to
 * either always use mapbox legacy shields [BannerComponents.imageBaseUrl] or use mapbox
 * designed shields [BannerComponents.mapboxShield]. In cases where API is unable to download
 * mapbox designed shields for any reason, the API falls back to download legacy shields if available.
 * Returns error otherwise.
 *
 * [MapboxRouteShieldApi] returns route shield with text in the form of a SVG wrapped in a [ByteArray].
 * Since this SVG would later have to be converted to vector drawable at runtime for rendering
 * purposes, it is important to note that the API supports most of the SVG features
 * from [Tiny SVG 1.2](https://www.w3.org/TR/SVGTiny12/).
 */
class MapboxRouteShieldApi {

    private val contentManager = RoadShieldContentManagerContainer
    private val mainJob = InternalJobControlFactory.createMainScopeJobControl()

    /**
     * Given a list of [BannerComponents] the function requests mapbox legacy road shields (if available)
     * using [BannerComponents.imageBaseUrl].
     *
     * If you do not wish to download all of the shields at once, make sure to pass in only a
     * list of banner components for which you'd like download the road shields.
     *
     * The function is safe to be called repeatably, all the results are cached in-memory
     * and requests are managed to avoid duplicating network bandwidth usage.
     *
     * The function returns list of [RouteShieldResult] or [RouteShieldError] in
     * [RouteShieldCallback.onRoadShields].
     *
     * To convert the returned in [RouteShield] SVG [ByteArray] to a [Bitmap] use [toBitmap].
     *
     * @param bannerComponents list of banner components
     * @param callback invoked with appropriate result
     * @see toBitmap
     */
    fun getRouteShields(
        bannerComponents: List<BannerComponents>?,
        callback: RouteShieldCallback,
    ) {
        getRouteShields(
            bannerComponents = bannerComponents,
            userId = null,
            styleId = null,
            callback = callback,
        )
    }

    /**
     * Given a list of [BannerComponents] the function requests mapbox designed road shields (if available)
     * using [BannerComponents.mapboxShield]. If for any reason the API is unable to download the
     * shield, it falls back to download and return the legacy shield if available. Returns error
     * otherwise.
     *
     * If you do not wish to download all of the shields at once, make sure to pass in only a
     * list of banner components for which you'd like download the road shields.
     *
     * The function is safe to be called repeatably, all the results are cached in-memory
     * and requests are managed to avoid duplicating network bandwidth usage.
     *
     * The function returns list of [RouteShieldResult] or [RouteShieldError] in
     * [RouteShieldCallback.onRoadShields].
     *
     * To convert the returned in [RouteShield] SVG [ByteArray] to a [Bitmap] use [toBitmap].
     *
     * @param bannerComponents list of banner components
     * @param userId Mapbox user account name
     * @param styleId style id used to render the map
     * @param callback invoked with appropriate result
     * @see toBitmap
     */
    fun getRouteShields(
        bannerComponents: List<BannerComponents>?,
        userId: String?,
        styleId: String?,
        callback: RouteShieldCallback,
    ) {
        val accessToken = MapboxOptions.accessToken
        val routeShieldToDownload = mutableListOf<RouteShieldToDownload>()
        routeShieldToDownload.addAll(
            bannerComponents?.findShieldsToDownload(
                accessToken = accessToken,
                userId = userId,
                styleId = styleId,
            ) ?: emptyList(),
        )
        getRouteShieldsInternal(routeShieldToDownload, callback)
    }

    /**
     * Given a [Road] object, the function requests mapbox legacy road shields (if available)
     * using [Road.components] shields url for the current road.
     *
     * The function returns list of [RouteShieldResult] or [RouteShieldError] in
     * [RouteShieldCallback.onRoadShields].
     *
     * To convert the returned in [RouteShield] SVG [ByteArray] to a [Bitmap] use [toBitmap].
     *
     * @param road object representing current road
     * @param callback invoked with appropriate result
     * @see toBitmap
     */
    fun getRouteShields(
        road: Road,
        callback: RouteShieldCallback,
    ) {
        getRouteShields(
            road = road,
            userId = null,
            styleId = null,
            callback = callback,
        )
    }

    /**
     * Given a [Road] object, the function requests mapbox designed road shields (if available)
     * using [Road.mapboxShield] for the current road. If for any reason the API is unable to
     * download the shield, it falls back to download and return the legacy shield if available.
     * Returns error otherwise.
     *
     * The function returns list of [RouteShieldResult] or [RouteShieldError] in
     * [RouteShieldCallback.onRoadShields].
     *
     * To convert the returned in [RouteShield] SVG [ByteArray] to a [Bitmap] use [toBitmap].
     *
     * @param road object representing current road
     * @param userId Mapbox user account name
     * @param styleId style id used to render the map
     * @param callback invoked with appropriate result
     * @see toBitmap
     */
    fun getRouteShields(
        road: Road,
        userId: String?,
        styleId: String?,
        callback: RouteShieldCallback,
    ) {
        val routeShieldToDownload = road.findShieldsToDownload(
            userId = userId,
            styleId = styleId,
            accessToken = MapboxOptions.accessToken,
        )
        getRouteShieldsInternal(routeShieldToDownload, callback)
    }

    /**
     * Invoke the function to cancel any job invoked through other APIs
     */
    fun cancel() {
        contentManager.cancelAll()
        mainJob.job.children.forEach {
            it.cancel()
        }
    }

    internal fun getRouteShieldsInternal(
        shieldsToDownload: List<RouteShieldToDownload>,
        callback: RouteShieldCallback,
    ) {
        mainJob.scope.launch {
            val result = contentManager.getShields(shieldsToDownload)
            callback.onRoadShields(result)
        }
    }

    private fun List<BannerComponents>.findShieldsToDownload(
        userId: String?,
        styleId: String?,
        accessToken: String,
    ): List<RouteShieldToDownload> {
        return this.mapNotNull { component ->
            if (component.type() == BannerComponents.ICON) {
                val legacyShieldUrl = component.imageBaseUrl()
                val legacy = if (legacyShieldUrl != null) {
                    RouteShieldToDownload.MapboxLegacy(legacyShieldUrl)
                } else {
                    null
                }
                val mapboxShield = component.mapboxShield()
                val designed = if (
                    userId != null && styleId != null &&
                    mapboxShield != null
                ) {
                    RouteShieldToDownload.MapboxDesign(
                        ShieldSpriteToDownload(
                            userId = userId,
                            styleId = styleId,
                        ),
                        accessToken = accessToken,
                        mapboxShield = mapboxShield,
                        legacyFallback = legacy,
                    )
                } else {
                    null
                }

                designed ?: legacy
            } else {
                null
            }
        }
    }

    private fun Road.findShieldsToDownload(
        accessToken: String,
        userId: String?,
        styleId: String?,
    ): List<RouteShieldToDownload> {
        val routeShieldToDownload = mutableListOf<RouteShieldToDownload>()
        components.forEach { roadComponent ->
            val legacyShieldUrl = roadComponent.imageBaseUrl
            val legacy = if (legacyShieldUrl != null) {
                RouteShieldToDownload.MapboxLegacy(legacyShieldUrl)
            } else {
                null
            }
            val mapboxDesign = if (
                userId != null &&
                styleId != null &&
                roadComponent.shield != null
            ) {
                RouteShieldToDownload.MapboxDesign(
                    ShieldSpriteToDownload(
                        userId = userId,
                        styleId = styleId,
                    ),
                    accessToken = accessToken,
                    mapboxShield = roadComponent.shield!!,
                    legacyFallback = legacy,
                )
            } else {
                null
            }
            if (mapboxDesign != null) {
                routeShieldToDownload.add(mapboxDesign)
            } else if (legacy != null) {
                routeShieldToDownload.add(legacy)
            }
        }
        return routeShieldToDownload
    }
}
