package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.UAComponents
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.maneuver.RoadShieldDownloader.downloadImage
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitComponentNode
import com.mapbox.navigation.ui.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.model.Lane
import com.mapbox.navigation.ui.maneuver.model.LaneIndicator
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.TextComponentNode
import com.mapbox.navigation.ui.maneuver.model.TotalManeuverDistance
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

internal class ManeuverProcessor {

    private val maneuverList = CopyOnWriteArrayList<Maneuver>()
    private val filteredInstructionList = CopyOnWriteArrayList<BannerInstructions>()
    private val bannerInstructionsComparator =
        Comparator<BannerInstructions> { instructions, nextInstructions ->
            instructions.distanceAlongGeometry().compareTo(nextInstructions.distanceAlongGeometry())
        }

    suspend fun process(action: ManeuverAction): ManeuverResult {
        return when (action) {
            is ManeuverAction.GetStepDistanceRemaining -> {
                processStepDistanceRemaining(action.stepProgress)
            }
            is ManeuverAction.GetAllBannerInstructions -> {
                processBannerInstructions(action.routeProgress)
            }
            is ManeuverAction.GetAllBannerInstructionsAfterStep -> {
                processBannerInstructionsAfterStep(action.routeProgress, action.bannerInstructions)
            }
            is ManeuverAction.GetAllManeuvers -> {
                processManeuverList(action.bannerInstructions)
            }
            is ManeuverAction.GetManeuver -> {
                processCurrentManeuver(action.bannerInstruction)
            }
        }
    }

    private suspend fun processCurrentManeuver(
        bannerInstruction: BannerInstructions
    ): ManeuverResult {
        val primaryManeuver = getPrimaryManeuver(bannerInstruction.primary())
        val secondaryManeuver = getSecondaryManeuver(bannerInstruction.secondary())
        val subManeuver = getSubManeuverText(bannerInstruction.sub())
        val totalDistance = getTotalStepDistance(bannerInstruction)
        val laneGuidance = getLaneGuidance(bannerInstruction)
        val maneuver = Maneuver(
            primaryManeuver,
            totalDistance,
            secondaryManeuver,
            subManeuver,
            laneGuidance
        )
        return ManeuverResult.GetManeuver(maneuver)
    }

    private fun getTotalStepDistance(bannerInstruction: BannerInstructions) =
        TotalManeuverDistance(bannerInstruction.distanceAlongGeometry())

    private suspend fun getPrimaryManeuver(bannerText: BannerText): PrimaryManeuver {
        val bannerComponentList = bannerText.components()
        return when (!bannerComponentList.isNullOrEmpty()) {
            true -> {
                PrimaryManeuver
                    .Builder()
                    .text(bannerText.text())
                    .type(bannerText.type())
                    .degrees(bannerText.degrees())
                    .modifier(bannerText.modifier())
                    .drivingSide(bannerText.drivingSide())
                    .componentList(createComponentList(bannerComponentList))
                    .build()
            }
            else -> {
                PrimaryManeuver.Builder().build()
            }
        }
    }

    private suspend fun getSecondaryManeuver(bannerText: BannerText?): SecondaryManeuver? {
        val bannerComponentList = bannerText?.components()
        return when (!bannerComponentList.isNullOrEmpty()) {
            true -> {
                SecondaryManeuver
                    .Builder()
                    .text(bannerText.text())
                    .type(bannerText.type())
                    .degrees(bannerText.degrees())
                    .modifier(bannerText.modifier())
                    .drivingSide(bannerText.drivingSide())
                    .componentList(createComponentList(bannerComponentList))
                    .build()
            }
            else -> {
                null
            }
        }
    }

