package com.mapbox.navigation.ui.androidauto.internal

import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI

internal object AndroidAutoLog {

    private const val LOG_CATEGORY = "MapboxAndroidAuto"

    fun logAndroidAuto(message: String) {
        logI(
            msg = "${Thread.currentThread().id}: $message",
            LOG_CATEGORY,
        )
    }

    fun logAndroidAutoFailure(message: String, throwable: Throwable? = null) {
        logE(
            msg = "${Thread.currentThread().id}: $message throwable: $throwable",
            LOG_CATEGORY,
        )
    }
}

fun logAndroidAuto(message: String) {
    AndroidAutoLog.logAndroidAuto(message)
}

fun logAndroidAutoFailure(message: String, throwable: Throwable? = null) {
    AndroidAutoLog.logAndroidAutoFailure(message, throwable)
}
