@file:JvmName("TextViewEx")

package com.mapbox.navigation.ui.utils.internal.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.TextView

fun TextView.getAsBitmap(): Bitmap {
    val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    measure(measureSpec, measureSpec)
    layout(0, 0, measuredWidth, measuredHeight)
    val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.TRANSPARENT)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

fun TextView.measureTextWidth(text: String): Float {
    val transformedText = transformationMethod?.getTransformation(text, this) ?: text
    return paint.measureText(transformedText.toString()) + paddingStart + paddingEnd
}
