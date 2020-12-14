package com.mapbox.navigation.ui.maps.snapshotter.internal

import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SkyLayer
import com.mapbox.navigation.ui.base.MapboxResult
import com.mapbox.navigation.ui.maps.snapshotter.model.CameraPosition

sealed class SnapshotterResult : MapboxResult {
    object SnapshotAvailable : SnapshotterResult()
    object SnapshotUnavailable : SnapshotterResult()

    data class SnapshotterCameraPosition(
        val cameraPosition: CameraPosition?
    ) : SnapshotterResult()

    sealed class Snapshot : SnapshotterResult() {
        data class Success(val bitmap: Bitmap) : Snapshot()
        data class Failure(val error: String?) : Snapshot()
        data class Empty(val error: String?) : Snapshot()
    }

    data class SnapshotSkyLayer(
        val layer: SkyLayer
    ) : SnapshotterResult()

    data class SnapshotLineLayer(
        val layer: LineLayer
    ) : SnapshotterResult()
}
