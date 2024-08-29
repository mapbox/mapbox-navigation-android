package com.mapbox.navigation.tripdata.shield.model

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * A factory exposed to build a [RouteShield] object.
 */
@ExperimentalMapboxNavigationAPI
object RouteShieldFactory {

    /**
     * Build [RouteShield.MapboxLegacyShield] given appropriate arguments.
     */
    @JvmStatic
    fun buildRouteShield(
        downloadUrl: String,
        byteArray: ByteArray,
        initialUrl: String,
    ): RouteShield.MapboxLegacyShield =
        RouteShield.MapboxLegacyShield(downloadUrl, byteArray, initialUrl)

    /**
     * Build [RouteShield.MapboxDesignedShield] given appropriate arguments.
     */
    @JvmStatic
    fun buildRouteShield(
        downloadUrl: String,
        byteArray: ByteArray,
        mapboxShield: MapboxShield,
        shieldSprite: ShieldSprite,
    ): RouteShield.MapboxDesignedShield =
        RouteShield.MapboxDesignedShield(downloadUrl, byteArray, mapboxShield, shieldSprite)
}
