package com.mapbox.navigation.dropin.component.marker

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.navigation.dropin.util.BitmapMemoryCache

/**
 * Factory class for creating all drop-in UI map point annotations.
 */
internal class MapMarkerFactory(
    val context: Context,
    val cache: BitmapMemoryCache,
) {
    fun createPin(
        point: Point,
        @DrawableRes iconImage: Int,
    ): PointAnnotationOptions {
        return PointAnnotationOptions()
            .withPoint(point)
            .withIconAnchor(IconAnchor.BOTTOM)
            .withIconImage(
                loadBitmap(iconImage)
            )
    }

    private fun loadBitmap(@DrawableRes drawableId: Int): Bitmap {
        val name = context.resources.getResourceName(drawableId)
        cache.get(name)?.also { return it }

        return ContextCompat.getDrawable(context, drawableId)?.toBitmap()?.also {
            cache.add(name, it)
        } ?: throw RuntimeException("Cannot load bitmap drawable: $name")
    }

    companion object {
        fun create(context: Context): MapMarkerFactory {
            return MapMarkerFactory(
                context,
                BitmapMemoryCache(4 * BitmapMemoryCache.MB_IN_BYTES)
            )
        }
    }
}
