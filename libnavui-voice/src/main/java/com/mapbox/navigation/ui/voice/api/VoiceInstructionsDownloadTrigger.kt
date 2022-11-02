package com.mapbox.navigation.ui.voice.api

import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull
import java.util.concurrent.CopyOnWriteArraySet

internal interface VoiceInstructionsDownloadTriggerObserver {

    fun trigger(voiceInstructions: List<VoiceInstructions>)
}

internal class VoiceInstructionsDownloadTrigger(
    private val observableTime: Int,
    private val timePercentageToTriggerAfter: Double,
    private val nextVoiceInstructionsProvider: NextVoiceInstructionsProvider
    = TimeBasedNextVoiceInstructionsProvider(observableTime),
    private val timeProvider: Time = Time.SystemImpl,
) : RoutesObserver, RouteProgressObserver {

    private val ignoredRouteUpdateReasons = setOf(
        RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
        RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE,
        RoutesExtra.ROUTES_UPDATE_REASON_REFRESH,
    )
    private var lastDownloadTime: Long = 0
    @VisibleForTesting
    internal val observers = CopyOnWriteArraySet<VoiceInstructionsDownloadTriggerObserver>()

    fun registerObserver(observer: VoiceInstructionsDownloadTriggerObserver) {
        observers.add(observer)
    }

    fun unregisterObserver(observer: VoiceInstructionsDownloadTriggerObserver) {
        observers.remove(observer)
    }

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

    private fun shouldDownloadBasedOnTime(): Boolean {
        return timeProvider.seconds() >= lastDownloadTime + observableTime * timePercentageToTriggerAfter
    }

    private fun triggerDownload(progressData: RouteProgressData) {
        lastDownloadTime = timeProvider.seconds()
        val nextInstructionsToDownload = nextVoiceInstructionsProvider
            .getNextVoiceInstructions(progressData)
        observers.forEach { it.trigger(nextInstructionsToDownload) }
    }
}
