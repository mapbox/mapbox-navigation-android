package com.mapbox.navigation.tripdata.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.IntersectionLanes
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
        val (previousRoute, previousManeuvers) = maneuverState.routeWithManeuvers
        val allManeuvers = if (route.isSameRoute(previousRoute)) {
            previousManeuvers
        } else {
            try {
                createAllManeuversForRoute(route, distanceFormatter)
            } catch (exception: RuntimeException) {
                return ManeuverResult.GetManeuverList.Failure(exception.message)
            }.also { maneuverState.routeWithManeuvers = route to it }
        }
        return try {
            val maneuverList = allManeuvers.getManeuversForRouteLeg(
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
        }.ifEmpty {
            throw RuntimeException("no maneuvers available for the current route or its leg")
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
                    val (previousRoute, previousManeuvers) = maneuverState.routeWithManeuvers
                    val allManeuvers = if (route.isSameRoute(previousRoute)) {
                        previousManeuvers
                    } else {
                        createAllManeuversForRoute(route, distanceFormatter)
                            .also { maneuverState.routeWithManeuvers = route to it }
                    }

                    val legToManeuvers = routeLegIndex.findIn(allManeuvers)
                    val stepsToManeuvers = legToManeuvers.stepIndexToManeuvers
                    val stepToManeuvers = stepIndex.findIn(stepsToManeuvers)
                    val indexOfStepToManeuvers = stepsToManeuvers.indexOf(stepToManeuvers)

                    val maneuverList = if (maneuverOptions.filterDuplicateManeuvers) {
                        stepsToManeuvers.getManeuversForStepsWithProgressAndFilter(
                            currentInstructionIndex,
                            stepDistanceRemaining,
                            indexOfStepToManeuvers,
                        )
                    } else {
                        stepsToManeuvers.getManeuversForStepsWithProgress(
                            currentInstructionIndex,
                            stepDistanceRemaining,
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
        distanceFormatter: DistanceFormatter,
    ): List<LegIndexToManeuvers> {
        val routeLegs = route.legs() ?: throw RuntimeException("Route should have valid legs")
        return routeLegs.mapIndexed { index, leg ->
            val steps = leg?.steps() ?: throw RuntimeException("RouteLeg should have valid steps")
            val stepList = steps.mapIndexed { stepIndex, element ->
                val nextStepIntersections = if (stepIndex == steps.lastIndex) {
                    element.intersections()
                } else {
                    steps[stepIndex + 1].intersections()
                } ?: throw RuntimeException("LegStep should have valid banner instructions")
                val bannerInstruction = element.bannerInstructions()
                    ?: throw RuntimeException("LegStep should have valid banner instructions")
                val maneuverPoint = nextStepIntersections.first().location()
                val lanes = nextStepIntersections.first().lanes()
                val drivingSide = element.drivingSide()!!
                val maneuverList = bannerInstruction.map { banner ->
                    transformToManeuver(
                        drivingSide = drivingSide,
                        bannerInstruction = banner,
                        maneuverPoint = maneuverPoint,
                        distanceFormatter = distanceFormatter,
                        intersectionLanes = lanes,
                    )
                }
                StepIndexToManeuvers(stepIndex, maneuverList)
            }
            LegIndexToManeuvers(index, stepList)
        }.ifEmpty { throw RuntimeException("Maneuver list could not be created") }
    }

    private fun Int.findIn(legs: List<LegIndexToManeuvers>): LegIndexToManeuvers {
        return legs.find { it.legIndex == this }
            ?: throw RuntimeException("Could not find leg with index $this")
    }

    private fun Int.findIn(steps: List<StepIndexToManeuvers>): StepIndexToManeuvers {
        return steps.find { it.stepIndex == this }
            ?: throw RuntimeException("Could not find step with index $this")
    }

    private fun List<StepIndexToManeuvers>.getManeuversForStepsAndFilter(): List<Maneuver> {
        return flatMap { it.maneuverList.take(1) }
    }

    private fun List<StepIndexToManeuvers>.getManeuversForSteps(): List<Maneuver> {
        return flatMap { it.maneuverList }
    }

    private fun List<StepIndexToManeuvers>.getManeuversForStepsWithProgress(
        currentInstructionIndex: Int,
        stepDistanceRemaining: Double,
        indexOfStepToManeuvers: Int,
    ): List<Maneuver> {
        val stepToManeuvers = this[indexOfStepToManeuvers]
        val maneuverAtCurrentIndex = stepToManeuvers.maneuverList[currentInstructionIndex]
        val remainingInstructions = drop(indexOfStepToManeuvers + 1)
        return buildList {
            // only take the current and remaining instructions for the current step
            add(
                maneuverAtCurrentIndex.copy(
                    stepDistance = StepDistance(
                        distanceFormatter = maneuverAtCurrentIndex.stepDistance.distanceFormatter,
                        totalDistance = maneuverAtCurrentIndex.stepDistance.totalDistance,
                        distanceRemaining = stepDistanceRemaining,
                    ),
                ),
            )
            addAll(stepToManeuvers.maneuverList.drop(currentInstructionIndex + 1))
            // add all remaining instructions after the current step
            addAll(remainingInstructions.getManeuversForSteps())
        }
    }

    private fun List<StepIndexToManeuvers>.getManeuversForStepsWithProgressAndFilter(
        currentInstructionIndex: Int,
        stepDistanceRemaining: Double,
        indexOfStepToManeuvers: Int,
    ): List<Maneuver> {
        val stepToManeuvers = this[indexOfStepToManeuvers]
        val maneuverAtCurrentIndex = stepToManeuvers.maneuverList[currentInstructionIndex]
        val remainingInstructions = drop(indexOfStepToManeuvers + 1)
        return buildList {
            // only take the current instructions for the current step
            add(
                maneuverAtCurrentIndex.copy(
                    stepDistance = StepDistance(
                        distanceFormatter = maneuverAtCurrentIndex.stepDistance.distanceFormatter,
                        totalDistance = maneuverAtCurrentIndex.stepDistance.totalDistance,
                        distanceRemaining = stepDistanceRemaining,
                    ),
                ),
            )
            // add all remaining instructions after the current step without duplicates
            addAll(remainingInstructions.getManeuversForStepsAndFilter())
        }
    }

    private fun transformToManeuver(
        drivingSide: String,
        maneuverPoint: Point,
        bannerInstruction: BannerInstructions,
        distanceFormatter: DistanceFormatter,
        intersectionLanes: List<IntersectionLanes>?,
    ): Maneuver {
        val primaryManeuver = getPrimaryManeuver(drivingSide, bannerInstruction.primary())
        val secondaryManeuver = getSecondaryManeuver(drivingSide, bannerInstruction.secondary())
        val subManeuver = getSubManeuverText(drivingSide, bannerInstruction.sub())
        val laneGuidance = getLaneGuidance(drivingSide, bannerInstruction, intersectionLanes)
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
        return if (bannerComponentList.isNullOrEmpty()) {
            PrimaryManeuver()
        } else {
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
    }

    private fun getSecondaryManeuver(
        drivingSide: String,
        bannerText: BannerText?,
    ): SecondaryManeuver? {
        val bannerComponentList = bannerText?.components()
        return if (bannerComponentList.isNullOrEmpty()) {
            null
        } else {
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
    }

    private fun getSubManeuverText(
        drivingSide: String,
        bannerText: BannerText?,
    ): SubManeuver? {
        if (bannerText != null && bannerText.type() != null && bannerText.text().isNotEmpty()) {
            val bannerComponentList = bannerText.components()
            if (!bannerComponentList.isNullOrEmpty()) {
                return SubManeuver(
                    UUID.randomUUID().toString(),
                    bannerText.text(),
                    bannerText.type(),
                    bannerText.degrees(),
                    bannerText.modifier(),
                    bannerText.drivingSide() ?: drivingSide,
                    createComponents(bannerComponentList),
                )
            }
        }
        return null
    }

    private fun getLaneGuidance(
        drivingSide: String,
        bannerInstruction: BannerInstructions,
        nextIntersectionLanes: List<IntersectionLanes>?,
    ): Lane? {
        return nextIntersectionLanes?.let { list ->
            val laneIndicatorList = list.mapNotNull { lane ->
                val directions = lane.indications()
                if (directions.isNullOrEmpty()) return@mapNotNull null
                val active = lane.active() == true
                val activeDirection = if (active && lane.validIndication() == null) {
                    bannerInstruction.primary().modifier()
                } else {
                    lane.validIndication()
                }
                val accessDesignatedPerLane = lane.access()?.designated().orEmpty()
                LaneIndicator
                    .Builder()
                    .isActive(active)
                    .directions(directions)
                    .drivingSide(drivingSide)
                    .activeDirection(activeDirection)
                    .accessDesignated(accessDesignatedPerLane)
                    .build()
            }
            Lane(laneIndicatorList)
        }
    }

    private fun createComponents(
        bannerComponentList: List<BannerComponents>,
    ): List<Component> {
        return bannerComponentList.mapNotNull { component ->
            when {
                component.type() == BannerComponents.EXIT -> {
                    val exit = ExitComponentNode
                        .Builder()
                        .text(component.text())
                        .build()
                    Component(BannerComponents.EXIT, exit)
                }

                component.type() == BannerComponents.EXIT_NUMBER -> {
                    val exitNumber = ExitNumberComponentNode
                        .Builder()
                        .text(component.text())
                        .build()
                    Component(BannerComponents.EXIT_NUMBER, exitNumber)
                }

                component.type() == BannerComponents.TEXT -> {
                    val text = TextComponentNode
                        .Builder()
                        .text(component.text())
                        .abbr(component.abbreviation())
                        .abbrPriority(component.abbreviationPriority())
                        .build()
                    Component(BannerComponents.TEXT, text)
                }

                component.type() == BannerComponents.DELIMITER -> {
                    val delimiter = DelimiterComponentNode
                        .Builder()
                        .text(component.text())
                        .build()
                    Component(BannerComponents.DELIMITER, delimiter)
                }

                component.type() == BannerComponents.ICON -> {
                    val roadShield = RoadShieldComponentNode
                        .Builder()
                        .text(component.text())
                        .shieldUrl(component.imageBaseUrl())
                        .mapboxShield(component.mapboxShield())
                        .build()
                    Component(BannerComponents.ICON, roadShield)
                }

                else -> null
            }
        }
    }
}
