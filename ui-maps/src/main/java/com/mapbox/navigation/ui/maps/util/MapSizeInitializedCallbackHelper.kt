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
        // Use cancellable wrapper to avoid memory leaks.
        return mapboxMap.cancellableWhenSizeReady(action)
    }

    private fun MapboxMap.cancellableWhenSizeReady(
        action: () -> Unit,
    ): Cancelable {
        val cancelable = CancellableImpl()

        val callback: () -> Unit = {
            cancelable.runIfNotComplete {
                action()
            }
            cancelable.complete()
        }

        whenSizeReady(callback)

        return cancelable
    }

    private class CancellableImpl : Cancelable {

        var completionAction: (() -> Unit)? = null

        private val isComplete = AtomicBoolean(false)

        override fun cancel() {
            complete()
        }

        fun complete() {
            isComplete.set(true)
            completionAction?.invoke()
            completionAction = null
        }

        inline fun runIfNotComplete(block: () -> Unit) {
            if (!isComplete.get()) {
                block()
            }
        }
    }
}
