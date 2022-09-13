package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.model.ManeuverInstructionGenerator
import com.mapbox.navigation.ui.maneuver.model.ManeuverPrimaryOptions
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.toRouteShield
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Default view to render primary banner instructions onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property attrs AttributeSet
 * @property defStyleAttr Int
 */
class MapboxPrimaryManeuver : AppCompatTextView {

    private var options = ManeuverPrimaryOptions.Builder().build()

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
        R.style.MapboxStylePrimaryManeuver
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
        options: ManeuverPrimaryOptions = ManeuverPrimaryOptions.Builder().build()
    ) : super(context, attrs, defStyleAttr) {
        this.options = options
    }

    /**
     * Invoke the method to render primary maneuver instructions
     * @param maneuver PrimaryManeuver
     */
    @Deprecated(
        message = "The method may or may not render multiple shields for a given instruction",
        replaceWith = ReplaceWith("renderManeuver(maneuver, roadShields)")
    )
    @JvmOverloads
    fun render(maneuver: PrimaryManeuver, roadShield: RoadShield? = null) {
        val roadShields = ifNonNull(roadShield) {
            setOf(it.toRouteShield())
        }
        renderManeuver(maneuver, roadShields)
    }

    /**
     * Invoke the method to render primary maneuver instructions
     * @param maneuver PrimaryManeuver
     */
    fun renderManeuver(maneuver: PrimaryManeuver, routeShields: Set<RouteShield>?) {
        val exitView = MapboxExitText(context)
        exitView.updateTextAppearance(options.exitOptions.textAppearance)
        // TODO: write when to check the type and pass MUTCD or VIENNA when the data is available
        exitView.updateExitProperties(options.exitOptions.mutcdExitProperties)
        val instruction = ManeuverInstructionGenerator.generatePrimary(
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
     * Invoke method to change the styling of [MapboxPrimaryManeuver] at runtime
     *
     * @param options to apply
     */
    fun updateOptions(options: ManeuverPrimaryOptions) {
        this.options = options
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun getOptions() = options
}
