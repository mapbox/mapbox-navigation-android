package com.mapbox.navigation.utils.internal

import android.view.View
import android.view.ViewGroup

var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

var View.isInvisible: Boolean
    get() = visibility == View.INVISIBLE
    set(value) {
        visibility = if (value) View.INVISIBLE else View.VISIBLE
    }

inline fun View.updateLayoutParams(block: ViewGroup.LayoutParams.() -> Unit) {
    val params = layoutParams
    block(params)
    layoutParams = params
}
