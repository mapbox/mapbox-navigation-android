package com.mapbox.navigation.ui.androidauto.navigation

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.car.app.model.CarIcon
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverError
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.androidauto.navigation.lanes.CarLanesImageRenderer
import com.mapbox.navigation.ui.androidauto.navigation.lanes.useMapboxLaneGuidance
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverIconRenderer
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverInstructionRenderer
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverMapper
import com.mapbox.navigation.ui.androidauto.ui.maneuver.model.ManeuverExitOptions
import com.mapbox.navigation.ui.androidauto.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.androidauto.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.androidauto.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.androidauto.ui.maneuver.view.MapboxExitText
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue

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
) {

    private val primaryExitOptions = ManeuverPrimaryOptions.Builder().build().exitOptions
    private val secondaryExitOptions = ManeuverSecondaryOptions.Builder().build().exitOptions
    private val subExitOptions = ManeuverSubOptions.Builder().build().exitOptions

    // Maneuver icons and instructions are rendered into bitmaps. They change only with the
    // maneuver or its shields, so they are rendered once and reused on every route progress.
    private var renderedSteps: RenderedSteps? = null

    @JvmOverloads
    fun mapNavigationInfo(
        expectedManeuvers: Expected<ManeuverError, List<Maneuver>>,
        routeShields: List<RouteShield>,
        routeProgress: RouteProgress,
        junctionValue: JunctionValue? = null,
    ): NavigationTemplate.NavigationInfo? {
        val currentStepProgress = routeProgress.currentLegProgress?.currentStepProgress
        val distanceRemaining = currentStepProgress?.distanceRemaining ?: return null
        val maneuver = expectedManeuvers.value?.firstOrNull()
        return maneuver?.primary?.let { primary ->
            val rendered = renderSteps(maneuver, routeShields)
            val carManeuver =
                CarManeuverMapper.fromAnnouncedStep(
                    primary.type,
                    primary.modifier,
                    primary.degrees,
                    primary.drivingSide,
                    primary.componentList,
                    routeProgress,
                    CarManeuverMapper.PRIMARY_MANEUVER_STEP_OFFSET,
                )
            rendered.primaryIcon?.let { carManeuver.setIcon(it) }
            val step = Step.Builder(rendered.instruction)
                .setManeuver(carManeuver.build())
                .useMapboxLaneGuidance(carLanesImageGenerator, maneuver.laneGuidance)
                .build()

            val stepDistance = CarDistanceFormatter.carDistance(distanceRemaining.toDouble())
            RoutingInfo.Builder()
                .setCurrentStep(step, stepDistance)
                .withOptionalNextStep(maneuver.sub, rendered, routeProgress)
                .withOptionalJunctionImage(junctionValue)
                .build()
        }
    }

    private fun renderSteps(maneuver: Maneuver, routeShields: List<RouteShield>): RenderedSteps {
        val key = RenderKey(maneuver.primary, maneuver.secondary, maneuver.sub, routeShields)
        renderedSteps?.takeIf { it.key == key }?.let { return it }

        val primary = maneuver.primary
        val instruction = SpannableStringBuilder.valueOf(
            renderManeuver(
                primary.componentList,
                routeShields,
                primaryExitOptions,
                primary.modifier,
            ),
        )
        maneuver.secondary?.let { secondary ->
            val secondaryInstruction =
                renderManeuver(
                    secondary.componentList,
                    routeShields,
                    secondaryExitOptions,
                    secondary.modifier,
                )
            instruction.append(System.lineSeparator())
            instruction.append(secondaryInstruction)
        }
        val sub = maneuver.sub
        return RenderedSteps(
            key = key,
            primaryIcon = carManeuverIconRenderer.renderManeuverIcon(primary),
            instruction = instruction,
            subIcon = sub?.let { carManeuverIconRenderer.renderManeuverIcon(it) },
            subInstruction = sub?.let {
                renderManeuver(it.componentList, routeShields, subExitOptions, it.modifier)
            },
        ).also { renderedSteps = it }
    }

    private fun RoutingInfo.Builder.withOptionalJunctionImage(
        junctionValue: JunctionValue?,
    ) = apply {
        junctionValue?.also {
            val carIcon = CarIcon.Builder(
                IconCompat.createWithBitmap(it.bitmap),
            ).build()
            setJunctionImage(carIcon)
        }
    }

    private fun RoutingInfo.Builder.withOptionalNextStep(
        subManeuver: SubManeuver?,
        rendered: RenderedSteps,
        routeProgress: RouteProgress,
    ) = apply {
        val instruction = rendered.subInstruction
        if (subManeuver != null && instruction != null) {
            val nextCarManeuver =
                CarManeuverMapper.fromAnnouncedStep(
                    subManeuver.type,
                    subManeuver.modifier,
                    subManeuver.degrees,
                    subManeuver.drivingSide,
                    subManeuver.componentList,
                    routeProgress,
                    CarManeuverMapper.SUB_MANEUVER_STEP_OFFSET,
                )
            rendered.subIcon?.let { nextCarManeuver.setIcon(it) }
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
        return carManeuverInstructionRenderer.renderInstruction(
            maneuver,
            shields,
            exitView,
            modifier,
            IMAGE_HEIGHT,
        )
    }

    private data class RenderKey(
        val primary: PrimaryManeuver,
        val secondary: SecondaryManeuver?,
        val sub: SubManeuver?,
        val routeShields: List<RouteShield>,
    )

    // The rendered values are never changed after they are created, so templates can share them.
    private class RenderedSteps(
        val key: RenderKey,
        val primaryIcon: CarIcon?,
        val instruction: CharSequence,
        val subIcon: CarIcon?,
        val subInstruction: CharSequence?,
    )

    private companion object {
        private const val IMAGE_HEIGHT = 72
    }
}
