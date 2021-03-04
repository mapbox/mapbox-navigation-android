package com.mapbox.navigation.base.utils

internal inline fun <R1, T> ifNonNull(r1: R1?, func: (R1) -> T): T? =
    if (r1 != null) {
        func(r1)
    } else {
        null
    }

internal inline fun <R1, R2, T> ifNonNull(r1: R1?, r2: R2?, func: (R1, R2) -> T): T? =
    if (r1 != null && r2 != null) {
        func(r1, r2)
    } else {
        null
    }
