package com.mapbox.navigation.ui.maps.guidance.model

import android.graphics.Bitmap
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Size
import com.mapbox.maps.Style
import com.mapbox.turf.TurfConstants

/**
 * Options to allow customizing snapshot based GuidanceImage
 * @property size Size Define aspect ratio of the [Bitmap]
 * @property styleUri String Define the [Style] of the components in the GuidanceImage
 * @property edgeInsets EdgeInsets Define the padding
 * @property bitmapConfig Config Define bitmap configurations
 * @property shouldRenderSignpost Boolean Define whether API should render signposts
 */
class GuidanceImageOptions private constructor(
    val size: Size,
    val density: Float,
    val styleUri: String,
    val edgeInsets: EdgeInsets,
    val bitmapConfig: Bitmap.Config,
    val shouldRenderSignpost: Boolean,
    val cameraCenterDistanceUnit: String,
    val cameraCenterDistanceFromJunction: Double
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): GuidanceImageOptions.Builder = GuidanceImageOptions.Builder().apply {
        size(size)
        density(density)
        styleUri(styleUri)
        edgeInsets(edgeInsets)
        bitmapConfig(bitmapConfig)
        shouldRenderSignpost(shouldRenderSignpost)
        cameraCenterDistanceUnit(cameraCenterDistanceUnit)
        cameraCenterDistanceFromJunction(cameraCenterDistanceFromJunction)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GuidanceImageOptions

        if (size != other.size) return false
        if (density != other.density) return false
        if (styleUri != other.styleUri) return false
        if (edgeInsets != other.edgeInsets) return false
        if (bitmapConfig != other.bitmapConfig) return false
        if (shouldRenderSignpost != other.shouldRenderSignpost) return false
        if (cameraCenterDistanceUnit != other.cameraCenterDistanceUnit) return false
        if (cameraCenterDistanceFromJunction != other.cameraCenterDistanceFromJunction) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = size.hashCode()
        result = 31 * result + density.hashCode()
        result = 31 * result + styleUri.hashCode()
        result = 31 * result + edgeInsets.hashCode()
        result = 31 * result + bitmapConfig.hashCode()
        result = 31 * result + shouldRenderSignpost.hashCode()
        result = 31 * result + cameraCenterDistanceUnit.hashCode()
        result = 31 * result + cameraCenterDistanceFromJunction.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "GuidanceViewOptions(" +
            "size=$size, " +
            "density=$density, " +
            "styleUri=$styleUri, " +
            "edgeInsets=$edgeInsets, " +
            "bitmapConfig=$bitmapConfig, " +
            "shouldRenderSignpost=$shouldRenderSignpost, " +
            "cameraCenterDistanceUnit=$cameraCenterDistanceUnit, " +
            "cameraCenterDistanceFromJunction=$cameraCenterDistanceFromJunction" +
            ")"
    }

    /**
     * Build a new [GuidanceImageOptions]
     */
    class Builder {

        private var size: Size = Size(1024f, 512f)
        private var density: Float = 1f
        private var styleUri: String = Style.MAPBOX_STREETS
        private var bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888
        private var edgeInsets: EdgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        private var shouldRenderSignpost: Boolean = false
        private var cameraCenterDistanceUnit: String = TurfConstants.UNIT_METERS
        private var cameraCenterDistanceFromJunction: Double = 100.0

        fun size(size: Size): Builder =
            apply { this.size = size }

        fun density(density: Float): Builder =
            apply { this.density = density }

        fun styleUri(styleUri: String): Builder =
            apply { this.styleUri = styleUri }

        fun edgeInsets(edgeInsets: EdgeInsets): Builder =
            apply { this.edgeInsets = edgeInsets }

        fun bitmapConfig(bitmapConfig: Bitmap.Config): Builder =
            apply { this.bitmapConfig = bitmapConfig }

        fun shouldRenderSignpost(shouldRenderSignpost: Boolean): Builder =
            apply { this.shouldRenderSignpost = shouldRenderSignpost }

        fun cameraCenterDistanceUnit(cameraCenterDistanceUnit: String): Builder =
            apply { this.cameraCenterDistanceUnit = cameraCenterDistanceUnit }

        fun cameraCenterDistanceFromJunction(cameraCenterDistanceFromJunction: Double): Builder =
            apply { this.cameraCenterDistanceFromJunction = cameraCenterDistanceFromJunction }

        /**
         * Build the [GuidanceImageOptions]
         */
        fun build(): GuidanceImageOptions {
            return GuidanceImageOptions(
                size = size,
                density = density,
                styleUri = styleUri,
                edgeInsets = edgeInsets,
                bitmapConfig = bitmapConfig,
                shouldRenderSignpost = shouldRenderSignpost,
                cameraCenterDistanceUnit = cameraCenterDistanceUnit,
                cameraCenterDistanceFromJunction = cameraCenterDistanceFromJunction
            )
        }
    }
}
