package com.mapbox.navigation.ui.maps.snapshotter.model

/**
 * The state is returned if there is an error generating the snapshot
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class SnapshotError internal constructor(
    val errorMessage: String?,
    val throwable: Throwable?
)
