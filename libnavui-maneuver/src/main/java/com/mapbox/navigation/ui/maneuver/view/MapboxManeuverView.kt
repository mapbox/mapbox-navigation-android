package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
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
import com.mapbox.navigation.ui.maneuver.databinding.MapboxMainManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.databinding.MapboxManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.databinding.MapboxSubManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.model.TurnIconResources

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
    private val binding = MapboxManeuverLayoutBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )
    private val mainLayoutBinding = MapboxMainManeuverLayoutBinding.bind(binding.root)
    private val subLayoutBinding = MapboxSubManeuverLayoutBinding.bind(binding.root)
    /**
     * Initialize.
     */
    init {
        binding.laneGuidanceRecycler.apply {
            layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
            adapter = laneGuidanceAdapter
        }

        binding.upcomingManeuverRecycler.apply {
            layoutManager = LinearLayoutManager(context, VERTICAL, false)
            adapter = upcomingManeuverAdapter
        }

        initAttributes(attrs)
        this.setOnClickListener {
            if (binding.upcomingManeuverRecycler.visibility == GONE) {
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
        mainLayoutBinding.maneuverIcon.updateTurnIconResources(turnIconResources)
        subLayoutBinding.subManeuverIcon.updateTurnIconResources(turnIconResources)
    }

    /**
     * Allows you to change the style of turn icon in main and sub maneuver view.
     * @param style Int
     */
    fun updateTurnIconStyle(@StyleRes style: Int) {
        mainLayoutBinding.maneuverIcon.updateTurnIconStyle(
            ContextThemeWrapper(context, style)
        )
        subLayoutBinding.subManeuverIcon.updateTurnIconStyle(
            ContextThemeWrapper(context, style)
        )
    }

    /**
     * Allows you to change the text appearance of primary maneuver text.
     * @param style Int
     */
    fun updatePrimaryManeuverTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(mainLayoutBinding.primaryManeuverText, style)
    }

    /**
     * Allows you to change the text appearance of secondary maneuver text.
     * @param style Int
     */
    fun updateSecondaryManeuverTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(mainLayoutBinding.secondaryManeuverText, style)
    }

    /**
     * Allows you to change the text appearance of sub maneuver text.
     * @param style Int
     */
    fun updateSubManeuverTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(subLayoutBinding.subManeuverText, style)
    }

    /**
     * Allows you to change the text appearance of step distance text.
     * @param style Int
     */
    fun updateStepDistanceTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(mainLayoutBinding.stepDistance, style)
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
        binding.mainManeuverView.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_maneuverViewBackgroundColor,
                    R.color.mapbox_main_maneuver_background_color
                )
            )
        )
        binding.subManeuverView.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_subManeuverViewBackgroundColor,
                    R.color.mapbox_sub_maneuver_background_color
                )
            )
        )
        binding.laneGuidanceCard.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_laneGuidanceViewBackgroundColor,
                    R.color.mapbox_lane_guidance_background_color
                )
            )
        )
        binding.upcomingManeuverRecycler.setBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_upcomingManeuverViewBackgroundColor,
                    R.color.mapbox_upcoming_maneuver_background_color
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
        mainLayoutBinding.primaryManeuverText.render(state)
    }

    private fun renderSecondaryManeuver(state: ManeuverState.ManeuverSecondary.Instruction) {
        mainLayoutBinding.secondaryManeuverText.render(state)
    }

    private fun hideSecondaryManeuver() {
        mainLayoutBinding.secondaryManeuverText.visibility = GONE
        updateConstraintsToOnlyPrimary()
        mainLayoutBinding.primaryManeuverText.maxLines = 2
    }

    private fun showSecondaryManeuver() {
        mainLayoutBinding.secondaryManeuverText.visibility = VISIBLE
        updateConstraintsToHaveSecondary()
        mainLayoutBinding.primaryManeuverText.maxLines = 1
    }

    private fun renderSubManeuver(state: ManeuverState.ManeuverSub.Instruction) {
        subLayoutBinding.subManeuverText.render(state)
    }

    private fun renderSubManeuverVisibility(visibility: Int) {
        binding.subManeuverView.visibility = visibility
    }

    private fun renderPrimaryTurnIcon(state: ManeuverState.ManeuverPrimary.Instruction) {
        mainLayoutBinding.maneuverIcon.render(state)
    }

    private fun renderSubTurnIcon(state: ManeuverState.ManeuverSub.Instruction) {
        subLayoutBinding.subManeuverIcon.render(state)
    }

    private fun removeUpcomingManeuver(state: ManeuverState.UpcomingManeuvers.RemoveUpcoming) {
        upcomingManeuverAdapter.removeManeuver(state.maneuver)
    }

    private fun renderDistanceRemaining(state: ManeuverState) {
        mainLayoutBinding.stepDistance.render(state)
    }

    private fun renderAddLanes(state: ManeuverState.LaneGuidanceManeuver.AddLanes) {
        laneGuidanceAdapter.addLanes(state.lane.allLanes, state.lane.activeDirection)
    }

    private fun renderRemoveLanes(state: ManeuverState.LaneGuidanceManeuver.RemoveLanes) {
        laneGuidanceAdapter.removeLanes()
    }

    private fun renderLaneGuidanceVisibility(visibility: Int) {
        binding.laneGuidanceCard.visibility = visibility
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
        binding.upcomingManeuverRecycler.visibility = visibility
    }

    private fun updateConstraintsToOnlyPrimary() {
        val params = mainLayoutBinding.primaryManeuverText.layoutParams as LayoutParams
        params.topToTop = LayoutParams.PARENT_ID
        params.bottomToBottom = LayoutParams.PARENT_ID
        requestLayout()
    }

    private fun updateConstraintsToHaveSecondary() {
        val params = mainLayoutBinding.primaryManeuverText.layoutParams as LayoutParams
        params.topToTop = LayoutParams.UNSET
        params.bottomToBottom = LayoutParams.UNSET
        params.bottomToTop = mainLayoutBinding.secondaryManeuverText.id
        requestLayout()
    }
}
