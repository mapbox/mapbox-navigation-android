package com.mapbox.navigation.base.route.model

class LegStepNavigation private constructor(
    private val distance: Double = 0.0,
    private val duration: Double = 0.0,
    private val geometry: String? = null,
    private val drivingSide: String? = null,
    private val maneuver: StepManeuverNavigation? = null,
    private val builder: Builder
) {
    /**
     * The distance traveled from the maneuver to the next {@link LegStepNavigation}.
     *
     * @return a double number with unit meters
     */
    fun distance(): Double = distance

    /**
     * The estimated travel time from the maneuver to the next {@link LegStepNavigation}.
     *
     * @return a double number with unit seconds
     */
    fun duration(): Double = duration

    fun geometry(): String? = geometry

    /**
     * The legal driving side at the location for this step. Result will either be {@code left} or
     * {@code right}.
     *
     * @return a string with either a left or right value
     */
    fun drivingSide(): String? = drivingSide

    /**
     * A {@link StepManeuverNavigation} object that typically represents the first coordinate making up the
     * {@link LegStepNavigation#geometry()}.
     *
     * @return new {@link StepManeuverNavigation} object
     */
    fun stepManeuver(): StepManeuverNavigation? = maneuver

    fun toBuilder() = builder

    class Builder {
        private var distance: Double = 0.0
        private var duration: Double = 0.0
        private var geometry: String? = null
        private var drivingSide: String? = null
        private lateinit var stepManeuverBuilder: StepManeuverNavigation

        fun distance(distance: Double) =
            apply { this.distance = distance }

        fun duration(duration: Double) =
            apply { this.duration = duration }

        fun geometry(geometry: String?) =
            apply { this.geometry = geometry }

        fun drivingSide(drivingSide: String?) =
            apply { this.drivingSide = drivingSide }

        fun stepManeuver(stepManeuver: StepManeuverNavigation) =
            apply { this.stepManeuverBuilder = stepManeuver }

        private fun validate() {
            var missing = ""
            if (!this::stepManeuverBuilder.isInitialized) {
                missing += " stepManeuverBuilder"
            }
            check(missing.isEmpty()) { "LegStepNavigation.Builder missing required properties: $missing" }
        }

        fun build(): LegStepNavigation {
            validate()

            return LegStepNavigation(
                distance,
                duration,
                geometry,
                drivingSide,
                stepManeuverBuilder,
                this
            )
        }
    }

    override fun toString(): String {
        return this.distance.toString() +
            this.duration.toString() +
            this.drivingSide +
            this.geometry +
            this.maneuver.toString()
    }

    override fun equals(other: Any?): Boolean {
        return when (other is LegStepNavigation) {
            true -> this.toString() == other.toString()
            false -> false
        }
    }
}
