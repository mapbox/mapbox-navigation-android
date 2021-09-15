package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.api.MapboxTurnIconsApi
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.TurnIconResources
import com.mapbox.navigation.ui.utils.internal.ifNonNull

/**
 * Default view to render the maneuver turn icon onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property turnIconResources TurnIconResources
 * @property turnIconsApi MapboxTurnIconsApi
 * @constructor
 */
class MapboxTurnIconManeuver @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private val TAG = MapboxTurnIconManeuver::class.java.simpleName
    }

    private var contextThemeWrapper: ContextThemeWrapper =
        ContextThemeWrapper(context, R.style.MapboxStyleTurnIconManeuver)
    private var turnIconResources = TurnIconResources.Builder().build()
    private val turnIconsApi = MapboxTurnIconsApi(turnIconResources)

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
        turnIconsApi.updateResources(turnIcon)
    }

    /**
     * Invoke to render a turn icon based on a [PrimaryManeuver].
     */
    fun renderPrimaryTurnIcon(maneuver: PrimaryManeuver) {
        turnIconsApi.generateTurnIcon(
            maneuver.type, maneuver.degrees?.toFloat(), maneuver.modifier, maneuver.drivingSide
        ).fold(
            {
                Log.e(TAG, it.errorMessage)
            },
            {
                renderIcon(it)
            }
        )
    }

    /**
     * Invoke to render a turn icon based on a [SubManeuver].
     */
    fun renderSubTurnIcon(maneuver: SubManeuver?) {
        ifNonNull(maneuver) { m ->
            turnIconsApi.generateTurnIcon(
                m.type, m.degrees?.toFloat(), m.modifier, m.drivingSide
            ).fold(
                {
                    Log.e(TAG, it.errorMessage)
                },
                {
                    renderIcon(it)
                }
            )
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
