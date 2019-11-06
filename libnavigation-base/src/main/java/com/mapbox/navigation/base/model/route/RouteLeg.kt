package com.mapbox.navigation.base.model.route

data class RouteLeg internal constructor(
    private var steps: List<LegStep>? = null,
    private var distance: Double? = null,
    private var duration: Double? = null
) {

    fun distance(): Double? = distance

    fun duration(): Double? = duration

    fun steps(): List<LegStep>? = steps

    class Builder {
        private var steps: List<LegStep>? = null
        private var distance: Double? = null
        private var duration: Double? = null

        fun steps(steps: List<LegStep>) =
                apply { this.steps = steps }

        fun distance(distance: Double) =
                apply { this.distance = distance }

        fun duration(duration: Double) =
                apply { this.duration = duration }

        fun build(): RouteLeg {
            return RouteLeg(steps, distance, duration)
        }
    }
}
