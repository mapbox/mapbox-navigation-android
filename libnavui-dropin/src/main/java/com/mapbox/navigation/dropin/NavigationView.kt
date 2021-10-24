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
class NavigationView: ConstraintLayout, LifecycleObserver {

    private val mapView: MapView
    private val lifeCycleOwner: LifecycleOwner
    private val mapboxNavigation: MapboxNavigation
    private val navigationViewModel = NavigationViewModel()
    private val navigationViewOptions: NavigationViewOptions
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
        val a = context.obtainStyledAttributes(attrs, R.styleable.NavigationView, 0 ,0)
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
     * The function bases [DropInOptions] to decide which views will be rendered on top of the map
     * view. The views provided via the [viewProvider] will be given preference. If null, default
     * Mapbox designed views will be used.
     */
    fun configure(viewProvider: ViewProvider) {
        binding.mapContainer.addView(mapView)
        if (navigationViewOptions.renderManeuvers) {
            maneuverView =
                viewProvider.maneuverProvider?.invoke() ?: MapboxManeuverView(context)
            binding.maneuverContainer.addView(maneuverView)
        }
        if (navigationViewOptions.renderSpeedLimit) {
            speedLimitView =
                viewProvider.speedLimitProvider?.invoke() ?: MapboxSpeedLimitView(context)
            binding.speedLimitContainer.addView(speedLimitView)
        }
        if (navigationViewOptions.renderTripProgress) {
            tripProgressView =
                viewProvider.tripProgressProvider?.invoke() ?: MapboxTripProgressView(context)
            binding.speedLimitContainer.addView(tripProgressView)
        }
        if (navigationViewOptions.renderVolumeButton) {
            soundButtonView =
                viewProvider.soundButtonProvider?.invoke() ?: MapboxSoundButton(context)
            binding.volumeContainer.addView(soundButtonView)
        }
        if (navigationViewOptions.renderRecenterButton) {
            recenterButtonView =
                viewProvider.recenterButtonProvider?.invoke() ?: MapboxRecenterButton(context)
            binding.recenterContainer.addView(recenterButtonView)
        }
        if (navigationViewOptions.renderRouteOverviewButton) {
            routeOverviewButtonView = viewProvider.routeOverviewButtonProvider?.invoke()
                ?: MapboxRouteOverviewButton(context)
            binding.routeOverviewContainer.addView(routeOverviewButtonView)
        }
        lifeCycleOwner.lifecycleScope.launch {
            actions().collect { navigationViewModel::processAction }
        }
        lifeCycleOwner.lifecycleScope.launch {
            navigationViewModel.viewStates().collect { navigationViewState ->
                when (navigationViewState) {
                    is NavigationViewState.UponEmpty -> {
                        renderUponEmpty(state = navigationViewState)
                    }
                    is NavigationViewState.UponFreeDrive -> {
                        renderUponFreeDrive(state = navigationViewState)
                    }
                    is NavigationViewState.UponRoutePreview -> {
                        renderUponRoutePreview(state = navigationViewState)
                    }
                    is NavigationViewState.UponActiveNavigation -> {
                        renderUponActiveNavigation(state = navigationViewState)
                    }
                    is NavigationViewState.UponArrival -> {
                        renderUponArrival(state = navigationViewState)
                    }
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun actions(): Flow<Action> = merge(
        // Merge multiple actions if required.
        initialStateTransition()
    )

    private fun initialStateTransition() = flowOf(
        NavigationStateTransitionAction.ToEmpty(from = NavigationState.Empty)
    )

    private fun renderUponEmpty(state: NavigationViewState) {
        binding.volumeContainer.visibility = GONE
        binding.recenterContainer.visibility = GONE
        binding.maneuverContainer.visibility = GONE
        binding.infoPanelContainer.visibility = GONE
        binding.speedLimitContainer.visibility = GONE
        binding.routeOverviewContainer.visibility = GONE
        // Based on state populate the data on the views
    }

    private fun renderUponFreeDrive(state: NavigationViewState) {
        binding.volumeContainer.visibility = GONE
        binding.maneuverContainer.visibility = GONE
        binding.recenterContainer.visibility = GONE
        binding.infoPanelContainer.visibility = GONE
        binding.speedLimitContainer.visibility = VISIBLE
        binding.routeOverviewContainer.visibility = VISIBLE
        // Based on state populate the data on the views
    }

    private fun renderUponRoutePreview(state: NavigationViewState) {
        binding.volumeContainer.visibility = GONE
        binding.maneuverContainer.visibility = GONE
        binding.speedLimitContainer.visibility = GONE
        binding.recenterContainer.visibility = VISIBLE
        binding.infoPanelContainer.visibility = VISIBLE
        binding.routeOverviewContainer.visibility = VISIBLE
        // Based on state populate the data on the views
    }

    private fun renderUponActiveNavigation(state: NavigationViewState) {
        binding.volumeContainer.visibility = VISIBLE
        binding.recenterContainer.visibility = VISIBLE
        binding.maneuverContainer.visibility = VISIBLE
        binding.infoPanelContainer.visibility = VISIBLE
        binding.speedLimitContainer.visibility = VISIBLE
        binding.routeOverviewContainer.visibility = VISIBLE
        // Based on state populate the data on the views
    }

    private fun renderUponArrival(state: NavigationViewState) {
        binding.volumeContainer.visibility = VISIBLE
        binding.recenterContainer.visibility = VISIBLE
        binding.maneuverContainer.visibility = VISIBLE
        binding.infoPanelContainer.visibility = VISIBLE
        binding.speedLimitContainer.visibility = VISIBLE
        binding.routeOverviewContainer.visibility = VISIBLE
        // Based on state populate the data on the views
    }
}

