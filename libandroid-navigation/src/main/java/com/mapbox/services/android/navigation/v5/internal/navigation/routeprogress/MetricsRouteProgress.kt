package com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

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
        if (routeProgress != null) {
            obtainRouteData(routeProgress.directionsRoute())
            obtainLegData(routeProgress.currentLegProgress())
            obtainStepData(routeProgress)
            this.distanceRemaining = routeProgress.distanceRemaining().toInt()
            this.durationRemaining = routeProgress.durationRemaining().toInt()
            this.distanceTraveled = routeProgress.distanceTraveled().toInt()
            this.legIndex = routeProgress.legIndex()
            this.legCount = routeProgress.directionsRoute().legs()?.size ?: 0
            this.stepIndex = routeProgress.currentLegProgress().stepIndex()
            this.stepCount = routeProgress.currentLeg().steps()?.size ?: 0
        } else {
            initDefaultValues()
        }
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
        currentStepDistance = legProgress.currentStep().distance().toInt()
        currentStepDuration = legProgress.currentStep().duration().toInt()
        currentStepDistanceRemaining = legProgress.currentStepProgress().distanceRemaining().toInt()
        currentStepDurationRemaining = legProgress.currentStepProgress().durationRemaining().toInt()
        currentStepName = legProgress.currentStep().name() ?: ""
    }

    private fun obtainStepData(routeProgress: RouteProgress) {
        val legProgress = routeProgress.currentLegProgress()
        legProgress.upComingStep()?.let { upcomingStep ->
            upcomingStepName = upcomingStep.name()
            upcomingStep.maneuver().let {
                upcomingStepInstruction = it.instruction()
                upcomingStepType = it.type()
                upcomingStepModifier = it.modifier()
            }
        }
        legProgress.currentStep().maneuver().let {
            previousStepInstruction = it.instruction()
            previousStepType = it.type()
            previousStepModifier = it.modifier()
        }
        previousStepName = currentStepName
    }

    private fun retrieveRouteDestination(route: DirectionsRoute): Point =
        route.legs()?.lastOrNull()?.steps()?.lastOrNull()?.maneuver()?.location()
            ?: DEFAULT_POINT
}
