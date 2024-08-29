package com.mapbox.navigation.voice.api

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Class that is responsible for listening to relevant updates and triggering
 * voice instructions predownloading when needed.
 * Register and unregister with [MapboxNavigationApp.registerObserver]
 * and [MapboxNavigationApp.unregisterObserver]
 * or invoke [onAttached] and [onDetached] manually if you are not using [MapboxNavigationApp].
 */
@ExperimentalPreviewMapboxNavigationAPI
class VoiceInstructionsPrefetcher internal constructor(
    private val speechApi: MapboxSpeechApi,
    private val observableTime: Int,
    private val timePercentageToTriggerAfter: Double,
    private val nextVoiceInstructionsProvider: NextVoiceInstructionsProvider =
        TimeBasedNextVoiceInstructionsProvider(observableTime),
    private val timeProvider: Time = Time.SystemClockImpl,
) : MapboxNavigationObserver {

    /**
     * Creates [VoiceInstructionsPrefetcher.
     *
     * @param speechApi [MapboxSpeechApi] instances that's used to generate instructions
     */
    constructor(speechApi: MapboxSpeechApi) : this(
        speechApi,
        DEFAULT_OBSERVABLE_TIME_SECONDS,
        DEFAULT_TIME_PERCENTAGE_TO_TRIGGER_AFTER,
    )

    private val routesObserver = RoutesObserver { onRoutesChanged(it) }

    private val routeProgressObserver = RouteProgressObserver { onRouteProgressChanged(it) }

    private val ignoredRouteUpdateReasons = setOf(
        RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
        RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE,
        RoutesExtra.ROUTES_UPDATE_REASON_REFRESH,
    )
    private var lastDownloadTime: Long = 0

    /**
     * See [MapboxNavigationObserver.onAttached].
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    /**
     * See [MapboxNavigationObserver.onDetached].
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        lastDownloadTime = 0
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        speechApi.cancelPredownload()
    }

    private fun onRoutesChanged(result: RoutesUpdatedResult) {
        if (result.reason in ignoredRouteUpdateReasons) {
            return
        }
        result.navigationRoutes.firstOrNull()?.let {
            val currentStep = it.directionsRoute.legs()?.firstOrNull()?.steps()?.firstOrNull()
            if (currentStep != null) {
                val progress = RouteProgressData(
                    it.directionsRoute,
                    0,
                    0,
                    currentStep.duration(),
                    currentStep.distance(),
                )
                triggerDownload(progress)
            }
        }
    }

    private fun onRouteProgressChanged(routeProgress: RouteProgress) {
        if (shouldDownloadBasedOnTime()) {
            val legIndex = routeProgress.currentLegProgress?.legIndex
            val currentStepProgress = routeProgress.currentLegProgress?.currentStepProgress
            val stepIndex = currentStepProgress?.stepIndex
            val durationRemaining = currentStepProgress?.durationRemaining
            val distanceRemaining = currentStepProgress?.distanceRemaining
            ifNonNull(
                legIndex,
                stepIndex,
                durationRemaining,
                distanceRemaining,
            ) { legIndex, stepIndex, durationRemaining, distanceRemaining ->
                val progressData = RouteProgressData(
                    routeProgress.route,
                    legIndex,
                    stepIndex,
                    durationRemaining,
                    distanceRemaining.toDouble(),
                )
                triggerDownload(progressData)
            }
        }
    }

    private fun shouldDownloadBasedOnTime(): Boolean {
        return timeProvider.seconds() >=
            lastDownloadTime + observableTime * timePercentageToTriggerAfter
    }

    private fun triggerDownload(progressData: RouteProgressData) {
        lastDownloadTime = timeProvider.seconds()
        val nextInstructionsToDownload = nextVoiceInstructionsProvider
            .getNextVoiceInstructions(progressData)
        speechApi.predownload(nextInstructionsToDownload)
    }

    companion object {
        /**
         * Default value used for [observableTime], specified in seconds.
         */
        const val DEFAULT_OBSERVABLE_TIME_SECONDS = 3 * 60 // 3 minutes

        /**
         * Default value for [timePercentageToTriggerAfter].
         */
        const val DEFAULT_TIME_PERCENTAGE_TO_TRIGGER_AFTER = 0.5
    }
}
