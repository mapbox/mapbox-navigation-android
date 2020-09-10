package com.mapbox.navigation.ui.feedback

/**
 * Navigation options to control feedback flow.
 *
 *  @param enableDetailedFeedbackAfterNavigation whether the user should be given
 *  opportunity to give more detailed feedback information when the the device
 *  arrives to the final destination.
 *  @param enableArrivalExperienceFeedback whether the user should be given
 *  opportunity to feedback about the arrival experience as the device comes
 *  to the final destination along the route.
 */
class NavigationFeedbackOptions private constructor(
    val enableDetailedFeedbackAfterNavigation: Boolean,
    val enableArrivalExperienceFeedback: Boolean
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        enableDetailedFeedbackAfterNavigation(enableDetailedFeedbackAfterNavigation)
        enableArrivalExperienceFeedback(enableArrivalExperienceFeedback)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NavigationFeedbackOptions
        if (enableDetailedFeedbackAfterNavigation != other.enableDetailedFeedbackAfterNavigation)
            return false
        if (enableArrivalExperienceFeedback != other.enableArrivalExperienceFeedback) return false
        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = enableDetailedFeedbackAfterNavigation.hashCode()
        result = 31 * result + enableArrivalExperienceFeedback.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NavigationFeedbackOptions(" +
            "enableDetailedFeedbackAfterNavigation=$enableDetailedFeedbackAfterNavigation, " +
            "enableArrivalExperienceFeedback=$enableArrivalExperienceFeedback" +
            ")"
    }

    /**
     * Build a new [NavigationFeedbackOptions]
     */
    class Builder() {
        private var enableDetailedFeedbackAfterNavigation: Boolean = false
        private var enableArrivalExperienceFeedback: Boolean = false

        /**
         * Set whether the user should be given opportunity to give more detailed
         * feedback information when the the device arrives to the final destination.
         *
         *  @param enableDetailedFeedbackAfterNavigation whether the user should be given
         *  opportunity to give more detailed feedback information when the the device
         *  arrives to the final destination.
         *
         *  @return this [Builder]
         */
        fun enableDetailedFeedbackAfterNavigation(
            enableDetailedFeedbackAfterNavigation: Boolean
        ): Builder =
            apply {
                this.enableDetailedFeedbackAfterNavigation = enableDetailedFeedbackAfterNavigation
            }

        /**
         * Set whether the user should be given opportunity to feedback about the arrival
         * experience as the device comes to the final destination along the route.
         *
         *  @param enableArrivalExperienceFeedback whether the user should be given
         *  opportunity to feedback about the arrival experience as the device comes
         *  to the final destination along the route.
         *
         *  @return this [Builder]
         */
        fun enableArrivalExperienceFeedback(enableArrivalExperienceFeedback: Boolean): Builder =
            apply { this.enableArrivalExperienceFeedback = enableArrivalExperienceFeedback }

        /**
         * Build a new instance of [NavigationFeedbackOptions]
         * @return NavigationFeedbackOptions
         */
        fun build(): NavigationFeedbackOptions {
            return NavigationFeedbackOptions(
                enableDetailedFeedbackAfterNavigation = enableDetailedFeedbackAfterNavigation,
                enableArrivalExperienceFeedback = enableArrivalExperienceFeedback,
            )
        }
    }
}
