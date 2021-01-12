package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.model.TurnIconResources
import kotlinx.android.synthetic.main.mapbox_layout_main_maneuver_view.view.*
import kotlinx.android.synthetic.main.mapbox_layout_sub_maneuver_view.view.*
import kotlinx.android.synthetic.main.mapbox_maneuver_view.view.*

/**
 * Default view to render a maneuver.
 * @property laneGuidanceAdapter MapboxLaneGuidanceAdapter
 * @property upcomingManeuverAdapter MapboxUpcomingManeuverAdapter
 * @constructor
 */
class MapboxManeuverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MapboxView<ManeuverState>, ConstraintLayout(context, attrs, defStyleAttr) {

    private val laneGuidanceAdapter = MapboxLaneGuidanceAdapter(context)
    private val upcomingManeuverAdapter = MapboxUpcomingManeuverAdapter(context)

    /**
     * Initialize.
     */
    init {
        inflate(context, R.layout.mapbox_maneuver_view, this)

        laneGuidanceRecycler.apply {
            layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
            adapter = laneGuidanceAdapter
        }

        upcomingManeuverRecycler.apply {
            layoutManager = LinearLayoutManager(context, VERTICAL, false)
            adapter = upcomingManeuverAdapter
        }

        initAttributes(attrs)
        this.setOnClickListener {
            if (upcomingManeuverRecycler.visibility == GONE) {
                render(ManeuverState.UpcomingManeuvers.Show)
            } else {
                render(ManeuverState.UpcomingManeuvers.Hide)
            }
        }
    }

    /**
     * Entry point for [MapboxManeuverView] to render itself based on a [ManeuverState].
     * @param state ManeuverState
     */
    override fun render(state: ManeuverState) {
        when (state) {
            is ManeuverState.ManeuverPrimary.Instruction -> {
                renderPrimaryManeuver(state)
                renderPrimaryTurnIcon(state)
            }
            is ManeuverState.ManeuverSecondary.Instruction -> {
                renderSecondaryManeuver(state)
            }
            is ManeuverState.ManeuverSecondary.Show -> {
                showSecondaryManeuver()
            }
            is ManeuverState.ManeuverSecondary.Hide -> {
                hideSecondaryManeuver()
            }
            is ManeuverState.ManeuverSub.Instruction -> {
                renderSubManeuver(state)
                renderSubTurnIcon(state)
            }
            is ManeuverState.ManeuverSub.Show -> {
                renderSubManeuverVisibility(VISIBLE)
            }
            is ManeuverState.ManeuverSub.Hide -> {
                renderSubManeuverVisibility(GONE)
            }
            is ManeuverState.LaneGuidanceManeuver.AddLanes -> {
                renderAddLanes(state)
            }
            is ManeuverState.LaneGuidanceManeuver.RemoveLanes -> {
                renderRemoveLanes(state)
            }
            is ManeuverState.LaneGuidanceManeuver.Show -> {
                renderLaneGuidanceVisibility(VISIBLE)
            }
            is ManeuverState.LaneGuidanceManeuver.Hide -> {
                renderLaneGuidanceVisibility(GONE)
            }
            is ManeuverState.DistanceRemainingToFinishStep -> {
                renderDistanceRemaining(state)
            }
            is ManeuverState.UpcomingManeuvers.Upcoming -> {
                renderUpcomingManeuvers(state)
            }
            is ManeuverState.UpcomingManeuvers.Show -> {
                renderUpcomingManeuverVisibility(VISIBLE)
            }
            is ManeuverState.UpcomingManeuvers.Hide -> {
                renderUpcomingManeuverVisibility(GONE)
            }
            is ManeuverState.UpcomingManeuvers.RemoveUpcoming -> {
                removeUpcomingManeuver(state)
            }
        }
    }

    /**
     * Invoke the method if there is a need to use other turn icon drawables than the default icons
     * supplied.
     * @param turnIconResources TurnIconResources
     */
    fun updateTurnIconResources(turnIconResources: TurnIconResources) {
        maneuverIcon.updateTurnIconResources(turnIconResources)
        subManeuverIcon.updateTurnIconResources(turnIconResources)
    }

    /**
     * Allows you to change the style of turn icon in main and sub maneuver view.
     * @param style Int
     */
    fun updateTurnIconStyle(@StyleRes style: Int) {
        maneuverIcon.updateTurnIconStyle(ContextThemeWrapper(context, style))
        subManeuverIcon.updateTurnIconStyle(ContextThemeWrapper(context, style))
    }

