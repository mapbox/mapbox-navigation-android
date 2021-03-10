package com.mapbox.navigation.examples.core.camera

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapLoadError
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.skyLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SkyType
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.LocationPuck3D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
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
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.camera.AnimationAdapter.OnAnimationButtonClicked
import com.mapbox.navigation.examples.core.databinding.LayoutActivityCameraBinding
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maneuver.api.ManeuverCallback
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.api.StepDistanceRemainingCallback
import com.mapbox.navigation.ui.maneuver.api.UpcomingManeuverListCallback
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.StepDistanceError
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSourceOptions
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureActionListener
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.turf.TurfMeasurement

class MapboxCameraAnimationsActivity :
    AppCompatActivity(),
    PermissionsListener,
    OnAnimationButtonClicked,
    OnMapLongClickListener {

    private val permissionsManager = PermissionsManager(this)
    private val navigationLocationProvider = NavigationLocationProvider()
    private lateinit var locationComponent: LocationComponentPlugin
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private val replayRouteMapper = ReplayRouteMapper()
    private val mapboxReplayer = MapboxReplayer()

    private var routeLineAPI: MapboxRouteLineApi? = null
    private val routeArrowAPI: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private var routeLineView: MapboxRouteLineView? = null
    private var routeArrowView: MapboxRouteArrowView? = null

    private lateinit var binding: LayoutActivityCameraBinding
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private lateinit var tripProgressApi: MapboxTripProgressApi
    private lateinit var maneuverApi: MapboxManeuverApi

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

    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            60.0 * pixelDensity,
            60.0 * pixelDensity,
            60.0 * pixelDensity,
            60.0 * pixelDensity
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

    private var lookAtPoint: Point? = null
        set(value) {
            field = value
            if (value != null) {
                // poiSource.geometry(value)
            } else {
                poiSource.featureCollection(FeatureCollection.fromFeatures(emptyList()))
            }
        }
    private val poiLayer = CircleLayer("circle_layer", "circle_source")
        .circleColor(Color.RED)
        .circleRadius(10.0)
    private val poiSource = GeoJsonSource(
        GeoJsonSource.Builder("circle_source").data("")
    )

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

            lookAtPoint?.run {
                val point = Point.fromLngLat(
                    mapMatcherResult.enhancedLocation.longitude,
                    mapMatcherResult.enhancedLocation.latitude,
                )
                val bearing = TurfMeasurement.bearing(point, this)
                viewportDataSource.followingBearingPropertyOverride(bearing)
            }

            viewportDataSource.evaluate()
            if (mapMatcherResult.isTeleport) {
                navigationCamera.resetFrame()
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            viewportDataSource.onRouteProgressChanged(routeProgress)
            viewportDataSource.evaluate()

            binding.tripProgressView.render(tripProgressApi.getTripProgress(routeProgress))
            maneuverApi.getUpcomingManeuverList(routeProgress, upcomingManeuversCallback)
            com.mapbox.navigation.ui.utils.internal.ifNonNull(routeProgress.currentLegProgress) { legProgress ->
                com.mapbox.navigation.ui.utils.internal.ifNonNull(legProgress.currentStepProgress) {
                    maneuverApi.getStepDistanceRemaining(it, stepDistanceRemainingCallback)
                }
            }

            routeLineAPI?.updateWithRouteProgress(routeProgress)
            routeArrowAPI.updateUpcomingManeuverArrow(routeProgress).apply {
                ifNonNull(routeArrowView, mapboxMap.getStyle()) { view, style ->
                    view.render(style, this)
                }
            }
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                routeLineAPI?.setRoutes(listOf(RouteLine(routes[0], null)))?.apply {
                    ifNonNull(routeLineView, mapboxMap.getStyle()) { view, style ->
                        view.renderRouteDrawData(style, this)
                    }
                }
                startSimulation(routes[0])
                viewportDataSource.onRouteChanged(routes.first())
                viewportDataSource.overviewPaddingPropertyOverride(overviewEdgeInsets)
                viewportDataSource.evaluate()
                navigationCamera.requestNavigationCameraToOverview()

                Handler().postDelayed({
                    runScenario()
                }, 2500)
            } else {
                navigationCamera.requestNavigationCameraToIdle()
            }
        }
    }

    private val onIndicatorPositionChangedListener = object : OnIndicatorPositionChangedListener {
        override fun onIndicatorPositionChanged(point: Point) {
            routeLineAPI?.updateTraveledRouteLine(point)?.let {
                routeLineView?.renderVanishingRouteLineUpdateValue(mapboxMap.getStyle()!!, it)
            }
        }
    }

    private val bannerInstructionsObserver = object : BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            maneuverApi.getManeuver(bannerInstructions, currentManeuverCallback)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.getLocationComponentPlugin().apply {
            pulsingEnabled = true
            setLocationProvider(navigationLocationProvider)
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            enabled = true
        }

        initNavigation()
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(this)
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
        )
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(DistanceFormatterOptions.Builder(this).build())
        )

        viewportDataSource = MapboxNavigationViewportDataSource(
            MapboxNavigationViewportDataSourceOptions.Builder().build(),
            binding.mapView.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.getCameraAnimationsPlugin(),
            viewportDataSource
        )
        /* Alternative to the NavigationScaleGestureHandler
        mapView.getCameraAnimationsPlugin().addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )*/
        binding.mapView.getCameraAnimationsPlugin().addCameraAnimationsLifecycleListener(
            NavigationScaleGestureHandler(
                this,
                navigationCamera,
                mapboxMap,
                getGesturesPlugin(),
                locationComponent,
                object : NavigationScaleGestureActionListener {
                    override fun onNavigationScaleGestureAction() {
                        viewportDataSource.followingZoomUpdatesAllowed = false
                    }
                }
            ).apply { initialize() }
        )

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            init()
        } else {
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun init() {
        initRouteLine()
        initAnimations()
        initStyle()
        initCameraListeners()
        initButtons()
        mapboxNavigation.startTripSession()
    }

    private fun initRouteLine() {
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label")
            .withVanishingRouteLineEnabled(true)
            .build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
    }

    private fun initButtons() {
        binding.gravitateTop.visibility = View.GONE
        binding.gravitateBottom.visibility = View.GONE
        binding.gravitateLeft.visibility = View.GONE
        binding.gravitateRight.visibility = View.GONE
        binding.gravitateLeft.setOnClickListener {
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    it.top,
                    0.0,
                    it.bottom,
                    120.0 * pixelDensity
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }

        binding.gravitateRight.setOnClickListener {
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    it.top,
                    120.0 * pixelDensity,
                    it.bottom,
                    0.0
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }

        binding.gravitateTop.setOnClickListener {
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    0.0,
                    it.left,
                    120.0 * pixelDensity,
                    it.right
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }

        binding.gravitateBottom.setOnClickListener {
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    120.0 * pixelDensity,
                    it.left,
                    0.0,
                    it.right
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }
    }

    private fun initCameraListeners() {
        mapboxMap.addOnCameraChangeListener {
            this.runOnUiThread {
                updateCameraChangeView()
            }
        }
    }

    private fun runScenario() {

        binding.mapView.getLocationComponentPlugin().apply {
            locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MapboxCameraAnimationsActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            pulsingEnabled = false
        }
        viewportDataSource.followingZoomUpdatesAllowed = true
        viewportDataSource.followingPaddingPropertyOverride(followingEdgeInsets)
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToFollowing()
        binding.start.visibility = View.GONE
        binding.tripProgressCard.visibility = View.VISIBLE
        binding.maneuverView.visibility = View.VISIBLE

        Handler().postDelayed({
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    it.top,
                    0.0,
                    it.bottom,
                    120.0 * pixelDensity
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }, 3000)

        Handler().postDelayed({
            viewportDataSource.followingPaddingPropertyOverride(followingEdgeInsets)
            viewportDataSource.evaluate()
        }, 11000)

        Handler().postDelayed({
            val center = mapboxMap.getCameraOptions(null).center
                ?: Point.fromLngLat(0.0, 0.0)
            lookAtPoint = Point.fromLngLat(
                (center.longitude()) + 0.003,
                (center.latitude()) + 0.003
            ).also {
                // viewportDataSource.additionalPointsToFrameForFollowing(listOf(it))
                viewportDataSource.followingPitchPropertyOverride(80.0)
                viewportDataSource.followingBearingPropertyOverride(
                    TurfMeasurement.bearing(center, it)
                )
                viewportDataSource.evaluate()
            }
            binding.mapView.getLocationComponentPlugin().apply {
                locationPuck = LocationPuck3D(
                    modelUri = "asset://race_car_model.gltf",
                    modelScale = listOf(0.3f, 0.3f, 0.3f)
                )
            }
        }, 20000)

        Handler().postDelayed({
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    it.top,
                    120.0 * pixelDensity,
                    it.bottom,
                    0.0
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }, 25000)

        Handler().postDelayed({
            lookAtPoint = null
            viewportDataSource.additionalPointsToFrameForFollowing(emptyList())
            viewportDataSource.followingBearingPropertyOverride(null)
            viewportDataSource.followingPitchPropertyOverride(null)
            viewportDataSource.evaluate()
        }, 25000)

        Handler().postDelayed({
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    it.top,
                    0.0,
                    it.bottom,
                    120.0 * pixelDensity
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }, 33000)

        Handler().postDelayed({
            /*viewportDataSource.overviewPaddingPropertyOverride(EdgeInsets(
                80.0 * pixelDensity,
                80.0 * pixelDensity,
                80.0 * pixelDensity,
                80.0 * pixelDensity
            ))
            viewportDataSource.evaluate()
            navigationCamera.requestNavigationCameraToOverview()*/

            binding.mapView.getCameraAnimationsPlugin().flyTo(
                CameraOptions.Builder().padding(EdgeInsets(0.0, 0.0, 0.0, 0.0))
                    .center(Point.fromLngLat( -77.05261278623257, 38.943639298127344))
                    .zoom(15.0)
                    .bearing(0.0)
                    .pitch(0.0)
                    .build(),
                MapAnimationOptions.mapAnimationOptions {
                    duration(1500)
                }
            )
            binding.mapView.getLocationComponentPlugin().apply {
                locationPuck = LocationPuck2D(
                    bearingImage = ContextCompat.getDrawable(
                        this@MapboxCameraAnimationsActivity,
                        R.drawable.mapbox_navigation_puck_icon
                    )
                )
            }
        }, 37000)
    }

    private fun initNavigation() {
        binding.start.setOnClickListener {





        }
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        ).apply {
            registerLocationObserver(
                object : LocationObserver {

                    override fun onRawLocationChanged(rawLocation: Location) {
                        navigationCamera.requestNavigationCameraToIdle()
                        val point = Point.fromLngLat(rawLocation.longitude, rawLocation.latitude)
                        val cameraOptions = CameraOptions.Builder()
                            .center(point)
                            .zoom(13.0)
                            .build()
                        mapboxMap.jumpTo(cameraOptions)
                        navigationLocationProvider.changePosition(rawLocation)
                        mapboxNavigation.unregisterLocationObserver(this)
                    }

                    override fun onEnhancedLocationChanged(
                        enhancedLocation: Location,
                        keyPoints: List<Location>
                    ) {
                        // no impl
                    }
                }
            )
            registerRouteProgressObserver(routeProgressObserver)
            registerRoutesObserver(routesObserver)
            registerMapMatcherResultObserver(mapMatcherResultObserver)
            registerBannerInstructionsObserver(bannerInstructionsObserver)
        }

        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(1.0)
        mapboxReplayer.play()
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = replayRouteMapper.mapDirectionsRouteGeometry(route)
        // val replayEvents = replayRouteMapper.mapDirectionsRouteLegAnnotation(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    companion object {
        private const val SOURCE = "TERRAIN_SOURCE"
        private const val SKY_LAYER = "sky"
        private const val TERRAIN_URL_TILE_RESOURCE = "mapbox://mapbox.terrain-rgb"
    }

    private fun initStyle() {
        mapboxMap.loadStyle(
            styleExtension = style(Style.MAPBOX_STREETS) {
                /*+rasterDemSource(SOURCE) {
                    url(TERRAIN_URL_TILE_RESOURCE)
                    tileSize(512)
                }
                +terrain(SOURCE)*/
                +skyLayer(SKY_LAYER) {
                    skyType(SkyType.ATMOSPHERE)
                    skyAtmosphereSun(listOf(-50.0, 90.2))
                }
            },
            onStyleLoaded = object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    getGesturesPlugin().addOnMapLongClickListener(
                        this@MapboxCameraAnimationsActivity
                    )
                    style.addSource(poiSource)
                    style.addLayer(poiLayer)
                }
            }
        )
        /*mapboxMap.loadStyleUri(
            MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    getGesturesPlugin().addOnMapLongClickListener(
                        this@MapboxCameraAnimationsActivity
                    )
                    style.addSource(poiSource)
                    style.addLayer(poiLayer)
                }
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapViewLoadError: MapLoadError, msg: String) {
                    Log.e(
                        "CameraAnimationsAct",
                        "Error loading map: %s".format(mapViewLoadError.name)
                    )
                }
            }
        )*/
    }

    private fun initAnimations() {
        val adapter = AnimationAdapter(this, this)
        val manager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.animationsList.layoutManager = manager
        binding.animationsList.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    private fun updateCameraChangeView() {
        mapboxMap.getCameraOptions(null).let { currentMapCamera ->
            binding.cameraChangeViewState.text = "state: ${navigationCamera.state}"
            binding.cameraChangeViewLng.text = "lng: " +
                currentMapCamera.center?.longitude().formatNumber()
            binding.cameraChangeViewLat.text =
                "lat: ${currentMapCamera.center?.latitude().formatNumber()}"
            binding.cameraChangeViewZoom.text = "zoom: ${currentMapCamera.zoom.formatNumber()}"
            binding.cameraChangeViewBearing.text =
                "bearing: ${currentMapCamera.bearing.formatNumber()}"
            binding.cameraChangeViewPitch.text = "pitch: ${currentMapCamera.pitch.formatNumber()}"
            binding.cameraChangeViewPadding.text =
                """
                    |padding:
                    |  top: ${currentMapCamera.padding?.top.formatNumber()}
                    |  left: ${currentMapCamera.padding?.left.formatNumber()}
                    |  bottom: ${currentMapCamera.padding?.bottom.formatNumber()}
                    |  right: ${currentMapCamera.padding?.right.formatNumber()}
               """.trimMargin()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onButtonClicked(animationType: AnimationType) {
        when (animationType) {
            AnimationType.Following -> {
                binding.mapView.getLocationComponentPlugin().apply {
                    locationPuck = LocationPuck2D(
                        bearingImage = ContextCompat.getDrawable(
                            this@MapboxCameraAnimationsActivity,
                            R.drawable.mapbox_navigation_puck_icon
                        )
                    )
                    pulsingEnabled = false
                }
                viewportDataSource.followingZoomUpdatesAllowed = true
                viewportDataSource.followingPaddingPropertyOverride(followingEdgeInsets)
                viewportDataSource.evaluate()
                navigationCamera.requestNavigationCameraToFollowing()
            }
            AnimationType.Overview -> {
                viewportDataSource.overviewPaddingPropertyOverride(overviewEdgeInsets)
                viewportDataSource.evaluate()
                navigationCamera.requestNavigationCameraToOverview()
                binding.mapView.getLocationComponentPlugin().apply {
                    locationPuck = LocationPuck2D(
                        bearingImage = ContextCompat.getDrawable(
                            this@MapboxCameraAnimationsActivity,
                            R.drawable.mapbox_navigation_puck_icon
                        )
                    )
                }
            }
            AnimationType.ToPOI -> {
                navigationLocationProvider.lastLocation?.let {
                    val center = Point.fromLngLat(
                        it.longitude + 0.0123,
                        it.latitude + 0.0123
                    )
                    binding.mapView.getCameraAnimationsPlugin().flyTo(
                        CameraOptions.Builder()
                            .center(center)
                            .bearing(0.0)
                            .zoom(14.0)
                            .pitch(0.0)
                            .build(),
                        MapAnimationOptions.mapAnimationOptions {
                            duration(1500)
                        }
                    )
                }
            }
            AnimationType.LookAtPOIWhenFollowing -> {
                if (lookAtPoint == null) {
                    val center = mapboxMap.getCameraOptions(null).center
                        ?: Point.fromLngLat(0.0, 0.0)
                    lookAtPoint = Point.fromLngLat(
                        (center.longitude()) + 0.003,
                        (center.latitude()) + 0.003
                    ).also {
                        // viewportDataSource.additionalPointsToFrameForFollowing(listOf(it))
                        viewportDataSource.followingPitchPropertyOverride(80.0)
                        viewportDataSource.followingBearingPropertyOverride(
                            TurfMeasurement.bearing(center, it)
                        )
                        viewportDataSource.evaluate()
                    }
                    binding.mapView.getLocationComponentPlugin().apply {
                        locationPuck = LocationPuck3D(
                            modelUri = "asset://race_car_model.gltf",
                            modelScale = listOf(0.3f, 0.3f, 0.3f)
                        )
                    }
                } else {
                    lookAtPoint = null
                    viewportDataSource.additionalPointsToFrameForFollowing(emptyList())
                    viewportDataSource.followingBearingPropertyOverride(null)
                    viewportDataSource.followingPitchPropertyOverride(null)
                    viewportDataSource.evaluate()

                    /*binding.mapView.getLocationComponentPlugin().apply {
                        locationPuck = LocationPuck2D(
                            bearingImage = ContextCompat.getDrawable(
                                this@MapboxCameraAnimationsActivity,
                                R.drawable.mapbox_navigation_puck_icon
                            )
                        )
                    }*/
                }
            }
        }
    }

    private fun findRoute(origin: Point, destination: Point) {
        val routeOptions: RouteOptions = RouteOptions.builder()
            .applyDefaultParams()
            .accessToken(getMapboxAccessTokenFromResources())
            .coordinates(listOf(origin, destination))
            .alternatives(true)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .annotationsList(
                listOf(
                    DirectionsCriteria.ANNOTATION_SPEED,
                    DirectionsCriteria.ANNOTATION_DISTANCE,
                    DirectionsCriteria.ANNOTATION_CONGESTION
                )
            )
            .build()

        mapboxNavigation.requestRoutes(routeOptions)
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

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        navigationCamera.resetFrame()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private fun getGesturesPlugin(): GesturesPlugin {
        return binding.mapView.getGesturesPlugin()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            this,
            "This app needs location and storage permissions in order to show its functionality.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            init()
        } else {
            Toast.makeText(
                this,
                "You didn't grant location permissions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun Number?.formatNumber() = "%.8f".format(this)
}
