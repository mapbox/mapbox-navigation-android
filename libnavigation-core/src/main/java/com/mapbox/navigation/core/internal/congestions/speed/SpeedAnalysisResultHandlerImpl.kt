package com.mapbox.navigation.core.internal.congestions.speed

import com.mapbox.api.directions.v5.models.SpeedLimit
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.internal.congestions.getOverriddenTrafficForProgress
import com.mapbox.navigation.core.internal.congestions.model.MetersPerSecond
import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.internal.congestions.model.toMetersPerSecond
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.utils.internal.logD

/**
 * Provides the result of analysis of the current speed:
 *
 * - if the user is driving at speed above [highSpeedThreshold] it produces
 * [SpeedAnalysisResult.HighSpeedDetected]
 * - in case the user drops their speed below restoreCongestionSpeed and the current primary route
 * has non null [NavigationRoute.overriddenTraffic] this handler considers the previous
 * [SpeedAnalysisResult.HighSpeedDetected] as wrong prediction and returns
 * [SpeedAnalysisResult.WrongFalsePositiveOverrideDetected]
 * - driving on motorway at the speed below 50% of the max speed triggers additional checks in
 * [lowSpeedAnalyzer] and produces [SpeedAnalysisResult.LowSpeedDetected] result if additional
 * requirements are met
 */
internal class SpeedAnalysisResultHandlerImpl(
    private val highSpeedThresholdInKmPerHour: Int,
    private val lowSpeedAnalyzer: SpeedAnalyzer = LowSpeedAnalyzer(),
) : SpeedAnalysisResultHandler {
    override fun invoke(
        routeProgress: RouteProgress,
        location: LocationMatcherResult,
    ): SpeedAnalysisResult {
        if (routeProgress.currentState != RouteProgressState.TRACKING) {
            return SpeedAnalysisResult.SkippedAnalysis(
                "current state ${routeProgress.currentState} isn't TRACKING",
            )
        }

        val currentSpeed = location.enhancedLocation.speed?.toMetersPerSecond()
            ?: return SpeedAnalysisResult.FailedToAnalyze("unknown current speed")

        val currentLegProgress = routeProgress.currentLegProgress
            ?: return SpeedAnalysisResult.FailedToAnalyze("no leg progress")

        val highSpeedThreshold =
            MetersPerSecond.fromKilometersPerHour(highSpeedThresholdInKmPerHour.toFloat())

        if (currentSpeed > highSpeedThreshold) {
            return SpeedAnalysisResult.HighSpeedDetected(
                currentSpeed,
                currentLegProgress,
                routeProgress.navigationRoute,
            )
        }

        val expectedSpeed = getExpectedSpeed(routeProgress)

        val overriddenTraffic =
            routeProgress.navigationRoute.getOverriddenTrafficForProgress(currentLegProgress)
        val restoreCongestionSpeed =
            (expectedSpeed ?: highSpeedThreshold) * RESTORE_CONGESTION_SPEED_FRACTION

        if (overriddenTraffic != null && currentSpeed < restoreCongestionSpeed) {
            return SpeedAnalysisResult.WrongFalsePositiveOverrideDetected(
                routeProgress.navigationRoute,
                overriddenTraffic,
            )
        }

        if (expectedSpeed == null) {
            return SpeedAnalysisResult.FailedToAnalyze("unknown expected speed")
        }
        val lowSpeed = expectedSpeed * LOW_SPEED_FRACTION
        return when {
            isOnMotorway(routeProgress) &&
                currentSpeed < lowSpeed ->
                lowSpeedAnalyzer(
                    currentLegProgress,
                    routeProgress.navigationRoute,
                    currentSpeed,
                    expectedSpeed,
                )

            else -> SpeedAnalysisResult.SpeedIsOk(currentSpeed, expectedSpeed)
        }
    }

    private fun isOnMotorway(routeProgress: RouteProgress): Boolean {
        val stepProgress = routeProgress.currentLegProgress?.currentStepProgress ?: return false
        val roadDefiningIntersection = stepProgress.step?.intersections()
            ?.getOrNull(stepProgress.intersectionIndex)
            ?: return false
        logD(TAG) {
            "current road class ${roadDefiningIntersection.mapboxStreetsV8()?.roadClass()}"
        }
        return roadDefiningIntersection.mapboxStreetsV8()?.roadClass() == MOTORWAY_STREET_TYPE
    }

    private fun getExpectedSpeed(routeProgress: RouteProgress): MetersPerSecond? {
        val unknownValue: MetersPerSecond? = null
        val legProgress = routeProgress.currentLegProgress ?: return unknownValue
        val annotations = legProgress.routeLeg?.annotation() ?: return unknownValue
        val result = annotations.freeflowSpeed()?.getOrNull(legProgress.geometryIndex)?.let {
            MetersPerSecond.fromKilometersPerHour(it.toFloat())
        }
            ?: getMaxSpeed(routeProgress)
        return result
    }

    private fun getMaxSpeed(routeProgress: RouteProgress): MetersPerSecond? {
        val unknownValue: MetersPerSecond? = null
        val legProgress = routeProgress.currentLegProgress ?: return unknownValue
        val annotations = legProgress.routeLeg?.annotation() ?: return unknownValue

        return annotations.maxspeed()?.get(legProgress.geometryIndex)?.let { speed ->
            when (speed.unit()) {
                SpeedLimit.KMPH -> speed.speed()?.let {
                    MetersPerSecond.fromKilometersPerHour(it.toFloat())
                }

                SpeedLimit.MPH -> speed.speed()?.let {
                    MetersPerSecond.fromMilesPerHour(it.toFloat())
                }

                else -> null
            }
        }
    }

    private companion object {
        private const val TAG = "TrafficOverride"
        private const val MOTORWAY_STREET_TYPE = "motorway"
        private const val LOW_SPEED_FRACTION = 0.5f
        private const val RESTORE_CONGESTION_SPEED_FRACTION = 0.7f
    }
}
