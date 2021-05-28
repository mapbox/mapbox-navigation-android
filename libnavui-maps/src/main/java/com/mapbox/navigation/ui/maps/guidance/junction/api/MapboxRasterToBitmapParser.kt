package com.mapbox.navigation.ui.maps.guidance.junction.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory

/**
 * Utility class that provides a method allowing to convert raster [ByteArray] to [Bitmap]
 */
internal object MapboxRasterToBitmapParser {

    /**
     * The function converts a raw raster in [ByteArray] to [Bitmap]
     *
     * @param raster raw data in [ByteArray]
     * @return [Expected] contains [Bitmap] if successful or error otherwise.
     */
    @JvmStatic
    fun parse(raster: ByteArray): Expected<String, Bitmap> {
        return when {
            raster.isNotEmpty() -> ExpectedFactory.createValue(
                BitmapFactory.decodeByteArray(raster, 0, raster.size)
            )
            else -> ExpectedFactory.createError("Error parsing raster to bitmap as raster is empty")
        }
    }
}
