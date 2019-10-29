package com.mapbox.navigation.base.route.model

import androidx.annotation.StringDef
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.geojson.Point

class StepManeuverNavigation(
    val location: Point,
    val bearingBefore: Double?,
    val bearingAfter: Double?,
    @StepManeuverTypeNavigation type: String?,
    val exit: Int?

) {
    companion object {

        const val UNKNOWN = "UNKNOWN"

        private val typesArray = arrayOf(
            StepManeuver.TURN,
            StepManeuver.NEW_NAME,
            StepManeuver.DEPART,
            StepManeuver.ARRIVE,
            StepManeuver.MERGE,
            StepManeuver.ON_RAMP,
            StepManeuver.OFF_RAMP,
            StepManeuver.FORK,
            StepManeuver.END_OF_ROAD,
            StepManeuver.CONTINUE,
            StepManeuver.ROUNDABOUT,
            StepManeuver.ROTARY,
            StepManeuver.ROUNDABOUT_TURN,
            StepManeuver.NOTIFICATION,
            StepManeuver.EXIT_ROUNDABOUT,
            StepManeuver.EXIT_ROTARY
        )

        fun typeExist(@StepManeuver.StepManeuverType type: String?): Boolean {
            return type in typesArray
        }
    }

    @Retention(AnnotationRetention.RUNTIME)
    @StringDef(
        StepManeuver.TURN,
        StepManeuver.NEW_NAME,
        StepManeuver.DEPART,
        StepManeuver.ARRIVE,
        StepManeuver.MERGE,
        StepManeuver.ON_RAMP,
        StepManeuver.OFF_RAMP,
        StepManeuver.FORK,
        StepManeuver.END_OF_ROAD,
        StepManeuver.CONTINUE,
        StepManeuver.ROUNDABOUT,
        StepManeuver.ROTARY,
        StepManeuver.ROUNDABOUT_TURN,
        StepManeuver.NOTIFICATION,
        StepManeuver.EXIT_ROUNDABOUT,
        StepManeuver.EXIT_ROTARY,
        UNKNOWN
    )
    annotation class StepManeuverTypeNavigation
}

fun StepManeuver.mapToManeuverStep() = StepManeuverNavigation(
    location = location(),
    bearingBefore = bearingBefore(),
    bearingAfter = bearingAfter(),
    type = if (StepManeuverNavigation.typeExist(type())) type() else StepManeuverNavigation.UNKNOWN,
    exit = exit()
)
