package com.mapbox.navigation.ui.base

/**
 * Object representing a UI that subscribes to a [State] for rendering its UI.
 *
 * @param S Top class of the [State] that the [View] will be subscribing to.
 */
interface View<in S : State> {

    /**
     * Entry point for the [View] to render itself based on a [State].
     */
    fun render(state: S)
}
