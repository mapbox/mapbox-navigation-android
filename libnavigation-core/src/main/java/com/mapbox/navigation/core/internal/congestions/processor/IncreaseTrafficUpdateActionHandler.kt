package com.mapbox.navigation.core.internal.congestions.processor

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.core.internal.congestions.model.CongestionRangeGroup
import com.mapbox.navigation.core.internal.congestions.model.CongestionSeverityType
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction
import com.mapbox.navigation.core.internal.congestions.speed.updateTraffic
import com.mapbox.navigation.utils.internal.logD

/**
 * Updates the primary route congestion numeric annotations for 12 segments starting from the
 * [RouteLegProgress.geometryIndex]. In case there is an exit from motorway within these segments
 * it changes data up to the index of this exit.
 *
 * We have 2 different adjustments for the upcoming segments:
 *  - [smoothifyCongestion] applies for the first 8 segments and overrides congestion with
 *  [TrafficUpdateAction.IncreaseTraffic.expectedCongestion] if it's more severe than directions
 *  response
 *  - [adjustCongestionFarAhead] applies for the next 4 segments making congestion 1 level
 *  less severe than the nearest segment
 */
internal class IncreaseTrafficUpdateActionHandler(
    private val congestionRangeGroup: CongestionRangeGroup,
) :
    TrafficUpdateActionHandler<TrafficUpdateAction.IncreaseTraffic> {
    override fun handleAction(
        action: TrafficUpdateAction.IncreaseTraffic,
    ): NavigationRoute {
        val trafficUpdateLimit = exitFromMotorwayGeometryIndex(action.legProgress)

        return updateTraffic(
            action.route,
            action.legProgress,
            action.expectedCongestion,
            GEOMETRY_LENGTH_TO_UPDATE_TRAFFIC_NEAR,
            GEOMETRY_LENGTH_TO_UPDATE_TRAFFIC_FAR,
            trafficUpdateLimit,
            shouldKeepOriginalTraffic = false,
            ::smoothifyCongestion,
            ::adjustCongestionFarAhead,
        )
    }

    private fun smoothifyCongestion(value: Int?, expectedValue: Int): Int {
        val currentValue = value ?: 0
        val currentSeverity = congestionRangeGroup.fromCongestionValue(currentValue)
        val expectedSeverity = congestionRangeGroup.fromCongestionValue(expectedValue)
        val result = when {
            currentSeverity == expectedSeverity -> expectedValue
            expectedSeverity < currentSeverity -> currentValue
            expectedSeverity > currentSeverity -> minOf(
                expectedValue,
                congestionRangeGroup.fromCongestionSeverityType(
                    // looking for the next possible congestion range group value
                    CongestionSeverityType.fromWeightValue(
                        currentSeverity.weight + 1,
                    ),
                ).first,
            )

            else -> error("this can't happen as the branch above is always true")
        }
        logD("IncreaseTraffic") {
            "near congestion: $result " +
                "because expected is $expectedValue and current is $currentValue"
        }
        return result
    }

    private fun adjustCongestionFarAhead(value: Int?, nearCongestion: Int): Int {
        val currentValue = value ?: 0
        val currentSeverity = congestionRangeGroup.fromCongestionValue(currentValue)
        val expectedSeverity = CongestionSeverityType.fromWeightValue(
            congestionRangeGroup.fromCongestionValue(nearCongestion).weight - 1,
        )
        val expectedSeverityRange =
            congestionRangeGroup.fromCongestionSeverityType(expectedSeverity)
        val result = when {
            currentSeverity == expectedSeverity ->
                expectedSeverityRange.first()

            expectedSeverity < currentSeverity -> currentValue
            expectedSeverity > currentSeverity -> minOf(
                nearCongestion,
                congestionRangeGroup.fromCongestionSeverityType(
                    CongestionSeverityType.fromWeightValue(
                        currentSeverity.weight + 1,
                    ),
                ).first,
            )

            else -> error("this can't happen as the branch above is always true")
        }
        logD("IncreaseTraffic") {
            "far congestion: $result because expected for near is $nearCongestion " +
                "and current is $currentValue"
        }
        return result
    }

    private fun exitFromMotorwayGeometryIndex(
        legProgress: RouteLegProgress,
    ): Int? {
        val stepProgress = legProgress.currentStepProgress ?: return null
        val intersectionsAheadOnCurrentStep = stepProgress.step?.intersections()
            ?.drop(stepProgress.intersectionIndex)
            .orEmpty()
        val intersectionsFromStepsAhead = legProgress.routeLeg?.steps()
            ?.drop(stepProgress.stepIndex + 1)
            ?.flatMap { it.intersections().orEmpty() }
            .orEmpty()

        val intersectionsAhead = intersectionsAheadOnCurrentStep + intersectionsFromStepsAhead
        val exitFromMotorway = intersectionsAhead
            .firstOrNull { it.mapboxStreetsV8()?.roadClass() != "motorway" }
        return exitFromMotorway?.geometryIndex()
    }

    companion object {
        private const val GEOMETRY_LENGTH_TO_UPDATE_TRAFFIC_NEAR = 8
        private const val GEOMETRY_LENGTH_TO_UPDATE_TRAFFIC_FAR = 4
    }
}
