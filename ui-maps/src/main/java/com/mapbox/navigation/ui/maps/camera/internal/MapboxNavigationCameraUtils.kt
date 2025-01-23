package com.mapbox.navigation.ui.maps.camera.internal

import android.animation.AnimatorSet
import androidx.annotation.RestrictTo
import com.mapbox.common.location.Location
import com.mapbox.navigation.ui.maps.camera.NavigationCamera

/**
 * Takes the longest animation in the set (including delay an duration) and scales the duration down to match the duration constraint if it's exceeded.
 * All other animations are scaled by the same factor which allows to mostly keep the same composition and "feel" of the animation set while shortening its duration.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun AnimatorSet.constraintDurationTo(maxDuration: Long): AnimatorSet {
    childAnimations.maxByOrNull { it.startDelay + it.duration }?.let {
        val longestExecutionTime = it.startDelay + it.duration
        if (longestExecutionTime > maxDuration) {
            val factor = maxDuration / (longestExecutionTime).toDouble()
            childAnimations.forEach { animator ->
                animator.startDelay = (animator.startDelay * factor).toLong()
                animator.duration = (animator.duration * factor).toLong()
            }
        }
    }
    return this
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NavigationCamera.jumpToLocationInternal(location: Location) {
    jumpToLocation(location)
}
