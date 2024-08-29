package com.mapbox.navigation.tripdata.maneuver.model

import androidx.annotation.DrawableRes

/**
 * Data structure that holds a styleable drawable resource.
 *
 * Within your style, specify the attributes to color the resource.
 * maneuverTurnIconColor. For active lanes
 * maneuverTurnIconShadowColor. For inactive lanes
 *
 * @param drawableResId Int
 * @param shouldFlip Boolean is true when the icon needs to be flipped to indicate leftward turns
 * else false for rightward turns.
 */
class LaneIcon internal constructor(@DrawableRes val drawableResId: Int, val shouldFlip: Boolean) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LaneIcon

        if (drawableResId != other.drawableResId) return false
        if (shouldFlip != other.shouldFlip) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = drawableResId
        result = 31 * result + shouldFlip.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "LaneIcon(drawableResId=$drawableResId, shouldFlip=$shouldFlip)"
    }
}
