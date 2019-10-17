package com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull

class MetricsRouteProgress(routeProgress: RouteProgress?) {

    companion object {
        private val DEFAULT_POINT = Point.fromLngLat(0.0, 0.0)
    }

    var directionsRouteDistance: Int = 0
        private set
    var directionsRouteDuration: Int = 0
        private set
    var directionsRouteProfile: String = ""
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
            routeProgress, routeProgress?.directionsRoute(),
            routeProgress?.currentLegProgress(),
            routeProgress?.distanceRemaining(), routeProgress?.durationRemaining()
        ) { _routeProgress, _directionsRoute, _currentLegProgress, _distanceRemaining, _durationRemaining ->
            obtainRouteData(_directionsRoute)
            obtainLegData(_currentLegProgress)
            obtainStepData(_routeProgress)
            distanceRemaining = _distanceRemaining.toInt()
            durationRemaining = _durationRemaining.toInt()
            distanceTraveled = _routeProgress.distanceTraveled()?.toInt() ?: 0
            legIndex = _routeProgress.legIndex() ?: 0
            legCount = _directionsRoute.legs()?.size ?: 0
            stepIndex = _currentLegProgress.stepIndex() ?: 0
            stepCount = _routeProgress.currentLeg()?.steps()?.size ?: 0
        } ?: initDefaultValues()
    }

    private fun initDefaultValues() {
        directionsRouteProfile = ""
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

    private fun obtainRouteData(route: DirectionsRoute) {
        directionsRouteDistance = route.distance()?.toInt() ?: 0
        directionsRouteDuration = route.duration()?.toInt() ?: 0
        directionsRouteProfile = route.routeOptions()?.profile() ?: ""
        directionsRouteDestination = retrieveRouteDestination(route)
    }

    private fun obtainLegData(legProgress: RouteLegProgress) {
        currentStepDistance = legProgress.currentStep()?.distance()?.toInt() ?: 0
        currentStepDuration = legProgress.currentStep()?.duration()?.toInt() ?: 0
        currentStepDistanceRemaining =
            legProgress.currentStepProgress()?.distanceRemaining()?.toInt() ?: 0
        currentStepDurationRemaining =
            legProgress.currentStepProgress()?.durationRemaining()?.toInt() ?: 0
        currentStepName = legProgress.currentStep()?.name() ?: ""
    }

    private fun obtainStepData(routeProgress: RouteProgress) {
        val legProgress = routeProgress.currentLegProgress()
        legProgress?.upComingStep()?.let { upcomingStep ->
            upcomingStepName = upcomingStep.name()
            upcomingStep.maneuver().let {
                upcomingStepInstruction = it.instruction()
                upcomingStepType = it.type()
                upcomingStepModifier = it.modifier()
            }
        }
        legProgress?.currentStep()?.maneuver().let { stepManeuver ->
            previousStepInstruction = stepManeuver?.instruction()
            previousStepType = stepManeuver?.type()
            previousStepModifier = stepManeuver?.modifier()
        }
        previousStepName = currentStepName
    }

    private fun retrieveRouteDestination(route: DirectionsRoute): Point =
        route.legs()?.lastOrNull()?.steps()?.lastOrNull()?.maneuver()?.location()
            ?: DEFAULT_POINT
}
