package com.mapbox.navigation.dropin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.databinding.MapboxLayoutDropInViewBinding
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton
import com.mapbox.navigation.ui.maps.camera.view.MapboxRouteOverviewButton
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.view.MapboxSoundButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class NavigationView : ConstraintLayout, LifecycleObserver {

    private val mapView: MapView
    private val lifeCycleOwner: LifecycleOwner
    private val mapboxNavigation: MapboxNavigation
    private val navigationViewModel = NavigationViewModel()
    private var navigationViewOptions: NavigationViewOptions
    private val binding = MapboxLayoutDropInViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private lateinit var maneuverView: View
    private lateinit var speedLimitView: View
    private lateinit var soundButtonView: View
    private lateinit var tripProgressView: View
    private lateinit var recenterButtonView: View
    private lateinit var routeOverviewButtonView: View

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        mapView = null,
        accessToken = null,
        null,
        NavigationViewOptions.Builder().build()
    )

    constructor(
        context: Context,
        accessToken: String,
        navigationViewOptions: NavigationViewOptions
    ) : this(
        context,
        null,
        null,
        accessToken,
        null,
        navigationViewOptions,
    )

    @JvmOverloads
    constructor(
        context: Context,
        mapView: MapView? = null,
        mapboxNavigation: MapboxNavigation,
        navigationViewOptions: NavigationViewOptions
    ) : this(
        context,
        null,
        mapView,
        mapboxNavigation.navigationOptions.accessToken,
        mapboxNavigation,
        navigationViewOptions
    )

    internal constructor(
        context: Context,
        attrs: AttributeSet?,
        mapView: MapView?,
        accessToken: String?,
        mapboxNavigation: MapboxNavigation?,
        navigationViewOptions: NavigationViewOptions
    ) : super(context, attrs) {
        check(mapView?.isAttachedToWindow != true) {
            "The provided Map View cannot be attached to a window"
        }
        val a = context.obtainStyledAttributes(attrs, R.styleable.NavigationView, 0, 0)
        val attrsAccessToken = a.getString(R.styleable.NavigationView_accessToken)
        a.recycle()
        this.navigationViewOptions = navigationViewOptions
        this.lifeCycleOwner = context as? LifecycleOwner ?: throw LifeCycleOwnerNotFoundException()
        this.lifeCycleOwner.lifecycle.addObserver(this)
        this.mapView = mapView ?: MapView(context, MapInitOptions(context))
        this.mapboxNavigation = mapboxNavigation ?: MapboxNavigation(
            NavigationOptions.Builder(
                context.applicationContext
            ).accessToken(accessToken ?: attrsAccessToken).build()
        )
    }

    /**
     * Creates references to view components. Views provided via the [viewProvider] will be given preference.
     * If null, default Mapbox designed views will be used.
     */
    fun configure(viewProvider: ViewProvider) {
        // add views
        binding.mapContainer.addView(mapView)
        maneuverView = viewProvider.maneuverProvider?.invoke() ?: MapboxManeuverView(context)
        binding.maneuverContainer.addView(maneuverView)
        speedLimitView = viewProvider.speedLimitProvider?.invoke() ?: MapboxSpeedLimitView(context)
        binding.speedLimitContainer.addView(speedLimitView)
        tripProgressView =
            viewProvider.tripProgressProvider?.invoke() ?: MapboxTripProgressView(context)
        binding.speedLimitContainer.addView(tripProgressView)
        soundButtonView = viewProvider.soundButtonProvider?.invoke() ?: MapboxSoundButton(context)
        binding.volumeContainer.addView(soundButtonView)
        recenterButtonView =
            viewProvider.recenterButtonProvider?.invoke() ?: MapboxRecenterButton(context)
        binding.recenterContainer.addView(recenterButtonView)
        routeOverviewButtonView =
            viewProvider.routeOverviewButtonProvider?.invoke() ?: MapboxRouteOverviewButton(context)
        binding.routeOverviewContainer.addView(routeOverviewButtonView)

        // add lifecycle
        lifeCycleOwner.lifecycleScope.launch {
            actions().collect { navigationViewModel::processAction }
        }
        lifeCycleOwner.lifecycleScope.launch {
            navigationViewModel.viewStates().collect { navigationViewState ->
                updateContainersVisibility(navigationViewState)
                // todo update containers data
            }
        }

        update(navigationViewOptions)
    }

    fun update(navigationViewOptions: NavigationViewOptions) {
        this.navigationViewOptions = navigationViewOptions
        // todo trigger relayout
    }

    @ExperimentalCoroutinesApi
    private fun actions(): Flow<Action> = merge(
        // Merge multiple actions if required.
        initialStateTransition()
    )

    private fun initialStateTransition() = flowOf(
        NavigationStateTransitionAction.ToEmpty(from = NavigationState.Empty)
    )

    private fun updateContainersVisibility(state: NavigationViewState) {
        binding.volumeContainer.visibility = state.volumeContainerVisible.toVisibility()
        binding.recenterContainer.visibility = state.recenterContainerVisible.toVisibility()
        binding.maneuverContainer.visibility = state.maneuverContainerVisible.toVisibility()
        binding.infoPanelContainer.visibility = state.infoPanelContainerVisible.toVisibility()
        binding.speedLimitContainer.visibility = state.speedLimitContainerVisible.toVisibility()
        binding.routeOverviewContainer.visibility =
            state.routeOverviewContainerVisible.toVisibility()
    }
}

private fun Boolean.toVisibility() = if (this) {
    View.VISIBLE
} else {
    View.GONE
}

