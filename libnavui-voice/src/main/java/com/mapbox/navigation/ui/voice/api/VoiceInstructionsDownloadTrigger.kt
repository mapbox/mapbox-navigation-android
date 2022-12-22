package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Class that is responsible for listening to relevant updates and triggering
 * voice instructions predownloading when needed.
 * Register and unregister with [MapboxNavigation.registerVoiceInstructionsTriggerObserver] and
 * [MapboxNavigation.unregisterVoiceInstructionsTriggerObserver].
 */
@ExperimentalPreviewMapboxNavigationAPI
class VoiceInstructionsDownloadTrigger internal constructor(
    private val observableTime: Int,
    private val timePercentageToTriggerAfter: Double,
    private val speechApi: MapboxSpeechApi,
    private val nextVoiceInstructionsProvider: NextVoiceInstructionsProvider,
    private val timeProvider: Time,
) : RouteProgressObserver, RoutesObserver {

    /**
     * Creates [VoiceInstructionsDownloadTrigger] with default
     * observableTime and timePercentageToTriggerAfter.
     * See [DEFAULT_OBSERVABLE_TIME_SECONDS] and [DEFAULT_TIME_PERCENTAGE_TO_TRIGGER_AFTER].
     *
     * @param speechApi [MapboxSpeechApi] instances that's used to generate instructions
     */
    constructor(speechApi: MapboxSpeechApi) : this(
        DEFAULT_OBSERVABLE_TIME_SECONDS,
        DEFAULT_TIME_PERCENTAGE_TO_TRIGGER_AFTER,
        speechApi
    )

    /**
     * Creates [VoiceInstructionsDownloadTrigger] with custom
     * observableTime and timePercentageToTriggerAfter.
     *
     * @param observableTime voice instructions will be predownloaded for `observableTime` seconds
     *  of route ahead
     * @param timePercentageToTriggerAfter voice instructions predownloading will not be triggered
     *  if `timePercentageToTriggerAfter` seconds did not pass since last predownloading session
     * @param speechApi [MapboxSpeechApi] instances that's used to generate instructions
     */
    constructor(
        observableTime: Int,
        timePercentageToTriggerAfter: Double,
        speechApi: MapboxSpeechApi,
    ) : this(
        observableTime,
        timePercentageToTriggerAfter,
        speechApi,
        TimeBasedNextVoiceInstructionsProvider(observableTime),
        Time.SystemImpl
    )

    private val ignoredRouteUpdateReasons = setOf(
        RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
        RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE,
        RoutesExtra.ROUTES_UPDATE_REASON_REFRESH,
    )
    private var lastDownloadTime: Long = 0

    /**
     * See [RoutesObserver.onRoutesChanged].
     */
    override fun onRoutesChanged(result: RoutesUpdatedResult) {
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
                    currentStep.distance()
                )
                triggerDownload(progress)
            }
        }
    }

    /**
     * See [RouteProgressObserver.onRouteProgressChanged].
     */
    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
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
                distanceRemaining
            ) { legIndex, stepIndex, durationRemaining, distanceRemaining ->
                val progressData = RouteProgressData(
                    routeProgress.route,
                    legIndex,
                    stepIndex,
                    durationRemaining,
                    distanceRemaining.toDouble()
                )
                triggerDownload(progressData)
            }
        }
    }

    /**
     * The method stops all work related to pre-downloading voice instructions and unregisters
     * all related callbacks. It should be invoked from `Activity#onDestroy`.
     */
    fun destroy() {
        speechApi.destroy()
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
