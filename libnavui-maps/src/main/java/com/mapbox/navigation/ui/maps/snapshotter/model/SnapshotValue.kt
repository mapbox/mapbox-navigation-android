package com.mapbox.navigation.ui.maps.snapshotter.model

import android.graphics.Bitmap

/**
 * The state is returned when the snapshotter Image is ready to be rendered on the UI.
 * @property snapshot Bitmap The [Bitmap] containing the snapshot.
 */
class SnapshotValue internal constructor(
    val snapshot: Bitmap
)
