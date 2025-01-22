package com.mapbox.navigation.ui.maps.guidance.junction.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.utils.internal.ByteBufferBackedInputStream
import com.mapbox.navigation.utils.internal.isNotEmpty

/**
 * Utility class that provides a method allowing to convert raster [ByteArray] to [Bitmap]
 */
internal object MapboxRasterToBitmapParser {

    /**
     * The function converts a raw raster in [DataRef] to [Bitmap]
     *
     * @param raster raw data in [ByteArray]
     * @return [Expected] contains [Bitmap] if successful or error otherwise.
     */
    @JvmStatic
    fun parse(raster: DataRef): Expected<String, Bitmap> {
        return when {
            raster.isNotEmpty() -> ByteBufferBackedInputStream(raster.buffer).use {
                BitmapFactory.decodeStream(it)
            }
                ?.let { ExpectedFactory.createValue(it) }
                ?: ExpectedFactory.createError("Raster is not a valid bitmap")
            else -> ExpectedFactory.createError("Error parsing raster to bitmap as raster is empty")
        }
    }
}
