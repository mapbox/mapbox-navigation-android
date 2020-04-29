package com.mapbox.navigation.ui.util

import android.content.res.TypedArray

/**
 * Extension function executing the function passed into it
 * @receiver TypedArray
 * @param block [@kotlin.ExtensionFunctionType] Function1<TypedArray, Unit>
 */
fun TypedArray.use(block: TypedArray.() -> Unit) {
    try {
        block()
    } finally {
        this.recycle()
    }
}
