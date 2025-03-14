package com.mapbox.navigation.ui.maps.camera.transition

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Options that specify the restrictions of update frame animations
 * returned by [NavigationCameraStateTransition.updateFrameForFollowing] and [NavigationCameraStateTransition.updateFrameForOverview].
 *
 * @param useSimplifiedAnimatorsDependency if `true`, NavSDK assumes that frame transition animations are simple in a sense that:
 * 1. They are played together (started at the same time);
 * 2. They don't have start delays.
 * Note 1: they can still be of different duration.
 * Note 2: this is ony relevant for update frame animations. For state transition animations ([NavigationCameraStateTransition.transitionToFollowing] and [NavigationCameraStateTransition.transitionToOverview]) no such assumptions are made.
 * This allows NavSDK to execute the animations in a more performant way.
 * Set this to true if that is not the case for your custom update frame animations.
 */
@ExperimentalPreviewMapboxNavigationAPI
class UpdateFrameAnimatorsOptions private constructor(
    val useSimplifiedAnimatorsDependency: Boolean,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder {
        return Builder().useSimplifiedAnimatorsDependency(useSimplifiedAnimatorsDependency)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpdateFrameAnimatorsOptions

        return useSimplifiedAnimatorsDependency == other.useSimplifiedAnimatorsDependency
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return useSimplifiedAnimatorsDependency.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "UpdateFrameAnimatorsOptions(" +
            "useSimplifiedAnimatorsDependency=$useSimplifiedAnimatorsDependency" +
            ")"
    }

    /**
     * Builder for [UpdateFrameAnimatorsOptions].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    class Builder {

        private var useSimplifiedAnimatorsDependency: Boolean = false

        /**
         * If `true`, NavSDK assumes that frame transition animations are simple in a sense that:
         *  * 1. They are played together (started at the same time);
         *  * 2. They don't have start delays.
         *  * Note 1: they can still be of different duration.
         *  * Note 2: this is ony relevant for update frame animations. For state transition animations ([NavigationCameraStateTransition.transitionToFollowing] and [NavigationCameraStateTransition.transitionToOverview]) no such assumptions are made.
         *  * This allows NavSDK to execute the animations in a more performant way.
         *  * Set this to true if that is not the case for your custom update frame animations.
         */
        fun useSimplifiedAnimatorsDependency(value: Boolean): Builder = apply {
            this.useSimplifiedAnimatorsDependency = value
        }

        /**
         * Builds a [UpdateFrameAnimatorsOptions] object.
         */
        fun build(): UpdateFrameAnimatorsOptions {
            return UpdateFrameAnimatorsOptions(useSimplifiedAnimatorsDependency)
        }
    }
}
