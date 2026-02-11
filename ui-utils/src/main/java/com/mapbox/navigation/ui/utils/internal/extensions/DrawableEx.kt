package com.mapbox.navigation.ui.utils.internal.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.roundToInt

/**
 * Converts a [Drawable] to [Bitmap]
 * @receiver Drawable
 * @return Bitmap
 */
fun Drawable.getBitmap(): Bitmap {
    return toBitmap()
}

/**
 * Converts this [Drawable] to a [Bitmap] with dimensions multiplied by the given [scale] factor.
 *
 * @param scale the multiplier applied to both width and height (e.g., 2.0 doubles the size)
 * @return a new [Bitmap]
 */
fun Drawable.toScaledBitmap(scale: Float): Bitmap {
    return toBitmap(
        (intrinsicWidth * scale).roundToInt(),
        (intrinsicHeight * scale).roundToInt(),
        Bitmap.Config.ARGB_8888,
    )
}

/**
 * Return Drawable with blur effect.
 */
fun Drawable.withBlurEffect(context: Context, radius: Float): Drawable {
    return BitmapDrawable(
        context.resources,
        blurBitmap(context, getBitmap(), radius),
    )
}

private fun blurBitmap(
    context: Context,
    image: Bitmap,
    radius: Float,
    scale: Float = 1.0f,
): Bitmap = runCatching {
    val width = (image.width * scale).roundToInt()
    val height = (image.height * scale).roundToInt()
    val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
    val outputBitmap = Bitmap.createBitmap(inputBitmap)
    val rs = RenderScript.create(context)
    val intrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
    val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
    val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)
    intrinsic.setRadius(radius)
    intrinsic.setInput(tmpIn)
    intrinsic.forEach(tmpOut)
    tmpOut.copyTo(outputBitmap)
    rs.destroy()
    outputBitmap
}.getOrDefault(image) // return original image on failure
