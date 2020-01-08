package com.mapbox.navigation.base.route.model

import com.mapbox.geojson.Point

class StepManeuverNavigation private constructor(

    @StepManeuverType
    private val type: String? = null,
    private val modifier: String? = null,
    private val location: DoubleArray? = null,
    private val bearingAfter: Double? = null,
    private val bearingBefore: Double? = null,
    private val instruction: String? = null,
    private val builder: Builder
) {

    /**
     * This indicates the type of maneuver.
     * @see StepManeuverType
     *
     * @return String with type of maneuver
     */
    @StepManeuverType
    fun type(): String? = type

    /**
     * This indicates the mode of the maneuver. If type is of turn, the modifier indicates the
     * change in direction accomplished through the turn. If the type is of depart/arrive, the
     * modifier indicates the position of waypoint from the current direction of travel.
     *
     * @return String with modifier
     */
    fun modifier(): String? = modifier

    /**
     * A [Point] representing this intersection location.
     *
     * @return GeoJson Point representing this intersection location
     */
    fun location(): Point {
        return Point.fromLngLat(location!![0], location[1])
    }

    /**
     * Number between 0 and 360 indicating the clockwise angle from true north to the direction of
     * travel right before the maneuver.
     *
     * @return double with value from 0 to 360
     */
    fun bearingBefore(): Double? = bearingBefore

    /**
     * Number between 0 and 360 indicating the clockwise angle from true north to the direction of
     * travel right after the maneuver.
     *
     * @return double with value from 0 to 360
     */
    fun bearingAfter(): Double? = bearingAfter

    /**
     * A human-readable instruction of how to execute the returned maneuver. This String is built
     * using OSRM-Text-Instructions and can be further customized inside either the Mapbox Navigation
     * SDK for Android or using the OSRM-Text-Instructions.java project in Project-OSRM.
     *
     * @return String with instruction
     * @see [Navigation SDK](https://github.com/mapbox/mapbox-navigation-android)
     *
     * @see [
     * OSRM-Text-Instructions.java](https://github.com/Project-OSRM/osrm-text-instructions.java)
     */
    fun instruction(): String? = instruction

    fun toBuilder() = builder

    class Builder {
        private var type: String? = null
        private var modifier: String? = null
        private var instruction: String? = null
        private var bearingAfter: Double? = null
        private var bearingBefore: Double? = null
        private lateinit var _location: DoubleArray

        fun type(type: String?) =
                apply { this.type = type }

        fun modifier(modifier: String?) =
                apply { this.modifier = modifier }

        fun instruction(instruction: String?) =
                apply { this.instruction = instruction }

        fun bearingAfter(bearingAfter: Double?) =
                apply { this.bearingAfter = bearingAfter }

        fun bearingBefore(bearingBefore: Double?) =
                apply { this.bearingBefore = bearingBefore }

        fun location(location: DoubleArray) =
                apply { this._location = location }

        private fun validate() {
            var missing = ""
            if (!this::_location.isInitialized) {
                missing += " _location"
            }
            check(missing.isEmpty()) { "StepManeuverNavigation.Builder missing required properties: $missing" }
        }

        fun build(): StepManeuverNavigation {
            validate()

            return StepManeuverNavigation(
                type,
                modifier,
                _location,
                bearingAfter,
                bearingBefore,
                instruction,
                this
            )
        }
    }
}
