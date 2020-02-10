// package com.mapbox.navigation.core.extensions
//
// import com.mapbox.api.directions.v5.models.BannerComponents
// import com.mapbox.api.directions.v5.models.BannerInstructions
// import com.mapbox.api.directions.v5.models.BannerText
// import com.mapbox.api.directions.v5.models.LegStep
// import com.mapbox.api.directions.v5.models.RouteLeg
// import com.mapbox.api.directions.v5.models.VoiceInstructions
// import com.mapbox.geojson.utils.PolylineUtils
// import com.mapbox.navigation.base.extensions.ifNonNull
// import com.mapbox.navigation.base.trip.model.RouteLegProgress
// import com.mapbox.navigation.base.trip.model.RouteProgress
// import com.mapbox.navigation.base.trip.model.RouteProgressState
// import com.mapbox.navigation.base.trip.model.RouteStepProgress
// import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
// import com.mapbox.navigator.BannerComponent
// import com.mapbox.navigator.BannerInstruction
// import com.mapbox.navigator.BannerSection
// import com.mapbox.navigator.NavigationStatus
// import com.mapbox.navigator.RouteState
// import com.mapbox.navigator.VoiceInstruction
// import kotlin.math.roundToLong
//
// private const val ONE_INDEX = 1
// private const val ONE_SECOND_IN_MILLISECONDS = 1000.0
// private const val FIRST_BANNER_INSTRUCTION = 0
// private const val TWO_LEGS: Short = 2
//
// fun NavigationStatus.getRouteProgress(): RouteProgress {
//     val upcomingStepIndex = stepIndex + ONE_INDEX
//
//     val routeProgressBuilder = RouteProgress.Builder()
//     val legProgressBuilder = RouteLegProgress.Builder()
//     val stepProgressBuilder = RouteStepProgress.Builder()
//
//     ifNonNull(MapboxNativeNavigatorImpl.route?.legs()) { legs ->
//         var currentLeg: RouteLeg? = null
//         if (legIndex < legs.size) {
//             currentLeg = legs[legIndex]
//             legProgressBuilder.legIndex(legIndex)
//             legProgressBuilder.routeLeg(currentLeg)
//
//             // todo mapbox java issue - leg distance is nullable
//             val distanceTraveled =
//                 (currentLeg.distance()?.toFloat() ?: 0f) - remainingLegDistance
//             legProgressBuilder.distanceTraveled(distanceTraveled)
//             legProgressBuilder.fractionTraveled(
//                 distanceTraveled / (currentLeg.distance()?.toFloat() ?: 0f)
//             )
//
//             var routeDistanceRemaining = remainingLegDistance
//             var routeDurationRemaining = remainingLegDuration
//             if (legs.size >= TWO_LEGS) {
//                 for (i in legIndex + ONE_INDEX until legs.size) {
//                     routeDistanceRemaining += legs[i].distance()?.toFloat() ?: 0f
//                     routeDurationRemaining += legs[i].duration()?.toLong() ?: 0L
//                 }
//             }
//             routeProgressBuilder.distanceRemaining(routeDistanceRemaining)
//             routeProgressBuilder.durationRemaining(routeDurationRemaining)
//
//             var routeDistance = 0f
//             for (leg in legs) {
//                 routeDistance += leg.distance()?.toFloat() ?: 0f
//             }
//             val routeDistanceTraveled = routeDistance - routeDistanceRemaining
//             routeProgressBuilder.distanceTraveled(routeDistanceTraveled)
//             routeProgressBuilder.fractionTraveled(routeDistanceTraveled / routeDistance)
//
//             routeProgressBuilder.remainingWaypoints(legs.size - (legIndex + 1))
//         }
//
//         ifNonNull(currentLeg?.steps()) { steps ->
//             val currentStep: LegStep?
//             if (stepIndex < steps.size) {
//                 currentStep = steps[stepIndex]
//                 stepProgressBuilder.stepIndex(stepIndex)
//                 stepProgressBuilder.step(currentStep)
//
//                 currentStep?.distance()
//                 val stepGeometry = currentStep.geometry()
//                 stepGeometry?.let {
//                     stepProgressBuilder.stepPoints(
//                         PolylineUtils.decode(
//                             stepGeometry, /* todo add core dependency PRECISION_6*/
//                             6
//                         )
//                     )
//                 }
//
//                 val distanceTraveled =
//                     currentStep.distance().toFloat() - remainingStepDistance
//                 stepProgressBuilder.distanceTraveled(distanceTraveled)
//                 stepProgressBuilder.fractionTraveled(distanceTraveled / currentStep.distance().toFloat())
//
//                 routeState.convertState()?.also {
//                     routeProgressBuilder.currentState(it)
//
//                     val bannerInstructions = bannerInstruction?.mapToDirectionsApi(currentStep)
//                     routeProgressBuilder.bannerInstructions(bannerInstructions)
//                 }
//             }
//
//             if (upcomingStepIndex < steps.size) {
//                 val upcomingStep = steps[upcomingStepIndex]
//                 legProgressBuilder.upcomingStep(upcomingStep)
//
//                 val stepGeometry = upcomingStep.geometry()
//                 stepGeometry?.let {
//                     routeProgressBuilder.upcomingStepPoints(
//                         PolylineUtils.decode(
//                             stepGeometry, /* todo add core dependency PRECISION_6*/
//                             6
//                         )
//                     )
//                 }
//             }
//         }
//     }
//
//     stepProgressBuilder.distanceRemaining(remainingStepDistance)
//     stepProgressBuilder.durationRemaining((remainingStepDuration / ONE_SECOND_IN_MILLISECONDS).roundToLong())
//
//     legProgressBuilder.currentStepProgress(stepProgressBuilder.build())
//     legProgressBuilder.distanceRemaining(remainingLegDistance)
//     legProgressBuilder.durationRemaining((remainingLegDuration / ONE_SECOND_IN_MILLISECONDS).roundToLong())
//
//     routeProgressBuilder.currentLegProgress(legProgressBuilder.build())
//
//     routeProgressBuilder.inTunnel(inTunnel)
//     routeProgressBuilder.routeGeometryWithBuffer(MapboxNativeNavigatorImpl.routeBufferGeoJson)
//
//     routeProgressBuilder.voiceInstructions(voiceInstruction?.mapToDirectionsApi())
//
//     ifNonNull(MapboxNativeNavigatorImpl.route) {
//         routeProgressBuilder.route(it)
//     }
//
//     return routeProgressBuilder.build()
// }
//
// private fun BannerInstruction.mapToDirectionsApi(currentStep: LegStep): BannerInstructions {
//     return BannerInstructions.builder()
//         .distanceAlongGeometry(this.remainingStepDistance.toDouble())
//         .primary(this.primary.mapToDirectionsApi())
//         .secondary(this.secondary?.mapToDirectionsApi())
//         .sub(this.sub?.mapToDirectionsApi())
//         .view(currentStep.bannerInstructions()?.get(this.index)?.view())
//         .build()
// }
//
// private fun BannerSection.mapToDirectionsApi(): BannerText {
//     return BannerText.builder()
//         .components(this.components?.mapToDirectionsApi())
//         .degrees(this.degrees?.toDouble())
//         .drivingSide(this.drivingSide)
//         .modifier(this.modifier)
//         .text(this.text)
//         .type(this.type)
//         .build()
// }
//
// private fun MutableList<BannerComponent>.mapToDirectionsApi(): MutableList<BannerComponents>? {
//     val components = mutableListOf<BannerComponents>()
//     this.forEach {
//         components.add(
//             BannerComponents.builder()
//                 .abbreviation(it.abbr)
//                 .abbreviationPriority(it.abbrPriority)
//                 .active(it.active)
//                 .directions(it.directions)
//                 .imageBaseUrl(it.imageBaseurl)
//                 .text(it.text)
//                 .type(it.type)
//                 .build()
//         )
//     }
//     return components
// }
//
// private fun VoiceInstruction.mapToDirectionsApi(): VoiceInstructions? {
//     return VoiceInstructions.builder()
//         .announcement(this.announcement)
//         .distanceAlongGeometry(this.remainingStepDistance.toDouble())
//         .ssmlAnnouncement(this.ssmlAnnouncement)
//         .build()
// }
//
// private fun RouteState.convertState(): RouteProgressState? {
//     return when (this) {
//         RouteState.INVALID -> RouteProgressState.ROUTE_INVALID
//         RouteState.INITIALIZED -> RouteProgressState.ROUTE_INITIALIZED
//         RouteState.TRACKING -> RouteProgressState.LOCATION_TRACKING
//         RouteState.COMPLETE -> RouteProgressState.ROUTE_ARRIVED
//         RouteState.OFFROUTE -> null // send in a callback instead
//         RouteState.STALE -> RouteProgressState.LOCATION_STALE
//     }
// }