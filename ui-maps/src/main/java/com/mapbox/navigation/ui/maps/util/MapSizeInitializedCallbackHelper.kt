package com.mapbox.navigation.ui.maps.util

import androidx.annotation.UiThread
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapboxMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A helper class that allows to receive a callback when map size is initialized.
 * It's useful to know when map size is initialized, for example, in case when we need to call
 * [MapboxMap.cameraForCoordinates]
 * because it's safe to call that function only when map size is known.
 */
@UiThread
internal class MapSizeInitializedCallbackHelper(
    private val mapboxMap: MapboxMap,
) {

    /**
     * Invokes [action] when [MapboxMap]'s size is ready.
     */
    fun onMapSizeInitialized(action: () -> Unit): Cancelable {
        // Use cancellable wrapper to avoid executing stale callbacks and retaining their actions.
        return mapboxMap.cancellableWhenSizeReady(action)
    }

    private fun MapboxMap.cancellableWhenSizeReady(
        action: () -> Unit,
    ): Cancelable {
        val cancelable = CancellableImpl(action)

        val callback: () -> Unit = {
            try {
                cancelable.runIfNotComplete()
            } finally {
                cancelable.complete()
            }
        }

        whenSizeReady(callback)

        return cancelable
    }

    private class CancellableImpl(
        private var action: (() -> Unit)?,
    ) : Cancelable {

        private val isComplete = AtomicBoolean(false)

        override fun cancel() {
            complete()
        }

        fun complete() {
            if (isComplete.compareAndSet(false, true)) {
                action = null
            }
        }

        fun runIfNotComplete() {
            if (!isComplete.get()) {
                action?.invoke()
            }
        }
    }
}
