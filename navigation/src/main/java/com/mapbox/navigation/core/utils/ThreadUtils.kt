package com.mapbox.navigation.core.utils

import android.os.HandlerThread
import android.os.Looper

internal object ThreadUtils {

    fun assertCurrentLooperIsMain() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Must be called on the main thread, but was called on ${Thread.currentThread().name}"
        }
    }

    fun prepareHandlerThread(name: String, priority: Int): HandlerThread {
        return HandlerThread(name, priority).apply {
            start()
        }
    }
}
