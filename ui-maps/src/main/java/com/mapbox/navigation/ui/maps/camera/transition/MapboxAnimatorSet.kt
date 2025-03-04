package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator

internal interface MapboxAnimatorSet {

    val children: List<Animator>

    fun addAnimationEndListener(listener: MapboxAnimatorSetEndListener)

    fun makeInstant()

    fun start()

    fun end()

    fun cancel()
}
