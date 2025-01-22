package com.mapbox.navigation.ui.maps.util

import androidx.annotation.UiThread
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A helper class that allows to receive a callback when map size is ready.
 * It's useful to know when map size is ready, for example, in case when we need to call
 * [MapboxMap.cameraForCoordinates]
 * because it's safe to call that function only when map sze is known.
 */
@UiThread
internal class MapSizeReadyCallbackHelper(
    private val mapboxMap: MapboxMap,
) {

    /**
     * Invokes [action] when [MapboxMap]'s size is ready.
     */
    fun onMapSizeReady(action: () -> Unit): Cancelable {
        // There's no Maps SDK function that let's us know when map size is ready.
        // We use a knowledge that [cameraForCoordinates] calls back only when map size is ready
        return mapboxMap.cancellableCameraForCoordinates(
            listOf(Point.fromLngLat(.0, .0)),
            CameraOptions.Builder().build(),
        ) {
            action()
        }
    }

    private fun MapboxMap.cancellableCameraForCoordinates(
        coordinates: List<Point>,
        camera: CameraOptions,
        coordinatesPadding: EdgeInsets? = null,
        maxZoom: Double? = null,
        offset: ScreenCoordinate? = null,
        result: (CameraOptions) -> Unit,
    ): Cancelable {
        val cancelable = CancellableImpl()

        val callback: (CameraOptions) -> Unit = { options ->
            cancelable.runIfNotComplete {
                result(options)
            }
            cancelable.complete()
        }

        cameraForCoordinates(
            coordinates,
            camera,
            coordinatesPadding,
            maxZoom,
            offset,
            callback,
        )

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

        fun runIfNotComplete(block: () -> Unit) {
            if (!isComplete.get()) {
                block()
            }
        }
    }
}
