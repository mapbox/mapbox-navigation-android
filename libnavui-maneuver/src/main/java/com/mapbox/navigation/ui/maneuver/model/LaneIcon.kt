package com.mapbox.navigation.ui.maneuver.model

import androidx.annotation.DrawableRes

/**
 * Data structure that holds a styleable drawable resource.
 *
 * Within your style, specify the attributes to color the resource.
 *   maneuverTurnIconColor. For active lanes
 *   maneuverTurnIconShadowColor. For inactive lanes
 *
 * @param drawableResId Int
 */
class LaneIcon internal constructor(
    @DrawableRes
    val drawableResId: Int
) {
    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LaneIcon

        if (drawableResId != other.drawableResId) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return drawableResId
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "LaneIcon(drawableResId=$drawableResId)"
    }
}
