package com.mapbox.navigation.core.internal.extensions

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

fun <T : MapboxNavigationObserver> LifecycleOwner.attachCreated(vararg observers: T) = apply {
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                observers.forEach { MapboxNavigationApp.registerObserver(it) }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
            }
        },
    )
}

fun <T : MapboxNavigationObserver> LifecycleOwner.attachStarted(vararg observers: T) = apply {
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                observers.forEach { MapboxNavigationApp.registerObserver(it) }
            }

            override fun onStop(owner: LifecycleOwner) {
                observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
            }
        },
    )
}

fun <T : MapboxNavigationObserver> LifecycleOwner.attachResumed(vararg observers: T) = apply {
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                observers.forEach { MapboxNavigationApp.registerObserver(it) }
            }

            override fun onPause(owner: LifecycleOwner) {
                observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                observers.forEach { MapboxNavigationApp.unregisterObserver(it) }
            }
        },
    )
}

fun LifecycleOwner.attachCreated(
    mapboxNavigation: MapboxNavigation,
    observer: MapboxNavigationObserver,
) = attachOnLifecycle(
    Lifecycle.Event.ON_CREATE,
    Lifecycle.Event.ON_DESTROY,
    mapboxNavigation,
    observer,
)

fun LifecycleOwner.attachStarted(
    mapboxNavigation: MapboxNavigation,
    observer: MapboxNavigationObserver,
) = attachOnLifecycle(
    Lifecycle.Event.ON_START,
    Lifecycle.Event.ON_STOP,
    mapboxNavigation,
    observer,
)

fun LifecycleOwner.attachResumed(
    mapboxNavigation: MapboxNavigation,
    observer: MapboxNavigationObserver,
) = attachOnLifecycle(
    Lifecycle.Event.ON_RESUME,
    Lifecycle.Event.ON_PAUSE,
    mapboxNavigation,
    observer,
)

fun LifecycleOwner.attachOnLifecycle(
    attachEvent: Lifecycle.Event,
    detachEvent: Lifecycle.Event,
    mapboxNavigation: MapboxNavigation,
    observer: MapboxNavigationObserver,
) {
    lifecycle.addObserver(AttachOnLifecycle(attachEvent, detachEvent, mapboxNavigation, observer))
}

internal class AttachOnLifecycle(
    private val attachEvent: Lifecycle.Event,
    private val detachEvent: Lifecycle.Event,
    private val mapboxNavigation: MapboxNavigation,
    private val observer: MapboxNavigationObserver,
) : LifecycleEventObserver {

    private var attached = false

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == attachEvent) attach()
        if (event == detachEvent || event == Lifecycle.Event.ON_DESTROY) detach()
    }

    private fun attach() {
        if (attached) return
        attached = true
        observer.onAttached(mapboxNavigation)
    }

    private fun detach() {
        if (!attached) return
        attached = false
        observer.onDetached(mapboxNavigation)
    }
}
