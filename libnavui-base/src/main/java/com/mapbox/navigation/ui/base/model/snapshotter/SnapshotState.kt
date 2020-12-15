package com.mapbox.navigation.ui.base.model.snapshotter

import android.graphics.Bitmap
import com.mapbox.navigation.ui.base.MapboxState

/**
 * Immutable object representing the snapshotter data to be rendered.
 */
sealed class SnapshotState : MapboxState {

    /**
     * The state is returned when the Guidance Image is ready to be rendered on the UI
     * @property bitmap Bitmap The [Bitmap] containing the GuidanceImage
     */
    data class SnapshotReady(val bitmap: Bitmap) : SnapshotState()

    /**
     * The state is returned in case of any errors while preparing the GuidanceImage
     */
    sealed class SnapshotFailure : SnapshotState() {
        /**
         * The state is returned if the intersection doesn't contain GuidanceImage
         */
        object SnapshotUnavailable : SnapshotFailure()

        /**
         * The state is returned if the [Bitmap] is empty
         * @property exception String Error message.
         */
        data class SnapshotEmpty(val exception: String?) : SnapshotFailure()

        /**
         * The state is returned if there is an error preparing the [Bitmap]
         * @property exception String Error message.
         */
        data class SnapshotError(val exception: String?) : SnapshotFailure()
    }
}
