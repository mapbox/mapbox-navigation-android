package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.extension.observable.model.SourceDataType
import com.mapbox.maps.extension.observable.subscribeSourceDataLoaded
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnSourceDataLoadedListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutRouteRefreshBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.min

/**
 * Use for testing alternative routes refresh.
 * How to test:
 * 1. Add a waypoint by long-tapping on the map.
 * 2. Optionally, add more waypoints the same way.
 * 3. Click "Start Navigation".
 * 4. Remove alternative traffic from the map by clicking "Clear traffic".
 * 5. Wait for refresh: the traffic should reappear.
 * 6. Optionally, remove the traffic and wait for refresh again.
 */
@ExperimentalPreviewMapboxNavigationAPI
class RouteRefreshActivity : AppCompatActivity() {

    /* ----- Layout binding reference ----- */
    private lateinit var binding: LayoutRouteRefreshBinding

    /* ----- Mapbox Maps components ----- */
    private lateinit var mapboxMap: MapboxMap

    /* ----- Mapbox Navigation components ----- */
    private lateinit var mapboxNavigation: MapboxNavigation

    // location puck integration
    private val navigationLocationProvider = NavigationLocationProvider()

    private val mapboxReplayer = MapboxReplayer()
    private val replayRouteMapper = ReplayRouteMapper()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private var coordinates = mutableListOf<Point>()
    private var previewedRoutes = emptyList<NavigationRoute>()

    // camera
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            20.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    // route line
    private lateinit var routeLineAPI: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private lateinit var routeArrowView: MapboxRouteArrowView
    private val routeArrowAPI: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineAPI.updateTraveledRouteLine(point)
        binding.mapView.getMapboxMap().getStyle()?.apply {
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }


    private var lastRouteTrafficUpdateTime: Long = 0

