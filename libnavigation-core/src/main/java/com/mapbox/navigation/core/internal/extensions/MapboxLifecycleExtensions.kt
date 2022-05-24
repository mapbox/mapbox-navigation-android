package com.mapbox.navigation.core.internal.extensions

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
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

@ExperimentalPreviewMapboxNavigationAPI
fun LifecycleOwner.attachOnLifecycle(
    attachEvent: Lifecycle.Event,
    detachEvent: Lifecycle.Event,
    mapboxNavigation: MapboxNavigation,
    observer: MapboxNavigationObserver
) {
    lifecycle.addObserver(AttachOnLifecycle(attachEvent, detachEvent, mapboxNavigation, observer))
}

@ExperimentalPreviewMapboxNavigationAPI
internal class AttachOnLifecycle(
    private val attachEvent: Lifecycle.Event,
    private val detachEvent: Lifecycle.Event,
    private val mapboxNavigation: MapboxNavigation,
    private val observer: MapboxNavigationObserver
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        if (attachEvent == Lifecycle.Event.ON_CREATE) observer.onAttached(mapboxNavigation)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (attachEvent == Lifecycle.Event.ON_START) observer.onAttached(mapboxNavigation)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (attachEvent == Lifecycle.Event.ON_RESUME) observer.onAttached(mapboxNavigation)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (detachEvent == Lifecycle.Event.ON_PAUSE) observer.onDetached(mapboxNavigation)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (detachEvent == Lifecycle.Event.ON_STOP) observer.onDetached(mapboxNavigation)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        observer.onDetached(mapboxNavigation) // we always detach in onDestroy
    }
}
