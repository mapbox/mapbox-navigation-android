package com.mapbox.navigation.ui.utils.internal.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.unwrapIfNeeded(): Context {
    return if (this !is Activity && this is ContextWrapper) {
        this.baseContext.unwrapIfNeeded()
    } else {
        this
    }
}
