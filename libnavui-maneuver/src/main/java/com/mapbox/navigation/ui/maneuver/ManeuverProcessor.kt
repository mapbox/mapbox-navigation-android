package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.internal.utils.isSameRoute
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.model.Lane
import com.mapbox.navigation.ui.maneuver.model.LaneIndicator
import com.mapbox.navigation.ui.maneuver.model.LegToManeuvers
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverOptions
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.StepIndexToManeuvers
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.TextComponentNode
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import java.util.UUID

internal object ManeuverProcessor {

    fun process(action: ManeuverAction): ManeuverResult {
        return when (action) {
            is ManeuverAction.GetManeuverListWithRoute -> {
                processManeuverList(
                    action.route,
                    action.routeLeg,
                    action.maneuverState,
                    action.maneuverOption,
                    action.distanceFormatter
                )
            }
            is ManeuverAction.GetManeuverList -> {
                processManeuverList(
                    action.routeProgress,
                    action.maneuverState,
                    action.maneuverOption,
                    action.distanceFormatter
                )
            }
        }
    }

    private fun processManeuverList(
        route: DirectionsRoute,
        routeLeg: RouteLeg? = null,
        maneuverState: ManeuverState,
        maneuverOptions: ManeuverOptions,
        distanceFormatter: DistanceFormatter,
    ): ManeuverResult.GetManeuverList {
        if (!route.isSameRoute(maneuverState.route)) {
            maneuverState.route = route
            maneuverState.allManeuvers.clear()
            maneuverState.roadShields.clear()
            try {
                createAllManeuversForRoute(route, maneuverState, distanceFormatter)
            } catch (exception: RuntimeException) {
                return ManeuverResult.GetManeuverList.Failure(exception.message)
            }
        }
        return try {
            val maneuverList = maneuverState.allManeuvers.getManeuversForRouteLeg(
                maneuverOptions.filterDuplicateManeuvers, routeLeg
            )
            ManeuverResult.GetManeuverList.Success(maneuverList)
        } catch (exception: RuntimeException) {
            ManeuverResult.GetManeuverList.Failure(exception.message)
        }
    }

    private fun List<LegToManeuvers>.getManeuversForRouteLeg(
        filterManeuvers: Boolean,
        routeLeg: RouteLeg? = null
    ): List<Maneuver> {
        if (isEmpty()) {
            throw RuntimeException("List cannot be empty")
        }
        val maneuverList: List<Maneuver> = when (routeLeg == null) {
            true -> {
                if (filterManeuvers) {
                    this[0].stepIndexToManeuvers.getManeuversForAllStepsAndFilter()
                } else {
                    this[0].stepIndexToManeuvers.getManeuversForAllSteps()
                }
            }
            else -> {
                this.find { item -> item.routeLeg == routeLeg }?.let { legToManeuver ->
                    if (filterManeuvers) {
                        legToManeuver.stepIndexToManeuvers.getManeuversForAllStepsAndFilter()
                    } else {
                        legToManeuver.stepIndexToManeuvers.getManeuversForAllSteps()
                    }
                } ?: throw RuntimeException("$routeLeg passed is different")
            }
        }
        if (maneuverList.isEmpty()) {
            throw RuntimeException("Maneuver list not found corresponding to $routeLeg")
        }
        return maneuverList
    }

    private fun List<StepIndexToManeuvers>.getManeuversForAllStepsAndFilter(): List<Maneuver> {
        val maneuverList = mutableListOf<Maneuver>()
        forEach { stepIndexToManeuver ->
            if (stepIndexToManeuver.maneuverList.size > 1) {
                maneuverList.add(stepIndexToManeuver.maneuverList[0])
            } else {
                maneuverList.addAll(stepIndexToManeuver.maneuverList)
            }
        }
        return maneuverList
    }

    private fun List<StepIndexToManeuvers>.getManeuversForAllSteps(): List<Maneuver> {
        val maneuverList = mutableListOf<Maneuver>()
        forEach { stepIndexToManeuver ->
            maneuverList.addAll(stepIndexToManeuver.maneuverList)
        }
        return maneuverList
    }

