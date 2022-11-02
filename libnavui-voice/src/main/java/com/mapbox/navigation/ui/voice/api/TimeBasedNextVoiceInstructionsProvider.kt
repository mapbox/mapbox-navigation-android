package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.utils.internal.logW

internal class TimeBasedNextVoiceInstructionsProvider(
    private val observableTimeSeconds: Int,
) : NextVoiceInstructionsProvider {

    override fun getNextVoiceInstructions(progress: RouteProgressData): List<VoiceInstructions> {
        val legs = progress.route.legs() ?: return emptyList<VoiceInstructions>().also {
            logW(
                "Route does not contain legs, progress=$progress",
                LOG_CATEGORY
            )
        }
        var legSteps = legs.getOrNull(progress.legIndex)?.steps()
        var currentStep = legSteps
            ?.getOrNull(progress.stepIndex)
            ?: return emptyList<VoiceInstructions>().also {
                logW(
                    "Route does not contain valid step, progress=$progress",
                    LOG_CATEGORY
                )
            }

        val voiceInstructions = mutableListOf<VoiceInstructions>()
        var cumulatedTime = progress.stepDurationRemaining
        val currentStepInstructions = currentStep.voiceInstructions()?.filter { instruction ->
            val distanceAlongGeometry = instruction.distanceAlongGeometry()
            distanceAlongGeometry != null
                && distanceAlongGeometry <= progress.stepDistanceRemaining
        }
        if (currentStepInstructions != null) {
            voiceInstructions.addAll(currentStepInstructions)
        }

        var currentStepIndex = progress.stepIndex
        var currentLegIndex = progress.legIndex
        while (cumulatedTime < observableTimeSeconds) {
            if (currentStepIndex + 1 < (legSteps?.size ?: 0)) {
                currentStep = legSteps!![currentStepIndex + 1]
                currentStepIndex++
            } else {
                currentStepIndex = 0
                if (currentLegIndex + 1 < legs.size) {
                    legSteps = legs[currentLegIndex + 1].steps()
                    currentLegIndex++
                    if (legSteps.isNullOrEmpty()) {
                        continue
                    } else {
                        currentStep = legSteps.first()
                    }
                } else {
                    break
                }
            }
            currentStep.voiceInstructions()?.let { voiceInstructions.addAll(it) }
            cumulatedTime += currentStep.duration()
        }
        return voiceInstructions
    }

    private companion object {
        private const val LOG_CATEGORY = "TimeBasedNextVoiceInstructionsProvider"
    }
}
