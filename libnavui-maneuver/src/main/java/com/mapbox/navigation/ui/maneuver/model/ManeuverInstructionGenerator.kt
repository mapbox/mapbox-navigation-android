package com.mapbox.navigation.ui.maneuver.model

import android.content.Context
import android.content.res.Resources
import android.text.SpannableStringBuilder
import com.mapbox.navigation.ui.maneuver.view.MapboxExitText
import com.mapbox.navigation.ui.shield.internal.model.getRefLen
import com.mapbox.navigation.ui.utils.internal.ifNonNull

internal object ManeuverInstructionGenerator {

    fun generatePrimary(
        context: Context,
        desiredHeight: Int,
        exitView: MapboxExitText,
        maneuver: PrimaryManeuver,
        roadShields: Set<RoadShield>? = null
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
                        instructionBuilder
                    )
                }
                is RoadShieldComponentNode -> {
                    addShieldToBuilder(
                        node.text,
                        desiredHeight,
                        context.resources,
                        getShieldToRender(node, roadShields),
                        instructionBuilder
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
        roadShields: Set<RoadShield>? = null
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
                        instructionBuilder
                    )
                }
                is RoadShieldComponentNode -> {
                    addShieldToBuilder(
                        node.text,
                        desiredHeight,
                        context.resources,
                        getShieldToRender(node, roadShields),
                        instructionBuilder
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
        roadShields: Set<RoadShield>? = null
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
                        instructionBuilder
                    )
                }
                is RoadShieldComponentNode -> {
                    addShieldToBuilder(
                        node.text,
                        desiredHeight,
                        context.resources,
                        getShieldToRender(node, roadShields),
                        instructionBuilder
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
        builder: SpannableStringBuilder
    ) {
        val exitBuilder = ExitStyleGenerator.styleAndGetExit(
            exitText,
            exitView,
            desiredHeight,
            resources
        )
        builder.append(exitBuilder)
        builder.append(" ")
    }

    private fun addShieldToBuilder(
        shieldText: String,
        desiredHeight: Int,
        resources: Resources,
        roadShield: RoadShield?,
        builder: SpannableStringBuilder
    ) {
        val roadShieldBuilder = RoadShieldGenerator.styleAndGetRoadShield(
            shieldText,
            desiredHeight,
            resources,
            roadShield
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
        roadShields: Set<RoadShield>?
    ): RoadShield? {
        return roadShields?.find {
            ifNonNull(node.mapboxShield) { mbxShield ->
                val shieldName = mbxShield.name()
                val displayRefLength = mbxShield.getRefLen()
                val shieldRequested = shieldName.plus("-$displayRefLength")
                it.shieldUrl.contains(shieldRequested)
            } ?: false
        } ?: roadShields?.find {
            it.shieldUrl == node.shieldUrl
        }
    }
}
