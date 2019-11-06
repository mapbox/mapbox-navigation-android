package com.mapbox.navigation.trip.notification

import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.model.route.LegStep
import com.mapbox.navigation.base.model.route.Route
import com.mapbox.navigation.base.model.route.RouteProgress
import java.util.ArrayList

class TestRouteProgressBuilder {

    fun buildDefaultTestRouteProgress(testRoute: Route): RouteProgress {
        return buildTestRouteProgress(testRoute, 100.0, 100.0,
                100.0, 0, 0)
    }

    fun buildTestRouteProgress(
        route: Route,
        stepDistanceRemaining: Double,
        legDistanceRemaining: Double,
        distanceRemaining: Double,
        stepIndex: Int,
        legIndex: Int
    ): RouteProgress {
        val legDurationRemaining = route.legs()!![0].duration()!!
        val steps = route.legs()!![legIndex].steps()
        val currentStep = steps!![stepIndex]
        val currentStepPoints = buildCurrentStepPoints(currentStep)
        val upcomingStepIndex = stepIndex + 1
        var upcomingStepPoints: List<Point> = ArrayList()
        var upcomingStep: LegStep?
        if (upcomingStepIndex < steps.size) {
            upcomingStep = steps[upcomingStepIndex]
            val upcomingStepGeometry = upcomingStep.geometry()
            upcomingStepPoints = buildStepPointsFromGeometry(upcomingStepGeometry)
        }

        return RouteProgress.Builder()
                .stepDistanceRemaining(stepDistanceRemaining)
                .legDistanceRemaining(legDistanceRemaining)
                .legDurationRemaining(legDurationRemaining!!)
                .distanceRemaining(distanceRemaining)
                .route(route)
                .currentStep(currentStep)
                .currentStepPoints(currentStepPoints)
                .upcomingStepPoints(upcomingStepPoints)
                .stepIndex(stepIndex)
                .legIndex(legIndex)
                .inTunnel(false)
                .build()
    }

    private fun buildCurrentStepPoints(currentStep: LegStep): List<Point> {
        val currentStepGeometry = currentStep.geometry()
        return buildStepPointsFromGeometry(currentStepGeometry)
    }

    private fun buildStepPointsFromGeometry(stepGeometry: String?): List<Point> {
        return PolylineUtils.decode(stepGeometry!!, 6)
    }
}
