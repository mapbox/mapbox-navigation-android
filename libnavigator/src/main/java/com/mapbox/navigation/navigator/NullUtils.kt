package com.mapbox.navigation.navigator

internal inline fun <R1, T> ifNonNull(r1: R1?, func: (R1) -> T): T? =
    if (r1 != null) {
        func(r1)
    } else {
        null
    }
