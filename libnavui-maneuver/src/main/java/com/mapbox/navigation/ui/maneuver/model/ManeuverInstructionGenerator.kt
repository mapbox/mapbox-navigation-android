package com.mapbox.navigation.ui.maneuver.model

import android.content.Context
import android.content.res.Resources
import android.text.SpannableStringBuilder
import com.mapbox.navigation.ui.maneuver.view.MapboxExitText

internal object ManeuverInstructionGenerator {

    fun generatePrimary(
        context: Context,
        desiredHeight: Int,
        exitView: MapboxExitText,
        maneuver: PrimaryManeuver,
        roadShields: List<RoadShield>? = null
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
                    val shield = roadShields?.find {
                        it.mapboxShield == node.mapboxShield
                    } ?: roadShields?.find {
                        it.shieldUrl == node.shieldUrl
                    }
                    addShieldToBuilder(
                        node.text,
                        desiredHeight,
                        context.resources,
                        shield,
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
        roadShields: List<RoadShield>? = null
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
                    val shield = roadShields?.find {
                        it.mapboxShield == node.mapboxShield
                    } ?: roadShields?.find {
                        it.shieldUrl == node.shieldUrl
                    }
                    addShieldToBuilder(
                        node.text,
                        desiredHeight,
                        context.resources,
                        shield,
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
        roadShields: List<RoadShield>? = null
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
                    val shield = roadShields?.find {
                        it.mapboxShield == node.mapboxShield
                    } ?: roadShields?.find {
                        it.shieldUrl == node.shieldUrl
                    }
                    addShieldToBuilder(
                        node.text,
                        desiredHeight,
                        context.resources,
                        shield,
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
}
