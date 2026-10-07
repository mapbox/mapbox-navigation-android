package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.model.Lane
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import kotlin.math.roundToLong

/**
 * The parts of a [CarNavigationInfo] that the host shows. Two infos with equal keys look the
 * same in the car, even when the remaining time or arrival time differ by seconds.
 */
internal data class CarNavigationInfoDisplayKey(
    val primary: PrimaryManeuver?,
    val secondary: SecondaryManeuver?,
    val sub: SubManeuver?,
    val lane: Lane?,
    val shields: List<RouteShield>,
    // Compared by identity: every banner produces a new junction value.
    val junctionValue: JunctionValue?,
    // The roundabout exit numbers are taken from the announced steps.
    val legIndex: Int?,
    val stepIndex: Int?,
    val hasNavigationInfo: Boolean,
    val stepDistance: Distance?,
    val hasTravelEstimate: Boolean,
    val remainingDistance: Distance?,
    val arrivalMinute: Long?,
    val remainingMinutes: Long?,
) {
    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val SECONDS_PER_MINUTE = 60.0

        fun create(
            maneuver: Maneuver?,
            shields: List<RouteShield>,
            junctionValue: JunctionValue?,
            routeProgress: RouteProgress,
            navigationInfo: NavigationTemplate.NavigationInfo?,
            travelEstimate: TravelEstimate?,
        ): CarNavigationInfoDisplayKey {
            val legProgress = routeProgress.currentLegProgress
            return CarNavigationInfoDisplayKey(
                primary = maneuver?.primary,
                secondary = maneuver?.secondary,
                sub = maneuver?.sub,
                lane = maneuver?.laneGuidance,
                shields = shields,
                junctionValue = junctionValue,
                legIndex = legProgress?.legIndex,
                stepIndex = legProgress?.currentStepProgress?.stepIndex,
                hasNavigationInfo = navigationInfo != null,
                stepDistance = (navigationInfo as? RoutingInfo)?.currentDistance,
                hasTravelEstimate = travelEstimate != null,
                remainingDistance = travelEstimate?.remainingDistance,
                // Arrival times are shown as clock minutes.
                arrivalMinute = travelEstimate?.arrivalTimeAtDestination
                    ?.timeSinceEpochMillis
                    ?.let { Math.floorDiv(it, MILLIS_PER_MINUTE) },
                // Remaining times are shown in whole minutes.
                remainingMinutes = travelEstimate?.remainingTimeSeconds
                    ?.let { seconds ->
                        if (seconds == TravelEstimate.REMAINING_TIME_UNKNOWN) {
                            seconds
                        } else {
                            (seconds / SECONDS_PER_MINUTE).roundToLong()
                        }
                    },
            )
        }
    }
}
