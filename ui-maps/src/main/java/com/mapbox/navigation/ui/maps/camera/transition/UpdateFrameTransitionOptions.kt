package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Options that specify the restrictions of update frame animations
 * returned by [NavigationCameraStateTransition.updateFrameForFollowing] and [NavigationCameraStateTransition.updateFrameForOverview].
 *
 * @param nonSimultaneousAnimatorsDependency if `false`, NavSDK assumes that frame transition animations are simple in a sense that:
 * 1. They are played together (started at the same time);
 * 2. They don't have start delays.
 * Note 1: they can still be of different duration.
 * Note 2: this is ony relevant for update frame animations. For state transition animations ([NavigationCameraStateTransition.transitionToFollowing] and [NavigationCameraStateTransition.transitionToOverview]) no such assumptions are made.
 * This allows NavSDK to execute the animations in a more performant way.
 * Set this to true if that is not the case for your custom update frame animations.
 */
@ExperimentalPreviewMapboxNavigationAPI
class UpdateFrameTransitionOptions private constructor(
    val nonSimultaneousAnimatorsDependency: Boolean,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpdateFrameTransitionOptions

        return nonSimultaneousAnimatorsDependency == other.nonSimultaneousAnimatorsDependency
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return nonSimultaneousAnimatorsDependency.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "UpdateFrameTransitionOptions(" +
            "nonSimultaneousAnimatorsDependency=$nonSimultaneousAnimatorsDependency" +
            ")"
    }

    /**
     * Builder for [UpdateFrameTransitionOptions].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    class Builder {

        private var nonSimultaneousAnimatorsDependency: Boolean = false

        /**
         * If `false`, NavSDK assumes that frame transition animations are simple in a sense that:
         *  * 1. They are played together (started at the same time);
         *  * 2. They don't have start delays.
         *  * Note 1: they can still be of different duration.
         *  * Note 2: this is ony relevant for update frame animations. For state transition animations ([NavigationCameraStateTransition.transitionToFollowing] and [NavigationCameraStateTransition.transitionToOverview]) no such assumptions are made.
         *  * This allows NavSDK to execute the animations in a more performant way.
         *  * Set this to true if that is not the case for your custom update frame animations.
         */
        fun nonSimultaneousAnimatorsDependency(value: Boolean): Builder = apply {
            this.nonSimultaneousAnimatorsDependency = value
        }

        /**
         * Builds a [UpdateFrameTransitionOptions] object.
         */
        fun build(): UpdateFrameTransitionOptions {
            return UpdateFrameTransitionOptions(nonSimultaneousAnimatorsDependency)
        }
    }
}
