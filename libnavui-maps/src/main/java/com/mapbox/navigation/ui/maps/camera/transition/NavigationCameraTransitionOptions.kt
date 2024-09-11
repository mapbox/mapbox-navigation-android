package com.mapbox.navigation.ui.maps.camera.transition

/**
 * Options that impact the transition animation.
 *
 * The styling of the animation is determined by the [NavigationCameraStateTransition] and [NavigationCameraTransition] implementations,
 * but options here provide higher-level constraints that those implementations need to obey.
 *
 * @param maxDuration maximum duration of the generated transitions set, including delays between animators and their respective durations.
 */
class NavigationCameraTransitionOptions private constructor(
    val maxDuration: Long,
) {
    /**
     * @return the builder that created the [NavigationCameraTransitionOptions]
     */
    fun toBuilder(): Builder = Builder()
        .maxDuration(maxDuration)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationCameraTransitionOptions

        if (maxDuration != other.maxDuration) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return maxDuration.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NavigationCameraTransitionOptions(maxDuration=$maxDuration)"
    }

    /**
     * Builder for [NavigationCameraTransitionOptions].
     */
    class Builder {
        private var maxDuration: Long = 1000L

        /**
         * Sets maximum duration of the generated transitions set in milliseconds, including delays between animators and their respective durations.
         *
         * Defaults to 1000 milliseconds.
         */
        fun maxDuration(maxDuration: Long): Builder = apply {
            this.maxDuration = maxDuration
        }

        /**
         * Builds [NavigationCameraTransitionOptions].
         */
        fun build() = NavigationCameraTransitionOptions(maxDuration)
    }
}
