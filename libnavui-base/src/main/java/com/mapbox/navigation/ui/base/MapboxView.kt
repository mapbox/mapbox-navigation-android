package com.mapbox.navigation.ui.base

/**
 * Object representing a UI that subscribes to a [MapboxState] for rendering its UI.
 *
 * @param S Top class of the [MapboxState] that the [MapboxView] will be subscribing to.
 */
interface MapboxView<in S : MapboxState> {

    /**
     * Entry point for the [MapboxView] to render itself based on a [MapboxState].
     */
    fun render(state: S)
}
