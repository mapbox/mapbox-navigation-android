package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.base.model.maneuver.PrimaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.SubManeuver
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.TurnIconHelper
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
) : MapboxView<ManeuverState>, AppCompatImageView(context, attrs, defStyleAttr) {

    private var contextThemeWrapper: ContextThemeWrapper =
        ContextThemeWrapper(context, R.style.MapboxStyleTurnIconManeuver)
    private var turnIconResources = TurnIconResources.Builder().build()
    private var turnIconHelper = TurnIconHelper(turnIconResources)

    /**
     * Entry point for [MapboxTurnIconManeuver] to render itself based on a [ManeuverState].
     */
    override fun render(state: ManeuverState) {
        when (state) {
            is ManeuverState.ManeuverPrimary.Instruction -> {
                renderPrimaryTurnIcon(state.maneuver)
            }
            is ManeuverState.ManeuverSub.Instruction -> {
                renderSubTurnIcon(state.maneuver)
            }
            is ManeuverState.ManeuverSub.Hide -> {
                updateVisibility(GONE)
            }
            is ManeuverState.ManeuverSub.Show -> {
                updateVisibility(VISIBLE)
            }
        }
    }

    /**
     * Invoke the method if there is a need to use other turn icon drawables than the default icons
     * supplied.
     * @param turnIcon TurnIconResources
     */
    fun updateTurnIconResources(turnIcon: TurnIconResources) {
        this.turnIconResources = turnIcon
        this.turnIconHelper = TurnIconHelper(turnIconResources)
    }

    /**
     * Invoke to change the styling of [MapboxTurnIconManeuver]
     * @param wrapper ContextThemeWrapper
     */
    fun updateTurnIconStyle(wrapper: ContextThemeWrapper) {
        this.contextThemeWrapper = wrapper
    }

    private fun renderPrimaryTurnIcon(maneuver: PrimaryManeuver) {
        val turnIcon = turnIconHelper.retrieveTurnIcon(
            maneuver.type, maneuver.degrees?.toFloat(), maneuver.modifier, maneuver.drivingSide
        )
        turnIcon?.let {
            ifNonNull(it.icon) { icon ->
                if (it.shouldFlipIcon) {
                    rotationY = 180f
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

    private fun renderSubTurnIcon(maneuver: SubManeuver?) {
        ifNonNull(maneuver) { m ->
            val turnIcon = turnIconHelper.retrieveTurnIcon(
                m.type, m.degrees?.toFloat(), m.modifier, m.drivingSide
            )
            turnIcon?.let {
                ifNonNull(it.icon) { icon ->
                    if (it.shouldFlipIcon) {
                        rotationY = 180f
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

    private fun updateVisibility(visibility: Int) {
        this.visibility = visibility
    }
}
