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
        maneuver: PrimaryManeuver
    ): SpannableStringBuilder {
        val instructionBuilder = SpannableStringBuilder()
        maneuver.componentList.forEach { component ->
            val node = component.node
            when (node) {
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
                        node.shieldIcon,
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
        maneuver: SecondaryManeuver?
    ): SpannableStringBuilder {
        val instructionBuilder = SpannableStringBuilder()
        maneuver?.componentList?.forEach { component ->
            val node = component.node
            when (node) {
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
                        node.shieldIcon,
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
        maneuver: SubManeuver?
    ): SpannableStringBuilder {
        val instructionBuilder = SpannableStringBuilder()
        maneuver?.componentList?.forEach { component ->
            val node = component.node
            when (node) {
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
                        node.shieldIcon,
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
        shieldIcon: ByteArray?,
        builder: SpannableStringBuilder
    ) {
        val roadShieldBuilder = RoadShieldGenerator.styleAndGetRoadShield(
            shieldText,
            desiredHeight,
            resources,
            shieldIcon
        )
        builder.append(roadShieldBuilder)
        builder.append(" ")
    }

    private fun addDelimiterToBuilder(text: String, builder: SpannableStringBuilder) {
        builder.append(text)
        builder.append(" ")
    }
}
