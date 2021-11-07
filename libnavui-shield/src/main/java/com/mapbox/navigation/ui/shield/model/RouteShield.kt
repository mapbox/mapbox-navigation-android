package com.mapbox.navigation.ui.shield.model

import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.navigation.ui.shield.api.toBitmap

/**
 * Data structure that holds information about route shield.
 *
 * To convert the returned SVG [ByteArray] to a [Bitmap] use [toBitmap].
 *
 * @property url used to download the shield
 * @property byteArray shield image returned in the form of svg wrapped in a [ByteArray]
 */
sealed class RouteShield(
    val url: String,
    val byteArray: ByteArray,
) {
    /**
     * Type representation of [RouteShield] which is to be used when requesting mapbox legacy shields.
     * @property url used to download the shield
     * @property byteArray shield image returned in the form of svg wrapped in a [ByteArray]
     */
    class MapboxLegacyShield internal constructor(
        url: String,
        byteArray: ByteArray,
    ) : RouteShield(url = url, byteArray = byteArray) {

        /**
         * Regenerate whenever a change is made
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MapboxLegacyShield

            if (url != other.url) return false
            if (!byteArray.contentEquals(other.byteArray)) return false

            return true
        }

        /**
         * Regenerate whenever a change is made
         */
        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + byteArray.contentHashCode()
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "MapboxLegacyShield(" +
                "url='$url', " +
                "byteArray=${byteArray.contentToString()}" +
                ")"
        }
    }

    /**
     * Type representation of [RouteShield] which is to be used when requesting mapbox designed shields.
     * @property url used to download the shield
     * @property byteArray shield image returned in the form of svg wrapped in a [ByteArray]
     * @property mapboxShield snippet of shield information associated with route
     * @property shieldSprite a sprite associated with the [mapboxShield] that would help in rendering
     * the [byteArray]
     */
    class MapboxDesignedShield internal constructor(
        url: String,
        byteArray: ByteArray,
        val mapboxShield: MapboxShield,
        val shieldSprite: ShieldSprite
    ) : RouteShield(url, byteArray) {

        /**
         * Regenerate whenever a change is made
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MapboxDesignedShield

            if (url != other.url) return false
            if (!byteArray.contentEquals(other.byteArray)) return false
            if (mapboxShield != other.mapboxShield) return false
            if (shieldSprite != other.shieldSprite) return false

            return true
        }

        /**
         * Regenerate whenever a change is made
         */
        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + byteArray.contentHashCode()
            result = 31 * result + mapboxShield.hashCode()
            result = 31 * result + (shieldSprite.hashCode())
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "MapboxDesignedShield(" +
                "url='$url', " +
                "byteArray=${byteArray.contentToString()}, " +
                "mapboxShield=$mapboxShield, " +
                "shieldSprite=$shieldSprite" +
                ")"
        }
    }
}