    /**
     * Allows you to change the text appearance of primary maneuver text.
     * @param style Int
     */
    fun updatePrimaryManeuverTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(primaryManeuverText, style)
    }

    /**
     * Allows you to change the text appearance of secondary maneuver text.
     * @param style Int
     */
    fun updateSecondaryManeuverTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(secondaryManeuverText, style)
    }

    /**
     * Allows you to change the text appearance of sub maneuver text.
     * @param style Int
     */
    fun updateSubManeuverTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(subManeuverText, style)
    }

    /**
     * Allows you to change the text appearance of step distance text.
     * @param style Int
     */
    fun updateStepDistanceTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(stepDistance, style)
    }

    /**
     * Allows you to change the style of [MapboxManeuverView].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        val typedArray = context.obtainStyledAttributes(style, R.styleable.MapboxManeuverView)
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MapboxManeuverView)
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun applyAttributes(typedArray: TypedArray) {
        mainManeuverView.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_maneuverViewBackgroundColor,
                    R.color.mainManeuverCardBackgroundColor
                )
            )
        )
        subManeuverView.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_subManeuverViewBackgroundColor,
                    R.color.subManeuverBackgroundColor
                )
            )
        )
        laneGuidanceCard.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_laneGuidanceViewBackgroundColor,
                    R.color.laneGuidanceBackgroundColor
                )
            )
        )
        upcomingManeuverRecycler.setBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_upcomingManeuverViewBackgroundColor,
                    R.color.upcomingManeuverBackgroundColor
                )
            )
        )
        laneGuidanceAdapter.updateStyle(
            typedArray.getResourceId(
                R.styleable.MapboxManeuverView_laneGuidanceManeuverArrowStyle,
                R.style.MapboxStyleTurnIconManeuver
            )
        )
    }

    private fun renderPrimaryManeuver(state: ManeuverState.ManeuverPrimary.Instruction) {
        primaryManeuverText.render(state)
    }

    private fun renderSecondaryManeuver(state: ManeuverState.ManeuverSecondary.Instruction) {
        secondaryManeuverText.render(state)
    }

    private fun hideSecondaryManeuver() {
        secondaryManeuverText.visibility = GONE
        updateConstraintsToOnlyPrimary()
        primaryManeuverText.maxLines = 2
    }

    private fun showSecondaryManeuver() {
        secondaryManeuverText.visibility = VISIBLE
        updateConstraintsToHaveSecondary()
        primaryManeuverText.maxLines = 1
    }

    private fun renderSubManeuver(state: ManeuverState.ManeuverSub.Instruction) {
        subManeuverText.render(state)
    }

    private fun renderSubManeuverVisibility(visibility: Int) {
        subManeuverView.visibility = visibility
    }

    private fun renderPrimaryTurnIcon(state: ManeuverState.ManeuverPrimary.Instruction) {
        maneuverIcon.render(state)
    }

    private fun renderSubTurnIcon(state: ManeuverState.ManeuverSub.Instruction) {
        subManeuverIcon.render(state)
    }

    private fun removeUpcomingManeuver(state: ManeuverState.UpcomingManeuvers.RemoveUpcoming) {
        upcomingManeuverAdapter.removeManeuver(state.maneuver)
    }

    private fun renderDistanceRemaining(state: ManeuverState) {
        stepDistance.render(state)
    }

    private fun renderAddLanes(state: ManeuverState.LaneGuidanceManeuver.AddLanes) {
        laneGuidanceAdapter.addLanes(state.lane.allLanes, state.lane.activeDirection)
    }

    private fun renderRemoveLanes(state: ManeuverState.LaneGuidanceManeuver.RemoveLanes) {
        laneGuidanceAdapter.removeLanes()
    }

    private fun renderLaneGuidanceVisibility(visibility: Int) {
        laneGuidanceCard.visibility = visibility
    }

    private fun renderUpcomingManeuvers(state: ManeuverState.UpcomingManeuvers.Upcoming) {
        val maneuvers = state.upcomingManeuverList
        if (maneuvers.isNotEmpty()) {
            upcomingManeuverAdapter.addUpcomingManeuvers(maneuvers)
        } else {
            upcomingManeuverAdapter.removeManeuvers()
        }
    }

    private fun renderUpcomingManeuverVisibility(visibility: Int) {
        upcomingManeuverRecycler.visibility = visibility
    }

    private fun updateConstraintsToOnlyPrimary() {
        val params = primaryManeuverText.layoutParams as LayoutParams
        params.topToTop = LayoutParams.PARENT_ID
        params.bottomToBottom = LayoutParams.PARENT_ID
        requestLayout()
    }

    private fun updateConstraintsToHaveSecondary() {
        val params = primaryManeuverText.layoutParams as LayoutParams
        params.topToTop = LayoutParams.UNSET
        params.bottomToBottom = LayoutParams.UNSET
        params.bottomToTop = secondaryManeuverText.id
        requestLayout()
    }
}
