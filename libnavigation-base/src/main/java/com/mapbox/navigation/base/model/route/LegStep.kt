package com.mapbox.navigation.base.model.route

data class LegStep internal constructor(
    private var distance: Double? = null,
    private var duration: Double? = null,
    private var name: String? = null,
    private var drivingSide: String? = null,
    private var geometry: String? = null,
    private var stepManeuver: StepManeuver
) {

    fun distance(): Double? = distance

    fun duration(): Double? = duration

    fun name(): String? = name

    fun drivingSide(): String? = drivingSide

    fun geometry(): String? = geometry

    fun stepManeuver(): StepManeuver = stepManeuver

    class Builder {
        private var distance: Double? = null
        private var duration: Double? = null
        private var name: String? = null
        private lateinit var maneuver: StepManeuver
        private var drivingSide: String? = null
        private var geometry: String? = null

        fun distance(distance: Double) =
                apply { this.distance = distance }

        fun duration(duration: Double) =
                apply { this.duration = duration }

        fun name(name: String) =
                apply { this.name = name }

        fun drivingSide(drivingSide: String) =
                apply { this.drivingSide = drivingSide }

        fun geometry(geometry: String) =
                apply { this.geometry = geometry }

        fun stepManeuver(maneuver: StepManeuver) =
                apply { this.maneuver = maneuver }

        fun build(): LegStep {
            check(::maneuver.isInitialized) { "Missing property stepManeuver" }
            return LegStep(distance, duration, name, drivingSide, geometry, maneuver)
        }
    }
}
