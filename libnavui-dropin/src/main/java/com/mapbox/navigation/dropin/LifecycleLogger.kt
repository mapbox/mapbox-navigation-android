package com.mapbox.navigation.dropin

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.utils.internal.logD

class LifecycleLogger(val name: String) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        logD("onCreate $name", "lifecycle_debug")
    }

    override fun onStart(owner: LifecycleOwner) {
        logD("onStart $name", "lifecycle_debug")
    }

    override fun onResume(owner: LifecycleOwner) {
        logD("onResume $name", "lifecycle_debug")
    }

    override fun onPause(owner: LifecycleOwner) {
        logD("onPause $name", "lifecycle_debug")
    }

    override fun onStop(owner: LifecycleOwner) {
        logD("onStop $name", "lifecycle_debug")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        logD("onDestroy $name", "lifecycle_debug")
    }
}
