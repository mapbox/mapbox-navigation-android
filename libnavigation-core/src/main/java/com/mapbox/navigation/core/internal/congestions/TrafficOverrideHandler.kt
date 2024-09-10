package com.mapbox.navigation.core.internal.congestions

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.common.location.Location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.TrafficOverrideOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.directions.session.RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.congestions.model.CongestionRangeGroup
import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction
import com.mapbox.navigation.core.internal.congestions.processor.CommonTrafficUpdateActionHandler
import com.mapbox.navigation.core.internal.congestions.processor.TrafficUpdateActionHandler
import com.mapbox.navigation.core.internal.congestions.scanner.HighSpeedDetectedTrafficUpdateActionScanner
import com.mapbox.navigation.core.internal.congestions.scanner.LowSpeedDetectedTrafficUpdateActionScanner
import com.mapbox.navigation.core.internal.congestions.scanner.NoActionTrafficUpdateActionScanner
import com.mapbox.navigation.core.internal.congestions.scanner.TrafficUpdateActionScanner
import com.mapbox.navigation.core.internal.congestions.scanner.TrafficUpdateActionScannerChain
import com.mapbox.navigation.core.internal.congestions.scanner.WrongFalsePositiveTrafficUpdateActionScanner
import com.mapbox.navigation.core.internal.congestions.speed.SpeedAnalysisResultHandler
import com.mapbox.navigation.core.internal.congestions.speed.SpeedAnalysisResultHandlerImpl
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.internal.logD

