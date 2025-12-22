package com.mapbox.navigation.tripdata.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ExitComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.Lane
import com.mapbox.navigation.tripdata.maneuver.model.LaneIndicator
import com.mapbox.navigation.tripdata.maneuver.model.LegIndexToManeuvers
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverOptions
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.StepDistance
import com.mapbox.navigation.tripdata.maneuver.model.StepIndexToManeuvers
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.maneuver.model.TextComponentNode
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import java.util.UUID

internal object ManeuverProcessor {

    fun process(action: ManeuverAction): ManeuverResult {
        return when (action) {
            is ManeuverAction.GetManeuverListWithRoute -> {
                processManeuverList(
                    action.route,
                    action.routeLegIndex,
                    action.maneuverState,
                    action.maneuverOption,
                    action.distanceFormatter,
                )
            }
            is ManeuverAction.GetManeuverList -> {
                processManeuverList(
                    action.routeProgress,
                    action.maneuverState,
                    action.maneuverOption,
                    action.distanceFormatter,
                )
            }
        }
    }

    private fun processManeuverList(
        route: DirectionsRoute,
        routeLegIndex: Int? = null,
        maneuverState: ManeuverState,
        maneuverOptions: ManeuverOptions,
        distanceFormatter: DistanceFormatter,
    ): ManeuverResult.GetManeuverList {
        if (!route.isSameRoute(maneuverState.route)) {
            maneuverState.route = route
            maneuverState.allManeuvers.clear()
            try {
                createAllManeuversForRoute(route, maneuverState, distanceFormatter)
            } catch (exception: RuntimeException) {
                return ManeuverResult.GetManeuverList.Failure(exception.message)
            }
        }
        return try {
            val maneuverList = maneuverState.allManeuvers.getManeuversForRouteLeg(
                maneuverOptions.filterDuplicateManeuvers,
                routeLegIndex,
            )
            ManeuverResult.GetManeuverList.Success(maneuverList)
        } catch (exception: RuntimeException) {
            ManeuverResult.GetManeuverList.Failure(exception.message)
        }
    }

    private fun List<LegIndexToManeuvers>.getManeuversForRouteLeg(
        filterManeuvers: Boolean,
        routeLegIndex: Int? = null,
    ): List<Maneuver> {
        if (isEmpty()) {
            throw RuntimeException("maneuver list cannot be empty")
        }

        val legToManeuver = if (routeLegIndex == null) {
            this[0]
        } else {
            this.find { item -> item.legIndex == routeLegIndex } ?: throw RuntimeException(
                "provided leg for which maneuvers should be generated is not found in the route",
            )
        }

        return if (filterManeuvers) {
            legToManeuver.stepIndexToManeuvers.getManeuversForStepsAndFilter()
        } else {
            legToManeuver.stepIndexToManeuvers.getManeuversForSteps()
        }.also {
            if (it.isEmpty()) {
                throw RuntimeException("no maneuvers available for the current route or its leg")
            }
        }
    }

