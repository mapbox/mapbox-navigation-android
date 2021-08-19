package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.ui.maneuver.api.MapboxLaneIconsApi
import com.mapbox.navigation.ui.maneuver.model.LaneIcon
import com.mapbox.navigation.ui.maneuver.model.LaneIndicator
import com.mapbox.navigation.utils.internal.LoggerProvider

/**
 * Default Lane Guidance View that renders the maneuver icons onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property laneIconsApi MapboxLaneIconsApi
 * @constructor
 */
class MapboxLaneGuidance @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val laneIconsApi = MapboxLaneIconsApi()

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
        val laneIconExpected = laneIconsApi.laneIcon(laneIndicator, activeDirection)
        laneIconExpected.fold(
            { error ->
                LoggerProvider.logger.i(Tag("MapboxLaneGuidance"), Message(error.errorMessage))
            },
            { laneIcon ->
                renderIcon(laneIcon, laneIndicator.isActive, wrapper)
            }
        )
    }

    private fun renderIcon(laneIcon: LaneIcon, isActive: Boolean, wrapper: ContextThemeWrapper) {
        val drawable = VectorDrawableCompat.create(
            context.resources,
            laneIcon.drawableResId,
            wrapper.theme
        )
        setImageDrawable(drawable)
        alpha = if (isActive) {
            1.0f
        } else {
            0.5f
        }
    }
}