/**
 * Allows to override traffic data on the client-side in the runtime.
 *
 * This mechanism is split into several steps:
 * 1. This handler observes [RouteProgress] and [LocationMatcherResult] to pass this data to
 * [SpeedAnalysisResultHandler]
 * 2. [SpeedAnalysisResultHandler] analyzes the provided data and make decision whether
 * we need to update the congestion in front of user's position
 * 3. Then [SpeedAnalysisResult] is processed in [TrafficUpdateActionScanner]. We need this scanner
 * to accumulate intermediate [SpeedAnalysisResult] and postpone some immediate actions
 * 4. Once [TrafficUpdateActionScanner] returns any action it's processed in
 * [CommonTrafficUpdateActionHandler] which recreates [NavigationRoute] with updated
 * [LegAnnotation.congestionNumeric]
 * 5. Finally we set the updated route and the existing alternatives to the navigator with reason
 * [SetRoutes.RefreshRoutes]
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class TrafficOverrideHandler(trafficOverrideOptions: TrafficOverrideOptions) :
    RouteProgressObserver, LocationObserver, RoutesObserver {

    private val speedAnalysisResultHandler: SpeedAnalysisResultHandler =
        SpeedAnalysisResultHandlerImpl(trafficOverrideOptions.highSpeedThresholdInKmPerHour)
    private val trafficUpdateActionScanner: TrafficUpdateActionScannerChain =
        TrafficUpdateActionScannerChain(
            fallbackValue = TrafficUpdateAction.NoAction,
            NoActionTrafficUpdateActionScanner(),
            HighSpeedDetectedTrafficUpdateActionScanner(),
            WrongFalsePositiveTrafficUpdateActionScanner(),
            LowSpeedDetectedTrafficUpdateActionScanner(),
        )
    private val trafficUpdateActionHandler: TrafficUpdateActionHandler<TrafficUpdateAction> =
        CommonTrafficUpdateActionHandler(
            CongestionRangeGroup(
                trafficOverrideOptions.lowCongestionRange,
                trafficOverrideOptions.moderateCongestionRange,
                trafficOverrideOptions.heavyCongestionRange,
                trafficOverrideOptions.severeCongestionRange,
            ),
        )

    private var lastLocationMatcherResult: LocationMatcherResult? = null
    private var lastTrafficUpdateAction: TrafficUpdateAction = TrafficUpdateAction.NoAction
    private var lastRoutesUpdateResult: RoutesUpdatedResult? = null
    private var trafficRefreshObserver: ((List<NavigationRoute>) -> Unit)? = null

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        processData(routeProgress)
    }

    override fun onNewRawLocation(rawLocation: Location) {
        // do nothing
    }

    override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
        lastLocationMatcherResult = locationMatcherResult
    }

    private fun processData(routeProgress: RouteProgress) {
        val location = lastLocationMatcherResult ?: return

        val speedAnalysisResult =
            speedAnalysisResultHandler(routeProgress, location).also(::logSpeedAnalyzeResult)
        lastTrafficUpdateAction =
            trafficUpdateActionScanner.scan(lastTrafficUpdateAction, speedAnalysisResult)
                .also(::logTrafficUpdateAction)
        val refreshedRoute = trafficUpdateActionHandler.handleAction(lastTrafficUpdateAction)

        val lastUpdatedRoute = lastRoutesUpdateResult?.navigationRoutes?.firstOrNull()
        if (refreshedRoute != null && lastUpdatedRoute != null &&
            lastUpdatedRoute.id == refreshedRoute.id
        ) {
            trafficRefreshObserver?.invoke(
                listOf(refreshedRoute) +
                    lastRoutesUpdateResult?.navigationRoutes?.drop(1).orEmpty(),
            )
        }
    }

    private fun logTrafficUpdateAction(trafficUpdateAction: TrafficUpdateAction) {
        when (trafficUpdateAction) {
            is TrafficUpdateAction.AccumulatingLowSpeed ->
                log("Traffic will be updated in ${trafficUpdateAction.timeUntilUpdate}")

            TrafficUpdateAction.NoAction -> log("Traffic won't be updated")
            is TrafficUpdateAction.IncreaseTraffic -> log("Traffic will be updated")
            is TrafficUpdateAction.DecreaseTraffic -> log("Traffic will be decreased")
            is TrafficUpdateAction.RestoreTraffic -> log("Origin traffic will be restored")
        }
    }

    private fun logSpeedAnalyzeResult(speedAnalysisResult: SpeedAnalysisResult) {
        when (speedAnalysisResult) {
            is SpeedAnalysisResult.FailedToAnalyze ->
                log("Failed to analyze speed: ${speedAnalysisResult.message}")

            is SpeedAnalysisResult.LowSpeedDetected ->
                log(
                    "Low speed ${speedAnalysisResult.currentSpeed} detected on congestion " +
                        "${speedAnalysisResult.currentCongestion} where expected " +
                        "speed is ${speedAnalysisResult.expectedSpeed}.",
                )

            is SpeedAnalysisResult.SpeedIsOk ->
                log("Speed ${speedAnalysisResult.speed} matches congestion matches expected speed")

            is SpeedAnalysisResult.SpeedMatchesCongestionLevel ->
                log(
                    "Speed ${speedAnalysisResult.currentSpeed}; congestion " +
                        "${speedAnalysisResult.congestion} matches expected congestion " +
                        "${speedAnalysisResult.expectedCongestionForCurrentSpeed}",
                )

            is SpeedAnalysisResult.SkippedAnalysis ->
                log("Speed isn't analysed: ${speedAnalysisResult.message}")

            is SpeedAnalysisResult.HighSpeedDetected -> log("High speed detected")
            is SpeedAnalysisResult.WrongFalsePositiveOverrideDetected ->
                log("Wrong false positive override detected")
        }
    }

    fun registerRouteTrafficRefreshObserver(observer: (List<NavigationRoute>) -> Unit) {
        trafficRefreshObserver = observer
    }

    override fun onRoutesChanged(result: RoutesUpdatedResult) {
        if (result.reason != ROUTES_UPDATE_REASON_REFRESH) {
            lastRoutesUpdateResult = result
            lastTrafficUpdateAction = TrafficUpdateAction.NoAction
        }
    }
}

private fun log(message: String) {
    logD("TrafficOverride") { message }
}
