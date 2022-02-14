package com.mapbox.navigation.dropin.extensions

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

@ExperimentalPreviewMapboxNavigationAPI
fun <T : MapboxNavigationObserver> LifecycleOwner.attachCreated(vararg observers: T) = apply {
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            observers.forEach { MapboxNavigationApp.registerObserver(it) }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
        }
    })
}

@ExperimentalPreviewMapboxNavigationAPI
fun <T : MapboxNavigationObserver> LifecycleOwner.attachStarted(vararg observers: T) = apply {
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            observers.forEach { MapboxNavigationApp.registerObserver(it) }
        }

        override fun onStop(owner: LifecycleOwner) {
            observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
        }
    })
}

@ExperimentalPreviewMapboxNavigationAPI
fun <T : MapboxNavigationObserver> LifecycleOwner.attachResumed(vararg observers: T) = apply {
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            observers.forEach { MapboxNavigationApp.registerObserver(it) }
        }

        override fun onPause(owner: LifecycleOwner) {
            observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
        }
    })
}
