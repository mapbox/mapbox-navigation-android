package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.tripdata.maneuver.api.MapboxTurnIconsApi
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.maneuver.model.TurnIconResources
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE

/**
 * Default view to render the maneuver turn icon onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property turnIconResources TurnIconResources
 * @property turnIconsApi MapboxTurnIconsApi
 * @constructor
 */
@UiThread
class MapboxTurnIconManeuver @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private companion object {
        private val TAG = MapboxTurnIconManeuver::class.java.simpleName
    }

    private var contextThemeWrapper: ContextThemeWrapper =
        ContextThemeWrapper(context, R.style.MapboxStyleTurnIconManeuver)
    private var turnIconResources = TurnIconResources.defaultIconSet()
    private val turnIconsApi = MapboxTurnIconsApi(turnIconResources)

    /**
     * Invoke to change the styling of [MapboxTurnIconManeuver]
     * @param wrapper ContextThemeWrapper
     */
    fun updateTurnIconStyle(wrapper: ContextThemeWrapper) {
        this.contextThemeWrapper = wrapper
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun getTurnIconTheme(): ContextThemeWrapper {
        return contextThemeWrapper
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
            maneuver.type,
            maneuver.degrees?.toFloat(),
            maneuver.modifier,
            maneuver.drivingSide,
        ).fold(
            {
                logE(TAG) { it.errorMessage }
            },
            {
                renderIcon(it)
            },
        )
    }

    /**
     * Invoke to render a turn icon based on a [SubManeuver].
     */
    fun renderSubTurnIcon(maneuver: SubManeuver?) {
        ifNonNull(maneuver) { m ->
            turnIconsApi.generateTurnIcon(
                m.type,
                m.degrees?.toFloat(),
                m.modifier,
                m.drivingSide,
            ).fold(
                {
                    logE(TAG) { it.errorMessage }
                },
                {
                    renderIcon(it)
                },
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
                    contextThemeWrapper.theme,
                )
                setImageDrawable(drawable)
            }
        }
    }
}
