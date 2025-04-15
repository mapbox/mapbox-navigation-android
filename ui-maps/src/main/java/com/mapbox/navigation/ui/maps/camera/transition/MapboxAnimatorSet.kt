package com.mapbox.navigation.ui.maps.camera.transition

internal interface MapboxAnimatorSet {

    fun addAnimationEndListener(listener: MapboxAnimatorSetEndListener)

    fun makeInstant()

    fun start()

    fun onFinished()

    fun cancel()
}
