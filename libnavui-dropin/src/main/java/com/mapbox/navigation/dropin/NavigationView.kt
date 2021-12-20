package com.mapbox.navigation.dropin

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
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
import com.mapbox.navigation.dropin.component.maneuver.ManeuverUIComponent
import com.mapbox.navigation.dropin.component.maneuver.ManeuverViewModel
import com.mapbox.navigation.dropin.component.navigationstate.NavigationStateAction
import com.mapbox.navigation.dropin.component.navigationstate.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.recenter.RecenterUIComponent
import com.mapbox.navigation.dropin.component.recenter.RecenterViewModel
import com.mapbox.navigation.dropin.component.routearrow.RouteArrowUIComponent
import com.mapbox.navigation.dropin.component.routearrow.RouteArrowViewModel
import com.mapbox.navigation.dropin.component.routearrow.RouteArrowViewModelFactory
import com.mapbox.navigation.dropin.component.routeline.RouteLineUIComponent
import com.mapbox.navigation.dropin.component.routeline.RouteLineViewModel
import com.mapbox.navigation.dropin.component.routeline.RouteLineViewModelFactory
import com.mapbox.navigation.dropin.component.routeoverview.RouteOverviewUIComponent
import com.mapbox.navigation.dropin.component.routeoverview.RouteOverviewViewModel
import com.mapbox.navigation.dropin.component.sound.SoundButtonUIComponent
import com.mapbox.navigation.dropin.component.sound.SoundButtonViewModel
import com.mapbox.navigation.dropin.component.speedlimit.SpeedLimitUIComponent
import com.mapbox.navigation.dropin.component.speedlimit.SpeedLimitViewModel
import com.mapbox.navigation.dropin.component.tripprogress.TripProgressUIComponent
import com.mapbox.navigation.dropin.component.tripprogress.TripProgressViewModel
import com.mapbox.navigation.dropin.databinding.MapboxLayoutDropInViewBinding
import com.mapbox.navigation.dropin.util.MapboxDropInUtils
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton
import com.mapbox.navigation.ui.maps.camera.view.MapboxRouteOverviewButton
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.voice.view.MapboxSoundButton
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationView : ConstraintLayout {

    val navigationViewApi: MapboxNavigationViewApi by lazy {
        MapboxNavigationViewApiImpl(this)
    }
    private val lifeCycleOwner: LifecycleOwner
    private val activity: FragmentActivity by lazy {
        try {
            context as FragmentActivity
        } catch (exception: ClassCastException) {
            throw ClassCastException(
                "Please ensure that the provided Context is a valid FragmentActivity"
            )
        }
    }
    private val mapView: MapView by lazy {
        MapView(context, mapInitOptions).also {
            it.getMapboxMap().addOnStyleLoadedListener(onStyleLoadedListener)
        }
    }

    private val binding = MapboxLayoutDropInViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    var navigationViewOptions: NavigationViewOptions
        private set

    @VisibleForTesting
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
                                cameraViewModel.consumeLocationUpdate(this)
                            }
                        }
                    )
                }
            )
        }
    }

    // --------------------------------------------------------
    // View Model and dependency definitions
    // --------------------------------------------------------
    private val mapboxNavigationViewModel: MapboxNavigationViewModel by lazy {
        ViewModelProvider(activity)[MapboxNavigationViewModel::class.java]
    }

    private val navigationStateViewModel: NavigationStateViewModel by lazy {
        ViewModelProvider(activity)[NavigationStateViewModel::class.java]
    }

    private val cameraViewModel: CameraViewModel by lazy {
        ViewModelProvider(activity)[CameraViewModel::class.java]
    }

    private val mapInitOptions: MapInitOptions
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

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        null,
        MapInitOptions(context),
        NavigationViewOptions.Builder(context).build()
    )

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        navigationOptions: NavigationOptions?,
        mapInitializationOptions: MapInitOptions = MapInitOptions(context),
        navigationViewOptions: NavigationViewOptions = NavigationViewOptions
            .Builder(context)
            .build()
    ) : super(context, attrs) {
        this.mapInitOptions = mapInitializationOptions
        this.navigationViewOptions = navigationViewOptions
        this.lifeCycleOwner = context as? LifecycleOwner ?: throw LifeCycleOwnerNotFoundException()
        this.lifeCycleOwner.lifecycle.addObserver(lifecycleObserver)

        val loadedNavigationOptions = navigationOptions
            ?: NavigationOptions.Builder(context.applicationContext)
                .accessToken(styledAccessToken(attrs))
                .build()
        MapboxNavigationApp.setup(loadedNavigationOptions)
            .attach(lifeCycleOwner)
    }

    private fun styledAccessToken(attrs: AttributeSet?): String {
        val attrsAccessToken = context.obtainStyledAttributes(
            attrs,
            R.styleable.NavigationView,
            0,
            0
        ).use {
            it.getString(R.styleable.NavigationView_accessToken)
        }
        checkNotNull(attrsAccessToken) {
            "Access token must be provided through xml attributes or constructor injection."
        }
        return attrsAccessToken
    }

    private fun bindRouteLine() {
        val routeLineViewModel = ViewModelProvider(
            activity,
            RouteLineViewModelFactory(navigationViewOptions.mapboxRouteLineOptions)
        )[RouteLineViewModel::class.java]
        val routeLineComponent = RouteLineUIComponent.MapboxRouteLineUIComponent(
            view = mapView,
            viewModel = routeLineViewModel
        )
        uiComponents.add(routeLineComponent)
    }

    private fun bindRouteArrow() {
        val routeArrowViewModel = ViewModelProvider(
            activity,
            RouteArrowViewModelFactory(navigationViewOptions.routeArrowOptions)
        )[RouteArrowViewModel::class.java]
        val routeArrowUIComponent = RouteArrowUIComponent.MapboxRouteArrowUIComponent(
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
                activity,
                ManeuverViewModel.Factory(navigationViewOptions.distanceFormatter)
            )[ManeuverViewModel::class.java]
            ManeuverUIComponent.MapboxManeuverUIComponent(
                container = binding.maneuverContainer,
                view = maneuverView,
                viewModel = maneuverViewModel,
                lifecycleOwner = lifeCycleOwner
            )
        } else {
            binding.maneuverContainer.addView(view)
            ManeuverUIComponent.CustomManeuverUIComponent(
                container = binding.maneuverContainer
            )
        }
        uiComponents.add(maneuverComponent)
    }

    private fun bindRecenterButtonView(view: View?) {
        val recenterComponent = if (view == null) {
            val recenterButtonView = MapboxRecenterButton(context, null)
            binding.recenterContainer.addView(recenterButtonView)
            val recenterViewModel = ViewModelProvider(activity)[RecenterViewModel::class.java]
            RecenterUIComponent.MapboxRecenterUIComponent(
                container = binding.recenterContainer,
                view = recenterButtonView,
                viewModel = recenterViewModel,
                lifecycleOwner = lifeCycleOwner
            )
        } else {
            binding.recenterContainer.addView(view)
            RecenterUIComponent.CustomRecenterUIComponent(
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
                activity
            )[RouteOverviewViewModel::class.java]
            RouteOverviewUIComponent.MapboxRouteOverviewUIComponent(
                container = binding.routeOverviewContainer,
                view = routeOverviewButtonView,
                viewModel = routeOverviewViewModel,
                lifecycleOwner = lifeCycleOwner
            )
        } else {
            binding.routeOverviewContainer.addView(view)
            RouteOverviewUIComponent.CustomRouteOverviewUIComponent(
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
                activity
            )[SoundButtonViewModel::class.java]
            SoundButtonUIComponent.MapboxSoundButtonUIComponent(
                container = binding.volumeContainer,
                view = soundButtonView,
                viewModel = soundButtonViewModel,
                lifeCycleOwner = lifeCycleOwner
            )
        } else {
            binding.volumeContainer.addView(view)
            SoundButtonUIComponent.CustomSoundButtonUIComponent(
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
                activity,
                SpeedLimitViewModel.Factory(navigationViewOptions.speedLimitFormatter)
            )[SpeedLimitViewModel::class.java]
            SpeedLimitUIComponent.MapboxSpeedLimitUIComponent(
                container = binding.speedLimitContainer,
                view = speedLimitView,
                viewModel = speedLimitViewModel,
                lifecycleOwner = lifeCycleOwner
            )
        } else {
            binding.speedLimitContainer.addView(view)
            SpeedLimitUIComponent.CustomSpeedLimitUIComponent(
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
                activity,
                TripProgressViewModel.Factory(navigationViewOptions.tripProgressUpdateFormatter)
            )[TripProgressViewModel::class.java]
            TripProgressUIComponent.MapboxTripProgressUIComponent(
                container = binding.infoPanelContainer,
                view = tripProgressView,
                viewModel = tripProgressViewModel,
                lifeCycleOwner = lifeCycleOwner
            )
        } else {
            binding.infoPanelContainer.addView(view)
            TripProgressUIComponent.CustomTripProgressUIComponent(
                container = binding.infoPanelContainer
            )
        }
        uiComponents.add(tripProgressComponent)
    }

    private fun observeNavigationState() {
        lifeCycleOwner.lifecycleScope.launch {
            navigationStateViewModel.navigationState().collect { state ->
                uiComponents.forEach {
                    it.onNavigationStateChanged(state)
                }
            }
        }
    }

    private fun observeRoutes() {
        lifeCycleOwner.lifecycleScope.launch {
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

    private fun observeRouteProgress() {
        lifeCycleOwner.lifecycleScope.launch {
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
        lifeCycleOwner.lifecycleScope.launch {
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
        lifeCycleOwner.lifecycleScope.launch {
            mapboxNavigationViewModel.rawLocationUpdates().collect { locationUpdate ->
                externalLocationObservers.forEach {
                    it.onNewRawLocation(locationUpdate)
                }
                cameraViewModel.consumeLocationUpdate(locationUpdate)
            }
        }
    }

    private fun observeWaypointArrivals() {
        lifeCycleOwner.lifecycleScope.launch {
            mapboxNavigationViewModel.wayPointArrivals.collect { routeProgress ->
                externalArrivalObservers.forEach {
                    it.onWaypointArrival(routeProgress)
                }
            }
        }
    }

    private fun observeNextRouteLegStart() {
        lifeCycleOwner.lifecycleScope.launch {
            mapboxNavigationViewModel.nextRouteLegStartUpdates.collect { routeLegProgress ->
                externalArrivalObservers.forEach {
                    it.onNextRouteLegStart(routeLegProgress)
                }
            }
        }
    }

    private fun observeFinalDestinationArrivals() {
        lifeCycleOwner.lifecycleScope.launch {
            mapboxNavigationViewModel.finalDestinationArrivals.collect { routeProgress ->
                externalArrivalObservers.forEach {
                    it.onFinalDestinationArrival(routeProgress)
                }
            }
        }
    }

    private fun observeVoiceInstructions() {
        lifeCycleOwner.lifecycleScope.launch {
            mapboxNavigationViewModel.voiceInstructions.collect { voiceInstructions ->
                // view models that need voice instruction updates should be added here
                externalVoiceInstructionsObservers.forEach {
                    it.onNewVoiceInstructions(voiceInstructions)
                }
            }
        }
    }

    private fun observeBannerInstructions() {
        lifeCycleOwner.lifecycleScope.launch {
            mapboxNavigationViewModel.bannerInstructions.collect { bannerInstructions ->
                externalBannerInstructionObservers.forEach {
                    it.onNewBannerInstructions(bannerInstructions)
                }
            }
        }
    }

    private fun observeTripSession() {
        lifeCycleOwner.lifecycleScope.launch {
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
        lifeCycleOwner.lifecycleScope.launch {
            cameraViewModel.cameraUpdates.collect {
                mapView.camera.easeTo(it.first, it.second)
            }
        }
    }

    private fun performActions() {
        lifeCycleOwner.lifecycleScope.launch {
            navigationStateViewModel.processAction(
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
}