    private fun processManeuverList(
        routeProgress: RouteProgress,
        maneuverState: ManeuverState,
        maneuverOptions: ManeuverOptions,
        distanceFormatter: DistanceFormatter
    ): ManeuverResult.GetManeuverListWithProgress {
        return try {
            val route = routeProgress.route
            val routeLeg = routeProgress.currentLegProgress?.routeLeg
            val stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex
            val currentInstructionIndex =
                routeProgress.currentLegProgress?.currentStepProgress?.instructionIndex
            val stepDistanceRemaining =
                routeProgress.currentLegProgress?.currentStepProgress?.distanceRemaining?.toDouble()

            when {
                routeLeg == null -> {
                    return ManeuverResult.GetManeuverListWithProgress.Failure(
                        "routeLeg is null"
                    )
                }
                stepIndex == null -> {
                    return ManeuverResult.GetManeuverListWithProgress.Failure(
                        "stepIndex is null"
                    )
                }
                currentInstructionIndex == null -> {
                    return ManeuverResult.GetManeuverListWithProgress.Failure(
                        "instructionIndex is null"
                    )
                }
                stepDistanceRemaining == null -> {
                    return ManeuverResult.GetManeuverListWithProgress.Failure(
                        "distanceRemaining is null"
                    )
                }
                else -> {
                    if (!route.isSameRoute(maneuverState.route)) {
                        maneuverState.route = route
                        maneuverState.allManeuvers.clear()
                        maneuverState.roadShields.clear()
                        createAllManeuversForRoute(route, maneuverState, distanceFormatter)
                    }

                    val legToManeuvers = routeLeg.findIn(maneuverState.allManeuvers)
                    val stepsToManeuvers = legToManeuvers.stepIndexToManeuvers
                    val stepToManeuvers = stepIndex.findIn(stepsToManeuvers)
                    val indexOfStepToManeuvers = stepsToManeuvers.indexOf(stepToManeuvers)

                    updateDistanceRemainingForCurrentManeuver(
                        stepToManeuvers,
                        currentInstructionIndex,
                        stepDistanceRemaining
                    )

                    val maneuverList = if (maneuverOptions.filterDuplicateManeuvers) {
                        stepsToManeuvers.getManeuversForAllStepsWithProgressAndFilter(
                            currentInstructionIndex,
                            indexOfStepToManeuvers
                        )
                    } else {
                        stepsToManeuvers.getManeuversForAllStepsWithProgress(
                            currentInstructionIndex,
                            indexOfStepToManeuvers
                        )
                    }

                    ManeuverResult.GetManeuverListWithProgress.Success(maneuverList)
                }
            }
        } catch (exception: Exception) {
            ManeuverResult.GetManeuverListWithProgress.Failure(exception.message)
        }
    }

    private fun createAllManeuversForRoute(
        route: DirectionsRoute,
        maneuverState: ManeuverState,
        distanceFormatter: DistanceFormatter
    ) {
        ifNonNull(route.legs()) { routeLegs ->
            routeLegs.forEach { routeLeg ->
                ifNonNull(routeLeg?.steps()) { steps ->
                    val stepList = mutableListOf<StepIndexToManeuvers>()
                    for (stepIndex in 0..steps.lastIndex) {
                        steps[stepIndex].bannerInstructions()?.let { bannerInstruction ->
                            val maneuverList = mutableListOf<Maneuver>()
                            bannerInstruction.forEach { banner ->
                                maneuverList.add(transformToManeuver(banner, distanceFormatter))
                            }
                            val stepIndexToManeuvers = StepIndexToManeuvers(
                                stepIndex,
                                maneuverList
                            )
                            stepList.add(stepIndexToManeuvers)
                        } ?: throw RuntimeException("LegStep should have valid banner instructions")
                    }
                    maneuverState.allManeuvers.add(LegToManeuvers(routeLeg, stepList))
                } ?: throw RuntimeException("RouteLeg should have valid steps")
            }
        } ?: throw RuntimeException("Route should have valid legs")
        if (maneuverState.allManeuvers.isEmpty()) {
            throw RuntimeException("Maneuver list could not be created")
        }
    }

