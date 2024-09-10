package com.mapbox.navigation.core.internal.congestions.processor

import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.core.internal.congestions.getOverriddenTrafficForProgress
import com.mapbox.navigation.core.internal.congestions.model.CongestionRangeGroup
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction
import com.mapbox.navigation.core.internal.congestions.speed.AheadDistanceCalculator
import com.mapbox.navigation.core.internal.congestions.speed.PredictedTimeAheadDistanceCalculator
import com.mapbox.navigation.core.internal.congestions.speed.updateTraffic
import com.mapbox.navigation.utils.internal.logD

/**
 * Updates the primary route congestion numeric annotations starting from the
 * [RouteLegProgress.geometryIndex] up to an index (in order of importance):
 * 1. the second intersection after on/off-ramp
 * 2. the exit from motorway along the route line
 * 3. the first intersection of upcoming step
 *
 * or up to the index calculated in [calculateMaxSegmentsAheadOfUserToReset] if this index is
 * closer to the user than any of the indexes above.
 *
 * It changes the value of congestion based on the rules in [resetCongestion].
 *
 * To prevent too frequent updates it checks if there is any congestion value more severe than LOW
 * level traffic and the user is not travelling on already overridden segment.
 */
internal class DecreaseTrafficUpdateActionHandler(
    private val congestionRangeGroup: CongestionRangeGroup,
    private val aheadDistanceCalculator: AheadDistanceCalculator =
        PredictedTimeAheadDistanceCalculator(),
) :
    TrafficUpdateActionHandler<TrafficUpdateAction.DecreaseTraffic> {
    override fun handleAction(
        action: TrafficUpdateAction.DecreaseTraffic,
    ): NavigationRoute? {
        val segmentsAheadOfUserToReset = calculateMaxSegmentsAheadOfUserToReset(action)
        val endIndexToReset = segmentsAheadOfUserToReset + action.legProgress.geometryIndex

        val secondIntersectionAfterOnOffRamp = secondIntersectionAfterOnOffRamp(
            action.navigationRoute,
            action.legProgress,
        )
        val exitFromMotorway = exitFromMotorway(action.legProgress)
        val firstIntersectionOfUpcomingStepGeometryIndex =
            firstIntersectionOfUpcomingStepGeometryIndex(action.legProgress)

        val intersectionLimit = secondIntersectionAfterOnOffRamp
            ?: exitFromMotorway
            ?: firstIntersectionOfUpcomingStepGeometryIndex
            ?: Int.MAX_VALUE

        val trafficUpdateLimit = minOf(
            intersectionLimit,
            endIndexToReset,
        )

        // no need to update traffic if upcoming congestion already equals to 0
        val upcomingCongestion = getUpcomingCongestion(
            action.legProgress,
            trafficUpdateLimit,
        ).filterNotNull()

        fun isTrafficOverridden() =
            action.navigationRoute.getOverriddenTrafficForProgress(action.legProgress) != null

        fun isTrafficInUpdateRange() = trafficUpdateLimit > 0 && upcomingCongestion.any {
            it > congestionRangeGroup.low.last
        }

        return if (!isTrafficOverridden() && isTrafficInUpdateRange()) {
            updateTraffic(
                action.navigationRoute,
                action.legProgress,
                expectedCongestion = 0,
                geometryLengthToUpdateTrafficNear = segmentsAheadOfUserToReset,
                geometryLengthToUpdateTrafficFar = 0,
                trafficUpdateLimitIndex = trafficUpdateLimit,
                shouldKeepOriginalTraffic = true,
                transformNearFunction = resetCongestion,
                transformFarFunction = resetCongestion,
            )
        } else {
            logD("DecreaseTraffic") { "Upcoming traffic is low - skipping reducing" }
            null
        }
    }

    private fun calculateMaxSegmentsAheadOfUserToReset(
        action: TrafficUpdateAction.DecreaseTraffic,
    ): Int {
        val (currentSpeed, currentLegProgress) = action

        val distances = currentLegProgress.routeLeg?.annotation()?.distance()
        var distanceIndex = currentLegProgress.geometryIndex

        val thresholdAheadDistanceMeters = aheadDistanceCalculator(currentSpeed)

        // we need to find the index ahead of user with distance to it proximately equals to thresholdDistanceAheadOfUserInMeters
        var accumulationDistance = 0.0
        if (!distances.isNullOrEmpty()) {
            while (accumulationDistance < thresholdAheadDistanceMeters &&
                distanceIndex <= distances.lastIndex
            ) {
                accumulationDistance += distances[distanceIndex]
                distanceIndex += 1
            }
        }

        return distanceIndex - currentLegProgress.geometryIndex
    }

    private fun exitFromMotorway(
        legProgress: RouteLegProgress,
    ): Int? {
        val stepProgress = legProgress.currentStepProgress ?: return null

        val intersectionsAheadOnCurrentStep = stepProgress.step?.intersections()
            ?.filter { legProgress.geometryIndex < (it.geometryIndex() ?: 0) }
            .orEmpty()
        val exitFromMotorway = intersectionsAheadOnCurrentStep
            .firstOrNull { intersection ->
                val lanes = intersection.lanes()

                lanes?.lastOrNull()?.indications()
                    ?.any { it == ManeuverModifier.SLIGHT_RIGHT } == true ||
                    lanes?.firstOrNull()?.indications()
                    ?.any { it == ManeuverModifier.SLIGHT_LEFT } == true
            }
        return exitFromMotorway?.geometryIndex()?.takeIf { legProgress.geometryIndex <= it }
    }

    private fun secondIntersectionAfterOnOffRamp(
        navigationRoute: NavigationRoute,
        legProgress: RouteLegProgress,
    ): Int? {
        val previousStepIndex =
            legProgress.currentStepProgress?.stepIndex?.minus(1)?.takeIf { it >= 0 }
                ?: return null
        val previousStep = navigationRoute.directionsRoute.legs()?.get(legProgress.legIndex)
            ?.steps()?.get(previousStepIndex)
        val previousPrimaryBanner = previousStep?.bannerInstructions()?.lastOrNull()?.primary()
        val previousManeuverType = previousStep?.maneuver()?.type()
        val previousManeuverModifier = previousStep?.maneuver()?.modifier()
        val previousBannerType = previousPrimaryBanner?.type()
        val previousBannerModifier = previousPrimaryBanner?.modifier()

        val isOnRamp =
            (
                previousManeuverType == StepManeuver.FORK &&
                    (
                        previousManeuverModifier == ManeuverModifier.LEFT ||
                            previousManeuverModifier == ManeuverModifier.SLIGHT_LEFT ||
                            previousManeuverModifier == ManeuverModifier.SLIGHT_RIGHT ||
                            previousManeuverModifier == ManeuverModifier.RIGHT
                        )
                ) ||
                (
                    previousBannerType == StepManeuver.FORK &&
                        (
                            previousBannerModifier == ManeuverModifier.LEFT ||
                                previousBannerModifier == ManeuverModifier.SLIGHT_LEFT ||
                                previousBannerModifier == ManeuverModifier.SLIGHT_RIGHT ||
                                previousBannerModifier == ManeuverModifier.RIGHT
                            )
                    ) ||
                previousManeuverType == StepManeuver.OFF_RAMP ||
                previousManeuverType == StepManeuver.ON_RAMP

        val stepIndex = legProgress.currentStepProgress?.stepIndex
            ?: return null

        return navigationRoute.directionsRoute.legs()
            ?.get(legProgress.legIndex)?.steps()?.get(stepIndex)?.intersections()
            ?.getOrNull(1)
            ?.geometryIndex()
            ?.takeIf { isOnRamp && legProgress.geometryIndex < it }
    }

    private fun firstIntersectionOfUpcomingStepGeometryIndex(
        legProgress: RouteLegProgress,
    ): Int? {
        val upcomingStep = legProgress.upcomingStep ?: return null

        return upcomingStep.intersections()?.firstOrNull()
            ?.geometryIndex()
    }

    private val resetCongestion: (Int?, Int) -> Int = { currentValue, _ ->
        when (currentValue) {
            in congestionRangeGroup.severe -> congestionRangeGroup.moderate.last
            else -> 0
        }
    }
}
