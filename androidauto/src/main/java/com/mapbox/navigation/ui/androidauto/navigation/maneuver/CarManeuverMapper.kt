package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.androidauto.navigation.CarDistanceFormatter
import java.util.Calendar
import java.util.TimeZone

object CarManeuverMapper {

    fun from(
        routeProgress: RouteProgress,
        maneuverApi: MapboxManeuverApi,
    ): Trip {
        val etaAsCalendar = Calendar.getInstance().also {
            it.add(Calendar.SECOND, routeProgress.durationRemaining.toInt())
        }

        val eta = TravelEstimate.Builder(
            CarDistanceFormatter.carDistance(routeProgress.distanceRemaining.toDouble()),
            DateTimeWithZone.create(etaAsCalendar.timeInMillis, TimeZone.getDefault()),
        ).build()
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        val maneuver = from(maneuvers).build()
        val step = Step.Builder().setManeuver(maneuver).build()
        return Trip.Builder().addStep(step, eta).build()
    }

    fun from(
        exp: Expected<ManeuverError, List<com.mapbox.navigation.tripdata.maneuver.model.Maneuver>>,
    ): Maneuver.Builder {
        return exp.mapValue {
            when (it.isEmpty()) {
                true -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
                false -> from(
                    it.first().primary.type,
                    it.first().primary.modifier,
                    it.first().primary.degrees,
                )
            }
        }.fold({
            Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
        }, {
            it
        },)
    }

    fun from(
        maneuverType: String?,
        maneuverModifier: String?,
        degrees: Double? = null,
    ): Maneuver.Builder {
        return when (maneuverType) {
            StepManeuver.TURN -> mapTurn(maneuverModifier)
            StepManeuver.DEPART -> Maneuver.Builder(Maneuver.TYPE_DEPART)
            StepManeuver.ARRIVE -> mapArrive(maneuverModifier)
            StepManeuver.MERGE -> mapMerge(maneuverModifier)
            StepManeuver.ON_RAMP -> mapOnRamp(maneuverModifier)
            StepManeuver.OFF_RAMP -> mapOffRamp(maneuverModifier)
            StepManeuver.FORK -> mapFork(maneuverModifier)
            StepManeuver.END_OF_ROAD -> mapEndOfRoad(maneuverModifier)
            StepManeuver.CONTINUE -> Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
            StepManeuver.ROTARY,
            StepManeuver.EXIT_ROTARY,
            StepManeuver.EXIT_ROUNDABOUT,
            StepManeuver.ROUNDABOUT_TURN,
            StepManeuver.ROUNDABOUT,
            -> mapRoundabout(maneuverModifier, degrees)
            StepManeuver.NOTIFICATION -> mapEmptyManeuverType(maneuverModifier)
            else -> mapEmptyManeuverType(maneuverModifier)
        }
    }

