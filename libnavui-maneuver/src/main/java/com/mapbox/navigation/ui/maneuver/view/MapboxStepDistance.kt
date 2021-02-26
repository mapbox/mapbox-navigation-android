package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.ui.maneuver.model.StepDistance

/**
 * Default view to render distance onto [MapboxManeuverView] and single
 * item in [MapboxUpcomingManeuverAdapter].
 * It can be directly used in any other layout.
 * @constructor
 */
class MapboxStepDistance @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    /**
     * Render distance remaining to finish step or total step distance based on [StepDistance].
     */
    fun render(stepDistance: StepDistance) {
        text = stepDistance.distanceFormatter.formatDistance(stepDistance.distance)
    }
}
