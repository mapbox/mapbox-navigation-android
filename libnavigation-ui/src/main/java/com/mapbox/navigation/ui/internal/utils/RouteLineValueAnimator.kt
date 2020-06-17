package com.mapbox.navigation.ui.internal.utils

import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

typealias RouteLineValueAnimatorHandler = (Float) -> Unit

internal class RouteLineValueAnimator(
    private val animatorFrequency: AnimatorFrequency = AnimatorFrequency.ThirtyFramesPerSecond
) {
    var valueAnimatorHandler: RouteLineValueAnimatorHandler? = null
    private var animationJob: Job? = null
    private var jobControl: JobControl
    var animationDelay: Long = 0
    private var counter = 0

    init {
        jobControl = ThreadController.getMainScopeAndRootJob()
    }

    fun start(startValue: Float, endValue: Float) {
        animationJob?.cancel()
        counter = 0
        animationJob = jobControl.scope.launch {
            val delta = endValue - startValue
            val step = delta / 60
            var animationValue = startValue + step
            delay(animationDelay)
            while (this.isActive && counter < 60) {
                launch {
                    valueAnimatorHandler?.invoke(animationValue)
                }
                animationValue += step
                counter += 1
                delay(animatorFrequency.frequency)
            }
        }
    }

    fun cancelAnimationCallbacks() {
        animationJob?.cancel()
    }

    fun reInitialize() {
        jobControl = ThreadController.getMainScopeAndRootJob()
    }
}

sealed class AnimatorFrequency(val frequency: Long) {
    object ThirtyFramesPerSecond : AnimatorFrequency(32L)
    object SixtyFramesPerSecond : AnimatorFrequency(16L)
}
