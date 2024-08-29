package com.mapbox.navigation.core.telemetry.events

import androidx.annotation.VisibleForTesting
import com.mapbox.common.Cancelable
import com.mapbox.common.LifecycleMonitorFactory
import com.mapbox.common.LifecycleMonitoringState
import com.mapbox.common.LifecycleObserver
import com.mapbox.common.LifecycleState

internal class LifecycleStateProvider @VisibleForTesting constructor(
    interfaceProvider: () -> LifecycleMonitorCancelableWrapper,
) {

    companion object {

        val instance = LifecycleStateProvider {
            LifecycleMonitorCancelableWrapper(LifecycleMonitorFactory.getOrCreate())
        }
    }

    private val lifecycleMonitor by lazy { interfaceProvider() }

    var getLifecycleStateTask: Cancelable? = null

    @Volatile
    var currentState: LifecycleState = LifecycleState.UNKNOWN
        private set(value) {
            field = value
            getLifecycleStateTask?.cancel()
            getLifecycleStateTask = null
        }

    private val observer = object : LifecycleObserver {

        override fun onMonitoringStateChanged(state: LifecycleMonitoringState, error: String?) {
            if (state == LifecycleMonitoringState.STOPPED) {
                currentState = LifecycleState.UNKNOWN
            }
        }

        override fun onLifecycleStateChanged(state: LifecycleState) {
            getLifecycleStateTask?.cancel()
            currentState = state
        }
    }

    fun init() {
        getLifecycleStateTask = lifecycleMonitor.getLifecycleState { result ->
            result.onValue {
                currentState = it
            }
        }
        lifecycleMonitor.registerObserver(observer)
    }

    fun destroy() {
        currentState = LifecycleState.UNKNOWN
        lifecycleMonitor.unregisterObserver(observer)
    }
}
