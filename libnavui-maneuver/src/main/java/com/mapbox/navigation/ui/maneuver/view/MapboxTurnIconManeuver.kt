package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.TurnIconHelper
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.TurnIconResources
import com.mapbox.navigation.ui.utils.internal.ifNonNull

/**
 * Default view to render the maneuver turn icon onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property turnIconResources TurnIconResources
 * @property turnIconHelper TurnIconHelper
 * @constructor
 */
class MapboxTurnIconManeuver @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var contextThemeWrapper: ContextThemeWrapper =
        ContextThemeWrapper(context, R.style.MapboxStyleTurnIconManeuver)
    private var turnIconResources = TurnIconResources.Builder().build()
    private var turnIconHelper = TurnIconHelper(turnIconResources)

    /**
     * Invoke to change the styling of [MapboxTurnIconManeuver]
     * @param wrapper ContextThemeWrapper
     */
    fun updateTurnIconStyle(wrapper: ContextThemeWrapper) {
        this.contextThemeWrapper = wrapper
    }

    /**
     * Invoke the method if there is a need to use other turn icon drawables than the default icons
     * supplied.
     * @param turnIcon TurnIconResources
     * Invoke to render a turn icon based on a [SubManeuver].
     */
    fun updateTurnIconResources(turnIcon: TurnIconResources) {
        this.turnIconResources = turnIcon
        this.turnIconHelper = TurnIconHelper(turnIconResources)
    }

    /**
     * Invoke to render a turn icon based on a [PrimaryManeuver].
     */
    fun renderPrimaryTurnIcon(maneuver: PrimaryManeuver) {
        val turnIcon = turnIconHelper.retrieveTurnIcon(
            maneuver.type, maneuver.degrees?.toFloat(), maneuver.modifier, maneuver.drivingSide
        )
        renderIcon(turnIcon)
    }

    /**
     * Invoke to render a turn icon based on a [SubManeuver].
     */
    fun renderSubTurnIcon(maneuver: SubManeuver?) {
        ifNonNull(maneuver) { m ->
            val turnIcon = turnIconHelper.retrieveTurnIcon(
                m.type, m.degrees?.toFloat(), m.modifier, m.drivingSide
            )
            renderIcon(turnIcon)
        } ?: setImageDrawable(null)
    }

    private fun renderIcon(turnIcon: ManeuverTurnIcon?) {
        turnIcon?.let {
            ifNonNull(it.icon) { icon ->
                rotationY = if (it.shouldFlipIcon) {
                    180f
                } else {
                    0f
                }
                val drawable = VectorDrawableCompat.create(
                    context.resources,
                    icon,
                    contextThemeWrapper.theme
                )
                setImageDrawable(drawable)
            }
        }
    }
}
