package com.mapbox.navigation.ui.utils.internal.extensions

import android.content.Context
import android.util.TypedValue

fun Context.dipToPixel(dip: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dip,
        this.resources.displayMetrics,
    )
}

fun Context.spToPixel(sp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        this.resources.displayMetrics,
    )
}
