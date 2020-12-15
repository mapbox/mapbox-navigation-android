package com.mapbox.navigation.ui.maps.signboard.api

import com.mapbox.navigation.ui.base.model.signboard.SignboardState

interface SignboardReadyCallback {

    /**
     * Invoked when signboard is ready.
     * @param bytes represents the signboard to be rendered on the view.
     */
    fun onSignboardReady(bytes: SignboardState.SignboardReady)

    /**
     * Invoked when there is an error generating the signboard.
     * @param error error message.
     */
    fun onFailure(error: SignboardState.SignboardFailure)
}
