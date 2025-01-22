package com.mapbox.navigation.voice.api

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.utils.internal.logW

internal class TimeBasedNextVoiceInstructionsProvider(
    private val observableTimeSeconds: Int,
) : NextVoiceInstructionsProvider {

    override fun getNextVoiceInstructions(progress: RouteProgressData): List<VoiceInstructions> {
        val legs = progress.route.legs() ?: return emptyList<VoiceInstructions>().also {
            logW(
                "Route does not contain legs, progress=$progress",
                LOG_CATEGORY,
            )
        }
        var legSteps = legs.getOrNull(progress.legIndex)?.steps()
        var currentStep = legSteps
            ?.getOrNull(progress.stepIndex)
            ?: return emptyList<VoiceInstructions>().also {
                logW(
                    "Route does not contain valid step, progress=$progress",
                    LOG_CATEGORY,
                )
            }

        val voiceInstructions = mutableListOf<VoiceInstructions>()
        fillCurrentStepVoiceInstructions(
            currentStep,
            progress.stepDistanceRemaining,
            voiceInstructions,
        )
        var cumulatedTime = progress.stepDurationRemaining

        // fill next steps
        var currentStepIndex = progress.stepIndex
        var currentLegIndex = progress.legIndex
        while (cumulatedTime < observableTimeSeconds) {
            if (isLastStep(currentStepIndex, legSteps)) {
                currentStepIndex = 0
                if (isLastLeg(currentLegIndex, legs)) {
                    break
                } else {
                    legSteps = legs[currentLegIndex + 1].steps()
                    currentLegIndex++
                    if (legSteps.isNullOrEmpty()) {
                        continue
                    } else {
                        currentStep = legSteps.first()
                    }
                }
            } else {
                currentStep = legSteps!![currentStepIndex + 1]
                currentStepIndex++
            }
            currentStep.voiceInstructions()?.let { voiceInstructions.addAll(it) }
            cumulatedTime += currentStep.duration()
        }
        return voiceInstructions
    }

    private fun fillCurrentStepVoiceInstructions(
        currentStep: LegStep,
        stepDistanceRemaining: Double,
        voiceInstructions: MutableList<VoiceInstructions>,
    ) {
        val currentStepInstructions = currentStep.voiceInstructions()?.filter { instruction ->
            val distanceAlongGeometry = instruction.distanceAlongGeometry()
            distanceAlongGeometry != null &&
                distanceAlongGeometry <= stepDistanceRemaining
        }
        if (currentStepInstructions != null) {
            voiceInstructions.addAll(currentStepInstructions)
        }
    }

    private fun isLastStep(currentStepIndex: Int, legSteps: List<LegStep>?): Boolean {
        return currentStepIndex + 1 >= (legSteps?.size ?: 0)
    }

    private fun isLastLeg(currentLegIndex: Int, legs: List<RouteLeg>): Boolean {
        return currentLegIndex + 1 >= legs.size
    }

    private companion object {
        private const val LOG_CATEGORY = "TimeBasedNextVoiceInstructionsProvider"
    }
}
