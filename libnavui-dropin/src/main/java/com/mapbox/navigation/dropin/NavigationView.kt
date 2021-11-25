package com.mapbox.navigation.dropin

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.camera.CameraViewModel
import com.mapbox.navigation.dropin.component.camera.MapboxCameraUIComponent
import com.mapbox.navigation.dropin.component.maneuver.CustomManeuverUIComponent
import com.mapbox.navigation.dropin.component.maneuver.ManeuverViewModel
import com.mapbox.navigation.dropin.component.maneuver.MapboxManeuverUIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationStateAction
import com.mapbox.navigation.dropin.component.navigationstate.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.recenter.CustomRecenterUIComponent
import com.mapbox.navigation.dropin.component.recenter.MapboxRecenterUIComponent
import com.mapbox.navigation.dropin.component.recenter.OnRecenterButtonClickedListener
import com.mapbox.navigation.dropin.component.recenter.RecenterViewModel
import com.mapbox.navigation.dropin.component.routearrow.MapboxRouteArrowUIComponent
import com.mapbox.navigation.dropin.component.routearrow.RouteArrowViewModel
import com.mapbox.navigation.dropin.component.routearrow.RouteArrowViewModelFactory
import com.mapbox.navigation.dropin.component.routeline.MapboxRouteLineUIComponent
import com.mapbox.navigation.dropin.component.routeline.RouteLineViewModel
import com.mapbox.navigation.dropin.component.routeline.RouteLineViewModelFactory
import com.mapbox.navigation.dropin.component.routeoverview.CustomRouteOverviewUIComponent
import com.mapbox.navigation.dropin.component.routeoverview.MapboxRouteOverviewUIComponent
import com.mapbox.navigation.dropin.component.routeoverview.OnOverviewButtonClickedListener
import com.mapbox.navigation.dropin.component.routeoverview.RouteOverviewViewModel
import com.mapbox.navigation.dropin.component.sound.CustomSoundButtonUIComponent
import com.mapbox.navigation.dropin.component.sound.MapboxSoundButtonUIComponent
import com.mapbox.navigation.dropin.component.sound.SoundButtonViewModel
import com.mapbox.navigation.dropin.component.speedlimit.CustomSpeedLimitUIComponent
import com.mapbox.navigation.dropin.component.speedlimit.MapboxSpeedLimitUIComponent
import com.mapbox.navigation.dropin.component.speedlimit.SpeedLimitViewModel
import com.mapbox.navigation.dropin.component.tripprogress.CustomTripProgressUIComponent
import com.mapbox.navigation.dropin.component.tripprogress.MapboxTripProgressUIComponent
import com.mapbox.navigation.dropin.component.tripprogress.TripProgressViewModel
import com.mapbox.navigation.dropin.databinding.MapboxLayoutDropInViewBinding
import com.mapbox.navigation.dropin.util.MapboxDropInUtils
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton
import com.mapbox.navigation.ui.maps.camera.view.MapboxRouteOverviewButton
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.utils.internal.extensions.unwrapIfNeeded
import com.mapbox.navigation.ui.utils.internal.lifecycle.ViewLifecycleRegistry
import com.mapbox.navigation.ui.utils.internal.lifecycle.keepExecutingWhenStarted
import com.mapbox.navigation.ui.voice.view.MapboxSoundButton
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet

