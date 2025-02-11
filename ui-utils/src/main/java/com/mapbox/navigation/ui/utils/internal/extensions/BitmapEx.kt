@file:JvmName("BitmapEx")

package com.mapbox.navigation.ui.utils.internal.extensions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

fun Bitmap.drawableWithHeight(
    drawableHeight: Int,
    resources: Resources,
): Drawable {
    val drawable: Drawable = BitmapDrawable(resources, this)
    val right = (drawableHeight * width.toDouble() / height.toDouble()).toInt()
    drawable.setBounds(0, 0, right, drawableHeight)
    return drawable
}
