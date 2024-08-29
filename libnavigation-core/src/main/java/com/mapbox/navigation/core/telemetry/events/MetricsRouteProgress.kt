package com.mapbox.navigation.core.telemetry.events

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.telemetry.obtainRouteDestination
import com.mapbox.navigation.core.telemetry.obtainStepCount
import com.mapbox.navigation.utils.internal.ifNonNull

internal class MetricsRouteProgress(routeProgress: RouteProgress?) {

    private companion object {
        private const val DEFAULT_PROFILE = "driving"
        private val DEFAULT_POINT = Point.fromLngLat(0.0, 0.0)
    }

    var directionsRouteGeometry: String? = null
        private set
    var directionsRouteRequestIdentifier: String? = null
        private set
    var directionsRouteStepCount: Int = 0
        private set
    var directionsRouteIndex: Int = 0
        private set
    var directionsRouteDistance: Int = 0
        private set
    var directionsRouteDuration: Int = 0
        private set
    var directionsRouteProfile: String = DEFAULT_PROFILE
        private set
    var directionsRouteDestination: Point = DEFAULT_POINT
        private set
    var distanceRemaining: Int = 0
        private set
    var durationRemaining: Int = 0
        private set
    var distanceTraveled: Int = 0
        private set

    var currentStepDistance: Int = 0
        private set
    var currentStepDuration: Int = 0
        private set
    var currentStepDistanceRemaining: Int = 0
        private set
    var currentStepDurationRemaining: Int = 0
        private set
    private var currentStepName: String = ""

    var upcomingStepInstruction: String? = null
        private set
    var upcomingStepModifier: String? = null
        private set
    var upcomingStepType: String? = null
        private set
    var upcomingStepName: String? = null
        private set

    var previousStepInstruction: String? = null
        private set
    var previousStepModifier: String? = null
        private set
    var previousStepType: String? = null
        private set
    var previousStepName: String = ""
        private set

    var legIndex: Int = 0
        private set
    var legCount: Int = 0
        private set
    var stepIndex: Int = 0
        private set
    var stepCount: Int = 0
        private set

    init {
        ifNonNull(
            routeProgress,
            routeProgress?.navigationRoute,
            routeProgress?.currentLegProgress,
            routeProgress?.distanceRemaining,
            routeProgress?.durationRemaining,
        ) { _routeProgress, _navigationRoute, _currentLegProgress,
            _distanceRemaining, _durationRemaining, ->
            obtainRouteData(_navigationRoute)
            obtainLegData(_currentLegProgress)
            obtainStepData(_routeProgress)
            distanceRemaining = _distanceRemaining.toInt()
            durationRemaining = _durationRemaining.toInt()
            distanceTraveled = _routeProgress.distanceTraveled.toInt()
            legIndex = _routeProgress.currentLegProgress?.legIndex ?: 0
            legCount = _navigationRoute.directionsRoute.legs()?.size ?: 0
            stepIndex = _currentLegProgress.currentStepProgress?.stepIndex ?: 0
            stepCount = _routeProgress.currentLegProgress?.routeLeg?.steps()?.size ?: 0
        } ?: initDefaultValues()
    }

    private fun initDefaultValues() {
        directionsRouteProfile = DEFAULT_PROFILE
        directionsRouteDestination = DEFAULT_POINT
        currentStepName = ""
        upcomingStepInstruction = ""
        upcomingStepModifier = ""
        upcomingStepType = ""
        upcomingStepName = ""
        previousStepInstruction = ""
        previousStepModifier = ""
        previousStepType = ""
        previousStepName = ""
    }

    private fun obtainRouteData(navigationRoute: NavigationRoute) {
        val route = navigationRoute.directionsRoute
        directionsRouteGeometry = route.geometry()
        directionsRouteRequestIdentifier = navigationRoute.responseUUID
        directionsRouteStepCount = obtainStepCount(route)
        directionsRouteIndex = navigationRoute.routeIndex
        directionsRouteDistance = route.distance().toInt()
        directionsRouteDuration = route.duration().toInt()
        directionsRouteProfile = navigationRoute.routeOptions.profile()
        directionsRouteDestination = obtainRouteDestination(route)
    }

    private fun obtainLegData(legProgress: RouteLegProgress) {
        currentStepDistance = legProgress.currentStepProgress?.step?.distance()?.toInt() ?: 0
        currentStepDuration = legProgress.currentStepProgress?.step?.duration()?.toInt() ?: 0
        currentStepDistanceRemaining =
            legProgress.currentStepProgress?.distanceRemaining?.toInt() ?: 0
        currentStepDurationRemaining =
            legProgress.currentStepProgress?.durationRemaining?.toInt() ?: 0
        currentStepName = legProgress.currentStepProgress?.step?.name() ?: ""
    }

