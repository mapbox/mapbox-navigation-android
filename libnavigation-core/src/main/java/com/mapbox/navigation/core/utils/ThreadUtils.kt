package com.mapbox.navigation.core.utils

import android.os.HandlerThread

internal object ThreadUtils {

    fun prepareHandlerThread(name: String): HandlerThread {
        return HandlerThread(name).apply {
            start()
        }
    }
}
