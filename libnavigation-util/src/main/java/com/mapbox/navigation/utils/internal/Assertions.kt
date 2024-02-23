package com.mapbox.navigation.utils.internal

import com.mapbox.navigation.utils.BuildConfig

inline fun assertDebug(value: Boolean, message: () -> Any) {
    if (BuildConfig.DEBUG) {
        check(value, message)
    }

    if (!value) {
        logW { message().toString() }
    }
}
