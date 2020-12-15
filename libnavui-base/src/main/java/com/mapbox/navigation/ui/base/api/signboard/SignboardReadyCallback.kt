package com.mapbox.navigation.ui.base.api.signboard

import com.mapbox.navigation.ui.base.model.signboard.SignboardState

/**
 * Interface definition for a callback to be invoked when a signboard data is processed.
 */
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
