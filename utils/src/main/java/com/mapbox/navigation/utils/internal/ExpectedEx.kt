package com.mapbox.navigation.utils.internal

import com.mapbox.bindgen.Expected

inline fun <E, V, R> Expected<E, V>.foldInline(
    errorHandler: (E) -> R,
    valueHandler: (V) -> R,
): R {
    val error = this.error
    val value = this.value
    return if (value != null) {
        valueHandler(value)
    } else if (error != null) {
        errorHandler(error)
    } else {
        throw IllegalArgumentException("Both value and error are nulls")
    }
}
