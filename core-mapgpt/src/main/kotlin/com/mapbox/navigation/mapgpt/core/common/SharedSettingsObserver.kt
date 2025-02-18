package com.mapbox.navigation.mapgpt.core.common

fun interface SharedSettingsObserver {
    fun onSettingsChanged(key: String, oldValue: SharedValue?, newValue: SharedValue?)
}
