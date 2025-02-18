package com.mapbox.navigation.mapgpt.core.api.native

import com.mapbox.navigation.mapgpt.core.api.SessionFrame

interface NativePlatformMapGptObserver {
    fun onSessionConnectionError(nativeSessionConnectionError: NativeSessionConnectionError)
    fun onSessionFrameReceived(sessionFrame: SessionFrame)
    fun onError(exception: Exception)
    fun onNewAsrInput(text: String)
}
