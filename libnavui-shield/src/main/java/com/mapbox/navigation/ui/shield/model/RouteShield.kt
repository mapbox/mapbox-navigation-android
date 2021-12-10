package com.mapbox.navigation.ui.shield.model

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite

/**
 * Data structure that holds information about route shield.
 */
sealed class RouteShield {
    class MapboxLegacyShield internal constructor(
        val url: String,
        val shield: ByteArray,
    ) : RouteShield() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MapboxLegacyShield

            if (url != other.url) return false
            if (!shield.contentEquals(other.shield)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + shield.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "MapboxLegacyShield(" +
                "url='$url', " +
                "shield=${shield.contentToString()}, " +
                ")"
        }
    }

    class MapboxDesignedShield internal constructor(
        val url: String,
        val shield: ByteArray,
        val mapboxShield: MapboxShield,
        val shieldSprite: ShieldSprite?
    ) : RouteShield() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MapboxDesignedShield

            if (url != other.url) return false
            if (!shield.contentEquals(other.shield)) return false
            if (mapboxShield != other.mapboxShield) return false
            if (shieldSprite != other.shieldSprite) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + shield.contentHashCode()
            result = 31 * result + mapboxShield.hashCode()
            result = 31 * result + (shieldSprite?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "MapboxDesignedShield(" +
                "url='$url', " +
                "shield=${shield.contentToString()}, " +
                "mapboxShield=$mapboxShield, " +
                "shieldSprite=$shieldSprite" +
                ")"
        }
    }
}
