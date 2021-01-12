package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.navigation.ui.base.model.maneuver.LaneIndicator
import com.mapbox.navigation.ui.maneuver.LaneIconHelper
import com.mapbox.navigation.ui.utils.internal.ifNonNull

/**
 * Default Lane Guidance View that renders the maneuver icons onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property laneIconHelper LaneIconHelper
 * @constructor
 */
class MapboxLaneGuidance @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val laneIconHelper = LaneIconHelper()

    /**
     * Invoke the method to render the turn icon given a [LaneIndicator] and the [activeDirection]
     * pointing towards the lane to take.
     * @param laneIndicator LaneIndicator
     * @param activeDirection String?
     */
    fun renderLane(
        laneIndicator: LaneIndicator,
        activeDirection: String?,
        wrapper: ContextThemeWrapper
    ) {
        val laneIcon = laneIconHelper.retrieveLaneToDraw(laneIndicator, activeDirection)
        ifNonNull(laneIcon) { icon ->
            val drawable = VectorDrawableCompat.create(
                context.resources,
                icon,
                wrapper.theme
            )
            setImageDrawable(drawable)
            alpha = if (laneIndicator.isActive) {
                1.0f
            } else {
                0.5f
            }
        }
    }
}
