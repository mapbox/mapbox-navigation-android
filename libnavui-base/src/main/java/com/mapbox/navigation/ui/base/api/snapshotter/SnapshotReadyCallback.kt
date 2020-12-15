package com.mapbox.navigation.ui.base.api.snapshotter

import com.mapbox.navigation.ui.base.model.snapshotter.SnapshotState

/**
 * Interface definition for a callback to be invoked when a snapshot is processed.
 */
interface SnapshotReadyCallback {

    /**
     * Invoked when snapshot is ready.
     * @param bitmap represents the snapshot to be rendered on the view.
     */
    fun onSnapshotReady(bitmap: SnapshotState.SnapshotReady)

    /**
     * Invoked when there is an error generating the snapshot.
     * @param error error message.
     */
    fun onFailure(error: SnapshotState.SnapshotFailure)
}
