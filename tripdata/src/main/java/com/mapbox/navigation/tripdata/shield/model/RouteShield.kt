package com.mapbox.navigation.tripdata.shield.model

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.TypedValue
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.api.directions.v5.models.ShieldSprite
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigation.utils.internal.obfuscateAccessToken
import java.io.ByteArrayInputStream

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
     * Invoke the method to convert [RouteShield.byteArray] to [Bitmap].
     */
    abstract fun toBitmap(resources: Resources, desiredHeight: Int? = null): Bitmap?

    /**
     * Type representation of [RouteShield] which is to be used when requesting mapbox legacy shields.
     * @property url used to download the shield
     * @property byteArray shield image returned in the form of svg wrapped in a [ByteArray]
     * @property initialUrl the original legacy url obtained from directions response pointing to
     * a shield. This initial url can be used to match the url from the maneuver object.
     */
    class MapboxLegacyShield internal constructor(
        url: String,
        byteArray: ByteArray,
        val initialUrl: String,
    ) : RouteShield(url = url, byteArray = byteArray) {

        private companion object {
            private const val DEFAULT_HEIGHT_FOR_LEGACY_DIP = 36f
        }

        /**
         * Regenerate whenever a change is made
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MapboxLegacyShield

            if (url != other.url) return false
            if (!byteArray.contentEquals(other.byteArray)) return false
            if (initialUrl != other.initialUrl) return false

            return true
        }

        /**
         * Regenerate whenever a change is made
         */
        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + byteArray.contentHashCode()
            result = 31 * result + initialUrl.hashCode()
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "MapboxLegacyShield(" +
                "url='${url.obfuscateAccessToken()}', " +
                "byteArray=${byteArray.contentToString()}, " +
                "initialUrl=${initialUrl.obfuscateAccessToken()}" +
                ")"
        }

        /**
         * Invoke the method to convert [RouteShield.MapboxLegacyShield.byteArray] to [Bitmap].
         *
         * @param desiredHeight desired height of the bitmap in pixel. Width is calculated automatically to
         * maintain the aspect ratio. If not specified, height is defaulted to 36dp
         */
        override fun toBitmap(resources: Resources, desiredHeight: Int?): Bitmap? {
            val heightPx = desiredHeight
                ?: TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    DEFAULT_HEIGHT_FOR_LEGACY_DIP,
                    resources.displayMetrics,
                ).toInt()
            val stream = ByteArrayInputStream(byteArray)
            return SvgUtil.renderAsBitmapWithHeight(stream, heightPx)
        }

        /**
         * Invoke the method to compare two legacy shields based on shield url
         *
         * @param other legacy shield url
         */
        fun compareWith(other: String?): Boolean {
            return this.initialUrl == other
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
        val shieldSprite: ShieldSprite,
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
                "url='${url.obfuscateAccessToken()}', " +
                "byteArray=${byteArray.contentToString()}, " +
                "mapboxShield=$mapboxShield, " +
                "shieldSprite=$shieldSprite" +
                ")"
        }

        /**
         * Invoke the method to convert [RouteShield.MapboxDesignedShield.byteArray] to [Bitmap].
         *
         * @param desiredHeight desired height of the bitmap in pixel. Width is calculated automatically to
         * maintain the aspect ratio. If not specified, height and width are obtained from
         * [RouteShield.MapboxDesignedShield.shieldSprite] associated with the shield.
         */
        override fun toBitmap(resources: Resources, desiredHeight: Int?): Bitmap? {
            return if (desiredHeight != null) {
                val stream = ByteArrayInputStream(byteArray)
                SvgUtil.renderAsBitmapWithHeight(stream, desiredHeight)
            } else {
                val spriteWidth =
                    shieldSprite.spriteAttributes().width().toFloat()
                val spriteHeight =
                    shieldSprite.spriteAttributes().height().toFloat()
                val widthPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    spriteWidth,
                    resources.displayMetrics,
                ).toInt()
                val heightPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    spriteHeight,
                    resources.displayMetrics,
                ).toInt()
                val stream = ByteArrayInputStream(byteArray)
                SvgUtil.renderAsBitmapWith(stream, widthPx, heightPx)
            }
        }

        /**
         * Invoke the method to compare two shields based on shield data
         *
         * @param other mapbox designed shield
         */
        fun compareWith(other: MapboxShield?): Boolean {
            return mapboxShield == other
        }
    }
}
