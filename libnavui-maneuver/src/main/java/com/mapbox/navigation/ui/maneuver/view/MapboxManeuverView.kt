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
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.databinding.MapboxMainManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.databinding.MapboxManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.databinding.MapboxSubManeuverLayoutBinding
import com.mapbox.navigation.ui.maneuver.model.Lane
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.TurnIconResources
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Default view to render a maneuver.
 *
 * @see MapboxManeuverApi
 */
class MapboxManeuverView : ConstraintLayout {

    /**
     * Default view to render a maneuver.
     *
     * @see MapboxManeuverApi
     */
    constructor(context: Context) : super(context)

    /**
     * Default view to render a maneuver.
     *
     * @see MapboxManeuverApi
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttributes(attrs)
    }

    /**
     * Default view to render a maneuver.
     *
     * @see MapboxManeuverApi
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    private val laneGuidanceAdapter = MapboxLaneGuidanceAdapter(context)
    private val upcomingManeuverAdapter = MapboxUpcomingManeuverAdapter(context)
    private val binding = MapboxManeuverLayoutBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )
    private val mainLayoutBinding = MapboxMainManeuverLayoutBinding.bind(binding.root)
    private val subLayoutBinding = MapboxSubManeuverLayoutBinding.bind(binding.root)

    private val maneuverToRoadShields = mutableMapOf<String, List<RoadShield>>()
    private val currentlyRenderedManeuvers: MutableList<Maneuver> = mutableListOf()

    /**
     * The property enables/disables showing the list of upcoming maneuvers.
     * Note: The View doesn't maintain the state of this property. If the device undergoes
     * screen rotation changes make sure to set this property again at appropriate place.
     */
    var upcomingManeuverRenderingEnabled = true
        set(value) {
            if (value != field) {
                if (!value && binding.upcomingManeuverRecycler.visibility == VISIBLE) {
                    updateUpcomingManeuversVisibility(GONE)
                }
            }
            field = value
        }

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

