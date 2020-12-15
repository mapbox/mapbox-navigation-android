package com.mapbox.navigation.ui.maps.snapshotter.internal

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.maps.MapSnapshotInterface
import com.mapbox.navigation.ui.maps.snapshotter.model.MapboxSnapshotterOptions

internal sealed class SnapshotterAction {
    data class GenerateSnapshot(
        val bannerInstruction: BannerInstructions
    ) : SnapshotterAction()

    data class GenerateCameraPosition(
        val currentStepGeometry: String?,
        val nextStepGeometry: String?,
        val options: MapboxSnapshotterOptions
    ) : SnapshotterAction()

    data class GenerateBitmap(
        val options: MapboxSnapshotterOptions,
        val snapshot: Expected<MapSnapshotInterface?, String?>
    ) : SnapshotterAction()

    object GenerateSkyLayer : SnapshotterAction()
    object GenerateLineLayer : SnapshotterAction()
}
