package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState

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
) : MapboxView<ManeuverState>, AppCompatTextView(context, attrs, defStyleAttr) {

    /**
     * Entry point for [MapboxStepDistance] to render itself based on a [ManeuverState].
     */
    override fun render(state: ManeuverState) {
        when (state) {
            is ManeuverState.DistanceRemainingToFinishStep -> {
                renderDistanceRemaining(state)
            }
            is ManeuverState.TotalStepDistance -> {
                renderTotalStepDistance(state)
            }
        }
    }

    private fun renderDistanceRemaining(state: ManeuverState.DistanceRemainingToFinishStep) {
        text = state.distanceFormatter.formatDistance(state.distanceRemaining)
    }

    private fun renderTotalStepDistance(state: ManeuverState.TotalStepDistance) {
        text = state.distanceFormatter.formatDistance(state.totalStepDistance)
    }
}
