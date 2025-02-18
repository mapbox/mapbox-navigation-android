package com.mapbox.navigation.mapgpt.core.common

sealed class SharedLogLevel(val logLevel: Int) {
    object Debug : SharedLogLevel(0)
    object Info : SharedLogLevel(1)
    object Warning : SharedLogLevel(2)
    object Error : SharedLogLevel(3)
}