        this.setOnClickListener {
            if (upcomingManeuverRenderingEnabled) {
                if (binding.upcomingManeuverRecycler.visibility == GONE) {
                    updateUpcomingManeuversVisibility(VISIBLE)
                } else {
                    updateUpcomingManeuversVisibility(GONE)
                    binding.upcomingManeuverRecycler.smoothScrollToPosition(0)
                    updateSubManeuverViewVisibility(binding.subManeuverLayout.visibility)
                }
            }
        }
    }

    /**
     * Invoke the method to render the maneuvers.
     *
     * The first maneuver on the list will be rendered as the current maneuver while the rest of the maneuvers will be rendered as upcoming maneuvers.
     *
     * You can control the number of maneuvers that will be rendered by manipulating the provided list,
     * for example, you can render the current maneuver and a list of up to 5 upcoming maneuvers like this:
     * ```
     * maneuverView.renderManeuvers(
     *     maneuvers.mapValue {
     *         it.take(6)
     *     }
     * )
     * ```
     *
     * If there were any shields provided via [renderManeuverShields], those shields will be attached to corresponding [Maneuver]s during rendering.
     *
     * This method ignores empty lists of maneuvers and will not clean up the view if an empty list is provided.
     *
     * @param maneuvers maneuvers to render
     * @see MapboxManeuverApi.getManeuvers
     */
    fun renderManeuvers(maneuvers: Expected<ManeuverError, List<Maneuver>>) {
        maneuvers.onValue { list ->
            if (list.isNotEmpty()) {
                currentlyRenderedManeuvers.clear()
                currentlyRenderedManeuvers.addAll(list)
                renderManeuvers()
            }
        }
    }

    /**
     * Invoke the method to update rendered maneuvers with road shields.
     *
     * The provided shields are mapped to IDs of [PrimaryManeuver], [SecondaryManeuver], and [SubManeuver]
     * and if a maneuver has already been rendered via [renderManeuvers], the respective shields' text will be changed to the shield icon.
     *
     * Invoking this method also caches all of the available shields. Whenever [renderManeuvers] is invoked, the cached shields are reused for rendering.
     *
     * @param shieldMap the map of maneuver IDs to available shields
     */
    @Deprecated(
        message = "The method can only render one shield if an instruction has multiple shields",
        replaceWith = ReplaceWith("renderManeuverWith(shields)")
    )
    fun renderManeuverShields(shieldMap: Map<String, RoadShield?>) {
        val shields = hashMapOf<String, List<RoadShield>>()
        shieldMap.forEach { entry ->
            shields[entry.key] = ifNonNull(entry.value) { value ->
                listOf(value)
            } ?: listOf()
        }
        renderManeuverWith(shields)
    }

    /**
     * Invoke the method to update rendered maneuvers with road shields.
     *
     * The provided shields are mapped to IDs of [PrimaryManeuver], [SecondaryManeuver], and [SubManeuver]
     * and if a maneuver has already been rendered via [renderManeuvers], the respective shields' text will be changed to the shield icon.
     *
     * Invoking this method also caches all of the available shields. Whenever [renderManeuvers] is invoked, the cached shields are reused for rendering.
     *
     * @param shields the map of maneuver IDs to available list of shields
     */
    fun renderManeuverWith(shields: Map<String, List<RoadShield>>) {
        maneuverToRoadShields.putAll(shields)
        renderManeuvers()
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
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updatePrimaryManeuverTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(mainLayoutBinding.primaryManeuverText, style)
    }

    /**
     * Allows you to change the text appearance of secondary maneuver text.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateSecondaryManeuverTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(mainLayoutBinding.secondaryManeuverText, style)
    }

    /**
     * Allows you to change the text appearance of sub maneuver text.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateSubManeuverTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(subLayoutBinding.subManeuverText, style)
    }

    /**
     * Allows you to change the text appearance of step distance text.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateStepDistanceTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(mainLayoutBinding.stepDistance, style)
    }

    /**
     * Allows you to change the text appearance of primary maneuver text in upcoming maneuver list.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateUpcomingPrimaryManeuverTextAppearance(@StyleRes style: Int) {
        upcomingManeuverAdapter.updateUpcomingPrimaryManeuverTextAppearance(style)
    }

    /**
     * Allows you to change the text appearance of secondary maneuver text in upcoming maneuver list.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateUpcomingSecondaryManeuverTextAppearance(@StyleRes style: Int) {
        upcomingManeuverAdapter.updateUpcomingSecondaryManeuverTextAppearance(style)
    }

    /**
     * Allows you to change the text appearance of step distance text in upcoming maneuver list.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateUpcomingManeuverStepDistanceTextAppearance(@StyleRes style: Int) {
        upcomingManeuverAdapter.updateUpcomingManeuverStepDistanceTextAppearance(style)
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
     * @param roadShield shield if available otherwise null
     */
    @Deprecated(
        message = "The method can only render one shield if an instruction has multiple shields",
        replaceWith = ReplaceWith("renderPrimary(primary, roadShields)")
    )
    @JvmOverloads
    fun renderPrimaryManeuver(primary: PrimaryManeuver, roadShield: RoadShield? = null) {
        val shields = ifNonNull(roadShield) {
            listOf(it)
        }
        renderPrimary(primary, shields)
    }

    /**
     * Invoke the method to render primary instructions on top of [MapboxManeuverView]
     * @param primary PrimaryManeuver
     * @param roadShields list of shields if available otherwise null
     */
    fun renderPrimary(primary: PrimaryManeuver, roadShields: List<RoadShield>?) {
        mainLayoutBinding.primaryManeuverText.renderManeuver(primary, roadShields)
        mainLayoutBinding.maneuverIcon.renderPrimaryTurnIcon(primary)
    }

    /**
     * Invoke the method to render secondary instructions on top of [MapboxManeuverView]
     * @param secondary SecondaryManeuver?
     * @param roadShield shield if available otherwise null
     */
    @Deprecated(
        message = "The method can only render one shield if an instruction has multiple shields",
        replaceWith = ReplaceWith("renderSecondary(secondary, roadShields)")
    )
    @JvmOverloads
    fun renderSecondaryManeuver(secondary: SecondaryManeuver?, roadShield: RoadShield? = null) {
        val shields = ifNonNull(roadShield) {
            listOf(it)
        }
        renderSecondary(secondary, shields)
    }

    /**
     * Invoke the method to render secondary instructions on top of [MapboxManeuverView]
     * @param secondary SecondaryManeuver?
     * @param roadShields list of shields if available otherwise null
     */
    fun renderSecondary(secondary: SecondaryManeuver?, roadShields: List<RoadShield>?) {
        mainLayoutBinding.secondaryManeuverText.renderManeuver(secondary, roadShields)
    }

    /**
     * Invoke the method to render sub instructions on top of [MapboxManeuverView]
     * @param sub SubManeuver?
     * @param roadShield shield if available otherwise null
     */
    @Deprecated(
        message = "The method can only render one shield if an instruction has multiple shields",
        replaceWith = ReplaceWith("renderSub(sub, roadShields)")
    )
    @JvmOverloads
    fun renderSubManeuver(sub: SubManeuver?, roadShield: RoadShield? = null) {
        val shields = ifNonNull(roadShield) {
            listOf(it)
        }
        renderSub(sub, shields)
    }

    /**
     * Invoke the method to render sub instructions on top of [MapboxManeuverView]
     * @param sub SubManeuver?
     * @param roadShields list of shields if available otherwise null
     */
    fun renderSub(sub: SubManeuver?, roadShields: List<RoadShield>?) {
        subLayoutBinding.subManeuverText.renderManeuver(sub, roadShields)
        subLayoutBinding.subManeuverIcon.renderSubTurnIcon(sub)
    }

    /**
     * Invoke the method to add lane information on top of [MapboxManeuverView]
     * @param lane Lane
     */
    fun renderAddLanes(lane: Lane) {
        laneGuidanceAdapter.addLanes(lane.allLanes)
    }

    /**
     * Invoke the method to remove lane information on top of [MapboxManeuverView]
     */
    fun renderRemoveLanes() {
        laneGuidanceAdapter.removeLanes()
    }

    /**
     * Invoke the method to render step distance remaining on top of [MapboxManeuverView]
     * @param stepDistance StepDistance
     */
    fun renderDistanceRemaining(stepDistance: StepDistance) {
        mainLayoutBinding.stepDistance.renderDistanceRemaining(stepDistance)
    }

    private fun renderManeuvers() {
        if (currentlyRenderedManeuvers.isNotEmpty()) {
            drawManeuver(currentlyRenderedManeuvers[0], maneuverToRoadShields)
            upcomingManeuverAdapter.updateShields(maneuverToRoadShields)
            drawUpcomingManeuvers(currentlyRenderedManeuvers.drop(1))
        }
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxManeuverView,
            0,
            R.style.MapboxStyleManeuverView
        )
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun applyAttributes(typedArray: TypedArray) {
        binding.mainManeuverLayout.setBackgroundColor(
            typedArray.getColor(
                R.styleable.MapboxManeuverView_maneuverViewBackgroundColor,
                ContextCompat.getColor(
                    context,
                    R.color.mapbox_main_maneuver_background_color
                )
            )
        )
        binding.subManeuverLayout.setBackgroundColor(
            typedArray.getColor(
                R.styleable.MapboxManeuverView_subManeuverViewBackgroundColor,
                ContextCompat.getColor(
                    context,
                    R.color.mapbox_sub_maneuver_background_color
                )
            )
        )
        binding.upcomingManeuverRecycler.setBackgroundColor(
            typedArray.getColor(
                R.styleable.MapboxManeuverView_upcomingManeuverViewBackgroundColor,
                ContextCompat.getColor(
                    context,
                    R.color.mapbox_upcoming_maneuver_background_color
                )
            )
        )
        laneGuidanceAdapter.updateStyle(
            typedArray.getResourceId(
                R.styleable.MapboxManeuverView_laneGuidanceManeuverIconStyle,
                R.style.MapboxStyleTurnIconManeuver
            )
        )
        mainLayoutBinding.maneuverIcon.updateTurnIconStyle(
            ContextThemeWrapper(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_maneuverViewIconStyle,
                    R.style.MapboxStyleTurnIconManeuver
                )
            )
        )
        subLayoutBinding.subManeuverIcon.updateTurnIconStyle(
            ContextThemeWrapper(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_maneuverViewIconStyle,
                    R.style.MapboxStyleTurnIconManeuver
                )
            )
        )
    }

    private fun drawUpcomingManeuvers(maneuvers: List<Maneuver>) {
        upcomingManeuverAdapter.addUpcomingManeuvers(maneuvers)
    }

    private fun drawManeuver(maneuver: Maneuver, shields: Map<String, List<RoadShield>?>) {
        val primary = maneuver.primary
        val secondary = maneuver.secondary
        val sub = maneuver.sub
        val lane = maneuver.laneGuidance
        val stepDistance = maneuver.stepDistance
        val primaryId = primary.id
        val secondaryId = secondary?.id
        val subId = sub?.id
        if (secondary?.componentList != null) {
            updateSecondaryManeuverVisibility(VISIBLE)
            renderSecondary(secondary, shields[secondaryId])
        } else {
            updateSecondaryManeuverVisibility(GONE)
        }
        renderPrimary(primary, shields[primaryId])
        renderDistanceRemaining(stepDistance)
        if (sub?.componentList != null || lane != null) {
            updateSubManeuverViewVisibility(VISIBLE)
        } else {
            updateSubManeuverViewVisibility(GONE)
        }
        if (sub?.componentList != null) {
            renderSub(sub, shields[subId])
        } else {
            renderSub(null, listOf())
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
