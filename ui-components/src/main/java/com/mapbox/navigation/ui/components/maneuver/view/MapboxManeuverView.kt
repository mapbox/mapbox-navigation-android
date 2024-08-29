package com.mapbox.navigation.ui.components.maneuver.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.Lane
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverError
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.StepDistance
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.maneuver.model.TurnIconResources
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import com.mapbox.navigation.ui.components.R
import com.mapbox.navigation.ui.components.databinding.MapboxMainManeuverLayoutBinding
import com.mapbox.navigation.ui.components.databinding.MapboxManeuverLayoutBinding
import com.mapbox.navigation.ui.components.databinding.MapboxSubManeuverLayoutBinding
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default view to render a maneuver.
 *
 * @see MapboxManeuverApi
 */
@UiThread
class MapboxManeuverView : ConstraintLayout {

    private val _maneuverViewState = MutableStateFlow<MapboxManeuverViewState>(
        MapboxManeuverViewState.COLLAPSED,
    )

    /**
     * Observe on [maneuverViewState] to get notified about changes to [MapboxManeuverViewState].
     */
    val maneuverViewState = _maneuverViewState.asStateFlow()

    private var maneuverViewOptions = ManeuverViewOptions.Builder().build()

    /**
     * Default view to render a maneuver.
     *
     * @see MapboxManeuverApi
     */
    constructor(context: Context) : this(context, null)

