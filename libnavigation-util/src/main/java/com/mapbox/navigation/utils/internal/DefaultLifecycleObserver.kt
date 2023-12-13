package com.mapbox.navigation.utils.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

abstract class DefaultLifecycleObserver: LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreate(source)
            Lifecycle.Event.ON_START -> onStart(source)
            Lifecycle.Event.ON_RESUME -> onResume(source)
            Lifecycle.Event.ON_PAUSE -> onPause(source)
            Lifecycle.Event.ON_STOP -> onStop(source)
            Lifecycle.Event.ON_DESTROY -> onDestroy(source)
        }
    }

    open fun onCreate(owner: LifecycleOwner) {

    }

    open fun onStart(owner: LifecycleOwner) {

    }

    open fun onResume(owner: LifecycleOwner) {

    }

    open fun onPause(owner: LifecycleOwner) {

    }

    open fun onStop(owner: LifecycleOwner) {

    }

    open fun onDestroy(owner: LifecycleOwner) {

    }
}
