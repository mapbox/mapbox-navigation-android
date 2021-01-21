package com.mapbox.navigation.ui.maps.snapshotter.model

import android.content.Context
import android.graphics.Bitmap
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Size
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.snapshotter.api.MapboxSnapshotterApi

/**
 * A class that allows you to control the look of the snapshot that will be generated using
 * [MapboxSnapshotterApi]
 * @property size bitmap size
 * @property density bitmap density
 * @property styleUri style uri
 * @property edgeInsets padding for the snapshot
 * @property bitmapConfig bitmap config, either RGB565 or ARGB8888
 */
class MapboxSnapshotterOptions private constructor(
    /* fixme we shouldn't hold on to context,
     * we need to change the BuilderTest to unlock this use-case
     */
    private val context: Context,
    val size: Size,
    val density: Float,
    val styleUri: String,
    val edgeInsets: EdgeInsets,
    val bitmapConfig: Bitmap.Config
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder = Builder(context).also {
        it.size(size)
        it.density(density)
        it.styleUri(styleUri)
        it.edgeInsets(edgeInsets)
        it.bitmapConfig(bitmapConfig)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxSnapshotterOptions

        if (context != context) return false
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
        var result = context.hashCode()
        result = 31 * result + size.hashCode()
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
            "context=$context, " +
            "size=$size, " +
            "density=$density, " +
            "styleUri=$styleUri, " +
            "edgeInsets=$edgeInsets, " +
            "bitmapConfig=$bitmapConfig" +
            ")"
    }

    /**
     * Build a new [MapboxSnapshotterOptions]
     * @property context Context
     * @property size builder for bitmap size
     * @property density builder for bitmap density
     * @property styleUri builder for style uri
     * @property bitmapConfig builder for bitmap config
     * @property edgeInsets builder for snapshot padding
     * @constructor
     */
    class Builder(private val context: Context) {

        private var size = Size(
            context.resources.displayMetrics.widthPixels.toFloat(),
            context.resources.displayMetrics.widthPixels.toFloat() / 2
        )
        private var density = context.resources.displayMetrics.density
        private var styleUri = Style.MAPBOX_STREETS
        private var bitmapConfig = Bitmap.Config.ARGB_8888
        private var edgeInsets = EdgeInsets(
            80.0 * density, 0.0 * density, 0.0 * density, 0.0 * density
        )

        /**
         * apply bitmap size to the builder
         * @param size Size
         * @return Builder
         */
        fun size(size: Size): Builder =
            apply { this.size = size }

        /**
         * apply bitmap density to the builder
         * @param density Float
         * @return Builder
         */
        fun density(density: Float): Builder =
            apply { this.density = density }

        /**
         * apply style uri to the builder
         * @param styleUri String
         * @return Builder
         */
        fun styleUri(styleUri: String): Builder =
            apply { this.styleUri = styleUri }

        /**
         * apply snapshot padding to the builder
         * @param edgeInsets EdgeInsets
         * @return Builder
         */
        fun edgeInsets(edgeInsets: EdgeInsets): Builder =
            apply { this.edgeInsets = edgeInsets }

        /**
         * apply bitmap config to the builder
         * @param bitmapConfig Config
         * @return Builder
         */
        fun bitmapConfig(bitmapConfig: Bitmap.Config): Builder =
            apply { this.bitmapConfig = bitmapConfig }

        /**
         * Build the [MapboxSnapshotterOptions]
         */
        fun build(): MapboxSnapshotterOptions {
            return MapboxSnapshotterOptions(
                context = context,
                size = size,
                density = density,
                styleUri = styleUri,
                edgeInsets = edgeInsets,
                bitmapConfig = bitmapConfig
            )
        }
    }
}