    private fun RouteLeg.findIn(legs: List<LegToManeuvers>): LegToManeuvers {
        return legs.find {
            it.routeLeg == this
        } ?: throw RuntimeException("Could not find the $this")
    }

    private fun Int.findIn(steps: List<StepIndexToManeuvers>): StepIndexToManeuvers {
        return steps.find {
            it.stepIndex == this
        } ?: throw RuntimeException("Could not find the $this")
    }

    private fun updateDistanceRemainingForCurrentManeuver(
        stepToManeuvers: StepIndexToManeuvers,
        currentInstructionIndex: Int,
        stepDistanceRemaining: Double
    ) {
        stepToManeuvers.maneuverList[currentInstructionIndex].stepDistance.distanceRemaining =
            stepDistanceRemaining
    }

    private fun List<StepIndexToManeuvers>.getManeuversForAllStepsWithProgress(
        currentInstructionIndex: Int,
        indexOfStepToManeuvers: Int
    ): List<Maneuver> {
        val list = mutableListOf<Maneuver>()
        for (i in indexOfStepToManeuvers..lastIndex) {
            if (this[i].maneuverList.size > 1 && i == indexOfStepToManeuvers) {
                list.addAll(
                    this[i].maneuverList.subList(currentInstructionIndex, this[i].maneuverList.size)
                )
            } else {
                list.addAll(this[i].maneuverList)
            }
        }
        return list
    }

    private fun List<StepIndexToManeuvers>.getManeuversForAllStepsWithProgressAndFilter(
        currentInstructionIndex: Int,
        indexOfStepToManeuvers: Int
    ): List<Maneuver> {
        val list = mutableListOf<Maneuver>()
        for (i in indexOfStepToManeuvers..lastIndex) {
            if (this[i].maneuverList.size > 1 && i == indexOfStepToManeuvers) {
                list.add(this[i].maneuverList[currentInstructionIndex])
            } else if (this[i].maneuverList.size > 1 && i != indexOfStepToManeuvers) {
                list.add(this[i].maneuverList[0])
            } else {
                list.addAll(this[i].maneuverList)
            }
        }
        return list
    }

    private fun transformToManeuver(
        bannerInstruction: BannerInstructions,
        distanceFormatter: DistanceFormatter
    ): Maneuver {
        val primaryManeuver = getPrimaryManeuver(bannerInstruction.primary())
        val secondaryManeuver = getSecondaryManeuver(bannerInstruction.secondary())
        val subManeuver = getSubManeuverText(bannerInstruction.sub())
        val laneGuidance = getLaneGuidance(bannerInstruction)
        val totalStepDistance = bannerInstruction.distanceAlongGeometry()
        val stepDistance = StepDistance(distanceFormatter, totalStepDistance, null)
        return Maneuver(
            primaryManeuver,
            stepDistance,
            secondaryManeuver,
            subManeuver,
            laneGuidance
        )
    }

    private fun getPrimaryManeuver(bannerText: BannerText): PrimaryManeuver {
        val bannerComponentList = bannerText.components()
        return when (!bannerComponentList.isNullOrEmpty()) {
            true -> {
                PrimaryManeuver
                    .Builder()
                    .id(UUID.randomUUID().toString())
                    .text(bannerText.text())
                    .type(bannerText.type())
                    .degrees(bannerText.degrees())
                    .modifier(bannerText.modifier())
                    .drivingSide(bannerText.drivingSide())
                    .componentList(createComponents(bannerComponentList))
                    .build()
            }
            else -> {
                PrimaryManeuver.Builder().build()
            }
        }
    }