/**
 * If the [NavigationView] is used in a [Fragment], you can use the fragment as [LifecycleOwner] and [ViewModelStoreOwner]
 * to tighten lifecycle and [ViewModel]s memory management to be cleaned up whenever the hosting [Fragment] is destroyed.
 *
 * @param lifecycleOwner wrapping lifecycle owner of this [View], by default uses the hosting [Activity].
 * Internal operations on this [NavigationView] will be run based on this [LifecycleOwner.getLifecycle] merged with
 * [View.OnAttachStateChangeListener], stopping whenever the view is detached.
 * @param viewModelStoreOwner provider and scope of the [ViewModel]s used by this view and nested UI components,
 * by default uses the hosting [Activity].
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class NavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    accessToken: String = attrs.let {
        attrs?.findAccessToken(context) ?: throw IllegalArgumentException(
            "Provide access token directly in the constructor or via 'accessToken' layout parameter"
        )
    },
    navigationOptions: NavigationOptions = NavigationOptions.Builder(context.applicationContext)
        .run {
            accessToken(accessToken)
            build()
        },
    mapInitializationOptions: MapInitOptions = MapInitOptions(context),
    navigationViewOptions: NavigationViewOptions = NavigationViewOptions
        .Builder(context)
        .build(),
    lifecycleOwner: LifecycleOwner =
        context.unwrapIfNeeded() as? LifecycleOwner ?: throw IllegalArgumentException(
            "Please ensure that the hosting Context is a valid LifecycleOwner"
        ),
    private val viewModelStoreOwner: ViewModelStoreOwner =
        context.unwrapIfNeeded() as? ViewModelStoreOwner ?: throw IllegalArgumentException(
            "Please ensure that the hosting Context is a valid ViewModelStoreOwner"
        )
) : ConstraintLayout(context, attrs), LifecycleOwner {

    val navigationViewApi: MapboxNavigationViewApi by lazy {
        MapboxNavigationViewApiImpl(this)
    }
    private val viewLifecycleRegistry: ViewLifecycleRegistry = ViewLifecycleRegistry(
        view = this,
        localLifecycleOwner = this,
        hostingLifecycleOwner = lifecycleOwner,
    )
    private val mapView: MapView by lazy {
        MapView(context, mapInitOptions).also {
            it.getMapboxMap().addOnStyleLoadedListener(onStyleLoadedListener)
        }
    }

    private val binding = MapboxLayoutDropInViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    var navigationViewOptions: NavigationViewOptions = navigationViewOptions
        private set

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            mapView.location.apply {
                this.locationPuck = LocationPuck2D(
                    bearingImage = ContextCompat.getDrawable(
                        this@NavigationView.context,
                        R.drawable.mapbox_navigation_puck_icon
                    )
                )
                setLocationProvider(navigationLocationProvider)
                enabled = true
                uiComponents.forEach { uiComponent ->
                    when (uiComponent) {
                        is OnIndicatorPositionChangedListener -> {
                            this.addOnIndicatorPositionChangedListener(uiComponent)
                        }
                    }
                }
            }
        }
    }

    // --------------------------------------------------------
    // View Model and dependency definitions
    // --------------------------------------------------------
    private val mapboxNavigationViewModel: MapboxNavigationViewModel
    private val routeLineViewModel: RouteLineViewModel
    private val navigationStateViewModel: NavigationStateViewModel
    private val cameraViewModel: CameraViewModel

    private val mapInitOptions: MapInitOptions = mapInitializationOptions
    private val navigationLocationProvider = NavigationLocationProvider()

    @VisibleForTesting
    internal val externalRouteProgressObservers = CopyOnWriteArraySet<RouteProgressObserver>()
    private val externalLocationObservers = CopyOnWriteArraySet<LocationObserver>()
    private val externalRoutesObservers = CopyOnWriteArraySet<RoutesObserver>()
    private val externalArrivalObservers = CopyOnWriteArraySet<ArrivalObserver>()
    private val externalBannerInstructionObservers =
        CopyOnWriteArraySet<BannerInstructionsObserver>()
    private val externalTripSessionStateObservers = CopyOnWriteArraySet<TripSessionStateObserver>()
    private val externalVoiceInstructionsObservers =
        CopyOnWriteArraySet<VoiceInstructionsObserver>()

    private val uiComponents: MutableList<UIComponent> = mutableListOf()

    override fun getLifecycle(): Lifecycle = viewLifecycleRegistry

    private fun bindRouteLine() {
        val routeLineViewModel = ViewModelProvider(
            viewModelStoreOwner,
            RouteLineViewModelFactory(navigationViewOptions.mapboxRouteLineOptions)
        )[RouteLineViewModel::class.java]
        val routeLineComponent = MapboxRouteLineUIComponent(
            view = mapView,
            viewModel = routeLineViewModel
        )
        uiComponents.add(routeLineComponent)
    }

    private fun bindRouteArrow() {
        val routeArrowViewModel = ViewModelProvider(
            viewModelStoreOwner,
            RouteArrowViewModelFactory(navigationViewOptions.routeArrowOptions)
        )[RouteArrowViewModel::class.java]
        val routeArrowUIComponent = MapboxRouteArrowUIComponent(
            view = mapView,
            viewModel = routeArrowViewModel
        )
        uiComponents.add(routeArrowUIComponent)
    }

    private fun bindManeuverView(view: View?) {
        val maneuverComponent = if (view == null) {
            val maneuverView = MapboxManeuverView(context, null)
            binding.maneuverContainer.addView(maneuverView)
            val maneuverViewModel = ViewModelProvider(
                viewModelStoreOwner,
                ManeuverViewModel.Factory(navigationViewOptions.distanceFormatter)
            )[ManeuverViewModel::class.java]
            MapboxManeuverUIComponent(
                container = binding.maneuverContainer,
                view = maneuverView,
                viewModel = maneuverViewModel,
                lifecycleOwner = this
            )
        } else {
            binding.maneuverContainer.addView(view)
            CustomManeuverUIComponent(
                container = binding.maneuverContainer
            )
        }
        uiComponents.add(maneuverComponent)
    }

    private fun bindRecenterButtonView(view: View?) {
        val recenterComponent = if (view == null) {
            val recenterButtonView = MapboxRecenterButton(context, null)
            binding.recenterContainer.addView(recenterButtonView)
            val recenterViewModel =
                ViewModelProvider(viewModelStoreOwner)[RecenterViewModel::class.java]
            MapboxRecenterUIComponent(
                container = binding.recenterContainer,
                view = recenterButtonView,
                viewModel = recenterViewModel,
                lifecycleOwner = this
            ).also { recenterComponent ->
                recenterComponent.registerOnRecenterButtonClickedListener {
                    uiComponents
                        .filter { it is OnRecenterButtonClickedListener }
                        .map { it as OnRecenterButtonClickedListener }
                        .forEach {
                            it.onRecenterButtonClicked()
                        }
                }
            }
        } else {
            binding.recenterContainer.addView(view)
            CustomRecenterUIComponent(
                container = binding.recenterContainer
            )
        }
        uiComponents.add(recenterComponent)
    }

    private fun bindRouteOverviewButtonView(view: View?) {
        val routeOverviewComponent = if (view == null) {
            val routeOverviewButtonView = MapboxRouteOverviewButton(context, null)
            binding.routeOverviewContainer.addView(routeOverviewButtonView)
            val routeOverviewViewModel = ViewModelProvider(
                viewModelStoreOwner
            )[RouteOverviewViewModel::class.java]
            MapboxRouteOverviewUIComponent(
                container = binding.routeOverviewContainer,
                view = routeOverviewButtonView,
                viewModel = routeOverviewViewModel,
                lifecycleOwner = this
            ).also { overviewComponent ->
                overviewComponent.registerOnOverviewButtonClickedListener {
                    uiComponents
                        .filter { it is OnOverviewButtonClickedListener }
                        .map { it as OnOverviewButtonClickedListener }
                        .forEach {
                            it.onOverviewButtonClicked()
                        }
                }
            }
        } else {
            binding.routeOverviewContainer.addView(view)
            CustomRouteOverviewUIComponent(
                container = binding.routeOverviewContainer
            )
        }
        uiComponents.add(routeOverviewComponent)
    }

    private fun bindSoundButtonView(view: View?) {
        val soundComponent = if (view == null) {
            val soundButtonView = MapboxSoundButton(context, null)
            binding.volumeContainer.addView(soundButtonView)
            val soundButtonViewModel = ViewModelProvider(
                viewModelStoreOwner
            )[SoundButtonViewModel::class.java]
            MapboxSoundButtonUIComponent(
                container = binding.volumeContainer,
                view = soundButtonView,
                viewModel = soundButtonViewModel,
                lifeCycleOwner = this
            )
        } else {
            binding.volumeContainer.addView(view)
            CustomSoundButtonUIComponent(
                container = binding.volumeContainer
            )
        }
        uiComponents.add(soundComponent)
    }

    private fun bindSpeedLimitView(view: View?) {
        val speedLimitComponent = if (view == null) {
            val speedLimitView = MapboxSpeedLimitView(context, null)
            binding.speedLimitContainer.addView(speedLimitView)
            val speedLimitViewModel = ViewModelProvider(
                viewModelStoreOwner,
                SpeedLimitViewModel.Factory(navigationViewOptions.speedLimitFormatter)
            )[SpeedLimitViewModel::class.java]
            MapboxSpeedLimitUIComponent(
                container = binding.speedLimitContainer,
                view = speedLimitView,
                viewModel = speedLimitViewModel,
                lifecycleOwner = this
            )
        } else {
            binding.speedLimitContainer.addView(view)
            CustomSpeedLimitUIComponent(
                container = binding.speedLimitContainer
            )
        }
        uiComponents.add(speedLimitComponent)
    }

    private fun bindTripProgressView(view: View?) {
        val tripProgressComponent = if (view == null) {
            val tripProgressView = MapboxTripProgressView(context, null)
            binding.infoPanelContainer.addView(tripProgressView)
            val tripProgressViewModel = ViewModelProvider(
                viewModelStoreOwner,
                TripProgressViewModel.Factory(navigationViewOptions.tripProgressUpdateFormatter)
            )[TripProgressViewModel::class.java]
            MapboxTripProgressUIComponent(
                container = binding.infoPanelContainer,
                view = tripProgressView,
                viewModel = tripProgressViewModel,
                lifeCycleOwner = this
            )
        } else {
            binding.infoPanelContainer.addView(view)
            CustomTripProgressUIComponent(
                container = binding.infoPanelContainer
            )
        }
        uiComponents.add(tripProgressComponent)
    }

    private fun bindCamera() {
        val cameraComponent = MapboxCameraUIComponent(
            mapView = mapView,
            viewModel = ViewModelProvider(viewModelStoreOwner)[CameraViewModel::class.java],
            lifecycleOwner = this
        )
        MapboxDropInUtils.getLastLocation(
            this@NavigationView.context,
            WeakReference<(Expected<Exception, LocationEngineResult>) -> Unit> { result ->
                result.fold(
                    {
                        Log.e(TAG, "Error obtaining current location", it)
                    },
                    {
                        it.lastLocation?.apply {
                            navigationLocationProvider.changePosition(
                                this,
                                listOf(),
                                null,
                                null
                            )
                            cameraComponent.onNewRawLocation(this)
                        }
                    }
                )
            }
        )
        keepExecutingWhenStarted {
            cameraComponent.navigationCameraState.collect {
                uiComponents.forEach { uiComponent ->
                    when (uiComponent) {
                        is NavigationCameraStateChangedObserver ->
                            uiComponent.onNavigationCameraStateChanged(it)
                    }
                }
            }
        }
        uiComponents.add(cameraComponent)
    }

    private fun observeNavigationState() {
        keepExecutingWhenStarted {
            navigationStateViewModel.state.collect { state ->
                uiComponents.forEach {
                    it.onNavigationStateChanged(state)
                }
            }
        }
    }

    private fun observeRoutes() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.routesUpdatedResults.collect { result ->
                externalRoutesObservers.forEach {
                    it.onRoutesChanged(result)
                }
                uiComponents.forEach { uiComponent ->
                    when (uiComponent) {
                        is RoutesObserver -> uiComponent.onRoutesChanged(result)
                    }
                }
            }
        }
    }

    private fun observeRouteResets() {
        keepExecutingWhenStarted {
            routeLineViewModel.routeResets.collect { routes ->
                mapboxNavigationViewModel.setRoutes(routes)
            }
        }
    }

    private fun observeRouteProgress() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.routeProgressUpdates.collect { routeProgress ->
                externalRouteProgressObservers.forEach {
                    it.onRouteProgressChanged(routeProgress)
                }
                uiComponents.forEach { uiComponent ->
                    when (uiComponent) {
                        is RouteProgressObserver -> {
                            uiComponent.onRouteProgressChanged(routeProgress)
                        }
                    }
                }
            }
        }
    }

    private fun observeLocationMatcherResults() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.newLocationMatcherResults.collect { locationMatcherResult ->
                externalLocationObservers.forEach {
                    it.onNewLocationMatcherResult(locationMatcherResult)
                }
                uiComponents.forEach { uiComponent ->
                    when (uiComponent) {
                        is LocationObserver -> {
                            uiComponent.onNewLocationMatcherResult(locationMatcherResult)
                        }
                    }
                }
                navigationLocationProvider.changePosition(
                    locationMatcherResult.enhancedLocation,
                    locationMatcherResult.keyPoints
                )
            }
        }
    }

    private fun observeRawLocation() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.rawLocationUpdates.collect { locationUpdate ->
                externalLocationObservers.forEach {
                    it.onNewRawLocation(locationUpdate)
                }
            }
        }
    }

    private fun observeWaypointArrivals() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.wayPointArrivals.collect { routeProgress ->
                externalArrivalObservers.forEach {
                    it.onWaypointArrival(routeProgress)
                }
            }
        }
    }

    private fun observeNextRouteLegStart() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.nextRouteLegStartUpdates.collect { routeLegProgress ->
                externalArrivalObservers.forEach {
                    it.onNextRouteLegStart(routeLegProgress)
                }
            }
        }
    }

    private fun observeFinalDestinationArrivals() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.finalDestinationArrivals.collect { routeProgress ->
                externalArrivalObservers.forEach {
                    it.onFinalDestinationArrival(routeProgress)
                }
            }
        }
    }

    private fun observeVoiceInstructions() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.voiceInstructions.collect { voiceInstructions ->
                // view models that need voice instruction updates should be added here
                externalVoiceInstructionsObservers.forEach {
                    it.onNewVoiceInstructions(voiceInstructions)
                }
            }
        }
    }

    private fun observeBannerInstructions() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.bannerInstructions.collect { bannerInstructions ->
                externalBannerInstructionObservers.forEach {
                    it.onNewBannerInstructions(bannerInstructions)
                }
            }
        }
    }

    private fun observeTripSession() {
        keepExecutingWhenStarted {
            mapboxNavigationViewModel.tripSessionStateUpdates.collect { tripSessionState ->
                externalTripSessionStateObservers.forEach {
                    it.onSessionStateChanged(tripSessionState)
                }
            }
        }
    }

    private val onStyleLoadedListener = OnStyleLoadedListener { styleLoadedEventData ->
        uiComponents.forEach { uiComponent ->
            when (uiComponent) {
                is OnStyleLoadedListener -> {
                    uiComponent.onStyleLoaded(styleLoadedEventData)
                }
            }
        }
    }

    private fun renderStateMutations() {
        observeRoutes()
        observeTripSession()
        observeRawLocation()
        observeRouteProgress()
        observeWaypointArrivals()
        observeNextRouteLegStart()
        observeVoiceInstructions()
        observeBannerInstructions()
        observeLocationMatcherResults()
        observeFinalDestinationArrivals()
        observeNavigationState()
    }

    private fun performActions() {
        keepExecutingWhenStarted {
            navigationStateViewModel.consumeAction(
                flowOf(
                    NavigationStateAction.ToRoutePreview
                )
            )
        }
    }

    internal fun retrieveMapView(): MapView = mapView

    internal fun updateNavigationViewOptions(navigationViewOptions: NavigationViewOptions) {
        this.navigationViewOptions = navigationViewOptions
    }

    internal fun configure(viewProvider: ViewProvider) {
        binding.mapContainer.addView(mapView)
        bindRouteLine()
        bindRouteArrow()
        bindManeuverView(viewProvider.maneuverProvider?.invoke())
        bindSpeedLimitView(viewProvider.speedLimitProvider?.invoke())
        bindSoundButtonView(viewProvider.soundButtonProvider?.invoke())
        bindTripProgressView(viewProvider.tripProgressProvider?.invoke())
        bindRecenterButtonView(viewProvider.recenterButtonProvider?.invoke())
        bindRouteOverviewButtonView(viewProvider.recenterButtonProvider?.invoke())
        bindCamera()

        renderStateMutations()
        performActions()

        updateNavigationViewOptions(navigationViewOptions)
    }

    internal fun addRouteProgressObserver(observer: RouteProgressObserver) {
        externalRouteProgressObservers.add(observer)
    }

    internal fun removeRouteProgressObserver(observer: RouteProgressObserver) {
        externalRouteProgressObservers.remove(observer)
    }

    internal fun addLocationObserver(observer: LocationObserver) {
        externalLocationObservers.add(observer)
    }

    internal fun removeLocationObserver(observer: LocationObserver) {
        externalLocationObservers.remove(observer)
    }

    internal fun addRoutesObserver(observer: RoutesObserver) {
        externalRoutesObservers.add(observer)
    }

    internal fun removeRoutesObserver(observer: RoutesObserver) {
        externalRoutesObservers.remove(observer)
    }

    internal fun addArrivalObserver(observer: ArrivalObserver) {
        externalArrivalObservers.add(observer)
    }

    internal fun removeArrivalObserver(observer: ArrivalObserver) {
        externalArrivalObservers.remove(observer)
    }

    internal fun addBannerInstructionsObserver(observer: BannerInstructionsObserver) {
        externalBannerInstructionObservers.add(observer)
    }

    internal fun removeBannerInstructionsObserver(observer: BannerInstructionsObserver) {
        externalBannerInstructionObservers.remove(observer)
    }

    internal fun addTripSessionStateObserver(observer: TripSessionStateObserver) {
        externalTripSessionStateObservers.add(observer)
    }

    internal fun removeTripSessionStateObserver(observer: TripSessionStateObserver) {
        externalTripSessionStateObservers.remove(observer)
    }

    internal fun addVoiceInstructionObserver(observer: VoiceInstructionsObserver) {
        externalVoiceInstructionsObservers.add(observer)
    }

    internal fun removeVoiceInstructionObserver(observer: VoiceInstructionsObserver) {
        externalVoiceInstructionsObservers.remove(observer)
    }

    // this is temporary so that we can use the replay engine or otherwise start navigation
    // for further development.
    internal fun temporaryStartNavigation() {
        when (navigationViewOptions.useReplayEngine) {
            false -> mapboxNavigationViewModel.startTripSession()
            true -> {
                ifNonNull(navigationLocationProvider.lastLocation) { location ->
                    mapboxNavigationViewModel.startSimulatedTripSession(location)
                }
            }
        }
    }

    internal fun setRoutes(routes: List<DirectionsRoute>) {
        mapboxNavigationViewModel.setRoutes(routes)
    }

    internal fun fetchAndSetRoute(points: List<Point>) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(context)
            .coordinatesList(points)
            .alternatives(true)
            .build()

        fetchAndSetRoute(routeOptions)
    }

    internal fun fetchAndSetRoute(routeOptions: RouteOptions) {
        mapboxNavigationViewModel.fetchAndSetRoute(routeOptions)
    }

    companion object {
        private val TAG = NavigationView::class.java.simpleName
    }

    init {
        mapboxNavigationViewModel = ViewModelProvider(
            viewModelStoreOwner
        )[MapboxNavigationViewModel::class.java]
        routeLineViewModel = ViewModelProvider(
            viewModelStoreOwner,
            RouteLineViewModelFactory(navigationViewOptions.mapboxRouteLineOptions)
        )[RouteLineViewModel::class.java]
        navigationStateViewModel = ViewModelProvider(
            viewModelStoreOwner
        )[NavigationStateViewModel::class.java]
        cameraViewModel = ViewModelProvider(
            viewModelStoreOwner
        )[CameraViewModel::class.java]
        lifecycle.addObserver(lifecycleObserver)
        MapboxNavigationApp.setup(navigationOptions)
            .attach(this)
    }
}

private fun AttributeSet.findAccessToken(context: Context): String? {
    return context.obtainStyledAttributes(
        this,
        R.styleable.NavigationView,
        0,
        0
    ).use {
        it.getString(R.styleable.NavigationView_accessToken)
    }
}
