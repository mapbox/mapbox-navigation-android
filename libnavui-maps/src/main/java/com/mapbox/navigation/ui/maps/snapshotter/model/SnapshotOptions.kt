package com.mapbox.navigation.ui.maps.snapshotter.model

import android.graphics.Bitmap
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Size
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.snapshotter.api.MapboxSnapshotterApi
import com.mapbox.turf.TurfConstants

/**
 * A class that allows you to control the look of the snapshot that will be generated using
 * [MapboxSnapshotterApi]
 */
class SnapshotOptions private constructor(
    val size: Size,
    val density: Float,
    val styleUri: String,
    val edgeInsets: EdgeInsets,
    val bitmapConfig: Bitmap.Config
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        size(size)
        density(density)
        styleUri(styleUri)
        edgeInsets(edgeInsets)
        bitmapConfig(bitmapConfig)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SnapshotOptions

        if (size != other.size) return false
        if (density != other.density) return false
        if (styleUri != other.styleUri) return false
        if (edgeInsets != other.edgeInsets) return false
        if (bitmapConfig != other.bitmapConfig) return false

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
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SnapshotOptions(" +
            "size=$size, " +
            "density=$density, " +
            "styleUri=$styleUri, " +
            "edgeInsets=$edgeInsets, " +
            "bitmapConfig=$bitmapConfig" +
            ")"
    }

    /**
     * Build a new [SnapshotOptions]
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
         * Build the [SnapshotOptions]
         */
        fun build(): SnapshotOptions {
            return SnapshotOptions(
                size = size,
                density = density,
                styleUri = styleUri,
                edgeInsets = edgeInsets,
                bitmapConfig = bitmapConfig
            )
        }
    }
}