    private suspend fun getSubManeuverText(bannerText: BannerText?): SubManeuver? {
        bannerText?.let { subBanner ->
            if (subBanner.type() != null && subBanner.text().isNotEmpty()) {
                val bannerComponentList = subBanner.components()
                return when (!bannerComponentList.isNullOrEmpty()) {
                    true -> {
                        SubManeuver
                            .Builder()
                            .text(bannerText.text())
                            .type(bannerText.type())
                            .degrees(bannerText.degrees())
                            .modifier(bannerText.modifier())
                            .drivingSide(bannerText.drivingSide())
                            .componentList(createComponentList(bannerComponentList))
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

    private suspend fun createComponentList(
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
                        .shieldIcon(
                            component.imageBaseUrl()?.let {
                                val roadShieldRequest = getHttpRequest(it)
                                return@let downloadImage(roadShieldRequest).data
                            }
                        )
                        .build()
                    componentList.add(Component(BannerComponents.ICON, roadShield))
                }
            }
        }
        return componentList
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

    private fun getHttpRequest(imageBaseUrl: String): HttpRequest {
        return HttpRequest.Builder()
            .url(imageBaseUrl.plus(SVG))
            .body(byteArrayOf())
            .headers(hashMapOf(Pair(USER_AGENT_KEY, USER_AGENT_VALUE)))
            .method(HttpMethod.GET)
            .uaComponents(
                UAComponents.Builder()
                    .sdkIdentifierComponent(SDK_IDENTIFIER)
                    .build()
            )
            .build()
    }

    private fun processStepDistanceRemaining(stepProgress: RouteStepProgress) =
        ManeuverResult.GetStepDistanceRemaining(stepProgress.distanceRemaining.toDouble())

    private fun processBannerInstructions(
        routeProgress: RouteProgress
    ): ManeuverResult {
        val bannerInstructionList = mutableListOf<BannerInstructions>()
        ifNonNull(routeProgress.currentLegProgress?.routeLeg?.steps()) { allSteps ->
            allSteps.forEach { step ->
                step.bannerInstructions()?.let { list ->
                    bannerInstructionList.addAll(list)
                }
            }
        }
        return ManeuverResult.GetAllBannerInstructions(bannerInstructionList)
    }

    private fun processBannerInstructionsAfterStep(
        routeProgress: RouteProgress,
        bannerInstructions: List<BannerInstructions>
    ): ManeuverResult {
        val currentStepProgress = routeProgress.currentLegProgress?.currentStepProgress
        val currentStep = currentStepProgress?.step
        val stepDistanceRemaining = currentStepProgress?.distanceRemaining?.toDouble()
        val filteredList = ifNonNull(stepDistanceRemaining) { distanceRemaining ->
            val currentBannerInstructions: BannerInstructions? = findCurrentBannerInstructions(
                currentStep, distanceRemaining
            )
            if (bannerInstructions.contains(currentBannerInstructions)) {
                val currentInstructionIndex = bannerInstructions.indexOf(currentBannerInstructions)
                if (currentInstructionIndex + 1 <= bannerInstructions.size) {
                    bannerInstructions.subList(currentInstructionIndex + 1, bannerInstructions.size)
                } else {
                    emptyList()
                }
            } else {
                bannerInstructions
            }
        } ?: bannerInstructions
        return ManeuverResult.GetAllBannerInstructionsAfterStep(filteredList)
    }

    private suspend fun processManeuverList(
        bannerInstructionsAfterStep: List<BannerInstructions>
    ): ManeuverResult {
        if (bannerInstructionsAfterStep != filteredInstructionList) {
            maneuverList.clear()
            filteredInstructionList.clear()
            filteredInstructionList.addAll(bannerInstructionsAfterStep)
            filteredInstructionList.forEach { bannerInstructions ->
                val result = processCurrentManeuver(bannerInstructions)
                maneuverList.add((result as ManeuverResult.GetManeuver).maneuver)
            }
        }
        return ManeuverResult.GetAllManeuvers(maneuverList)
    }

    private fun findCurrentBannerInstructions(
        currentStep: LegStep?,
        stepDistanceRemaining: Double
    ): BannerInstructions? {
        if (currentStep == null) {
            return null
        }
        val instructions = currentStep.bannerInstructions()
        return if (!instructions.isNullOrEmpty()) {
            val sortedInstructions: List<BannerInstructions> = sortBannerInstructions(instructions)
            for (bannerInstructions in sortedInstructions) {
                val distanceAlongGeometry = bannerInstructions.distanceAlongGeometry().toInt()
                if (distanceAlongGeometry >= stepDistanceRemaining.toInt()) {
                    return bannerInstructions
                }
            }
            instructions[0]
        } else {
            null
        }
    }

    private fun sortBannerInstructions(
        instructions: List<BannerInstructions>
    ): List<BannerInstructions> {
        val sortedInstructions: List<BannerInstructions> = ArrayList(instructions)
        Collections.sort(sortedInstructions, bannerInstructionsComparator)
        return sortedInstructions
    }

    private companion object {
        private const val SVG = ".svg"
        private const val USER_AGENT_KEY = "User-Agent"
        private const val USER_AGENT_VALUE = "MapboxJava/"
        private const val SDK_IDENTIFIER = "mapbox-navigation-ui-android"
    }
}
