package com.mapbox.navigation.ui.maps.camera.transition

internal interface MapboxAnimatorSetListener : MapboxAnimatorSetEndListener {

    fun onAnimationStart(animation: MapboxAnimatorSet)

    fun onAnimationCancel(animation: MapboxAnimatorSet)
}
