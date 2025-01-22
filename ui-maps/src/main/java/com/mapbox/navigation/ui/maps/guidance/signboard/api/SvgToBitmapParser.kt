package com.mapbox.navigation.ui.maps.guidance.signboard.api

import android.graphics.Bitmap
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import java.nio.ByteBuffer

/**
 * An interface that exposes a function to allow the conversion of svg in a raw [ByteArray]
 * format to [Expected] format that either holds a [Bitmap] if the conversion is successful
 * or an error if it is not.
 */
fun interface SvgToBitmapParser {

    /**
     * The function converts a raw svg in [ByteArray] to [Bitmap] using [MapboxSignboardOptions]
     * specified.
     *
     * @param svg raw data in [ByteArray]
     * @param options additional option to format the svg.
     * @return [Expected] contains [Bitmap] if successful or error otherwise.
     */
    fun parse(
        svg: ByteBuffer,
        options: MapboxSignboardOptions,
    ): Expected<String, Bitmap>
}
