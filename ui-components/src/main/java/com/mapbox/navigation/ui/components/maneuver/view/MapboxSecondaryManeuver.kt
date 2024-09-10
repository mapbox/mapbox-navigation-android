package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverInstructionGenerator
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverSecondaryOptions

/**
 * Default view to render secondary banner instructions onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property attrs AttributeSet
 * @property defStyleAttr Int
 */
@UiThread
class MapboxSecondaryManeuver : AppCompatTextView {

    private var options = ManeuverSecondaryOptions.Builder().build()

    /**
     *
     * @param context Context
     * @constructor
     */
    constructor(context: Context) : this(context, null)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.style.MapboxStyleSecondaryManeuver,
    )

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
        options: ManeuverSecondaryOptions = ManeuverSecondaryOptions.Builder().build(),
    ) : super(context, attrs, defStyleAttr) {
        this.options = options
    }

    /**
     * Invoke the method to render secondary maneuver instructions
     * @param maneuver SecondaryManeuver
     */
    fun renderManeuver(maneuver: SecondaryManeuver?, routeShields: Set<RouteShield>?) {
        val exitView = MapboxExitText(context)
        exitView.updateTextAppearance(options.exitOptions.textAppearance)
        // TODO: write when to check the type and pass MUTCD or VIENNA when the data is available
        exitView.updateExitProperties(options.exitOptions.mutcdExitProperties)
        val instruction = ManeuverInstructionGenerator.generateSecondary(
            context,
            lineHeight,
            exitView,
            maneuver,
            routeShields,
        )
        if (instruction.isNotEmpty()) {
            text = instruction
        }
    }

    /**
     * Invoke method to change the styling of [MapboxSecondaryManeuver] at runtime
     *
     * @param options to apply
     */
    fun updateOptions(options: ManeuverSecondaryOptions) {
        this.options = options
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun getOptions() = options
}
