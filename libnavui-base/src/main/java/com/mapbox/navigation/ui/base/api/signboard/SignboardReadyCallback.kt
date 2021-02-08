package com.mapbox.navigation.ui.base.api.signboard

import com.mapbox.navigation.ui.base.model.signboard.SignboardState

/**
 * Interface definition for a callback to be invoked when a signboard data is processed.
 */
interface SignboardReadyCallback {

    /**
     * Invoked when signboard is ready.
     * @param state represents the signboard to be rendered on the view.
     */
    fun onAvailable(state: SignboardState.Signboard.Available)

    /**
     * Invoked when the route doesn't have a signboard or the signboard request returns empty data.
     * @param state represents the empty data.
     */
    fun onUnavailable(state: SignboardState.Signboard.Empty)

    /**
     * Invoked when there is an error generating the signboard.
     * @param state error message.
     */
    fun onError(state: SignboardState.Signboard.Error)
}
