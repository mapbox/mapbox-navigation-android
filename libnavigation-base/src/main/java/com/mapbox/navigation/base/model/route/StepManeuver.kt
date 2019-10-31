package com.mapbox.navigation.base.model.route

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.typedef.ManeuverType

data class StepManeuver(
    private var instruction: String? = null,
    private var type: ManeuverType? = null,
    private var modifier: String? = null,
    private var rawLocation: DoubleArray
) {

    fun instruction(): String? = null

    @ManeuverType
    fun type(): String? = null

    fun modifier(): String? = null

    fun location(): Point {
        return Point.fromLngLat(rawLocation[0], rawLocation[1])
    }

    internal fun rawLocation(): DoubleArray? = rawLocation

    class Builder {
        private var instruction: String? = null
        private var type: ManeuverType? = null
        private var modifier: String? = null
        private lateinit var rawLoc: DoubleArray

        fun instruction(instruction: String) =
                apply { this.instruction = instruction }

        fun type(type: ManeuverType) =
                apply { this.type = type }

        fun modifier(modifier: String) =
                apply { this.modifier = modifier }

        fun rawLocation(rawLocation: DoubleArray) =
                apply { this.rawLoc = rawLocation }

        fun build(): StepManeuver {
            check(::rawLoc.isInitialized) { "Missing property rawLocation" }
            return StepManeuver(instruction, type, modifier, rawLoc)
        }
    }
}
