package com.mapbox.navigation.ui.maps.camera.transition

/**
 * Wrapper over multiple animations.
 */
internal interface MapboxAnimatorSet {

    /**
     * Adda a listener that is triggered when the compound animation is finished.
     */
    fun addAnimationEndListener(listener: MapboxAnimatorSetEndListener)

    /**
     * Sets the duration of compound animation to 0.
     */
    fun makeInstant()

    /**
     * Starts the compound animation.
     */
    fun start()

    /**
     * Cancels the compound animation.
     */
    fun cancel()
}
