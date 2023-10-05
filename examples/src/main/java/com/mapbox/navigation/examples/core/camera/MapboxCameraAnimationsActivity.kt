package com.mapbox.navigation.examples.core.camera

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.replay.route.ReplayRouteOptions
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.databinding.LayoutActivityCameraBinding
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.FollowingCameraFramingStrategy
import com.mapbox.navigation.ui.maps.camera.data.FollowingFrameOptions
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandler
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.clearRouteLine
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapboxCameraAnimationsActivity : AppCompatActivity(), OnMapLongClickListener {

    private val navigationLocationProvider = NavigationLocationProvider()
    private lateinit var locationComponent: LocationComponentPlugin
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private val replayRouteMapper = ReplayRouteMapper(ReplayRouteOptions.Builder().frequency(1.0).build())
    private val mapboxReplayer = MapboxReplayer()

    private var routeLineAPI: MapboxRouteLineApi? = null
    private val routeArrowAPI: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private var routeLineView: MapboxRouteLineView? = null
    private var routeArrowView: MapboxRouteArrowView? = null

    private lateinit var binding: LayoutActivityCameraBinding
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private val pixelDensity = Resources.getSystem().displayMetrics.density

    private val paddedFollowingEdgeInsets = EdgeInsets(
        164.0 * pixelDensity,
        16.0 * pixelDensity,
        64.0 * pixelDensity,
        16.0 * pixelDensity,
    )

    private val locationObserver = object : LocationObserver {

        override fun onNewRawLocation(rawLocation: Location) {}

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val transitionOptions: (ValueAnimator.() -> Unit) =
                if (locationMatcherResult.isTeleport) {
                    {
                        duration = 0
                    }
                } else {
                    {
                        duration = 1000
                    }
                }
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
                latLngTransitionOptions = transitionOptions,
                bearingTransitionOptions = transitionOptions,
            )
            viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)

            viewportDataSource.evaluate()
            if (locationMatcherResult.isTeleport) {
                navigationCamera.resetFrame()
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        routeProgress.currentLegProgress?.currentStepProgress?.distanceRemaining?.let {
            if (it <= 2000) {
                val points = listOfNotNull(
                    routeProgress.currentLegProgress?.upcomingStep?.maneuver()?.location(),
                )

                viewportDataSource.additionalPointsToFrameForFollowing(points)
                viewportDataSource.followingZoomPropertyOverride(null)
            }
        }

        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        routeLineAPI?.updateWithRouteProgress(routeProgress) { result ->
            mapboxMap.getStyle()?.apply {
                routeLineView?.renderRouteLineUpdate(this, result)
            }
        }

        routeArrowAPI.addUpcomingManeuverArrow(routeProgress).apply {
            ifNonNull(routeArrowView, mapboxMap.getStyle()) { view, style ->
                view.renderManeuverUpdate(style, this)
            }
        }
    }

    private val routesObserver = RoutesObserver { result ->
        if (result.routes.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                routeLineAPI?.setNavigationRoutes(result.navigationRoutes)?.apply {
                    ifNonNull(routeLineView, mapboxMap.getStyle()) { view, style ->
                        view.renderRouteDrawData(style, this)
                    }
                }
            }
            startSimulation(result.navigationRoutes[0])
            viewportDataSource.onRouteChanged(result.routes.first())
            setupCameraOptions()
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                routeArrowAPI.clearArrows().apply {
                    ifNonNull(routeArrowView, mapboxMap.getStyle()) { view, style ->
                        view.render(style, this)
                    }
                }
                routeLineAPI?.clearRouteLine()?.apply {
                    ifNonNull(routeLineView, mapboxMap.getStyle()) { view, style ->
                        view.renderClearRouteLineValue(style, this)
                    }
                }
            }
            viewportDataSource.clearRouteData()
            navigationCamera.requestNavigationCameraToIdle()
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MapboxCameraAnimationsActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }

        initNavigation()

        val debugger = MapboxNavigationViewportDataSourceDebugger(
            context = this,
            mapView = binding.mapView,
            layerAbove = "road-label"
        ).apply {
            enabled = true
        }
        viewportDataSource = MapboxNavigationViewportDataSource(
            binding.mapView.getMapboxMap()
        )

        viewportDataSource.debugger = debugger
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource
        )
        navigationCamera.debugger = debugger

        binding.mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationScaleGestureHandler(
                this,
                navigationCamera,
                mapboxMap,
                binding.mapView.gestures,
                locationComponent,
                {
                    viewportDataSource
                        .options
                        .followingFrameOptions
                        .zoomUpdatesAllowed = false
                }
            ).apply { initialize() }
        )

        init()
    }

    @SuppressLint("MissingPermission")
    private fun init() {
        initRouteLine()
        initStyle()
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

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        val tilesDataset = "bmw-production"
        val tilesProfile = "driving-traffic"

        val routingTilesOptions = RoutingTilesOptions.Builder()
            .tilesDataset(tilesDataset)
            .tilesProfile(tilesProfile)
            .build()

        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .routingTilesOptions(routingTilesOptions)
                .build()
        ).apply {
            registerLocationObserver(
                object : LocationObserver {

                    override fun onNewRawLocation(rawLocation: Location) {
                        navigationCamera.requestNavigationCameraToIdle()
                        val point = Point.fromLngLat(rawLocation.longitude, rawLocation.latitude)
                        val cameraOptions = CameraOptions.Builder()
                            .center(point)
                            .build()
                        mapboxMap.setCamera(cameraOptions)
                        navigationLocationProvider.changePosition(rawLocation)
                        mapboxNavigation.unregisterLocationObserver(this)
                    }

                    override fun onNewLocationMatcherResult(
                        locationMatcherResult: LocationMatcherResult,
                    ) {
                        // no impl
                    }
                }
            )
            registerRouteProgressObserver(routeProgressObserver)
            registerRoutesObserver(routesObserver)
            registerLocationObserver(locationObserver)
        }

        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(1.0)
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun startSimulation(route: NavigationRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = replayRouteMapper.mapDirectionsRouteGeometry(route.directionsRoute)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    private fun initStyle() {
        mapboxMap.loadStyleUri("mapbox://styles/bmw-production/clglaaxgl00ap01pk72p3eyjj") {
            binding.mapView.gestures.addOnMapLongClickListener(
                this@MapboxCameraAnimationsActivity
            )
        }
    }

    private fun setupCameraOptions() {
        viewportDataSource.apply {
            followingPadding = paddedFollowingEdgeInsets
            followingPitchPropertyOverride(0.0)
            followingZoomPropertyOverride(12.0)
            followingBearingPropertyOverride(null)

            options.followingFrameOptions.apply {
                framingStrategy = EmptyFollowingCameraFramingStrategy

                focalPoint = FollowingFrameOptions.FocalPoint(0.5, 1.0)
                bearingSmoothing.maxBearingAngleDiff = 60.0
                maxZoom = 17.0
                minZoom = 3.0
                bearingUpdatesAllowed = true
                pitchUpdatesAllowed = true
                zoomUpdatesAllowed = true
                paddingUpdatesAllowed = true
            }
        }
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToFollowing()
    }

    private fun findRoute(origin: Point, destination: Point) {
        val routeOptions: RouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .profile("driving-traffic")
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .build()

        mapboxNavigation.requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setNavigationRoutes(routes)
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    // no impl
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
                }
            }
        )
    }

    override fun onMapLongClick(point: Point): Boolean {
        val startPoint = Point.fromLngLat(16.225984418396525, 50.90855472797008)
        val wroclaw = Point.fromLngLat(17.034520251843787, 51.11006892885478)
        findRoute(startPoint, wroclaw)
        return false
    }

    override fun onStart() {
        super.onStart()
        navigationCamera.resetFrame()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private object EmptyFollowingCameraFramingStrategy : FollowingCameraFramingStrategy {
        override fun getPointsToFrameOnCurrentStep(
            routeProgress: RouteProgress,
            followingFrameOptions: FollowingFrameOptions,
            averageIntersectionDistancesOnRoute: List<List<Double>>,
        ): List<Point> = emptyList()

        override fun getPointsToFrameAfterCurrentManeuver(
            routeProgress: RouteProgress,
            followingFrameOptions: FollowingFrameOptions,
            postManeuverFramingPoints: List<List<List<Point>>>,
        ): List<Point> = emptyList()
    }
}
