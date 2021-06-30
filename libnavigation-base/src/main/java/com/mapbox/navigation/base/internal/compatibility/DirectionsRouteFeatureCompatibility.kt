package com.mapbox.navigation.base.internal.compatibility

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger

fun List<DirectionsRoute>.verifyCompatibility(
    logger: Logger,
    testersConfiguration: RouteFeatureCompatibilityTesters.() -> Unit
) {
    this.forEach {
        it.verifyCompatibility(logger, testersConfiguration)
    }
}

fun DirectionsRoute.verifyCompatibility(
    logger: Logger,
    testersConfiguration: RouteFeatureCompatibilityTesters.() -> Unit
) {
    this.verifyCompatibility(
        logger = logger,
        testers = RouteFeatureCompatibilityTesters(),
        testersConfiguration = testersConfiguration
    )
}

class RouteFeatureCompatibilityTesters : FeatureCompatibilityTesters<DirectionsRoute>() {
    val routeOptionsAndGeometry = FeatureCompatibilityTester<DirectionsRoute>(
        initialMessage = "Provided route is not compatible with the Navigation SDK.",
        severity = FeatureCompatibilityTester.Severity.Exception,
        compatibilityChecks = listOf(
            CompatibilityCheck("Route is missing route options") { route ->
                route.routeOptions() != null
            },
            CompatibilityCheck("Route is missing geometry.") { route ->
                !route.geometry().isNullOrBlank()
            },
            CompatibilityCheck("Route options require overview=full.") { route ->
                route.routeOptions()!!.overview() == DirectionsCriteria.OVERVIEW_FULL
            }
        )
    )

    val preciseEta = FeatureCompatibilityTester<DirectionsRoute>(
        initialMessage = "Provided route might not offer the best ETA precision.",
        severity = FeatureCompatibilityTester.Severity.Warning,
        compatibilityChecks = listOf(
            CompatibilityCheck("Route request didn't ask for duration annotations.") {
                it.routeOptions()!!.annotationsList()
                    ?.contains(DirectionsCriteria.ANNOTATION_DURATION) == true
            },
            CompatibilityCheck("Route is missing duration annotations.") { route ->
                route.legs()!!.all { it.annotation()!!.duration() != null }
            }
        )
    )

    val trafficRendering = FeatureCompatibilityTester<DirectionsRoute>(
        initialMessage = "Provided route doesn't support traffic rendering.",
        severity = FeatureCompatibilityTester.Severity.Error,
        compatibilityChecks = listOf(
            CompatibilityCheck("Route request didn't specify driving-traffic profile.") {
                it.routeOptions()!!.profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            },
            CompatibilityCheck("Route request didn't ask for distance annotations.") {
                it.routeOptions()!!.annotationsList()
                    ?.contains(DirectionsCriteria.ANNOTATION_DISTANCE) == true
            },
            CompatibilityCheck("Route is missing distance annotations.") { route ->
                route.legs()!!.all { it.annotation()!!.distance() != null }
            },
            CompatibilityCheck("Route request didn't ask for congestion annotations.") {
                it.routeOptions()!!.annotationsList()
                    ?.contains(DirectionsCriteria.ANNOTATION_CONGESTION) == true
            },
            CompatibilityCheck("Route is missing congestion annotations.") { route ->
                route.legs()!!.all { it.annotation()!!.congestion() != null }
            }
        )
    )

    override fun getAllTesters(): List<FeatureCompatibilityTester<DirectionsRoute>> = listOf(
        routeOptionsAndGeometry,
        preciseEta,
        trafficRendering
    )
}
