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
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.databinding.MapboxMainManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.databinding.MapboxManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.databinding.MapboxSubManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.model.Lane
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
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
) : ConstraintLayout(context, attrs, defStyleAttr) {

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
                updateUpcomingManeuversVisibility(VISIBLE)
            } else {
                updateUpcomingManeuversVisibility(GONE)
                binding.upcomingManeuverRecycler.smoothScrollToPosition(0)
                updateSubManeuverViewVisibility(binding.subManeuverLayout.visibility)
            }
        }
    }

    /**
     * Invoke the method to render primary, secondary, sub instructions and lane information.
     * @param maneuver Expected
     */
    fun renderManeuver(maneuver: Expected<Maneuver, ManeuverError>) {
        when (maneuver) {
            is Expected.Success -> {
                drawManeuver(maneuver.value)
            }
            is Expected.Failure -> {
                // Not handled
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

    /**
     * Invoke the method to control the visibility of [MapboxPrimaryManeuver]
     * @param visibility Int
     */
    fun updatePrimaryManeuverTextVisibility(visibility: Int) {
        mainLayoutBinding.primaryManeuverText.visibility = visibility
    }

    /**
     * Invoke the method to control the visibility of [MapboxSecondaryManeuver]
     * @param visibility Int
     */
    fun updateSecondaryManeuverVisibility(visibility: Int) {
        when (visibility) {
            VISIBLE -> {
                showSecondaryManeuver()
            }
            INVISIBLE -> {
                hideSecondaryManeuver(INVISIBLE)
            }
            GONE -> {
                hideSecondaryManeuver(GONE)
            }
        }
    }

    /**
     * Invoke the method to control the visibility of [MapboxSubManeuver]
     * @param visibility Int
     */
    fun updateSubManeuverViewVisibility(visibility: Int) {
        binding.subManeuverLayout.visibility = visibility
    }

    /**
     * Invoke the method to control the visibility of upcoming instructions list
     * @param visibility Int
     */
    fun updateUpcomingManeuversVisibility(visibility: Int) {
        binding.upcomingManeuverRecycler.visibility = visibility
    }

    /**
     * Invoke the method to render primary instructions on top of [MapboxManeuverView]
     * @param primary PrimaryManeuver
     */
    fun renderPrimaryManeuver(primary: PrimaryManeuver) {
        mainLayoutBinding.primaryManeuverText.render(primary)
        mainLayoutBinding.maneuverIcon.renderPrimaryTurnIcon(primary)
    }

    /**
     * Invoke the method to render secondary instructions on top of [MapboxManeuverView]
     * @param secondary SecondaryManeuver?
     */
    fun renderSecondaryManeuver(secondary: SecondaryManeuver?) {
        mainLayoutBinding.secondaryManeuverText.render(secondary)
    }

    /**
     * Invoke the method to render sub instructions on top of [MapboxManeuverView]
     * @param sub SubManeuver?
     */
    fun renderSubManeuver(sub: SubManeuver?) {
        subLayoutBinding.subManeuverText.render(sub)
        subLayoutBinding.subManeuverIcon.renderSubTurnIcon(sub)
    }

    /**
     * Invoke the method to add lane information on top of [MapboxManeuverView]
     * @param lane Lane
     */
    fun renderAddLanes(lane: Lane) {
        laneGuidanceAdapter.addLanes(lane.allLanes, lane.activeDirection)
    }

    /**
     * Invoke the method to remove lane information on top of [MapboxManeuverView]
     */
    fun renderRemoveLanes() {
        laneGuidanceAdapter.removeLanes()
    }

    /**
     * Invoke the method to render list of upcoming instructions on top of [MapboxManeuverView]
     * @param maneuvers List<Maneuver>
     */
    fun renderUpcomingManeuvers(maneuvers: List<Maneuver>) {
        upcomingManeuverAdapter.addUpcomingManeuvers(maneuvers)
    }

    /**
     * Invoke the method to render step distance remaining on top of [MapboxManeuverView]
     * @param stepDistance StepDistance
     */
    fun renderDistanceRemaining(stepDistance: StepDistance) {
        mainLayoutBinding.stepDistance.render(stepDistance)
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MapboxManeuverView)
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun applyAttributes(typedArray: TypedArray) {
        binding.mainManeuverLayout.setBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_maneuverViewBackgroundColor,
                    R.color.mapbox_main_maneuver_background_color
                )
            )
        )
        binding.subManeuverLayout.setBackgroundColor(
            ContextCompat.getColor(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_subManeuverViewBackgroundColor,
                    R.color.mapbox_sub_maneuver_background_color
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

    private fun drawManeuver(maneuver: Maneuver) {
        val primary = maneuver.primary
        val secondary = maneuver.secondary
        val sub = maneuver.sub
        val lane = maneuver.laneGuidance
        if (secondary?.componentList != null) {
            updateSecondaryManeuverVisibility(VISIBLE)
            renderSecondaryManeuver(secondary)
        } else {
            updateSecondaryManeuverVisibility(GONE)
        }
        renderPrimaryManeuver(primary)
        if (sub?.componentList != null || lane != null) {
            updateSubManeuverViewVisibility(VISIBLE)
        } else {
            updateSubManeuverViewVisibility(GONE)
        }
        if (sub?.componentList != null) {
            renderSubManeuver(sub)
        } else {
            renderSubManeuver(null)
        }
        when (lane != null) {
            true -> {
                renderAddLanes(lane)
            }
            else -> {
                renderRemoveLanes()
            }
        }
    }

    private fun hideSecondaryManeuver(visibility: Int) {
        mainLayoutBinding.secondaryManeuverText.visibility = visibility
        updateConstraintsToOnlyPrimary()
        mainLayoutBinding.primaryManeuverText.maxLines = 2
    }

    private fun showSecondaryManeuver() {
        mainLayoutBinding.secondaryManeuverText.visibility = VISIBLE
        updateConstraintsToHaveSecondary()
        mainLayoutBinding.primaryManeuverText.maxLines = 1
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
