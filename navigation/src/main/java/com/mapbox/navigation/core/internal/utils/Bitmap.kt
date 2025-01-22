package com.mapbox.navigation.core.internal.utils

import android.graphics.Bitmap
import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

@JvmSynthetic
internal fun Bitmap.encode(
    options: BitmapEncodeOptions = BitmapEncodeOptions.Builder().build(),
): ByteArray {
    val scaled = scale(options.width)

    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, options.compressQuality, out)
    return out.toByteArray()
}

/**
 * @param newWidth maximum width of an encoded screenshot
 */
@JvmSynthetic
internal fun Bitmap.scale(newWidth: Int): Bitmap {
    val finalWidth = min(this.width, newWidth)
    val newHeight = (finalWidth.toFloat() * height / width).roundToInt()
    return Bitmap.createScaledBitmap(this, finalWidth, newHeight, true)
}
