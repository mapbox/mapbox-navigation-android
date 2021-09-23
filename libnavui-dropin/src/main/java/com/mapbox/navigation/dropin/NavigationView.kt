package com.mapbox.navigation.dropin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.contract.ContainerVisibilityAction
import com.mapbox.navigation.dropin.contract.NavigationStateAction
import com.mapbox.navigation.dropin.databinding.MapboxLayoutDropInViewBinding
import com.mapbox.navigation.dropin.state.ContainerVisibilityState
import com.mapbox.navigation.dropin.viewmodel.ContainerVisibilityViewModel
import com.mapbox.navigation.dropin.viewmodel.NavigationStateViewModel
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton
import com.mapbox.navigation.ui.maps.camera.view.MapboxRouteOverviewButton
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.view.MapboxSoundButton
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

@ExperimentalCoroutinesApi
class NavigationView : ConstraintLayout, LifecycleObserver {

    val mapView: MapView
    lateinit var navigationViewModel: NavigationViewModel
        private set

    private val activity: FragmentActivity by lazy {
        try {
            context as FragmentActivity
        } catch (exception: ClassCastException) {
            throw ClassCastException(
                "Please ensure that the provided Context is a valid FragmentActivity"
            )
        }
    }
    private val lifeCycleOwner: LifecycleOwner
    private val binding = MapboxLayoutDropInViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private var navigationViewOptions: NavigationViewOptions

    private lateinit var maneuverView: View
    private lateinit var speedLimitView: View
    private lateinit var soundButtonView: View
    private lateinit var tripProgressView: View
    private lateinit var recenterButtonView: View
    private lateinit var routeOverviewButtonView: View
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigationStateViewModel: NavigationStateViewModel
    private lateinit var containerVisibilityViewModel: ContainerVisibilityViewModel

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        null,
        MapInitOptions(context),
        NavigationViewOptions.Builder().build()
    )

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        accessToken: String?,
        mapInitOptions: MapInitOptions = MapInitOptions(context),
        navigationViewOptions: NavigationViewOptions = NavigationViewOptions.Builder().build()
    ) : super(context, attrs) {
        this.navigationViewModel = if (accessToken != null) {
            ViewModelProvider(
                activity,
                NavigationViewModelFactory(accessToken, activity.application)
            )[NavigationViewModel::class.java]
        } else {
            val a = context.obtainStyledAttributes(attrs, R.styleable.NavigationView, 0, 0)
            val attrsAccessToken = a.getString(R.styleable.NavigationView_accessToken)
            a.recycle()
            ifNonNull(attrsAccessToken) { token ->
                ViewModelProvider(
                    activity,
                    NavigationViewModelFactory(token, activity.application)
                )[NavigationViewModel::class.java]
            } ?: throw IllegalStateException(
                "Access token must be provided through xml attributes or constructor injection."
            )
        }
        this.navigationViewOptions = navigationViewOptions
        this.mapboxNavigation = navigationViewModel.mapboxNavigation
        initializeViewModels()
        this.lifeCycleOwner = context as? LifecycleOwner ?: throw LifeCycleOwnerNotFoundException()
        this.lifeCycleOwner.lifecycle.addObserver(this)
        this.mapView = MapView(context, mapInitOptions)
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

        renderStateMutations()
        performActions()

        update(navigationViewOptions)
    }

    fun update(navigationViewOptions: NavigationViewOptions) {
        this.navigationViewOptions = navigationViewOptions
        // todo trigger relayout
    }

    private fun initializeViewModels() {
        this.navigationStateViewModel =
            ViewModelProvider(activity).get(NavigationStateViewModel::class.java)
        this.containerVisibilityViewModel =
            ViewModelProvider(activity).get(ContainerVisibilityViewModel::class.java)
    }

    private fun performActions() {
        lifeCycleOwner.lifecycleScope.launch {
            navigationStateViewModel.processAction(
                flowOf(
                    NavigationStateAction.ToEmpty
                )
            )
            containerVisibilityViewModel.processAction(
                flowOf(
                    ContainerVisibilityAction.ForEmpty
                )
            )
        }
    }

    private fun renderStateMutations() {
        lifeCycleOwner.lifecycleScope.launch {
            navigationStateViewModel.navigationState().collect { _ ->
            }
        }
        lifeCycleOwner.lifecycleScope.launch {
            containerVisibilityViewModel.containerVisibilityState().collect { state ->
                renderVisibility(state)
            }
        }
    }

    private fun renderVisibility(state: ContainerVisibilityState) {
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
