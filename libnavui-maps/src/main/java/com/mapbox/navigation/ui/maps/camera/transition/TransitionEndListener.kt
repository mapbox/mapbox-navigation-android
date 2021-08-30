package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.navigation.ui.maps.camera.NavigationCamera

/**
 * A listener that is notified when a [NavigationCamera] transition ends.
 */
fun interface TransitionEndListener {

    /**
     * Notifies the end of the transition.
     *
     * @param isCanceled whether the transition was canceled.
     */
    fun onTransitionEnd(isCanceled: Boolean)
}
