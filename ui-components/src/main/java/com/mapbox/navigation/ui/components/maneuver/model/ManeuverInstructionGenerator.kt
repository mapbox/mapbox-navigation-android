package com.mapbox.navigation.ui.components.maneuver.model

import android.content.Context
import android.content.res.Resources
import android.text.SpannableStringBuilder
import com.mapbox.navigation.tripdata.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.maneuver.model.TextComponentNode
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.components.maneuver.view.MapboxExitText

internal object ManeuverInstructionGenerator {

    fun generatePrimary(
        context: Context,
        desiredHeight: Int,
        exitView: MapboxExitText,
        maneuver: PrimaryManeuver,
        roadShields: Set<RouteShield>? = null,
    ): SpannableStringBuilder {
        val instructionBuilder = SpannableStringBuilder()
        maneuver.componentList.forEach { component ->
            when (val node = component.node) {
                is TextComponentNode -> {
                    addTextToBuilder(node.text, instructionBuilder)
                }
                is ExitNumberComponentNode -> {
                    exitView.setExit(maneuver.modifier, node)
                    addExitToBuilder(
                        node.text,
                        exitView,
                        desiredHeight,
                        context.resources,
                        instructionBuilder,
                    )
                }
                is RoadShieldComponentNode -> {
                    addShieldToBuilder(
                        node.text,
                        desiredHeight,
                        context.resources,
                        getShieldToRender(node, roadShields),
                        instructionBuilder,
                    )
                }
                is DelimiterComponentNode -> {
                    addDelimiterToBuilder(node.text, instructionBuilder)
                }
            }
        }
        return instructionBuilder
    }

    fun generateSecondary(
        context: Context,
        desiredHeight: Int,
        exitView: MapboxExitText,
        maneuver: SecondaryManeuver?,
        roadShields: Set<RouteShield>? = null,
    ): SpannableStringBuilder {
        val instructionBuilder = SpannableStringBuilder()
        maneuver?.componentList?.forEach { component ->
            when (val node = component.node) {
                is TextComponentNode -> {
                    addTextToBuilder(node.text, instructionBuilder)
                }
                is ExitNumberComponentNode -> {
                    exitView.setExit(maneuver.modifier, node)
                    addExitToBuilder(
                        node.text,
                        exitView,
                        desiredHeight,
                        context.resources,
                        instructionBuilder,
                    )
                }
                is RoadShieldComponentNode -> {
                    addShieldToBuilder(
                        node.text,
                        desiredHeight,
                        context.resources,
                        getShieldToRender(node, roadShields),
                        instructionBuilder,
                    )
                }
                is DelimiterComponentNode -> {
                    addDelimiterToBuilder(node.text, instructionBuilder)
                }
            }
        }
        return instructionBuilder
    }

    fun generateSub(
        context: Context,
        desiredHeight: Int,
        exitView: MapboxExitText,
        maneuver: SubManeuver?,
        roadShields: Set<RouteShield>? = null,
    ): SpannableStringBuilder {
        val instructionBuilder = SpannableStringBuilder()
        maneuver?.componentList?.forEach { component ->
            when (val node = component.node) {
                is TextComponentNode -> {
                    addTextToBuilder(node.text, instructionBuilder)
                }
                is ExitNumberComponentNode -> {
                    exitView.setExit(maneuver.modifier, node)
                    addExitToBuilder(
                        node.text,
                        exitView,
                        desiredHeight,
                        context.resources,
                        instructionBuilder,
                    )
                }
                is RoadShieldComponentNode -> {
                    addShieldToBuilder(
                        node.text,
                        desiredHeight,
                        context.resources,
                        getShieldToRender(node, roadShields),
                        instructionBuilder,
                    )
                }
                is DelimiterComponentNode -> {
                    addDelimiterToBuilder(node.text, instructionBuilder)
                }
            }
        }
        return instructionBuilder
    }

    private fun addTextToBuilder(text: String, builder: SpannableStringBuilder) {
        builder.append(text)
        builder.append(" ")
    }

    private fun addExitToBuilder(
        exitText: String,
        exitView: MapboxExitText,
        desiredHeight: Int,
        resources: Resources,
        builder: SpannableStringBuilder,
    ) {
        val exitBuilder = ExitStyleGenerator.styleAndGetExit(
            exitText,
            exitView,
            desiredHeight,
            resources,
        )
        builder.append(exitBuilder)
        builder.append(" ")
    }

    private fun addShieldToBuilder(
        shieldText: String,
        desiredHeight: Int,
        resources: Resources,
        routeShield: RouteShield?,
        builder: SpannableStringBuilder,
    ) {
        val roadShieldBuilder = RoadShieldGenerator.styleAndGetRoadShield(
            shieldText,
            desiredHeight,
            resources,
            routeShield,
        )
        builder.append(roadShieldBuilder)
        builder.append(" ")
    }

    private fun addDelimiterToBuilder(text: String, builder: SpannableStringBuilder) {
        builder.append(text)
        builder.append(" ")
    }

    private fun getShieldToRender(
        node: RoadShieldComponentNode,
        roadShields: Set<RouteShield>?,
    ): RouteShield? {
        return node.mapboxShield?.let { shield ->
            roadShields?.find { it is RouteShield.MapboxDesignedShield && it.compareWith(shield) }
        } ?: node.shieldUrl?.let { shieldUrl ->
            roadShields?.find { it is RouteShield.MapboxLegacyShield && it.compareWith(shieldUrl) }
        }
    }
}
