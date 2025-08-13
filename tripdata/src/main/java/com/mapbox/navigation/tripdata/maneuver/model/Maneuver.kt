package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.geojson.Point

/**
 * Data structure that holds information about a maneuver.
 * @property primary PrimaryManeuver
 * @property stepDistance StepDistance
 * @property secondary SecondaryManeuver
 * @property sub SubManeuver
 * @property laneGuidance Lane
 * @property maneuverPoint Point
 */
class Maneuver internal constructor(
    val primary: PrimaryManeuver,
    val stepDistance: StepDistance,
    val secondary: SecondaryManeuver? = null,
    val sub: SubManeuver? = null,
    val laneGuidance: Lane? = null,
    val maneuverPoint: Point,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Maneuver

        if (primary != other.primary) return false
        if (secondary != other.secondary) return false
        if (sub != other.sub) return false
        if (laneGuidance != other.laneGuidance) return false
        if (stepDistance != other.stepDistance) return false
        if (maneuverPoint != other.maneuverPoint) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = primary.hashCode()
        result = 31 * result + secondary.hashCode()
        result = 31 * result + sub.hashCode()
        result = 31 * result + laneGuidance.hashCode()
        result = 31 * result + maneuverPoint.hashCode()
        return result
    }

    internal fun copy(
        primary: PrimaryManeuver = this.primary,
        stepDistance: StepDistance = this.stepDistance,
        secondary: SecondaryManeuver? = this.secondary,
        sub: SubManeuver? = this.sub,
        laneGuidance: Lane? = this.laneGuidance,
        maneuverPoint: Point = this.maneuverPoint,
    ) = Maneuver(primary, stepDistance, secondary, sub, laneGuidance, maneuverPoint)

    override fun toString(): String {
        return "Maneuver(" +
            "primary=$primary, " +
            "stepDistance=$stepDistance, " +
            "secondary=$secondary, " +
            "sub=$sub, " +
            "laneGuidance=$laneGuidance, " +
            "maneuverPoint=$maneuverPoint" +
            ")"
    }
}
