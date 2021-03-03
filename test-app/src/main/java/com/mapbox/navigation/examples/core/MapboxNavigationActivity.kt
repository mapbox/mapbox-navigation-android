package com.mapbox.navigation.examples.core

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapLoadError
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
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
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityNavigationBinding
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.base.api.maneuver.ManeuverCallback
import com.mapbox.navigation.ui.base.api.maneuver.StepDistanceRemainingCallback
import com.mapbox.navigation.ui.base.api.maneuver.UpcomingManeuversCallback
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import com.mapbox.navigation.ui.base.api.voice.VoiceInstructionsPlayer
import com.mapbox.navigation.ui.base.api.voice.VoiceInstructionsPlayerCallback
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.base.model.tripprogress.DistanceRemainingFormatter
import com.mapbox.navigation.ui.base.model.tripprogress.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.base.model.tripprogress.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.base.model.tripprogress.TimeRemainingFormatter
import com.mapbox.navigation.ui.base.model.tripprogress.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSourceOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
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
    private lateinit var speechAPI: SpeechApi

    private var isNavigating = false
    private var routeLineAPI: MapboxRouteLineApi? = null
    private var routeLineView: MapboxRouteLineView? = null
    private var routeArrowView: MapboxRouteArrowView? = null
    private var voiceInstructionsPlayer: VoiceInstructionsPlayer? = null
    private val routeArrowAPI: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private val navigationLocationProvider = NavigationLocationProvider()
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            20.0 * pixelDensity,
            20.0 * pixelDensity,
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
    private val currentManeuverCallback = object : ManeuverCallback {
        override fun onManeuver(currentManeuver: ManeuverState.CurrentManeuver) {
            val maneuver = currentManeuver.maneuver
            val primary = maneuver.primary
            val secondary = maneuver.secondary
            val sub = maneuver.sub
            val lane = maneuver.laneGuidance
            if (secondary?.componentList != null) {
                binding.maneuverView.render(ManeuverState.ManeuverSecondary.Show)
                binding.maneuverView.render(ManeuverState.ManeuverSecondary.Instruction(secondary))
            } else {
                binding.maneuverView.render(ManeuverState.ManeuverSecondary.Hide)
            }
            if (sub?.componentList != null) {
                binding.maneuverView.render(ManeuverState.ManeuverSub.Show)
                binding.maneuverView.render(ManeuverState.ManeuverSub.Instruction(sub))
            } else {
                binding.maneuverView.render(ManeuverState.ManeuverSub.Hide)
            }
            binding.maneuverView.render(ManeuverState.ManeuverPrimary.Instruction(primary))
            binding.maneuverView.render(ManeuverState.UpcomingManeuvers.RemoveUpcoming(maneuver))
            if (lane != null) {
                binding.maneuverView.render(ManeuverState.LaneGuidanceManeuver.Show)
                binding.maneuverView.render(ManeuverState.LaneGuidanceManeuver.AddLanes(lane))
            } else {
                binding.maneuverView.render(ManeuverState.LaneGuidanceManeuver.Hide)
                binding.maneuverView.render(ManeuverState.LaneGuidanceManeuver.RemoveLanes)
            }
        }
    }

    private val stepDistanceRemainingCallback = object : StepDistanceRemainingCallback {
        override fun onStepDistanceRemaining(
            distanceRemaining: ManeuverState.DistanceRemainingToFinishStep
        ) {
            binding.maneuverView.render(distanceRemaining)
        }
    }

    private val upcomingManeuversCallback = object : UpcomingManeuversCallback {
        override fun onUpcomingManeuvers(state: ManeuverState.UpcomingManeuvers.Upcoming) {
            binding.maneuverView.render(state)
        }
    }

    private val voiceInstructionsPlayerCallback: VoiceInstructionsPlayerCallback =
        object : VoiceInstructionsPlayerCallback {
            override fun onDone(state: SpeechState.DonePlaying) {
                speechAPI.clean(state.announcement)
            }
        }

    private val speechCallback = object : SpeechCallback {
        override fun onAvailable(state: SpeechState.Speech.Available) {
            val currentPlay = SpeechState.ReadyToPlay(state.announcement)
            voiceInstructionsPlayer?.play(currentPlay, voiceInstructionsPlayerCallback)
        }

        override fun onError(
            error: SpeechState.Speech.Error,
            fallback: SpeechState.Speech.Available
        ) {
            val currentPlay = SpeechState.ReadyToPlay(fallback.announcement)
            voiceInstructionsPlayer?.play(currentPlay, voiceInstructionsPlayerCallback)
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
                routeLineAPI?.setRoutes(listOf(RouteLine(routes[0], null)))?.apply {
                    ifNonNull(routeLineView, mapboxMap.getStyle()) { view, style ->
                        view.render(style, this)
                    }
                }
                viewportDataSource.onRouteChanged(routes[0])
                if (!isNavigating) {
                    binding.start.visibility = VISIBLE
                    updateCameraToOverview()
                }
                val routeLeg = routes.first().legs()
                ifNonNull(routeLeg) {
                    maneuverApi.retrieveUpcomingManeuvers(it.first(), upcomingManeuversCallback)
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
            routeArrowAPI.updateUpcomingManeuverArrow(routeProgress).apply {
                ifNonNull(routeArrowView, mapboxMap.getStyle()) { view, style ->
                    view.render(style, this)
                }
            }
            binding.tripProgressView.render(tripProgressApi.getTripProgress(routeProgress))
            ifNonNull(routeProgress.currentLegProgress) { legProgress ->
                ifNonNull(legProgress.currentStepProgress) {
                    maneuverApi.retrieveStepDistanceRemaining(it, stepDistanceRemainingCallback)
                }
            }
        }
    }

    private val bannerInstructionsObserver = object : BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            maneuverApi.retrieveManeuver(bannerInstructions, currentManeuverCallback)
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
            MapboxNavigationViewportDataSourceOptions.Builder().build(),
            binding.mapView.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.getCameraAnimationsPlugin(),
            viewportDataSource
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
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterMapMatcherResultObserver(mapMatcherResultObserver)
        }
        mapboxNavigation.onDestroy()
        speechAPI.cancel()
        voiceInstructionsPlayer?.shutdown()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onMapLongClick(point: Point): Boolean {
        val currentLocation = navigationLocationProvider.lastLocation
        if (currentLocation != null) {
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            findRoute(originPoint, point)
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
            binding.start.visibility = GONE
            binding.maneuverView.visibility = VISIBLE
            binding.tripProgressCard.visibility = VISIBLE
        }
        binding.stop.setOnClickListener {
            stopNavigation()
            binding.maneuverView.visibility = GONE
            binding.tripProgressCard.visibility = GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(LocationEngineProvider.getBestLocationEngine(this))
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
            registerRoutesObserver(routesObserver)
            registerMapMatcherResultObserver(mapMatcherResultObserver)
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
                override fun onMapLoadError(mapViewLoadError: MapLoadError, msg: String) {
                }
            }
        )
    }

    private fun findRoute(origin: Point, destination: Point) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultParams()
                .accessToken(getMapboxAccessTokenFromResources())
                .coordinates(listOf(origin, destination))
                .alternatives(true)
                .build()
        )
    }

    private fun updateCameraToOverview() {
        viewportDataSource.overviewPaddingPropertyOverride(overviewEdgeInsets)
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToOverview()
    }

    private fun updateCameraToFollowing() {
        viewportDataSource.followingPaddingPropertyOverride(followingEdgeInsets)
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToFollowing()
    }

    private fun updateCameraToIdle() {
        navigationCamera.requestNavigationCameraToIdle()
    }

    private fun startNavigation() {
        isNavigating = true
        updateCameraToFollowing()
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionsObserver)
    }

    private fun stopNavigation() {
        isNavigating = false
        updateCameraToIdle()
        clearRouteLine()
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
    }

    private fun clearRouteLine() {
        ifNonNull(routeLineAPI, routeLineView, mapboxMap.getStyle()) { api, view, style ->
            view.render(style, api.clearRouteLine())
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
