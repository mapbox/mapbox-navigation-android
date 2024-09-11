package com.mapbox.navigation.core.internal.utils

import android.graphics.Bitmap
import android.util.Base64
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.core.telemetry.events.BitmapEncodeOptions
import com.mapbox.navigator.ScreenshotFormat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.math.min
import kotlin.math.roundToInt

@JvmSynthetic
internal fun String.encodedBitmapToNativeScreenshotFormat(): ScreenshotFormat {
    val encoded = toByteArray(Charset.defaultCharset())

    val byteBuffer = ByteBuffer.allocateDirect(encoded.size)
    byteBuffer.put(encoded)
    val dataRef = DataRef(byteBuffer)

    return ScreenshotFormat(
        dataRef,
        Base64.encodeToString(encoded, Base64.DEFAULT),
    )
}

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
