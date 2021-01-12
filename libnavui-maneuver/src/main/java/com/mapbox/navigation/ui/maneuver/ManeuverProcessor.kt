package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.UserAgentComponents
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.base.model.maneuver.Component
import com.mapbox.navigation.ui.base.model.maneuver.DelimiterComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitNumberComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.Lane
import com.mapbox.navigation.ui.base.model.maneuver.LaneIndicator
import com.mapbox.navigation.ui.base.model.maneuver.Maneuver
import com.mapbox.navigation.ui.base.model.maneuver.PrimaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.RoadShieldComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.SecondaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.SubManeuver
import com.mapbox.navigation.ui.base.model.maneuver.TextComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.TotalManeuverDistance
import com.mapbox.navigation.ui.maneuver.RoadShieldDownloader.downloadImage
import com.mapbox.navigation.ui.utils.internal.ifNonNull

internal object ManeuverProcessor {

    private const val SVG = ".svg"
    private const val USER_AGENT_KEY = "User-Agent"
    private const val USER_AGENT_VALUE = "MapboxJava/"
    private const val SDK_IDENTIFIER = "mapbox-navigation-ui-android"

    suspend fun process(action: ManeuverAction): ManeuverResult {
        return when (action) {
            is ManeuverAction.FindStepDistanceRemaining -> {
                processStepDistanceRemaining(action.stepProgress)
            }
            is ManeuverAction.FindAllUpcomingManeuvers -> {
                processUpcomingManeuvers(action.routeLeg)
            }
            is ManeuverAction.ParseCurrentManeuver -> {
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
        val maneuver =
            Maneuver
                .Builder()
                .primary(primaryManeuver)
                .totalManeuverDistance(totalDistance)
                .secondary(secondaryManeuver)
                .sub(subManeuver)
                .laneGuidance(laneGuidance)
                .build()
        return ManeuverResult.CurrentManeuver(maneuver)
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
            .userAgentComponents(
                UserAgentComponents
                    .Builder()
                    .sdkIdentifier(SDK_IDENTIFIER)
                    .build()
            )
            .build()
    }

    private fun processStepDistanceRemaining(stepProgress: RouteStepProgress) =
        ManeuverResult.StepDistanceRemaining(stepProgress.distanceRemaining.toDouble())

    private suspend fun processUpcomingManeuvers(routeLeg: RouteLeg): ManeuverResult {
        val maneuverList = mutableListOf<Maneuver>()
        routeLeg.steps()?.let { allSteps ->
            allSteps.forEach { step ->
                step.bannerInstructions()?.let { bannerInstructionList ->
                    bannerInstructionList.forEach { bannerInstructions ->
                        val result = processCurrentManeuver(bannerInstructions)
                        maneuverList.add((result as ManeuverResult.CurrentManeuver).currentManeuver)
                    }
                }
            }
        }
        return ManeuverResult.UpcomingManeuvers(maneuverList)
    }
}
