package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.tripdata.maneuver.model.StepDistance

/**
 * Default view to render distance onto [MapboxManeuverView] and single
 * item in [MapboxUpcomingManeuverAdapter].
 * It can be directly used in any other layout.
 */
@UiThread
class MapboxStepDistance : AppCompatTextView {

    /**
     *
     * @param context Context
     * @constructor
     */
    constructor(context: Context) : super(context)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @param defStyleAttr Int
     * @constructor
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    /**
     * Render distance remaining to finish step based on [StepDistance].
     */
    fun renderDistanceRemaining(stepDistance: StepDistance) {
        stepDistance.distanceRemaining?.let { distance ->
            text = stepDistance.distanceFormatter.formatDistance(distance)
        }
    }

    /**
     * Render total step distance based on [StepDistance].
     */
    fun renderTotalStepDistance(stepDistance: StepDistance) {
        text = stepDistance.distanceFormatter.formatDistance(stepDistance.totalDistance)
    }
}
