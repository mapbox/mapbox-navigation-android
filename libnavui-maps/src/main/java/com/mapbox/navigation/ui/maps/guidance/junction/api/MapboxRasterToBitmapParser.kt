package com.mapbox.navigation.ui.maps.guidance.junction.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mapbox.navigation.ui.base.model.Expected

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
    fun parse(raster: ByteArray): Expected<Bitmap, String> {
        return when {
            raster.isNotEmpty() -> Expected.Success(
                BitmapFactory.decodeByteArray(raster, 0, raster.size)
            )
            else -> Expected.Failure("Error parsing raster to bitmap as raster is empty")
        }
    }
}
