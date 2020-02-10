package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.navigator.BannerComponent
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.BannerSection
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.VoiceInstruction
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressStateMap
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull

internal class NavigationRouteProcessor {
    private val ONE_INDEX = 1
    private val ONE_SECOND_IN_MILLISECONDS = 1000.0
    private val FIRST_BANNER_INSTRUCTION = 0

    private val progressStateMap = RouteProgressStateMap()
    private var previousRouteProgress: RouteProgress? = null
    private var previousStatus: NavigationStatus? = null
    private var route: DirectionsRoute? = null
    private var currentLeg: RouteLeg? = null
    private var currentStep: LegStep? = null
    private var currentStepPoints: List<Point>? = null
    private var upcomingStepPoints: List<Point>? = null
    private var currentLegAnnotation: CurrentLegAnnotation? = null
    private var routeGeometryWithBuffer: Geometry? = null

    fun buildNewRouteProgress(
        navigator: MapboxNavigator,
        status: NavigationStatus,
        route: DirectionsRoute
    ): RouteProgress? {
        previousStatus = status
        updateRoute(route, navigator)
        return buildRouteProgressFrom(status, navigator)
    }

    fun updatePreviousRouteProgress(routeProgress: RouteProgress) {
        previousRouteProgress = routeProgress
    }

    fun retrievePreviousRouteProgress(): RouteProgress? {
        return previousRouteProgress
    }

    fun retrievePreviousStatus(): NavigationStatus? {
        return previousStatus
    }

    private fun updateRoute(route: DirectionsRoute, navigator: MapboxNavigator) {
        if (this.route != route) {
            this.route = route
            // routeGeometryWithBuffer = navigator.retrieveRouteGeometryWithBuffer()
        }
    }

    private fun buildRouteProgressFrom(
        status: NavigationStatus,
        navigator: MapboxNavigator
    ): RouteProgress? {
        val legIndex = status.legIndex
        val stepIndex = status.stepIndex
        val upcomingStepIndex = stepIndex + ONE_INDEX

        return ifNonNull(route) { route ->
            updateSteps(route, legIndex, stepIndex)
            updateStepPoints(route, legIndex, stepIndex, upcomingStepIndex)

            val legDistanceRemaining = status.remainingLegDistance.toDouble()
            val routeDistanceRemaining = NavigationHelper.routeDistanceRemaining(
                legDistanceRemaining,
                legIndex, route
            )
            val stepDistanceRemaining = status.remainingStepDistance.toDouble()
            val legDurationRemaining = status.remainingLegDuration / ONE_SECOND_IN_MILLISECONDS

            currentLegAnnotation = ifNonNull(currentLeg) { currentLeg ->
                NavigationHelper.createCurrentAnnotation(
                    currentLegAnnotation,
                    currentLeg, legDistanceRemaining
                )
            }
            val routeState = status.routeState
            val currentRouteState = progressStateMap[routeState]

            val progressBuilder = RouteProgress.Builder()
                .distanceRemaining(routeDistanceRemaining)
                .legDistanceRemaining(legDistanceRemaining)
                .legDurationRemaining(legDurationRemaining)
                .stepDistanceRemaining(stepDistanceRemaining)
                .directionsRoute(route)
                .currentStep(currentStep)
                .currentStepPoints(currentStepPoints)
                .upcomingStepPoints(upcomingStepPoints)
                .stepIndex(stepIndex)
                .legIndex(legIndex)
                .inTunnel(status.inTunnel)
                .currentState(currentRouteState)

            addRouteGeometries(progressBuilder)
            addVoiceInstructions(status, progressBuilder)
            addBannerInstructions(status, navigator, progressBuilder)
            addUpcomingStepPoints(progressBuilder)
            progressBuilder.build()
        }
    }

    private fun updateSteps(route: DirectionsRoute, legIndex: Int, stepIndex: Int) {
        ifNonNull(route.legs()) { legs ->
            if (legIndex < legs.size) {
                currentLeg = legs[legIndex]
            }
            ifNonNull(currentLeg?.steps()) { steps ->
                if (stepIndex < steps.size) {
                    currentStep = steps[stepIndex]
                }
            }
        }
    }

    private fun updateStepPoints(
        route: DirectionsRoute,
        legIndex: Int,
        stepIndex: Int,
        upcomingStepIndex: Int
    ) {
        currentStepPoints = NavigationHelper.decodeStepPoints(
            route, currentStepPoints,
            legIndex, stepIndex
        )
        upcomingStepPoints = NavigationHelper.decodeStepPoints(
            route, null,
            legIndex, upcomingStepIndex
        )
    }

    private fun addUpcomingStepPoints(progressBuilder: RouteProgress.Builder) {
        ifNonNull(upcomingStepPoints) { upcomingStepPoints ->
            if (upcomingStepPoints.isNotEmpty())
                progressBuilder.upcomingStepPoints(upcomingStepPoints)
        }
    }

    private fun addRouteGeometries(progressBuilder: RouteProgress.Builder) {
        progressBuilder.routeGeometryWithBuffer(routeGeometryWithBuffer)
    }

    private fun addVoiceInstructions(
        status: NavigationStatus,
        progressBuilder: RouteProgress.Builder
    ) {
        val voiceInstruction = status.voiceInstruction
        progressBuilder.voiceInstruction(voiceInstruction?.mapToDirectionsApi())
    }

    private fun addBannerInstructions(
        status: NavigationStatus,
        navigator: MapboxNavigator,
        progressBuilder: RouteProgress.Builder
    ) {
        var bannerInstruction = status.bannerInstruction
        // if (status.routeState == RouteState.INITIALIZED) {
        //     bannerInstruction = navigator.retrieveBannerInstruction(FIRST_BANNER_INSTRUCTION)
        // }
        currentStep?.let {
            progressBuilder.bannerInstruction(bannerInstruction?.mapToDirectionsApi(it))
        }
    }

    private fun BannerInstruction.mapToDirectionsApi(currentStep: LegStep): BannerInstructions {
        return BannerInstructions.builder()
            .distanceAlongGeometry(this.remainingStepDistance.toDouble())
            .primary(this.primary.mapToDirectionsApi())
            .secondary(this.secondary?.mapToDirectionsApi())
            .sub(this.sub?.mapToDirectionsApi())
            .view(currentStep.bannerInstructions()?.get(this.index)?.view())
            .build()
    }

    private fun BannerSection.mapToDirectionsApi(): BannerText {
        return BannerText.builder()
            .components(this.components?.mapToDirectionsApi())
            .degrees(this.degrees?.toDouble())
            .drivingSide(this.drivingSide)
            .modifier(this.modifier)
            .text(this.text)
            .type(this.type)
            .build()
    }

    private fun MutableList<BannerComponent>.mapToDirectionsApi(): MutableList<BannerComponents>? {
        val components = mutableListOf<BannerComponents>()
        this.forEach {
            components.add(
                BannerComponents.builder()
                    .abbreviation(it.abbr)
                    .abbreviationPriority(it.abbrPriority)
                    .active(it.active)
                    .directions(it.directions)
                    .imageBaseUrl(it.imageBaseurl)
                    .text(it.text)
                    .type(it.type)
                    .build()
            )
        }
        return components
    }

    private fun VoiceInstruction.mapToDirectionsApi(): VoiceInstructions? {
        return VoiceInstructions.builder()
            .announcement(this.announcement)
            .distanceAlongGeometry(this.remainingStepDistance.toDouble())
            .ssmlAnnouncement(this.ssmlAnnouncement)
            .build()
    }
}
