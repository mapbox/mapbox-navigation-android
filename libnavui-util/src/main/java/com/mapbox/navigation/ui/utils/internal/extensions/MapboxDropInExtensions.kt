package com.mapbox.navigation.ui.utils.internal.extensions

import android.content.res.TypedArray

internal fun <T> TypedArray.use(block: TypedArray.() -> T): T {
    try {
        return block()
    } finally {
        this.recycle()
    }
}
