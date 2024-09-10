package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.navigation.tripdata.maneuver.model.LaneIcon

/**
 * Default Lane Guidance View that renders the maneuver icons onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @constructor
 */
@UiThread
class MapboxLaneGuidance @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /**
     * Invoke the method to render the turn icon given a [LaneIcon] pointing towards the lane to take.
     * @param laneIcon LaneIcon
     * @param wrapper ContextThemeWrapper
     */
    fun renderLane(laneIcon: LaneIcon, wrapper: ContextThemeWrapper) {
        rotationY = if (laneIcon.shouldFlip) {
            180f
        } else {
            0f
        }
        val drawable = VectorDrawableCompat.create(
            context.resources,
            laneIcon.drawableResId,
            wrapper.theme,
        )
        setImageDrawable(drawable)
    }
}
