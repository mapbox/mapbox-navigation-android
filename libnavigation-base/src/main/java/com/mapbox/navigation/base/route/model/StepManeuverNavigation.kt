package com.mapbox.navigation.base.route.model

class StepManeuverNavigation private constructor(

    @StepManeuverType
    private val type: String? = null,
    private val modifier: String? = null,
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

    fun toBuilder() = builder

    class Builder {
        private var type: String? = null
        private var modifier: String? = null

        fun type(type: String?) =
            apply { this.type = type }

        fun modifier(modifier: String?) =
            apply { this.modifier = modifier }

        fun build(): StepManeuverNavigation {

            return StepManeuverNavigation(
                type,
                modifier,
                this
            )
        }
    }

    override fun toString(): String {
        return this.modifier + this.type
    }

    override fun equals(other: Any?): Boolean {
        return when (other is StepManeuverNavigation) {
            true -> this.toString() == other.toString()
            false -> false
        }
    }
}
