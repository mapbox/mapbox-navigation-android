package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.model.ManeuverInstructionGenerator
import com.mapbox.navigation.ui.maneuver.model.ManeuverSubOptions
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.toRouteShield
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Default view to render sub banner instructions onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property attrs AttributeSet
 * @property defStyleAttr Int
 */
class MapboxSubManeuver : AppCompatTextView {

    private var options = ManeuverSubOptions.Builder().build()

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
        R.style.MapboxStyleSubManeuver
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
        options: ManeuverSubOptions = ManeuverSubOptions.Builder().build()
    ) : super(context, attrs, defStyleAttr) {
        this.options = options
    }

    /**
     * Invoke the method to render sub maneuver instructions
     * @param maneuver SubManeuver
     */
    @Deprecated(
        message = "The method may or may not render multiple shields for a given instruction",
        replaceWith = ReplaceWith("renderManeuver(maneuver, roadShields)")
    )
    @JvmOverloads
    fun render(maneuver: SubManeuver?, roadShield: RoadShield? = null) {
        val roadShields = ifNonNull(roadShield) {
            setOf(it.toRouteShield())
        }
        renderManeuver(maneuver, roadShields)
    }

    /**
     * Invoke the method to render sub maneuver instructions
     * @param maneuver SubManeuver
     */
    fun renderManeuver(maneuver: SubManeuver?, routeShields: Set<RouteShield>?) {
        val exitView = MapboxExitText(context)
        exitView.updateTextAppearance(options.exitOptions.textAppearance)
        // TODO: write when to check the type and pass MUTCD or VIENNA when the data is available
        exitView.updateExitProperties(options.exitOptions.mutcdExitProperties)
        val instruction = ManeuverInstructionGenerator.generateSub(
            context,
            lineHeight,
            exitView,
            maneuver,
            routeShields
        )
        text = instruction
    }

    /**
     * Invoke method to change the styling of [MapboxSubManeuver] at runtime
     *
     * @param options to apply
     */
    fun updateOptions(options: ManeuverSubOptions) {
        this.options = options
    }
}
