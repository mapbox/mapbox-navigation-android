package com.mapbox.navigation.tripdata.maneuver.internal

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import com.mapbox.navigation.tripdata.maneuver.model.LaneIcon

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object LaneIconFactory {

    @JvmSynthetic
    fun createLaneIcon(
        @DrawableRes drawableResId: Int,
        shouldFlip: Boolean,
    ) = LaneIcon(
        drawableResId = drawableResId,
        shouldFlip = shouldFlip,
    )
}
