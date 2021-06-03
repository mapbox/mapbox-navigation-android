package com.mapbox.navigation.ui.maneuver.model

import android.content.res.Resources
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigation.ui.utils.internal.extensions.drawableWithHeight
import java.io.ByteArrayInputStream

internal object RoadShieldGenerator {

    fun styleAndGetRoadShield(
        shieldText: String,
        desiredHeight: Int,
        resources: Resources,
        roadShield: RoadShield? = null
    ): SpannableStringBuilder {
        val roadShieldBuilder = SpannableStringBuilder(shieldText)
        val shieldIcon = roadShield?.shieldIcon
        if (shieldIcon != null && shieldIcon.isNotEmpty()) {
            val stream = ByteArrayInputStream(shieldIcon)
            val svgBitmap = SvgUtil.renderAsBitmapWithHeight(stream, desiredHeight)
            svgBitmap?.let { b ->
                val drawable = b.drawableWithHeight(desiredHeight, resources)
                roadShieldBuilder.setSpan(
                    ImageSpan(drawable),
                    0,
                    shieldText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return roadShieldBuilder
    }
}
