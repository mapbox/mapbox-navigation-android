package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.model.ManeuverInstructionGenerator
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield

/**
 * Default view to render primary banner instructions onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property attrs AttributeSet
 * @property defStyleAttr Int
 */
class MapboxPrimaryManeuver : AppCompatTextView {

    private var defStyleAttr = 0
    private var attrs: AttributeSet? = null

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
    ) : super(context, attrs, defStyleAttr) {
        this.attrs = attrs
        this.defStyleAttr = defStyleAttr
    }

    private var leftDrawable = ContextCompat.getDrawable(
        context, R.drawable.mapbox_ic_exit_arrow_left
    )
    private var rightDrawable = ContextCompat.getDrawable(
        context, R.drawable.mapbox_ic_exit_arrow_right
    )
    private var exitBackground = ContextCompat.getDrawable(
        context, R.drawable.mapbox_exit_board_background
    )

    /**
     * Invoke the method to render primary maneuver instructions
     * @param maneuver PrimaryManeuver
     */
    @JvmOverloads
    fun render(maneuver: PrimaryManeuver, roadShield: RoadShield? = null) {
        val exitView = MapboxExitText(context, attrs, defStyleAttr)
        exitView.setExitStyle(exitBackground, leftDrawable, rightDrawable)
        val instruction = ManeuverInstructionGenerator.generatePrimary(
            context,
            lineHeight,
            exitView,
            maneuver,
            roadShield
        )
        if (instruction.isNotEmpty()) {
            text = instruction
        }
    }
}