    private fun processManeuverList(
        routeProgress: RouteProgress,
        maneuverState: ManeuverState,
        maneuverOptions: ManeuverOptions,
        distanceFormatter: DistanceFormatter,
    ): ManeuverResult.GetManeuverListWithProgress {
        return try {
            val route = routeProgress.route
            val routeLegIndex = routeProgress.currentLegProgress?.legIndex
            val stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex
            val currentInstructionIndex =
                routeProgress.currentLegProgress?.currentStepProgress?.instructionIndex
            val stepDistanceRemaining =
                routeProgress.currentLegProgress?.currentStepProgress?.distanceRemaining?.toDouble()

            when {
                routeLegIndex == null -> {
                    return ManeuverResult.GetManeuverListWithProgress.Failure(
                        "currentLegProgress is null",
                    )
                }
                stepIndex == null -> {
                    return ManeuverResult.GetManeuverListWithProgress.Failure(
                        "stepIndex is null",
                    )
                }
                currentInstructionIndex == null -> {
                    return ManeuverResult.GetManeuverListWithProgress.Failure(
                        "instructionIndex is null",
                    )
                }
                stepDistanceRemaining == null -> {
                    return ManeuverResult.GetManeuverListWithProgress.Failure(
                        "distanceRemaining is null",
                    )
                }
                else -> {
                    if (!route.isSameRoute(maneuverState.route)) {
                        maneuverState.route = route
                        maneuverState.allManeuvers.clear()
                        createAllManeuversForRoute(route, maneuverState, distanceFormatter)
                    }

                    val legToManeuvers = routeLegIndex.findIn(maneuverState.allManeuvers)
                    val stepsToManeuvers = legToManeuvers.stepIndexToManeuvers
                    val stepToManeuvers = stepIndex.findIn(stepsToManeuvers)
                    val indexOfStepToManeuvers = stepsToManeuvers.indexOf(stepToManeuvers)

                    stepToManeuvers.updateDistanceRemainingForCurrentManeuver(
                        currentInstructionIndex = currentInstructionIndex,
                        stepDistanceRemaining = stepDistanceRemaining,
                    )

                    val maneuverList = if (maneuverOptions.filterDuplicateManeuvers) {
                        stepsToManeuvers.getManeuversForStepsWithProgressAndFilter(
                            currentInstructionIndex,
                            indexOfStepToManeuvers,
                        )
                    } else {
                        stepsToManeuvers.getManeuversForStepsWithProgress(
                            currentInstructionIndex,
                            indexOfStepToManeuvers,
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
        distanceFormatter: DistanceFormatter,
    ) {
        ifNonNull(route.legs()) { routeLegs ->
            routeLegs.forEachIndexed { index, routeLeg ->
                ifNonNull(routeLeg?.steps()) { steps ->
                    val stepList = mutableListOf<StepIndexToManeuvers>()
                    for (stepIndex in 0..steps.lastIndex) {
                        val stepIntersections = if (stepIndex == steps.lastIndex) {
                            steps[stepIndex].intersections()
                        } else {
                            steps[stepIndex + 1].intersections()
                        }
                        ifNonNull(
                            steps[stepIndex].bannerInstructions(),
                            stepIntersections,
                        ) { bannerInstruction, intersections ->
                            val maneuverPoint = intersections.first().location()
                            val maneuverList = mutableListOf<Maneuver>()
                            val drivingSide = steps[stepIndex].drivingSide()!!
                            bannerInstruction.forEach { banner ->
                                maneuverList.add(
                                    transformToManeuver(
                                        drivingSide = drivingSide,
                                        bannerInstruction = banner,
                                        maneuverPoint = maneuverPoint,
                                        distanceFormatter = distanceFormatter,
                                    ),
                                )
                            }
                            val stepIndexToManeuvers = StepIndexToManeuvers(
                                stepIndex,
                                maneuverList,
                            )
                            stepList.add(stepIndexToManeuvers)
                        } ?: throw RuntimeException("LegStep should have valid banner instructions")
                    }
                    maneuverState.allManeuvers.add(LegIndexToManeuvers(index, stepList))
                } ?: throw RuntimeException("RouteLeg should have valid steps")
            }
        } ?: throw RuntimeException("Route should have valid legs")
        if (maneuverState.allManeuvers.isEmpty()) {
            throw RuntimeException("Maneuver list could not be created")
        }
    }

    private fun Int.findIn(legs: List<LegIndexToManeuvers>): LegIndexToManeuvers {
        return legs.find {
            it.legIndex == this
        } ?: throw RuntimeException("Could not find leg with index $this")
    }

    private fun Int.findIn(steps: List<StepIndexToManeuvers>): StepIndexToManeuvers {
        return steps.find {
            it.stepIndex == this
        } ?: throw RuntimeException("Could not find step with index $this")
    }

    private fun StepIndexToManeuvers.updateDistanceRemainingForCurrentManeuver(
        currentInstructionIndex: Int,
        stepDistanceRemaining: Double,
    ) {
        val maneuverAtCurrentIndex = maneuverList[currentInstructionIndex]
        val maneuverWithUpdatedDistRemaining = maneuverAtCurrentIndex.copy(
            stepDistance = StepDistance(
                distanceFormatter = maneuverAtCurrentIndex.stepDistance.distanceFormatter,
                totalDistance = maneuverAtCurrentIndex.stepDistance.totalDistance,
                distanceRemaining = stepDistanceRemaining,
            ),
        )
        maneuverList.set(
            index = currentInstructionIndex,
            element = maneuverWithUpdatedDistRemaining,
        )
    }

    private fun List<StepIndexToManeuvers>.getManeuversForStepsAndFilter(): List<Maneuver> {
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

    private fun List<StepIndexToManeuvers>.getManeuversForSteps(): List<Maneuver> {
        val maneuverList = mutableListOf<Maneuver>()
        forEach { stepIndexToManeuver ->
            maneuverList.addAll(stepIndexToManeuver.maneuverList)
        }
        return maneuverList
    }

    private fun List<StepIndexToManeuvers>.getManeuversForStepsWithProgress(
        currentInstructionIndex: Int,
        indexOfStepToManeuvers: Int,
    ): List<Maneuver> {
        val list = mutableListOf<Maneuver>()
        // only take the current and remaining instructions for the current step
        list.addAll(
            this[indexOfStepToManeuvers].maneuverList.drop(currentInstructionIndex),
        )
        // add all remaining instructions after the current step
        list.addAll(this.drop(indexOfStepToManeuvers + 1).getManeuversForSteps())
        return list
    }

    private fun List<StepIndexToManeuvers>.getManeuversForStepsWithProgressAndFilter(
        currentInstructionIndex: Int,
        indexOfStepToManeuvers: Int,
    ): List<Maneuver> {
        val list = mutableListOf<Maneuver>()
        // only take the current instructions for the current step
        list.add(this[indexOfStepToManeuvers].maneuverList[currentInstructionIndex])
        // add all remaining instructions after the current step without duplicates
        list.addAll(this.drop(indexOfStepToManeuvers + 1).getManeuversForStepsAndFilter())
        return list
    }

    private fun transformToManeuver(
        drivingSide: String,
        maneuverPoint: Point,
        bannerInstruction: BannerInstructions,
        distanceFormatter: DistanceFormatter,
    ): Maneuver {
        val primaryManeuver = getPrimaryManeuver(drivingSide, bannerInstruction.primary())
        val secondaryManeuver = getSecondaryManeuver(drivingSide, bannerInstruction.secondary())
        val subManeuver = getSubManeuverText(drivingSide, bannerInstruction.sub())
        val laneGuidance = getLaneGuidance(drivingSide, bannerInstruction)
        val totalStepDistance = bannerInstruction.distanceAlongGeometry()
        val stepDistance = StepDistance(distanceFormatter, totalStepDistance, null)
        return Maneuver(
            primaryManeuver,
            stepDistance,
            secondaryManeuver,
            subManeuver,
            laneGuidance,
            maneuverPoint,
        )
    }

    private fun getPrimaryManeuver(drivingSide: String, bannerText: BannerText): PrimaryManeuver {
        val bannerComponentList = bannerText.components()
        return when (!bannerComponentList.isNullOrEmpty()) {
            true -> {
                PrimaryManeuver(
                    UUID.randomUUID().toString(),
                    bannerText.text(),
                    bannerText.type(),
                    bannerText.degrees(),
                    bannerText.modifier(),
                    bannerText.drivingSide() ?: drivingSide,
                    createComponents(bannerComponentList),
                )
            }
            else -> {
                PrimaryManeuver()
            }
        }
    }

    private fun getSecondaryManeuver(
        drivingSide: String,
        bannerText: BannerText?,
    ): SecondaryManeuver? {
        val bannerComponentList = bannerText?.components()
        return when (!bannerComponentList.isNullOrEmpty()) {
            true -> {
                SecondaryManeuver(
                    UUID.randomUUID().toString(),
                    bannerText.text(),
                    bannerText.type(),
                    bannerText.degrees(),
                    bannerText.modifier(),
                    bannerText.drivingSide() ?: drivingSide,
                    createComponents(bannerComponentList),
                )
            }
            else -> {
                null
            }
        }
    }

    private fun getSubManeuverText(
        drivingSide: String,
        bannerText: BannerText?,
    ): SubManeuver? {
        bannerText?.let { subBanner ->
            if (subBanner.type() != null && subBanner.text().isNotEmpty()) {
                val bannerComponentList = subBanner.components()
                return when (!bannerComponentList.isNullOrEmpty()) {
                    true -> {
                        SubManeuver(
                            UUID.randomUUID().toString(),
                            bannerText.text(),
                            bannerText.type(),
                            bannerText.degrees(),
                            bannerText.modifier(),
                            bannerText.drivingSide() ?: drivingSide,
                            createComponents(bannerComponentList),
                        )
                    }
                    else -> {
                        null
                    }
                }
            }
        }
        return null
    }

    private fun getLaneGuidance(
        drivingSide: String,
        bannerInstruction: BannerInstructions,
    ): Lane? {
        val subBannerText = bannerInstruction.sub()
        return subBannerText?.let { subBanner ->
            if (subBanner.type() == null && subBanner.text().isEmpty()) {
                val bannerComponentList = subBanner.components()
                return ifNonNull(bannerComponentList) { list ->
                    val laneIndicatorList = mutableListOf<LaneIndicator>()
                    list.forEach {
                        if (it.type() == BannerComponents.LANE) {
                            val directions = it.directions()
                            val active = it.active()
                            val activeDirection: String? =
                                if (active!! && it.activeDirection() == null) {
                                    bannerInstruction.primary().modifier()
                                } else {
                                    it.activeDirection()
                                }
                            if (!directions.isNullOrEmpty()) {
                                laneIndicatorList.add(
                                    LaneIndicator
                                        .Builder()
                                        .isActive(active)
                                        .directions(directions)
                                        .drivingSide(drivingSide)
                                        .activeDirection(activeDirection)
                                        .build(),
                                )
                            }
                        }
                    }
                    Lane(laneIndicatorList)
                }
            }
            null
        }
    }

    private fun createComponents(
        bannerComponentList: List<BannerComponents>,
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
                        .mapboxShield(component.mapboxShield())
                        .build()
                    componentList.add(Component(BannerComponents.ICON, roadShield))
                }
            }
        }
        return componentList
    }
}
