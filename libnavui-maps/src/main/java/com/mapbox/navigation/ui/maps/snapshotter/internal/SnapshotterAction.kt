package com.mapbox.navigation.ui.maps.snapshotter.internal

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.maps.MapSnapshotInterface
import com.mapbox.navigation.ui.base.MapboxAction
import com.mapbox.navigation.ui.maps.snapshotter.model.SnapshotOptions

sealed class SnapshotterAction : MapboxAction {
    data class GenerateSnapshot(
        val bannerInstruction: BannerInstructions
    ) : SnapshotterAction()

    data class GenerateCameraPosition(
        val currentStepGeometry: String?,
        val nextStepGeometry: String?,
        val options: SnapshotOptions
    ) : SnapshotterAction()

    data class GenerateBitmap(
        val options: SnapshotOptions,
        val snapshot: Expected<MapSnapshotInterface?, String?>
    ) : SnapshotterAction()

    object GenerateSkyLayer : SnapshotterAction()
    object GenerateLineLayer : SnapshotterAction()
}
