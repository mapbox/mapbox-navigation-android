package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarIconSpan
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.DelimiterComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.TextComponentNode
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.androidauto.ui.maneuver.view.MapboxExitText
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import java.io.ByteArrayInputStream

/**
 * Create instructions from the Mapbox navigation maneuvers.
 */
class CarManeuverInstructionRenderer {

    fun renderInstruction(
        maneuver: List<Component>,
        shields: List<RouteShield>,
        exitView: MapboxExitText,
        modifier: String?,
        desiredHeight: Int,
    ): CharSequence {
        val instructionBuilder = SpannableStringBuilder()
        for (component in maneuver) {
            instructionBuilder.append(component, shields, exitView, modifier, desiredHeight)
        }
        return instructionBuilder
    }

    private fun Appendable.append(
        component: Component,
        shields: List<RouteShield>,
        exitView: MapboxExitText,
        modifier: String?,
        desiredHeight: Int,
    ): Appendable {
        when (val node = component.node) {
            is TextComponentNode -> {
                append(node.text).append(" ")
            }
            is ExitNumberComponentNode -> {
                append(getRenderedExit(node, exitView, modifier)).append(" ")
            }
            is RoadShieldComponentNode -> {
                append(
                    getRenderedShield(
                        node.text,
                        desiredHeight,
                        getShieldToRender(node, shields),
                    ),
                ).append(" ")
            }
            is DelimiterComponentNode -> {
                append(node.text).append(" ")
            }
        }
        return this
    }

    private fun getRenderedExit(
        node: ExitNumberComponentNode,
        exitView: MapboxExitText,
        modifier: String?,
    ): CharSequence {
        exitView.setExit(modifier, node)
        val exitBuilder = SpannableStringBuilder(node.text)
        val exitBitmap = exitView.getViewAsBitmap()
        val icon = CarIcon.Builder(IconCompat.createWithBitmap(exitBitmap)).build()
        val carIconSpan = CarIconSpan.create(icon, CarIconSpan.ALIGN_CENTER)
        exitBuilder.setSpan(carIconSpan, 0, node.text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return exitBuilder
    }

    private fun getShieldToRender(
        node: RoadShieldComponentNode,
        roadShields: List<RouteShield>,
    ): RouteShield? {
        return node.mapboxShield?.let { shield ->
            roadShields.find { it is RouteShield.MapboxDesignedShield && it.compareWith(shield) }
        } ?: node.shieldUrl?.let { shieldUrl ->
            roadShields.find { it is RouteShield.MapboxLegacyShield && it.compareWith(shieldUrl) }
        }
    }

    private fun getRenderedShield(
        shieldText: String,
        desiredHeight: Int,
        shield: RouteShield?,
    ): CharSequence {
        val roadShieldBuilder = SpannableStringBuilder(shieldText)
        val shieldIcon = shield?.byteArray
        if (shieldIcon != null && shieldIcon.isNotEmpty()) {
            val stream = ByteArrayInputStream(shieldIcon)
            SvgUtil.renderAsBitmapWithHeight(stream, desiredHeight)?.let { svgBitmap ->
                val icon = CarIcon.Builder(IconCompat.createWithBitmap(svgBitmap)).build()
                val carIconSpan = CarIconSpan.create(icon, CarIconSpan.ALIGN_CENTER)
                roadShieldBuilder.setSpan(
                    carIconSpan,
                    0,
                    shieldText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        return roadShieldBuilder
    }
}
