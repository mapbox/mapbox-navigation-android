package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.androidauto.navigation.CarDistanceFormatter
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

object CarManeuverMapper {

    fun from(
        routeProgress: RouteProgress,
        maneuverApi: MapboxManeuverApi,
    ): Trip {
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        val maneuver = from(maneuvers).build()
        val step = Step.Builder().setManeuver(maneuver).build()

        return Trip.Builder()
            .addStep(step, currentStepTravelEstimate(routeProgress))
            .addDestination(destination(), destinationTravelEstimate(routeProgress))
            .build()
    }

    private fun currentStepTravelEstimate(routeProgress: RouteProgress): TravelEstimate {
        val stepProgress = routeProgress.currentLegProgress?.currentStepProgress
            ?: return destinationTravelEstimate(routeProgress)

        return TravelEstimate.Builder(
            CarDistanceFormatter.carDistance(stepProgress.distanceRemaining.toDouble()),
            arrivalTime(stepProgress.durationRemaining),
        ).setRemainingTimeSeconds(remainingTimeSeconds(stepProgress.durationRemaining)).build()
    }

    private fun destinationTravelEstimate(routeProgress: RouteProgress): TravelEstimate {
        return TravelEstimate.Builder(
            CarDistanceFormatter.carDistance(routeProgress.distanceRemaining.toDouble()),
            arrivalTime(routeProgress.durationRemaining),
        ).setRemainingTimeSeconds(remainingTimeSeconds(routeProgress.durationRemaining)).build()
    }

    private fun destination(): Destination =
        Destination.Builder().setName(DESTINATION_NAME).build()

    private fun arrivalTime(secondsRemaining: Double): DateTimeWithZone {
        val safeSeconds = if (secondsRemaining.isFinite()) secondsRemaining else 0.0
        val calendar = Calendar.getInstance().also {
            it.add(Calendar.SECOND, safeSeconds.toInt())
        }
        return DateTimeWithZone.create(calendar.timeInMillis, TimeZone.getDefault())
    }

    private fun remainingTimeSeconds(secondsRemaining: Double): Long {
        return if (secondsRemaining.isFinite() && secondsRemaining >= 0.0) {
            secondsRemaining.toLong()
        } else {
            TravelEstimate.REMAINING_TIME_UNKNOWN
        }
    }

    private const val DESTINATION_NAME = "Destination"

    fun from(
        exp: Expected<ManeuverError, List<com.mapbox.navigation.tripdata.maneuver.model.Maneuver>>,
    ): Maneuver.Builder {
        return exp.mapValue {
            when (it.isEmpty()) {
                true -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
                false -> {
                    val primary = it.first().primary
                    from(
                        primary.type,
                        primary.modifier,
                        primary.degrees,
                        primary.drivingSide,
                        roundaboutExitNumber(primary.componentList),
                    )
                }
            }
        }.fold(
            {
                Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
            },
            {
                it
            },
        )
    }

    @JvmOverloads
    fun from(
        maneuverType: String?,
        maneuverModifier: String?,
        degrees: Double? = null,
        drivingSide: String? = null,
        roundaboutExitNumber: Int? = null,
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
            -> mapRoundabout(maneuverModifier, degrees, drivingSide, roundaboutExitNumber)

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

    private fun mapRoundabout(
        maneuverModifier: String?,
        degrees: Double?,
        drivingSide: String?,
        roundaboutExitNumber: Int?,
    ): Maneuver.Builder {
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
                val exitNumber = roundaboutExitNumber
                    ?.takeIf { it >= MIN_ROUNDABOUT_EXIT_NUMBER }
                    ?: DEFAULT_ROUNDABOUT_EXIT_NUMBER
                val exitAngle = degrees
                    ?.takeIf { it.isFinite() }
                    ?.roundToInt()
                    ?.takeIf { it in MIN_ROUNDABOUT_EXIT_ANGLE..MAX_ROUNDABOUT_EXIT_ANGLE }
                val (enterAndExitType, enterAndExitWithAngleType) =
                    if (drivingSide == DRIVING_SIDE_LEFT) {
                        Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW to
                            Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE
                    } else {
                        Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW to
                            Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE
                    }
                if (exitAngle != null) {
                    Maneuver.Builder(enterAndExitWithAngleType)
                        .setRoundaboutExitNumber(exitNumber)
                        .setRoundaboutExitAngle(exitAngle)
                } else {
                    Maneuver.Builder(enterAndExitType)
                        .setRoundaboutExitNumber(exitNumber)
                }
            }

            else -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
        }
    }

    internal fun roundaboutExitNumber(componentList: List<Component>): Int? {
        return componentList
            .firstOrNull { it.type == BannerComponents.EXIT_NUMBER }
            ?.let { it.node as? ExitNumberComponentNode }
            ?.text
            ?.toIntOrNull()
    }

    private const val DRIVING_SIDE_LEFT = "left"
    private const val DEFAULT_ROUNDABOUT_EXIT_NUMBER = 1
    private const val MIN_ROUNDABOUT_EXIT_NUMBER = 1
    private const val MIN_ROUNDABOUT_EXIT_ANGLE = 1
    private const val MAX_ROUNDABOUT_EXIT_ANGLE = 360
}
