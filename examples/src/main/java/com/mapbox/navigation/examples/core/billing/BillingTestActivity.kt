package com.mapbox.navigation.examples.core.billing

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.delegates.listeners.eventdata.MapLoadErrorType
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.databinding.LayoutActivityBillingBinding
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandler
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.clearRouteLine
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BillingTestActivity :
    AppCompatActivity(),
    ActionAdapter.OnActionButtonClicked,
    OnMapLongClickListener {

    private var cachedRoute: DirectionsRoute? = null
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

    private lateinit var binding: LayoutActivityBillingBinding
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private val mapMatcherResultObserver = MapMatcherResultObserver { mapMatcherResult ->
        val transitionOptions: (ValueAnimator.() -> Unit) = if (mapMatcherResult.isTeleport) {
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

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
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

    private val routesObserver = RoutesObserver { routes ->
        if (routes.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                routeLineAPI?.setRoutes(listOf(RouteLine(routes[0], null)))?.apply {
                    ifNonNull(routeLineView, mapboxMap.getStyle()) { view, style ->
                        view.renderRouteDrawData(style, this)
                    }
                }
            }
            startSimulation(routes[0])
            viewportDataSource.onRouteChanged(routes.first())
            viewportDataSource.evaluate()
            navigationCamera.requestNavigationCameraToOverview()
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
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityBillingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val adapter = ActionAdapter(this, this)
        val manager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.animationsList.layoutManager = manager
        binding.animationsList.adapter = adapter

        mapboxMap = binding.mapView.getMapboxMap()
        mapboxMap.loadStyleUri(
            MAPBOX_STREETS,
            {
                binding.mapView.gestures.addOnMapLongClickListener(
                    this@BillingTestActivity
                )
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(
                    mapLoadErrorType: MapLoadErrorType,
                    message: String
                ) {
                    Log.e(
                        "CameraAnimationsAct",
                        "Error loading map - error type: $mapLoadErrorType, message: $message"
                    )
                }
            }
        )
        locationComponent = binding.mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@BillingTestActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }

        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label")
            .withVanishingRouteLineEnabled(true)
            .build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)
        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
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
                        mapboxMap.setCamera(cameraOptions)
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
        }

        viewportDataSource = MapboxNavigationViewportDataSource(
            binding.mapView.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource
        )
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
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    override fun onButtonClicked(actionType: ActionType) {
        when (actionType) {
            ActionType.StartSession -> {
                mapboxNavigation.startTripSession()
                mapboxReplayer.pushRealLocation(this, 0.0)
                mapboxReplayer.playbackSpeed(1.0)
                mapboxReplayer.play()
            }
            ActionType.StopSession -> {
                mapboxNavigation.stopTripSession()
            }
            ActionType.RequestRandomRoute -> {
                findRoute(
                    Point.fromLngLat(17.054013559442637, 51.20471802210465),
                    Point.fromLngLat(17.035893251811565, 51.23273921880337)
                )
            }
            ActionType.Request2LegRoute -> {
                navigationLocationProvider.lastLocation?.toPoint()?.let {
                    findRoute(
                        it,
                        Point.fromLngLat(it.longitude() + 0.001, it.latitude() + 0.001),
                        Point.fromLngLat(it.longitude() + 0.003, it.latitude() + 0.003)
                    )
                }
            }
            ActionType.SetAvailableRoute -> {
                cachedRoute?.let {
                    mapboxNavigation.setRoutes(listOf(it))
                }
            }
            ActionType.RemoveRoute -> {
                mapboxNavigation.setRoutes(emptyList())
            }
        }
    }

    private fun findRoute(origin: Point, destination: Point, waypoint: Point? = null) {
        Utils.vibrate(this)
        val routeOptions: RouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinatesList(
                mutableListOf(
                    origin,
                    destination
                ).apply { if (waypoint != null) this.add(1, waypoint) })
            .build()

        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    Toast.makeText(this@BillingTestActivity, "route ready", Toast.LENGTH_LONG)
                        .show()
                    cachedRoute = routes.first()
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    // no impl
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    // no impl
                }
            }
        )
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
        navigationCamera.resetFrame()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation.onDestroy()
    }
}
