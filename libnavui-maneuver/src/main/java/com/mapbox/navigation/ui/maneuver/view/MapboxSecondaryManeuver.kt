package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.ui.maneuver.model.ManeuverInstructionGenerator
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Default view to render secondary banner instructions onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property attrs AttributeSet
 * @property defStyleAttr Int
 */
class MapboxSecondaryManeuver : AppCompatTextView {

    /**
     *
     * @param context Context
     * @constructor
     */
    constructor(context: Context) : super(context)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

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
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

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
            setOf(it)
        }
        renderManeuver(maneuver, roadShields)
    }

    /**
     * Invoke the method to render secondary maneuver instructions
     * @param maneuver SecondaryManeuver
     */
    fun renderManeuver(maneuver: SecondaryManeuver?, roadShields: Set<RoadShield>?) {
        val exitView = MapboxExitText(context)
        // TODO: write when to check the type and pass MUTCD or VIENNA when the data is available
        exitView.setExitProperties(maneuver?.exitMutcdProperties)
        val instruction = ManeuverInstructionGenerator.generateSecondary(
            context,
            lineHeight,
            exitView,
            maneuver,
            roadShields
        )
        if (instruction.isNotEmpty()) {
            text = instruction
        }
    }
}
