package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

/**
 * A factory exposed to build a [Maneuver] object.
 */
@ExperimentalMapboxNavigationAPI
object ManeuverFactory {

    /**
     * Build [Maneuver] given appropriate arguments
     */
    @JvmStatic
    fun buildManeuver(
        primary: PrimaryManeuver,
        stepDistance: StepDistance,
        secondary: SecondaryManeuver?,
        sub: SubManeuver?,
        lane: Lane?,
        point: Point,
    ) = Maneuver(primary, stepDistance, secondary, sub, lane, point)
}
