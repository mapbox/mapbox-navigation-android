package com.mapbox.navigation.ui.maps.signboard.api

import com.mapbox.navigation.ui.base.model.signboard.SignboardState
import com.mapbox.navigation.ui.base.model.snapshotter.SnapshotState

interface SignboardReadyCallback {

    /**
     * Invoked when signboard is ready.
     * @param stream represents the signboard to be rendered on the view.
     */
    fun onSignboardReady(stream: SignboardState.SignboardReady)

    /**
     * Invoked when there is an error generating the signboard.
     * @param error error message.
     */
    fun onFailure(error: SignboardState.SignboardFailure)
}
