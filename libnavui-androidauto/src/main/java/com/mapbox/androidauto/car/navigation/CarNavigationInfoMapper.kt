package com.mapbox.androidauto.car.navigation

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import com.mapbox.androidauto.car.navigation.lanes.CarLanesImageRenderer
import com.mapbox.androidauto.car.navigation.lanes.useMapboxLaneGuidance
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverIconRenderer
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverInstructionRenderer
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverMapper
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.maneuver.view.MapboxExitText
import com.mapbox.navigation.ui.shield.model.RouteShield

/**
 * The car library provides an [NavigationTemplate.NavigationInfo] interface to show
 * in a similar way we show [Maneuver]s. This class takes our maneuvers and maps them to the
 * provided [RoutingInfo] for now.
 */
class CarNavigationInfoMapper(
    private val context: Context,
    private val carManeuverInstructionRenderer: CarManeuverInstructionRenderer,
    private val carManeuverIconRenderer: CarManeuverIconRenderer,
    private val carLanesImageGenerator: CarLanesImageRenderer,
    private val carDistanceFormatter: CarDistanceFormatter
) {

    private val primaryExitOptions = ManeuverPrimaryOptions.Builder().build().exitOptions
    private val secondaryExitOptions = ManeuverSecondaryOptions.Builder().build().exitOptions
    private val subExitOptions = ManeuverSubOptions.Builder().build().exitOptions

    fun mapNavigationInfo(
        expectedManeuvers: Expected<ManeuverError, List<Maneuver>>,
        routeShields: List<RouteShield>,
        routeProgress: RouteProgress,
    ): NavigationTemplate.NavigationInfo? {
        val currentStepProgress = routeProgress.currentLegProgress?.currentStepProgress
        val distanceRemaining = currentStepProgress?.distanceRemaining ?: return null
        val maneuver = expectedManeuvers.value?.firstOrNull()
        return maneuver?.primary?.let { primary ->
            val carManeuver = CarManeuverMapper.from(primary.type, primary.modifier, primary.degrees)
            carManeuverIconRenderer.renderManeuverIcon(primary)?.let { carManeuver.setIcon(it) }
            val primaryInstruction =
                renderManeuver(primary.componentList, routeShields, primaryExitOptions, primary.modifier)
            val instruction = SpannableStringBuilder.valueOf(primaryInstruction)
            maneuver.secondary?.let { secondary ->
                val secondaryInstruction =
                    renderManeuver(secondary.componentList, routeShields, secondaryExitOptions, secondary.modifier)
                instruction.append(System.lineSeparator())
                instruction.append(secondaryInstruction)
            }
            val step = Step.Builder(instruction)
                .setManeuver(carManeuver.build())
                .useMapboxLaneGuidance(carLanesImageGenerator, maneuver.laneGuidance)
                .build()

            val stepDistance = carDistanceFormatter.carDistance(distanceRemaining.toDouble())
            RoutingInfo.Builder()
                .setCurrentStep(step, stepDistance)
                .withOptionalNextStep(maneuver, routeShields)
                .build()
        }
    }

    private fun RoutingInfo.Builder.withOptionalNextStep(maneuver: Maneuver, routeShields: List<RouteShield>) = apply {
        maneuver.sub?.let { subManeuver ->
            val nextCarManeuver = CarManeuverMapper.from(subManeuver.type, subManeuver.modifier, subManeuver.degrees)
            carManeuverIconRenderer.renderManeuverIcon(subManeuver)?.let { nextCarManeuver.setIcon(it) }
            val instruction =
                renderManeuver(subManeuver.componentList, routeShields, subExitOptions, subManeuver.modifier)
            val nextStep = Step.Builder(instruction)
                .setManeuver(nextCarManeuver.build())
                .build()
            setNextStep(nextStep)
        }
    }

    private fun renderManeuver(
        maneuver: List<Component>,
        shields: List<RouteShield>,
        exitOptions: ManeuverExitOptions,
        modifier: String?,
    ): CharSequence {
        val exitView = MapboxExitText(context)
        exitView.updateTextAppearance(exitOptions.textAppearance)
        // TODO write when to check the type and pass MUTCD or VIENNA when the data is available
        exitView.updateExitProperties(exitOptions.mutcdExitProperties)
        return carManeuverInstructionRenderer.renderInstruction(maneuver, shields, exitView, modifier, IMAGE_HEIGHT)
    }

    private companion object {
        private const val IMAGE_HEIGHT = 72
    }
}
