package com.mapbox.navigation.ui.maps.camera.transition

fun interface TransitionEndListener {
    fun onTransitionEnd(isCanceled: Boolean)
}
