package com.mapbox.navigation.ui.utils.internal.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import kotlin.math.roundToInt

/**
 * Converts a [Drawable] to [Bitmap]
 * @receiver Drawable
 * @return Bitmap
 */
fun Drawable.getBitmap(): Bitmap {
    return if (this is BitmapDrawable) {
        this.bitmap
    } else {
        val bitmap = Bitmap.createBitmap(
            intrinsicWidth,
            intrinsicHeight,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap
    }
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
