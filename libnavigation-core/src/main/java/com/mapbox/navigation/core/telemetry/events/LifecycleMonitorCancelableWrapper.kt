package com.mapbox.navigation.core.telemetry.events

import com.mapbox.common.Cancelable
import com.mapbox.common.GetLifecycleStateCallback
import com.mapbox.common.LifecycleMonitorInterface
import com.mapbox.common.LifecycleObserver

internal class LifecycleMonitorCancelableWrapper(
    private val lifecycleMonitorInterface: LifecycleMonitorInterface,
) {

    fun getLifecycleState(callback: GetLifecycleStateCallback): Cancelable {
        var cancelled = false
        lifecycleMonitorInterface.getLifecycleState {
            if (!cancelled) {
                callback.run(it)
            }
        }
        return Cancelable { cancelled = true }
    }

    fun registerObserver(observer: LifecycleObserver) {
        lifecycleMonitorInterface.registerObserver(observer)
    }

    fun unregisterObserver(observer: LifecycleObserver) {
        lifecycleMonitorInterface.unregisterObserver(observer)
    }
}
