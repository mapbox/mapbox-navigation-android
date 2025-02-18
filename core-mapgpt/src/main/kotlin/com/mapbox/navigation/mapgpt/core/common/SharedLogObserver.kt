package com.mapbox.navigation.mapgpt.core.common

interface SharedLogObserver {
    fun onInfo(message: String)
    fun onDebug(message: String)
    fun onWarning(message: String)
    fun onError(message: String)
}
