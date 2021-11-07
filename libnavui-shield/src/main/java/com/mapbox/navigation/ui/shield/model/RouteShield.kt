package com.mapbox.navigation.ui.shield.model

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite

/**
 * Data structure that holds information about route shield.
 */
sealed class RouteShield {
    sealed class Success : RouteShield() {
        class GenericShield internal constructor(
            val shield: ByteArray,
            val sprite: ShieldSprite
        ) : Success() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as GenericShield

                if (!shield.contentEquals(other.shield)) return false
                if (sprite != other.sprite) return false

                return true
            }

            override fun hashCode(): Int {
                var result = shield.contentHashCode()
                result = 31 * result + sprite.hashCode()
                return result
            }

            override fun toString(): String {
                return "GenericShield(shield=${shield.contentToString()}, sprite=$sprite)"
            }

        }

        class MapboxLegacyShield internal constructor(
            val url: String,
            val shield: ByteArray,
            val isFromFallback: Boolean,
            val shieldSprite: ShieldSprite?
        ) : Success() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as MapboxLegacyShield

                if (url != other.url) return false
                if (!shield.contentEquals(other.shield)) return false
                if (isFromFallback != other.isFromFallback) return false
                if (shieldSprite != other.shieldSprite) return false

                return true
            }

            override fun hashCode(): Int {
                var result = url.hashCode()
                result = 31 * result + shield.contentHashCode()
                result = 31 * result + isFromFallback.hashCode()
                result = 31 * result + (shieldSprite?.hashCode() ?: 0)
                return result
            }

            override fun toString(): String {
                return "MapboxLegacyShield(" +
                    "url='$url', " +
                    "shield=${shield.contentToString()}, " +
                    "isFromFallback=$isFromFallback, " +
                    "shieldSprite=$shieldSprite" +
                    ")"
            }
        }

        class MapboxDesignedShield internal constructor(
            val url: String,
            val shield: ByteArray,
            val isFromFallback: Boolean,
            val mapboxShield: MapboxShield,
            val shieldSprite: ShieldSprite?
        ) : Success() {

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
                result = 31 * result + shieldSprite.hashCode()
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

    class Failure internal constructor(
        val url: String,
        val message: String
    ): RouteShield() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Failure

            if (url != other.url) return false
            if (message != other.message) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + message.hashCode()
            return result
        }

        override fun toString(): String {
            return "Failure(url='$url', message='$message')"
        }

    }
}