    private fun getSecondaryManeuver(bannerText: BannerText?): SecondaryManeuver? {
        val bannerComponentList = bannerText?.components()
        return when (!bannerComponentList.isNullOrEmpty()) {
            true -> {
                SecondaryManeuver
                    .Builder()
                    .id(UUID.randomUUID().toString())
                    .text(bannerText.text())
                    .type(bannerText.type())
                    .degrees(bannerText.degrees())
                    .modifier(bannerText.modifier())
                    .drivingSide(bannerText.drivingSide())
                    .componentList(createComponents(bannerComponentList))
                    .build()
            }
            else -> {
                null
            }
        }
    }

    private fun getSubManeuverText(bannerText: BannerText?): SubManeuver? {
        bannerText?.let { subBanner ->
            if (subBanner.type() != null && subBanner.text().isNotEmpty()) {
                val bannerComponentList = subBanner.components()
                return when (!bannerComponentList.isNullOrEmpty()) {
                    true -> {
                        SubManeuver
                            .Builder()
                            .id(UUID.randomUUID().toString())
                            .text(bannerText.text())
                            .type(bannerText.type())
                            .degrees(bannerText.degrees())
                            .modifier(bannerText.modifier())
                            .drivingSide(bannerText.drivingSide())
                            .componentList(createComponents(bannerComponentList))
                            .build()
                    }
                    else -> {
                        null
                    }
                }
            }
        }
        return null
    }

    private fun getLaneGuidance(bannerInstruction: BannerInstructions): Lane? {
        val subBannerText = bannerInstruction.sub()
        val primaryBannerText = bannerInstruction.primary()
        return subBannerText?.let { subBanner ->
            if (subBanner.type() == null && subBanner.text().isEmpty()) {
                val bannerComponentList = subBanner.components()
                return ifNonNull(bannerComponentList) { list ->
                    val laneIndicatorList = mutableListOf<LaneIndicator>()
                    list.forEach {
                        val directions = it.directions()
                        val active = it.active()
                        if (!directions.isNullOrEmpty() && active != null) {
                            laneIndicatorList.add(
                                LaneIndicator
                                    .Builder()
                                    .isActive(active)
                                    .directions(directions)
                                    .build()
                            )
                        }
                    }
                    // TODO: This is a fallback solution. Remove this and add all active_directions
                    //  to LaneIndicator() once directions have migrated all customers to valhalla
                    Lane
                        .Builder()
                        .allLanes(laneIndicatorList)
                        .activeDirection(primaryBannerText.modifier())
                        .build()
                }
            }
            null
        }
    }

    private fun createComponents(
        bannerComponentList: List<BannerComponents>
    ): List<Component> {
        val componentList = mutableListOf<Component>()
        bannerComponentList.forEach { component ->
            when {
                component.type() == BannerComponents.EXIT -> {
                    val exit = ExitComponentNode
                        .Builder()
                        .text(component.text())
                        .build()
                    componentList.add(Component(BannerComponents.EXIT, exit))
                }
                component.type() == BannerComponents.EXIT_NUMBER -> {
                    val exitNumber = ExitNumberComponentNode
                        .Builder()
                        .text(component.text())
                        .build()
                    componentList.add(Component(BannerComponents.EXIT_NUMBER, exitNumber))
                }
                component.type() == BannerComponents.TEXT -> {
                    val text = TextComponentNode
                        .Builder()
                        .text(component.text())
                        .abbr(component.abbreviation())
                        .abbrPriority(component.abbreviationPriority())
                        .build()
                    componentList.add(Component(BannerComponents.TEXT, text))
                }
                component.type() == BannerComponents.DELIMITER -> {
                    val delimiter = DelimiterComponentNode
                        .Builder()
                        .text(component.text())
                        .build()
                    componentList.add(Component(BannerComponents.DELIMITER, delimiter))
                }
                component.type() == BannerComponents.ICON -> {
                    val roadShield = RoadShieldComponentNode
                        .Builder()
                        .text(component.text())
                        .shieldUrl(component.imageBaseUrl())
                        .build()
                    componentList.add(Component(BannerComponents.ICON, roadShield))
                }
            }
        }
        return componentList
    }
}
