package com.mapbox.navigation.base.model.route

data class RouteLeg internal constructor(
    private var steps: List<LegStep>? = null,
    private var distance: Double? = null
) {

    /**
     * The distance traveled from one waypoint to another.
     *
     * @return a double number with unit meters
     * @since 1.0.0
     */
    fun distance(): Double? = distance

    /**
     * Gives a List including all the steps to get from one waypoint to another.
     *
     * @return List of [LegStep]
     * @since 1.0.0
     */
    fun steps(): List<LegStep>? = steps

    class Builder {
        private var steps: List<LegStep>? = null
        private var distance: Double? = null

        fun steps(steps: List<LegStep>) =
                apply { this.steps = steps }

        fun distance(distance: Double) =
                apply { this.distance = distance }

        fun build(): RouteLeg {
            return RouteLeg(steps, distance)
        }
    }
}
