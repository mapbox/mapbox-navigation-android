package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.model.ManeuverInstructionGenerator
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver

/**
 * Default view to render primary banner instructions onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property attrs AttributeSet
 * @property defStyleAttr Int
 */
class MapboxPrimaryManeuver @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

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
     * Invoke the method to render sub instructions
     * @param maneuver PrimaryManeuver
     */
    fun render(maneuver: PrimaryManeuver) {
        val exitView = MapboxExitText(context, attrs, defStyleAttr)
        exitView.setExitStyle(exitBackground, leftDrawable, rightDrawable)
        val instruction = ManeuverInstructionGenerator.generatePrimary(
            context,
            lineHeight,
            exitView,
            maneuver
        )
        if (instruction.isNotEmpty()) {
            text = instruction
        }
    }
}
