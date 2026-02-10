package com.mapbox.navigation.core.utils

import android.os.HandlerThread

internal object ThreadUtils {

    fun prepareHandlerThread(name: String, priority: Int): HandlerThread {
        return HandlerThread(name, priority).apply {
            start()
        }
    }
}
