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

    /**
     * Maps the [routeProgress] to a [Trip]. The destination is named after the final waypoint of
     * the route. This overload has no Context to read string resources, so a route without a
     * destination name falls back to the English "Destination".
     */
    fun from(
        routeProgress: RouteProgress,
        maneuverApi: MapboxManeuverApi,
    ): Trip {
        return from(
            routeProgress,
            maneuverApi,
            destinationName(routeProgress) ?: DEFAULT_DESTINATION_NAME,
        )
    }

    /**
     * Same as the public [from], with the [destinationName] resolved by the caller, which can
     * localize the fallback used when the route has no destination waypoint name.
     */
    internal fun from(
        routeProgress: RouteProgress,
        maneuverApi: MapboxManeuverApi,
        destinationName: String,
    ): Trip {
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        val maneuver = from(maneuvers, routeProgress).build()
        val step = Step.Builder().setManeuver(maneuver).build()

        return Trip.Builder()
            .addStep(step, currentStepTravelEstimate(routeProgress))
            .addDestination(destination(destinationName), destinationTravelEstimate(routeProgress))
            .build()
    }

    /**
     * The name of the final waypoint of the route, or null when the route has none. Prefers the
     * name given in the route request over the name of the road the waypoint snapped to.
     */
    internal fun destinationName(routeProgress: RouteProgress): String? {
        val route = routeProgress.navigationRoute
        val requestedName = route.directionsRoute.routeOptions()
            ?.waypointNamesList()
            ?.lastOrNull()
            ?.takeIf { it.isNotBlank() }
        return requestedName ?: route.waypoints
            ?.lastOrNull()
            ?.name()
            ?.takeIf { it.isNotBlank() }
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

    private fun destination(name: String): Destination =
        Destination.Builder().setName(name).build()

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

    private const val DEFAULT_DESTINATION_NAME = "Destination"

    fun from(
        exp: Expected<ManeuverError, List<com.mapbox.navigation.tripdata.maneuver.model.Maneuver>>,
    ): Maneuver.Builder = from(exp, routeProgress = null)

    private fun from(
        exp: Expected<ManeuverError, List<com.mapbox.navigation.tripdata.maneuver.model.Maneuver>>,
        routeProgress: RouteProgress?,
    ): Maneuver.Builder {
        return exp.mapValue {
            when (it.isEmpty()) {
                true -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
                false -> {
                    val primary = it.first().primary
                    fromAnnouncedStep(
                        primary.type,
                        primary.modifier,
                        primary.degrees,
                        primary.drivingSide,
                        primary.componentList,
                        routeProgress,
                        PRIMARY_MANEUVER_STEP_OFFSET,
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
            StepManeuver.TURN -> mapTurn(maneuverModifier, drivingSide)
            StepManeuver.DEPART -> Maneuver.Builder(Maneuver.TYPE_DEPART)
            StepManeuver.ARRIVE -> mapArrive(maneuverModifier)
            StepManeuver.MERGE -> mapMerge(maneuverModifier)
            StepManeuver.ON_RAMP -> mapOnRamp(maneuverModifier)
            StepManeuver.OFF_RAMP -> mapOffRamp(maneuverModifier, drivingSide)
            StepManeuver.FORK -> mapFork(maneuverModifier, drivingSide)
            StepManeuver.END_OF_ROAD -> mapEndOfRoad(maneuverModifier, drivingSide)
            StepManeuver.CONTINUE -> Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
            StepManeuver.EXIT_ROTARY,
            StepManeuver.EXIT_ROUNDABOUT,
            -> mapRoundaboutExit(drivingSide)

            StepManeuver.ROTARY,
            StepManeuver.ROUNDABOUT_TURN,
            StepManeuver.ROUNDABOUT,
            -> mapRoundabout(maneuverModifier, degrees, drivingSide, roundaboutExitNumber)

            StepManeuver.NOTIFICATION -> mapEmptyManeuverType(maneuverModifier, drivingSide)
            else -> mapEmptyManeuverType(maneuverModifier, drivingSide)
        }
    }

    private fun isLeftHandTraffic(drivingSide: String?): Boolean =
        drivingSide == DRIVING_SIDE_LEFT

    // U-turns are made towards the centre of the road: to the left in right-hand traffic
    // and to the right in left-hand traffic.
    private fun uTurn(drivingSide: String?): Maneuver.Builder = Maneuver.Builder(
        if (isLeftHandTraffic(drivingSide)) {
            Maneuver.TYPE_U_TURN_RIGHT
        } else {
            Maneuver.TYPE_U_TURN_LEFT
        },
    )

    private fun mapTurn(maneuverModifier: String?, drivingSide: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN -> uTurn(drivingSide)
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

    private fun mapEmptyManeuverType(
        maneuverModifier: String?,
        drivingSide: String?,
    ): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN -> uTurn(drivingSide)
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

            // Arrival steps often carry no modifier.
            else -> Maneuver.Builder(Maneuver.TYPE_DESTINATION)
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

    private fun mapOffRamp(maneuverModifier: String?, drivingSide: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            // A ramp without a side leaves on the near side of the road.
            ManeuverModifier.UTURN,
            ManeuverModifier.STRAIGHT,
            -> if (isLeftHandTraffic(drivingSide)) {
                Maneuver.Builder(Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT)
            } else {
                Maneuver.Builder(Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT)
            }

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

    private fun mapFork(maneuverModifier: String?, drivingSide: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            // A fork without a side keeps to the near side of the road.
            ManeuverModifier.UTURN,
            ManeuverModifier.STRAIGHT,
            -> if (isLeftHandTraffic(drivingSide)) {
                Maneuver.Builder(Maneuver.TYPE_FORK_LEFT)
            } else {
                Maneuver.Builder(Maneuver.TYPE_FORK_RIGHT)
            }

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

    private fun mapEndOfRoad(maneuverModifier: String?, drivingSide: String?): Maneuver.Builder {
        return when (maneuverModifier) {
            ManeuverModifier.UTURN -> uTurn(drivingSide)
            ManeuverModifier.STRAIGHT -> Maneuver.Builder(Maneuver.TYPE_STRAIGHT)
            ManeuverModifier.RIGHT -> Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT)
            ManeuverModifier.SLIGHT_RIGHT -> Maneuver.Builder(Maneuver.TYPE_TURN_SLIGHT_RIGHT)
            ManeuverModifier.SHARP_RIGHT -> Maneuver.Builder(Maneuver.TYPE_TURN_SHARP_RIGHT)
            ManeuverModifier.LEFT -> Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_LEFT)
            ManeuverModifier.SLIGHT_LEFT -> Maneuver.Builder(Maneuver.TYPE_TURN_SLIGHT_LEFT)
            ManeuverModifier.SHARP_LEFT -> Maneuver.Builder(Maneuver.TYPE_TURN_SHARP_LEFT)
            else -> Maneuver.Builder(Maneuver.TYPE_UNKNOWN)
        }
    }

    // The host does not accept an exit number for the roundabout exit types.
    private fun mapRoundaboutExit(drivingSide: String?): Maneuver.Builder = Maneuver.Builder(
        if (isLeftHandTraffic(drivingSide)) {
            Maneuver.TYPE_ROUNDABOUT_EXIT_CW
        } else {
            Maneuver.TYPE_ROUNDABOUT_EXIT_CCW
        },
    )

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

    /**
     * Maps a banner maneuver using the route step it announces, [stepOffset] steps after the
     * current step.
     *
     * The banner instructions of a step announce the maneuver that starts the next step, so the
     * primary maneuver uses [PRIMARY_MANEUVER_STEP_OFFSET] and the sub-maneuver uses
     * [SUB_MANEUVER_STEP_OFFSET]. The announced step provides the roundabout exit number, and
     * tells a roundabout banner that only leaves a roundabout the driver is already in.
     */
    internal fun fromAnnouncedStep(
        maneuverType: String?,
        maneuverModifier: String?,
        degrees: Double?,
        drivingSide: String?,
        componentList: List<Component>,
        routeProgress: RouteProgress?,
        stepOffset: Int,
    ): Maneuver.Builder {
        val announcedType = announcedStepManeuver(routeProgress, stepOffset)?.type()
        val type = if (
            maneuverType in ROUNDABOUT_STEP_MANEUVER_TYPES &&
            announcedType in ROUNDABOUT_EXIT_STEP_MANEUVER_TYPES
        ) {
            announcedType
        } else {
            maneuverType
        }
        return from(
            type,
            maneuverModifier,
            degrees,
            drivingSide,
            roundaboutExitNumber(routeProgress, stepOffset, componentList),
        )
    }

    /**
     * The roundabout exit number of the step [stepOffset] steps after the current step. Falls
     * back to the exit number banner component of [componentList] when that step is not a
     * roundabout with an exit number.
     */
    internal fun roundaboutExitNumber(
        routeProgress: RouteProgress?,
        stepOffset: Int,
        componentList: List<Component>,
    ): Int? {
        val stepExit = announcedStepManeuver(routeProgress, stepOffset)
            ?.takeIf { it.type() in ROUNDABOUT_STEP_MANEUVER_TYPES }
            ?.exit()
        return stepExit ?: roundaboutExitNumber(componentList)
    }

    private fun announcedStepManeuver(
        routeProgress: RouteProgress?,
        stepOffset: Int,
    ): StepManeuver? {
        val legProgress = routeProgress?.currentLegProgress ?: return null
        val stepIndex = legProgress.currentStepProgress?.stepIndex ?: return null
        return legProgress.routeLeg?.steps()?.getOrNull(stepIndex + stepOffset)?.maneuver()
    }

    internal fun roundaboutExitNumber(componentList: List<Component>): Int? {
        return componentList
            .firstOrNull { it.type == BannerComponents.EXIT_NUMBER }
            ?.let { it.node as? ExitNumberComponentNode }
            ?.text
            ?.toIntOrNull()
    }

    internal const val PRIMARY_MANEUVER_STEP_OFFSET = 1
    internal const val SUB_MANEUVER_STEP_OFFSET = 2
    private val ROUNDABOUT_STEP_MANEUVER_TYPES = setOf(
        StepManeuver.ROUNDABOUT,
        StepManeuver.ROUNDABOUT_TURN,
        StepManeuver.ROTARY,
    )
    private val ROUNDABOUT_EXIT_STEP_MANEUVER_TYPES = setOf(
        StepManeuver.EXIT_ROUNDABOUT,
        StepManeuver.EXIT_ROTARY,
    )
    private const val DRIVING_SIDE_LEFT = "left"
    private const val DEFAULT_ROUNDABOUT_EXIT_NUMBER = 1
    private const val MIN_ROUNDABOUT_EXIT_NUMBER = 1
    private const val MIN_ROUNDABOUT_EXIT_ANGLE = 1
    private const val MAX_ROUNDABOUT_EXIT_ANGLE = 360
}
