package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.model.ManeuverInstructionGenerator
import com.mapbox.navigation.ui.maneuver.model.ManeuverSecondaryOptions
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.toRouteShield
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Default view to render secondary banner instructions onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property attrs AttributeSet
 * @property defStyleAttr Int
 */
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
        R.style.MapboxStyleSecondaryManeuver
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
        options: ManeuverSecondaryOptions = ManeuverSecondaryOptions.Builder().build()
    ) : super(context, attrs, defStyleAttr) {
        this.options = options
    }

    /**
     * Invoke the method to render secondary maneuver instructions
     * @param maneuver SecondaryManeuver
     */
    @Deprecated(
        message = "The method may or may not render multiple shields for a given instruction",
        replaceWith = ReplaceWith("renderManeuver(maneuver, roadShields)")
    )
    @JvmOverloads
    fun render(maneuver: SecondaryManeuver?, roadShield: RoadShield? = null) {
        val roadShields = ifNonNull(roadShield) {
            setOf(it.toRouteShield())
        }
        renderManeuver(maneuver, roadShields)
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
            routeShields
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
}