    private fun obtainStepData(routeProgress: RouteProgress) {
        val legProgress = routeProgress.currentLegProgress
        legProgress?.upcomingStep?.let { upcomingStep ->
            upcomingStepName = upcomingStep.name()
            upcomingStep.maneuver().let {
                upcomingStepInstruction = it.instruction()
                upcomingStepType = it.type()
                upcomingStepModifier = it.modifier()
            }
        }
        legProgress?.currentStepProgress?.step?.maneuver().let { stepManeuver ->
            previousStepInstruction = stepManeuver?.instruction()
            previousStepType = stepManeuver?.type()
            previousStepModifier = stepManeuver?.modifier()
        }
        previousStepName = currentStepName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MetricsRouteProgress

        if (directionsRouteDistance != other.directionsRouteDistance) return false
        if (directionsRouteDuration != other.directionsRouteDuration) return false
        if (directionsRouteProfile != other.directionsRouteProfile) return false
        if (directionsRouteDestination != other.directionsRouteDestination) return false
        if (distanceRemaining != other.distanceRemaining) return false
        if (durationRemaining != other.durationRemaining) return false
        if (distanceTraveled != other.distanceTraveled) return false
        if (currentStepDistance != other.currentStepDistance) return false
        if (currentStepDuration != other.currentStepDuration) return false
        if (currentStepDistanceRemaining != other.currentStepDistanceRemaining) return false
        if (currentStepDurationRemaining != other.currentStepDurationRemaining) return false
        if (currentStepName != other.currentStepName) return false
        if (upcomingStepInstruction != other.upcomingStepInstruction) return false
        if (upcomingStepModifier != other.upcomingStepModifier) return false
        if (upcomingStepType != other.upcomingStepType) return false
        if (upcomingStepName != other.upcomingStepName) return false
        if (previousStepInstruction != other.previousStepInstruction) return false
        if (previousStepModifier != other.previousStepModifier) return false
        if (previousStepType != other.previousStepType) return false
        if (previousStepName != other.previousStepName) return false
        if (legIndex != other.legIndex) return false
        if (legCount != other.legCount) return false
        if (stepIndex != other.stepIndex) return false
        if (stepCount != other.stepCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = directionsRouteDistance
        result = 31 * result + directionsRouteDuration
        result = 31 * result + directionsRouteProfile.hashCode()
        result = 31 * result + directionsRouteDestination.hashCode()
        result = 31 * result + distanceRemaining
        result = 31 * result + durationRemaining
        result = 31 * result + distanceTraveled
        result = 31 * result + currentStepDistance
        result = 31 * result + currentStepDuration
        result = 31 * result + currentStepDistanceRemaining
        result = 31 * result + currentStepDurationRemaining
        result = 31 * result + currentStepName.hashCode()
        result = 31 * result + upcomingStepInstruction.hashCode()
        result = 31 * result + upcomingStepModifier.hashCode()
        result = 31 * result + upcomingStepType.hashCode()
        result = 31 * result + upcomingStepName.hashCode()
        result = 31 * result + previousStepInstruction.hashCode()
        result = 31 * result + previousStepModifier.hashCode()
        result = 31 * result + previousStepType.hashCode()
        result = 31 * result + previousStepName.hashCode()
        result = 31 * result + legIndex
        result = 31 * result + legCount
        result = 31 * result + stepIndex
        result = 31 * result + stepCount
        return result
    }

    override fun toString(): String {
        return "MetricsRouteProgress(" +
            "directionsRouteDistance=$directionsRouteDistance, " +
            "directionsRouteDuration=$directionsRouteDuration, " +
            "directionsRouteProfile='$directionsRouteProfile', " +
            "directionsRouteDestination=$directionsRouteDestination, " +
            "distanceRemaining=$distanceRemaining, " +
            "durationRemaining=$durationRemaining, " +
            "distanceTraveled=$distanceTraveled, " +
            "currentStepDistance=$currentStepDistance, " +
            "currentStepDuration=$currentStepDuration, " +
            "currentStepDistanceRemaining=$currentStepDistanceRemaining, " +
            "currentStepDurationRemaining=$currentStepDurationRemaining, " +
            "currentStepName='$currentStepName', " +
            "upcomingStepInstruction=$upcomingStepInstruction, " +
            "upcomingStepModifier=$upcomingStepModifier, " +
            "upcomingStepType=$upcomingStepType, " +
            "upcomingStepName=$upcomingStepName, " +
            "previousStepInstruction=$previousStepInstruction, " +
            "previousStepModifier=$previousStepModifier, " +
            "previousStepType=$previousStepType, " +
            "previousStepName='$previousStepName', " +
            "legIndex=$legIndex, " +
            "legCount=$legCount, " +
            "stepIndex=$stepIndex, " +
            "stepCount=$stepCount" +
            ")"
    }
}