    /**
     * Default view to render a maneuver.
     *
     * @see MapboxManeuverApi
     */
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.style.MapboxStyleManeuverView,
    )

    /**
     * Default view to render a maneuver.
     *
     * @see MapboxManeuverApi
     */
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        options: ManeuverViewOptions = ManeuverViewOptions.Builder().build(),
    ) : super(context, attrs, defStyleAttr) {
        this.maneuverViewOptions = options
        initAttributes(attrs)
    }

    private val laneGuidanceAdapter = MapboxLaneGuidanceAdapter(context)
    private val upcomingManeuverAdapter = MapboxUpcomingManeuverAdapter(context)
    private val binding = MapboxManeuverLayoutBinding.inflate(
        LayoutInflater.from(context),
        this,
        true,
    )
    private val mainLayoutBinding = MapboxMainManeuverLayoutBinding.bind(binding.root)
    private val subLayoutBinding = MapboxSubManeuverLayoutBinding.bind(binding.root)

    private val routeShields = mutableSetOf<RouteShield>()
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

        subLayoutBinding.subManeuverText.updateOptions(
            maneuverViewOptions.subManeuverOptions,
        )
        mainLayoutBinding.primaryManeuverText.updateOptions(
            maneuverViewOptions.primaryManeuverOptions,
        )
        mainLayoutBinding.secondaryManeuverText.updateOptions(
            maneuverViewOptions.secondaryManeuverOptions,
        )
    }

    /**
     * Invoke the method to change the styling of various [Maneuver] components at runtime.
     *
     * @param options ManeuverViewOptions
     */
    fun updateManeuverViewOptions(options: ManeuverViewOptions) {
        this.maneuverViewOptions = options
        subLayoutBinding.subManeuverText.updateOptions(
            maneuverViewOptions.subManeuverOptions,
        )
        mainLayoutBinding.primaryManeuverText.updateOptions(
            maneuverViewOptions.primaryManeuverOptions,
        )
        mainLayoutBinding.secondaryManeuverText.updateOptions(
            maneuverViewOptions.secondaryManeuverOptions,
        )
        TextViewCompat.setTextAppearance(
            mainLayoutBinding.primaryManeuverText,
            maneuverViewOptions.primaryManeuverOptions.textAppearance,
        )
        TextViewCompat.setTextAppearance(
            mainLayoutBinding.secondaryManeuverText,
            maneuverViewOptions.secondaryManeuverOptions.textAppearance,
        )
        TextViewCompat.setTextAppearance(
            subLayoutBinding.subManeuverText,
            maneuverViewOptions.subManeuverOptions.textAppearance,
        )
        TextViewCompat.setTextAppearance(
            mainLayoutBinding.stepDistance,
            maneuverViewOptions.stepDistanceTextAppearance,
        )
        binding.mainManeuverLayout.setBackgroundColor(
            ContextCompat.getColor(context, maneuverViewOptions.maneuverBackgroundColor),
        )
        binding.subManeuverLayout.setBackgroundColor(
            ContextCompat.getColor(context, maneuverViewOptions.subManeuverBackgroundColor),
        )
        laneGuidanceAdapter.updateStyle(maneuverViewOptions.laneGuidanceTurnIconManeuver)
        mainLayoutBinding.maneuverIcon.updateTurnIconStyle(
            ContextThemeWrapper(context, maneuverViewOptions.turnIconManeuver),
        )
        subLayoutBinding.subManeuverIcon.updateTurnIconStyle(
            ContextThemeWrapper(context, maneuverViewOptions.turnIconManeuver),
        )
        binding.upcomingManeuverRecycler.setBackgroundColor(
            ContextCompat.getColor(context, maneuverViewOptions.upcomingManeuverBackgroundColor),
        )
        upcomingManeuverAdapter.updateManeuverViewOptions(maneuverViewOptions)
        upcomingManeuverAdapter.updateUpcomingManeuverIconStyle(
            ContextThemeWrapper(context, maneuverViewOptions.turnIconManeuver),
        )
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
     * If there were any shields provided via [renderManeuverWith], those shields will be attached to corresponding [Maneuver]s during rendering.
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
     * The provided shields are list of either [RouteShieldError] or [RouteShieldResult].
     * If a maneuver has already been rendered via [renderManeuvers], the respective shields'
     * text will be changed to the shield icon.
     *
     * Invoking this method also caches all of the available shields. Whenever [renderManeuvers] is
     * invoked, the cached shields are reused for rendering.
     *
     * @param shields the map of maneuver IDs to available list of shields
     */
    fun renderManeuverWith(shields: List<Expected<RouteShieldError, RouteShieldResult>>) {
        val partitionedList = shields.partition { it.isError }
        partitionedList.first.forEach { errorExpected ->
            logE(
                "id: $id -- error: ${errorExpected.error?.url} - " +
                    "${errorExpected.error?.errorMessage}",
                "MapboxManeuverView",
            )
        }
        partitionedList.second.mapNotNull { it.value }.map { it.shield }.apply {
            routeShields.clear()
            routeShields.addAll(this)
        }
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
        when (visibility) {
            VISIBLE -> {
                _maneuverViewState.value = MapboxManeuverViewState.EXPANDED
            }
            else -> {
                _maneuverViewState.value = MapboxManeuverViewState.COLLAPSED
            }
        }
        binding.upcomingManeuverRecycler.visibility = visibility
    }

    /**
     * Invoke the method to render primary instructions on top of [MapboxManeuverView]
     * @param primary PrimaryManeuver
     * @param routeShields set of shields if available otherwise null
     */
    fun renderPrimary(primary: PrimaryManeuver, routeShields: Set<RouteShield>?) {
        mainLayoutBinding.primaryManeuverText.renderManeuver(primary, routeShields)
        mainLayoutBinding.maneuverIcon.renderPrimaryTurnIcon(primary)
    }

    /**
     * Invoke the method to render secondary instructions on top of [MapboxManeuverView]
     * @param secondary SecondaryManeuver?
     * @param routeShields set of shields if available otherwise null
     */
    fun renderSecondary(secondary: SecondaryManeuver?, routeShields: Set<RouteShield>?) {
        mainLayoutBinding.secondaryManeuverText.renderManeuver(secondary, routeShields)
    }

    /**
     * Invoke the method to render sub instructions on top of [MapboxManeuverView]
     * @param sub SubManeuver?
     * @param routeShields set of shields if available otherwise null
     */
    fun renderSub(sub: SubManeuver?, routeShields: Set<RouteShield>?) {
        subLayoutBinding.subManeuverText.renderManeuver(sub, routeShields)
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
            drawManeuver(currentlyRenderedManeuvers[0], routeShields)
            upcomingManeuverAdapter.updateShields(routeShields)
            drawUpcomingManeuvers(currentlyRenderedManeuvers.drop(1))
        }
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxManeuverView,
            0,
            R.style.MapboxStyleManeuverView,
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
                    R.color.mapbox_main_maneuver_background_color,
                ),
            ),
        )
        binding.subManeuverLayout.setBackgroundColor(
            typedArray.getColor(
                R.styleable.MapboxManeuverView_subManeuverViewBackgroundColor,
                ContextCompat.getColor(
                    context,
                    R.color.mapbox_sub_maneuver_background_color,
                ),
            ),
        )
        binding.upcomingManeuverRecycler.setBackgroundColor(
            typedArray.getColor(
                R.styleable.MapboxManeuverView_upcomingManeuverViewBackgroundColor,
                ContextCompat.getColor(
                    context,
                    R.color.mapbox_upcoming_maneuver_background_color,
                ),
            ),
        )
        laneGuidanceAdapter.updateStyle(
            typedArray.getResourceId(
                R.styleable.MapboxManeuverView_laneGuidanceManeuverIconStyle,
                R.style.MapboxStyleTurnIconManeuver,
            ),
        )
        mainLayoutBinding.maneuverIcon.updateTurnIconStyle(
            ContextThemeWrapper(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_maneuverViewIconStyle,
                    R.style.MapboxStyleTurnIconManeuver,
                ),
            ),
        )
        subLayoutBinding.subManeuverIcon.updateTurnIconStyle(
            ContextThemeWrapper(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_maneuverViewIconStyle,
                    R.style.MapboxStyleTurnIconManeuver,
                ),
            ),
        )
        upcomingManeuverAdapter.updateUpcomingManeuverIconStyle(
            ContextThemeWrapper(
                context,
                typedArray.getResourceId(
                    R.styleable.MapboxManeuverView_upcomingManeuverListIconStyle,
                    R.style.MapboxStyleTurnIconManeuver,
                ),
            ),
        )
    }

    private fun drawUpcomingManeuvers(maneuvers: List<Maneuver>) {
        upcomingManeuverAdapter.addUpcomingManeuvers(maneuvers)
    }

    private fun drawManeuver(maneuver: Maneuver, shields: Set<RouteShield>?) {
        val primary = maneuver.primary
        val secondary = maneuver.secondary
        val sub = maneuver.sub
        val lane = maneuver.laneGuidance
        val stepDistance = maneuver.stepDistance
        if (secondary?.componentList != null) {
            updateSecondaryManeuverVisibility(VISIBLE)
            renderSecondary(secondary, shields)
        } else {
            updateSecondaryManeuverVisibility(GONE)
        }
        renderPrimary(primary, shields)
        renderDistanceRemaining(stepDistance)
        if (sub?.componentList != null || lane != null) {
            updateSubManeuverViewVisibility(VISIBLE)
        } else {
            updateSubManeuverViewVisibility(GONE)
        }
        if (sub?.componentList != null) {
            renderSub(sub, shields)
        } else {
            renderSub(null, setOf())
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

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun getUpcomingManeuverAdapter(): MapboxUpcomingManeuverAdapter {
        return upcomingManeuverAdapter
    }
}
