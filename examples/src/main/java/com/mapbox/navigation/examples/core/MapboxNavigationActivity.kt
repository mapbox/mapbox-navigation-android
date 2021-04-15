package com.mapbox.navigation.examples.core

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.delegates.listeners.eventdata.MapLoadErrorType
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.getGesturesPlugin
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.getLocationComponentPlugin
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityNavigationBinding
import com.mapbox.navigation.examples.core.waypoints.WaypointsController
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.ManeuverCallback
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.api.StepDistanceRemainingCallback
import com.mapbox.navigation.ui.maneuver.api.UpcomingManeuverListCallback
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.StepDistanceError
import com.mapbox.navigation.ui.maps.arrival.api.MapboxBuildingArrivalApi
import com.mapbox.navigation.ui.maps.arrival.api.MapboxBuildingHighlightApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MapboxNavigationActivity :
    AppCompatActivity(),
    OnMapLongClickListener {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var binding: LayoutActivityNavigationBinding
    private lateinit var locationComponent: LocationComponentPlugin
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var tripProgressApi: MapboxTripProgressApi
    private lateinit var maneuverApi: MapboxManeuverApi
    private lateinit var speechAPI: MapboxSpeechApi

    private var isMuted = false
    private var isNavigating = false
    private var routeLineAPI: MapboxRouteLineApi? = null
    private var routeLineView: MapboxRouteLineView? = null
    private var routeArrowView: MapboxRouteArrowView? = null
    private var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer? = null
    private val mapboxReplayer: MapboxReplayer = MapboxReplayer()
    private val buildingsArrivalApi = MapboxBuildingArrivalApi()
    private val waypointsController = WaypointsController()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private val routeArrowAPI: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private val navigationLocationProvider = NavigationLocationProvider()
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            40.0 * pixelDensity,
            40.0 * pixelDensity,
            40.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            20.0 * pixelDensity,
            mapboxMap.getSize().width.toDouble() / 1.75,
            20.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            mapboxMap.getSize().height.toDouble() * 2.0 / 3.0,
            0.0 * pixelDensity,
            0.0 * pixelDensity,
            0.0 * pixelDensity
        )
    }
    private val landscapeFollowingEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            mapboxMap.getSize().height.toDouble() * 2.0 / 5.0,
            mapboxMap.getSize().width.toDouble() / 2.0,
            0.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    private val muteUnmuteCallback = object : MapboxNavigationConsumer<Boolean> {
        override fun accept(value: Boolean) {
            isMuted = value
        }
    }

    private val currentManeuverCallback = object : ManeuverCallback {
        override fun onManeuver(maneuver: Expected<Maneuver, ManeuverError>) {
            binding.maneuverView.renderManeuver(maneuver)
        }
    }

    private val stepDistanceRemainingCallback = object : StepDistanceRemainingCallback {
        override fun onStepDistanceRemaining(
            distanceRemaining: Expected<StepDistance, StepDistanceError>
        ) {

            when (distanceRemaining) {
                is Expected.Success -> {
                    binding.maneuverView.renderDistanceRemaining(distanceRemaining.value)
                }
                is Expected.Failure -> {
                    // Not handled
                }
            }
        }
    }

    private val upcomingManeuversCallback = object : UpcomingManeuverListCallback {
        override fun onUpcomingManeuvers(maneuvers: Expected<List<Maneuver>, ManeuverError>) {
            when (maneuvers) {
                is Expected.Success -> {
                    binding.maneuverView.renderUpcomingManeuvers(maneuvers.value)
                }
                is Expected.Failure -> {
                    // Not handled
                }
            }
        }
    }

    private val voiceInstructionsPlayerCallback =
        object : MapboxNavigationConsumer<SpeechAnnouncement> {
            override fun accept(consumer: SpeechAnnouncement) {
                speechAPI.clean(consumer)
            }
        }

    private val speechCallback =
        object : MapboxNavigationConsumer<Expected<SpeechValue, SpeechError>> {
            override fun accept(consumer: Expected<SpeechValue, SpeechError>) {
                when (consumer) {
                    is Expected.Success -> {
                        val currentSpeechValue = consumer.value
                        voiceInstructionsPlayer?.play(
                            currentSpeechValue.announcement,
                            voiceInstructionsPlayerCallback
                        )
                    }
                    is Expected.Failure -> {
                        val currentSpeechError = consumer.error
                        voiceInstructionsPlayer?.play(
                            currentSpeechError.fallback,
                            voiceInstructionsPlayerCallback
                        )
                    }
                }
            }
        }

    private val mapMatcherResultObserver = object : MapMatcherResultObserver {
        override fun onNewMapMatcherResult(mapMatcherResult: MapMatcherResult) {
            val transitionOptions: (ValueAnimator.() -> Unit)? = if (mapMatcherResult.isTeleport) {
                {
                    duration = 0
                }
            } else {
                {
                    duration = 1000
                }
            }
            navigationLocationProvider.changePosition(
                mapMatcherResult.enhancedLocation,
                mapMatcherResult.keyPoints,
                latLngTransitionOptions = transitionOptions,
                bearingTransitionOptions = transitionOptions
            )
            viewportDataSource.onLocationChanged(mapMatcherResult.enhancedLocation)

            viewportDataSource.evaluate()
            if (mapMatcherResult.isTeleport) {
                navigationCamera.resetFrame()
            }
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    routeLineAPI?.setRoutes(listOf(RouteLine(routes[0], null)))?.apply {
                        ifNonNull(routeLineView, mapboxMap.getStyle()) { view, style ->

                            view.renderRouteDrawData(style, this)
                        }
                    }
                }

                viewportDataSource.onRouteChanged(routes[0])
                if (!isNavigating) {
                    binding.start.visibility = VISIBLE
                    updateCameraToOverview()
                }
            } else {
                viewportDataSource.clearRouteData()
                updateCameraToIdle()
                clearRouteLine()
            }
        }
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        routeLineAPI?.updateTraveledRouteLine(point)
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            viewportDataSource.onRouteProgressChanged(routeProgress)
            viewportDataSource.evaluate()
            routeArrowAPI.addUpcomingManeuverArrow(routeProgress).apply {
                ifNonNull(routeArrowView, mapboxMap.getStyle()) { view, style ->
                    view.renderManeuverUpdate(style, this)
                }
            }
            binding.tripProgressView.render(tripProgressApi.getTripProgress(routeProgress))
            maneuverApi.getUpcomingManeuverList(routeProgress, upcomingManeuversCallback)
            ifNonNull(routeProgress.currentLegProgress) { legProgress ->
                ifNonNull(legProgress.currentStepProgress) {
                    maneuverApi.getStepDistanceRemaining(it, stepDistanceRemainingCallback)
                }
            }
        }
    }

    private val bannerInstructionsObserver = object : BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            if (binding.maneuverView.visibility != VISIBLE) {
                binding.maneuverView.visibility = VISIBLE
            }
            maneuverApi.getManeuver(bannerInstructions, currentManeuverCallback)
        }
    }

    private val voiceInstructionsObserver = object : VoiceInstructionsObserver {
        override fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions) {
            speechAPI.generate(
                voiceInstructions,
                speechCallback
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.getLocationComponentPlugin().apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MapboxNavigationActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            enabled = true
        }
        initNavigation()
        viewportDataSource = MapboxNavigationViewportDataSource(
            binding.mapView.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.getCameraAnimationsPlugin(),
            viewportDataSource
        )
        binding.mapView.getCameraAnimationsPlugin().addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
        init()
        tripProgressApi = MapboxTripProgressApi(getTripProgressFormatter())
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(DistanceFormatterOptions.Builder(this).build())
        )
        speechAPI = MapboxSpeechApi(
            this,
            getMapboxAccessTokenFromResources(),
            Locale.US.language
        )

        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            this,
            getMapboxAccessTokenFromResources(),
            Locale.US.language
        )

        buildingsArrivalApi.buildingHighlightApi(MapboxBuildingHighlightApi(mapboxMap))
        buildingsArrivalApi.enable(mapboxNavigation)
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        navigationCamera.resetFrame()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.unregisterMapMatcherResultObserver(mapMatcherResultObserver)
            mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
        }
        mapboxNavigation.onDestroy()
        speechAPI.cancel()
        voiceInstructionsPlayer?.shutdown()
        buildingsArrivalApi.disable()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onMapLongClick(point: Point): Boolean {
        val currentLocation = navigationLocationProvider.lastLocation
        if (currentLocation != null) {
            waypointsController.add(point)
            findRoute(Point.fromLngLat(currentLocation.longitude, currentLocation.latitude))
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun init() {
        initRouteLine()
        initStyle()
        initViews()
        mapboxNavigation.startTripSession()
    }

    private fun initRouteLine() {
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label")
            .build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
    }

    private fun initViews() {
        binding.start.setOnClickListener {
            startNavigation()
            binding.soundButton.unmuteAndExtend(2000L, muteUnmuteCallback)
            binding.start.visibility = GONE
            binding.soundButton.visibility = VISIBLE
            binding.routeOverview.visibility = VISIBLE
            binding.tripProgressCard.visibility = VISIBLE
            binding.routeOverview.showTextAndExtend(2000L)
        }
        binding.stop.setOnClickListener {
            stopNavigation()
            binding.recenter.visibility = GONE
            binding.soundButton.visibility = GONE
            binding.maneuverView.visibility = GONE
            binding.routeOverview.visibility = GONE
            binding.tripProgressCard.visibility = GONE
        }
        binding.recenter.setOnClickListener {
            updateCameraToFollowing()
            binding.recenter.visibility = GONE
        }
        binding.routeOverview.setOnClickListener {
            updateCameraToOverview()
            binding.recenter.visibility = VISIBLE
            binding.recenter.showTextAndExtend(2000L)
        }
        binding.soundButton.setOnClickListener {
            if (isMuted) {
                binding.soundButton.unmuteAndExtend(2000L, muteUnmuteCallback)
            } else {
                binding.soundButton.muteAndExtend(2000L, muteUnmuteCallback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        ).apply {
            registerLocationObserver(object : LocationObserver {
                override fun onRawLocationChanged(rawLocation: Location) {
                    updateCameraToIdle()
                    val point = Point.fromLngLat(rawLocation.longitude, rawLocation.latitude)
                    val cameraOptions = CameraOptions.Builder()
                        .center(point)
                        .zoom(13.0)
                        .pitch(45.0)
                        .build()
                    mapboxMap.easeTo(cameraOptions)
                    navigationLocationProvider.changePosition(rawLocation)
                    mapboxNavigation.unregisterLocationObserver(this)
                }

                override fun onEnhancedLocationChanged(
                    enhancedLocation: Location,
                    keyPoints: List<Location>
                ) {
                }
            })
            mapboxReplayer.pushRealLocation(this@MapboxNavigationActivity, 0.0)
            mapboxReplayer.play()
            registerRouteProgressObserver(replayProgressObserver)
            registerRoutesObserver(routesObserver)
            registerRouteProgressObserver(routeProgressObserver)
            registerMapMatcherResultObserver(mapMatcherResultObserver)
            registerVoiceInstructionsObserver(voiceInstructionsObserver)
            registerBannerInstructionsObserver(bannerInstructionsObserver)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS,
            {
                getGesturePlugin().addOnMapLongClickListener(this)
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapLoadErrorType: MapLoadErrorType, msg: String) {
                }
            }
        )
    }

    private fun findRoute(origin: Point) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultParams()
                .accessToken(getMapboxAccessTokenFromResources())
                .coordinates(waypointsController.coordinates(origin))
                .alternatives(true)
                .build(),
            object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    mapboxNavigation.setRoutes(routes)
                }

                override fun onRoutesRequestFailure(
                    throwable: Throwable,
                    routeOptions: RouteOptions
                ) {
                    // no impl
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    // no impl
                }
            }
        )
    }

    private fun updateCameraToOverview() {
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewEdgeInsets
        } else {
            viewportDataSource.overviewPadding = overviewEdgeInsets
        }
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToOverview()
    }

    private fun updateCameraToFollowing() {
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingEdgeInsets
        } else {
            viewportDataSource.followingPadding = followingEdgeInsets
        }
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToFollowing()
    }

    private fun updateCameraToIdle() {
        navigationCamera.requestNavigationCameraToIdle()
    }

    private fun startNavigation() {
        isNavigating = true
        updateCameraToFollowing()
    }

    private fun stopNavigation() {
        isNavigating = false
        updateCameraToIdle()
        clearRouteLine()
    }

    private fun clearRouteLine() {
        ifNonNull(routeLineAPI, routeLineView, mapboxMap.getStyle()) { api, view, style ->
            api.clearRouteLine(
                object : MapboxNavigationConsumer<Expected<RouteLineClearValue, RouteLineError>> {
                    override fun accept(value: Expected<RouteLineClearValue, RouteLineError>) {
                        view.renderClearRouteLineValue(style, value)
                    }
                }
            )
        }
    }

    private fun getGesturePlugin(): GesturesPlugin {
        return binding.mapView.getGesturesPlugin()
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return Utils.getMapboxAccessToken(this)
    }

    private fun getTripProgressFormatter(): TripProgressUpdateFormatter {
        return TripProgressUpdateFormatter.Builder(this)
            .distanceRemainingFormatter(
                DistanceRemainingFormatter(
                    mapboxNavigation.navigationOptions.distanceFormatterOptions
                )
            )
            .timeRemainingFormatter(TimeRemainingFormatter(this))
            .percentRouteTraveledFormatter(PercentDistanceTraveledFormatter())
            .estimatedTimeToArrivalFormatter(
                EstimatedTimeToArrivalFormatter(
                    this,
                    TimeFormat.NONE_SPECIFIED
                )
            ).build()
    }
}
