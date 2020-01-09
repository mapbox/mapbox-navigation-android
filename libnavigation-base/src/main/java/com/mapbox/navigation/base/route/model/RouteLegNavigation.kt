package com.mapbox.navigation.base.route.model

class RouteLegNavigation private constructor(
    private val distance: Double? = null,
    private val duration: Double? = null,
    private val summary: String? = null,
    private val steps: List<LegStepNavigation>? = null,
    private val builder: Builder
) {
    /**
     * The distance traveled from one waypoint to another.
     *
     * @return a double number with unit meters
     */
    fun distance(): Double? = distance

    /**
     * The estimated travel time from one waypoint to another.
     *
     * @return a double number with unit seconds
     */
    fun duration(): Double? = duration

    /**
     * A short human-readable summary of major roads traversed. Useful to distinguish alternatives.
     *
     * @return String with summary
     */
    fun summary(): String? = summary

    /**
     * Gives a List including all the steps to get from one waypoint to another.
     *
     * @return List of [LegStep]
     */
    fun steps(): List<LegStepNavigation>? = steps

    fun toBuilder() = builder

    class Builder {
        private var distance: Double? = null
        private var duration: Double? = null
        private var summary: String? = null
        private var steps: List<LegStepNavigation>? = null

        fun distance(distance: Double?) =
                apply { this.distance = distance }

        fun duration(duration: Double?) =
                apply { this.duration = duration }

        fun summary(summary: String?) =
                apply { this.summary = summary }

        fun steps(steps: List<LegStepNavigation>?) =
                apply { this.steps = steps }

        fun build(): RouteLegNavigation {
            return RouteLegNavigation(
                    distance,
                    duration,
                    summary,
                    steps,
                    this
            )
        }
    }

    override fun toString(): String {
        return this.distance.toString() +
                this.duration.toString() +
                this.summary +
                this.steps.toString()
    }

    override fun equals(other: Any?): Boolean {
        return when (other is RouteLegNavigation) {
            true -> this.toString() == other.toString()
            false -> false
        }
    }
}