    private fun mapTurn(maneuverModifier: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN -> Maneuver.Builder(Maneuver.TYPE_U_TURN_LEFT)
            ManeuverModifier.STRAIGHT -> Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
            ManeuverModifier.RIGHT -> Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT)
            ManeuverModifier.SLIGHT_RIGHT -> Maneuver.Builder(Maneuver.TYPE_TURN_SLIGHT_RIGHT)
            ManeuverModifier.SHARP_RIGHT -> Maneuver.Builder(Maneuver.TYPE_TURN_SHARP_RIGHT)
            ManeuverModifier.LEFT -> Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_LEFT)
            ManeuverModifier.SLIGHT_LEFT -> Maneuver.Builder(Maneuver.TYPE_TURN_SLIGHT_LEFT)
            ManeuverModifier.SHARP_LEFT -> Maneuver.Builder(Maneuver.TYPE_TURN_SHARP_LEFT)
            else -> Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
        }
    }

    private fun mapEmptyManeuverType(maneuverModifier: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN -> Maneuver.Builder(Maneuver.TYPE_DESTINATION)
            ManeuverModifier.STRAIGHT -> Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
            ManeuverModifier.RIGHT -> Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT)
            ManeuverModifier.SLIGHT_RIGHT -> Maneuver.Builder(Maneuver.TYPE_TURN_SLIGHT_RIGHT)
            ManeuverModifier.SHARP_RIGHT -> Maneuver.Builder(Maneuver.TYPE_TURN_SHARP_RIGHT)
            ManeuverModifier.LEFT -> Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_LEFT)
            ManeuverModifier.SLIGHT_LEFT -> Maneuver.Builder(Maneuver.TYPE_TURN_SLIGHT_LEFT)
            ManeuverModifier.SHARP_LEFT -> Maneuver.Builder(Maneuver.TYPE_TURN_SHARP_LEFT)
            else -> Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
        }
    }

    private fun mapArrive(maneuverModifier: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN -> Maneuver.Builder(Maneuver.TYPE_DESTINATION)
            ManeuverModifier.STRAIGHT -> Maneuver.Builder(Maneuver.TYPE_DESTINATION_STRAIGHT)
            ManeuverModifier.RIGHT,
            ManeuverModifier.SLIGHT_RIGHT,
            ManeuverModifier.SHARP_RIGHT,
            -> Maneuver.Builder(Maneuver.TYPE_DESTINATION_RIGHT)
            ManeuverModifier.LEFT,
            ManeuverModifier.SLIGHT_LEFT,
            ManeuverModifier.SHARP_LEFT,
            -> Maneuver.Builder(Maneuver.TYPE_DESTINATION_LEFT)
            else -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
        }
    }

    private fun mapMerge(maneuverModifier: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN,
            ManeuverModifier.STRAIGHT,
            -> Maneuver.Builder(Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED)
            ManeuverModifier.RIGHT,
            ManeuverModifier.SLIGHT_RIGHT,
            ManeuverModifier.SHARP_RIGHT,
            -> Maneuver.Builder(Maneuver.TYPE_MERGE_RIGHT)
            ManeuverModifier.LEFT,
            ManeuverModifier.SLIGHT_LEFT,
            ManeuverModifier.SHARP_LEFT,
            -> Maneuver.Builder(Maneuver.TYPE_MERGE_LEFT)
            else -> Maneuver.Builder(Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED)
        }
    }

    private fun mapOnRamp(maneuverModifier: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN,
            ManeuverModifier.STRAIGHT,
            -> Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
            ManeuverModifier.RIGHT -> Maneuver.Builder(Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT)
            ManeuverModifier.SLIGHT_RIGHT -> Maneuver.Builder(Maneuver.TYPE_ON_RAMP_SLIGHT_RIGHT)
            ManeuverModifier.SHARP_RIGHT -> Maneuver.Builder(Maneuver.TYPE_ON_RAMP_SHARP_RIGHT)
            ManeuverModifier.LEFT -> Maneuver.Builder(Maneuver.TYPE_ON_RAMP_NORMAL_LEFT)
            ManeuverModifier.SLIGHT_LEFT -> Maneuver.Builder(Maneuver.TYPE_ON_RAMP_SLIGHT_LEFT)
            ManeuverModifier.SHARP_LEFT -> Maneuver.Builder(Maneuver.TYPE_ON_RAMP_SHARP_LEFT)
            else -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
        }
    }

    private fun mapOffRamp(maneuverModifier: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN,
            ManeuverModifier.STRAIGHT,
            ManeuverModifier.RIGHT,
            ManeuverModifier.SHARP_RIGHT,
            -> Maneuver.Builder(Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT)
            ManeuverModifier.SLIGHT_RIGHT -> Maneuver.Builder(Maneuver.TYPE_OFF_RAMP_SLIGHT_RIGHT)
            ManeuverModifier.LEFT,
            ManeuverModifier.SHARP_LEFT,
            -> Maneuver.Builder(Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT)
            ManeuverModifier.SLIGHT_LEFT -> Maneuver.Builder(Maneuver.TYPE_OFF_RAMP_SLIGHT_LEFT)
            else -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
        }
    }

    private fun mapFork(maneuverModifier: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN,
            ManeuverModifier.STRAIGHT,
            ManeuverModifier.RIGHT,
            ManeuverModifier.SLIGHT_RIGHT,
            ManeuverModifier.SHARP_RIGHT,
            -> Maneuver.Builder(Maneuver.TYPE_FORK_RIGHT)
            ManeuverModifier.LEFT,
            ManeuverModifier.SLIGHT_LEFT,
            ManeuverModifier.SHARP_LEFT,
            -> Maneuver.Builder(Maneuver.TYPE_FORK_LEFT)
            else -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
        }
    }

    private fun mapEndOfRoad(maneuverModifier: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN,
            ManeuverModifier.STRAIGHT,
            -> Maneuver.Builder(Maneuver.TYPE_DESTINATION_STRAIGHT)
            ManeuverModifier.RIGHT,
            ManeuverModifier.SLIGHT_RIGHT,
            ManeuverModifier.SHARP_RIGHT,
            -> Maneuver.Builder(Maneuver.TYPE_DESTINATION_RIGHT)
            ManeuverModifier.LEFT,
            ManeuverModifier.SLIGHT_LEFT,
            ManeuverModifier.SHARP_LEFT,
            -> Maneuver.Builder(Maneuver.TYPE_DESTINATION_LEFT)
            else -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
        }
    }

    private fun mapRoundabout(maneuverModifier: String?, degrees: Double?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN,
            ManeuverModifier.STRAIGHT,
            ManeuverModifier.RIGHT,
            ManeuverModifier.SLIGHT_RIGHT,
            ManeuverModifier.SHARP_RIGHT,
            ManeuverModifier.LEFT,
            ManeuverModifier.SLIGHT_LEFT,
            ManeuverModifier.SHARP_LEFT,
            -> {
                // TODO fix hardcoded roundabout exit number https://github.com/mapbox/mapbox-navigation-android/issues/4855
                if (degrees != null) {
                    Maneuver.Builder(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE)
                        .setRoundaboutExitNumber(1).setRoundaboutExitAngle(degrees.toInt())
                } else {
                    Maneuver.Builder(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW)
                        .setRoundaboutExitNumber(1)
                }
            }
            else -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
        }
    }
}
