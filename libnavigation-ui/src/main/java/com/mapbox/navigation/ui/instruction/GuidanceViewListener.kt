package com.mapbox.navigation.ui.instruction

/**
 * Interface definition for a callback to be invoked when the Guidance view appears or disappears.
 */
interface GuidanceViewListener {
    /**
     * Callback when Guidance view appears
     *
     * @param left The new value of the guidance view's left property.
     * @param top The new value of the guidance view's top property.
     * @param width The new value of the guidance view's width property.
     * @param height The new value of the guidance view's height property.
     */
    fun onShownAt(left: Int, top: Int, width: Int, height: Int)

    /**
     * Callback when Guidance view disappears
     */
    fun onHidden()
}