    /* ----- Location and route progress callbacks ----- */
    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            // update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = locationMatcherResult.enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            // update camera position to account for new location
            viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
            viewportDataSource.evaluate()
        }
    }

    private val routeProgressObserver =
        RouteProgressObserver { routeProgress ->
            // update the camera position to account for the progressed fragment of the route
            viewportDataSource.onRouteProgressChanged(routeProgress)
            viewportDataSource.evaluate()

            // show arrow on the route line with the next maneuver
            val maneuverArrowResult = routeArrowAPI.addUpcomingManeuverArrow(routeProgress)
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
                routeLineAPI.updateWithRouteProgress(routeProgress) {
                    routeLineView.renderRouteLineUpdate(style, it)
                }
            }
        }

    private val routesObserver = RoutesObserver { result ->
        if (result.navigationRoutes.isNotEmpty()) {
            // generate route geometries asynchronously and render them
            CoroutineScope(Dispatchers.Main).launch {
                val result = routeLineAPI.setNavigationRoutes(result.navigationRoutes)
                val style = mapboxMap.getStyle()
                if (style != null) {
                    routeLineView.renderRouteDrawData(style, result)
                }
            }

            // update the camera position to account for the new route
            viewportDataSource.onRouteChanged(result.navigationRoutes.first())
            viewportDataSource.evaluate()
        } else {
            // remove the route line and route arrow from the map
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeLineAPI.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowAPI.clearArrows())
            }

            // remove the route reference to change camera position
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutRouteRefreshBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()

        // initialize the location puck
        binding.mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@RouteRefreshActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }

        // initialize Mapbox Navigation
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .routeRefreshOptions(RouteRefreshOptions.Builder().intervalMillis(30000).build())
                .build()
        )
        mapboxNavigation.registerRouteAlternativesObserver(
            object :
                NavigationRouteAlternativesObserver {
                override fun onRouteAlternatives(
                    routeProgress: RouteProgress,
                    alternatives: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setNavigationRoutes(
                        listOf(routeProgress.navigationRoute) + alternatives
                    )
                }

                override fun onRouteAlternativesError(error: RouteAlternativesError) {
                    // no-op
                }
            }
        )
        // move the camera to current location on the first update
        mapboxNavigation.registerLocationObserver(object : LocationObserver {
            override fun onNewRawLocation(rawLocation: Location) {
                val point = Point.fromLngLat(rawLocation.longitude, rawLocation.latitude)
                val cameraOptions = CameraOptions.Builder()
                    .center(point)
                    .zoom(13.0)
                    .build()
                mapboxMap.setCamera(cameraOptions)
                mapboxNavigation.unregisterLocationObserver(this)
            }

            override fun onNewLocationMatcherResult(
                locationMatcherResult: LocationMatcherResult,
            ) {
                // not handled
            }
        })

        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(
            binding.mapView.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource
        )
        binding.mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )
        navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            // shows/hide the recenter button depending on the camera state
            when (navigationCameraState) {
                NavigationCameraState.TRANSITION_TO_FOLLOWING,
                NavigationCameraState.FOLLOWING -> binding.recenter.visibility = INVISIBLE

                NavigationCameraState.TRANSITION_TO_OVERVIEW,
                NavigationCameraState.OVERVIEW,
                NavigationCameraState.IDLE -> binding.recenter.visibility = VISIBLE
            }
        }
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
        }
        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.followingPadding = followingPadding
        }

        // initialize route line
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withVanishingRouteLineEnabled(true)
            .withRouteLineResources(
                RouteLineResources.Builder()
                    .routeLineColorResources(
                        RouteLineColorResources.Builder()
                            .routeDefaultColor(Color.parseColor("#0000FF"))
                            .routeUnknownCongestionColor(Color.parseColor("#0000FF"))
                            .routeLowCongestionColor(Color.parseColor("#00FF00"))
                            .routeModerateCongestionColor(Color.parseColor("#FFFF00"))
                            .routeHeavyCongestionColor(Color.parseColor("#FF0000"))
                            .routeSevereCongestionColor(Color.parseColor("#DC143C"))
                            .routeClosureColor(Color.parseColor("#000000"))
                            .alternativeRouteDefaultColor(Color.parseColor("#94D1DF"))
                            .alternativeRouteUnknownCongestionColor(Color.parseColor("#94D1DF"))
                            .alternativeRouteLowCongestionColor(Color.parseColor("#CCD1C8"))
                            .alternativeRouteModerateCongestionColor(Color.parseColor("#FFEA7F"))
                            .alternativeRouteHeavyCongestionColor(Color.parseColor("#DB6A6C"))
                            .alternativeRouteSevereCongestionColor(Color.parseColor("#FFB3A7"))
                            .alternativeRouteClosureColor(Color.parseColor("#1F262A"))
                            .build()
                    )
                    .build()
            )
            .build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)
        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
        val styleURL = "mapbox://styles/mapbox/traffic-day-v2"
        // load map style
        mapboxMap.loadStyleUri(styleURL) { style ->
            routeLineView.initializeLayers(style)
            // add long click listener that search for a route to the clicked destination
            binding.mapView.gestures.addOnMapLongClickListener { point ->
                findRoute(point)
                true
            }
            val listener = OnSourceDataLoadedListener { eventData ->
                if (!mapboxNavigation.getNavigationRoutes().isEmpty() && eventData.id == "mapbox://mapbox.mapbox-traffic-v2-beta" && eventData.type == SourceDataType.TILE) {
                    val cameraZoom = mapboxMap.cameraState.zoom.toLong()
                    if (cameraZoom == eventData.tileID!!.zoom) {
                        val currentTime = System.currentTimeMillis()
                        val updatePeriodMs = 17000 // FIXME: to be fetched from eventData.
                        if (currentTime - lastRouteTrafficUpdateTime >= updatePeriodMs) {
                            Log.d("Traffic", "Tile that triggered update: " + eventData.tileID)
                            lastRouteTrafficUpdateTime = currentTime;
                            mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
                        }
                    }
                }
            }
            mapboxMap.addOnSourceDataLoadedListener(listener)
        }

        // initialize view interactions
        binding.recenter.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
        }
        binding.routeOverview.setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            binding.recenter.showTextAndExtend(2000L)
        }
        binding.startNavigation.setOnClickListener {
            mapboxNavigation.setNavigationRoutes(previewedRoutes)
            coordinates = mutableListOf()
            val primaryRoute = previewedRoutes.firstOrNull()
            previewedRoutes = mutableListOf()
            if (primaryRoute != null) {
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.startTripSession()
                binding.startNavigation.visibility = INVISIBLE

                navigationCamera.requestNavigationCameraToFollowing()

                startSimulation(primaryRoute.directionsRoute)
            }
        }
        binding.clearAnnotations.setOnClickListener {
            val routes = mapboxNavigation.getNavigationRoutes()
            val newRoutes = routes.map {
                it.directionsRoute
                    .toBuilder()
                    .legs(
                        it.directionsRoute.legs()?.map { leg ->
                            leg.toBuilder()
                                .annotation(
                                    leg.annotation()?.let { annotation ->
                                        annotation.toBuilder()
                                            .congestion(annotation.congestion()?.map { "unknown" })
                                            .congestionNumeric(
                                                annotation.congestionNumeric()?.map { null }
                                            )
                                            .build()
                                    }
                                )
                                .build()
                        }
                    )
                    .build()
                    .toNavigationRoute(it.origin)
            }
            mapboxNavigation.setNavigationRoutes(newRoutes)
            mapboxNavigation.routeRefreshController.requestImmediateRouteRefresh()
        }

        // start the trip session to being receiving location updates in free drive
        // and later when a route is set, also receiving route progress updates
        mapboxNavigation.startTripSession()
    }

    override fun onStart() {
        super.onStart()
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
        binding.mapView.location.addOnIndicatorPositionChangedListener(onPositionChangedListener)

        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(1.5)
        mapboxReplayer.play()
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        binding.mapView.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineAPI.cancel()
        routeLineView.cancel()
        mapboxNavigation.onDestroy()
    }

    private fun findRoute(destination: Point) {
        if (coordinates.isEmpty()) {
            val origin = navigationLocationProvider.lastLocation?.let {
                Point.fromLngLat(it.longitude, it.latitude)
            } ?: return
            coordinates.add(origin)
        }
        coordinates.add(destination)

        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(coordinates)
                .alternatives(true)
                .build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    binding.startNavigation.visibility = VISIBLE
                    previewedRoutes = routes
                    previewRoutes(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                }
            }
        )
    }

    private fun previewRoutes(routes: List<NavigationRoute>) {
        // set route
        routeLineAPI.setNavigationRoutes(routes) {
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeLineView.renderRouteDrawData(style, it)
            }
        }
        // update the camera position to account for the new route
        viewportDataSource.onRouteChanged(routes.first())
        viewportDataSource.evaluate()

        // show UI elements
        binding.routeOverview.visibility = VISIBLE
        binding.routeOverview.showTextAndExtend(2000L)

        // move the camera to overview when new route is available
        navigationCamera.requestNavigationCameraToOverview()
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return Utils.getMapboxAccessToken(this)
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData: List<ReplayEventBase> = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        mapboxReplayer.play()
    }
}
