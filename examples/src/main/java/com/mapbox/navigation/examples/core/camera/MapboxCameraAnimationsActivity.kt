package com.mapbox.navigation.examples.core.camera

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.delegates.listeners.eventdata.MapLoadErrorType
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.getGesturesPlugin
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.getLocationComponentPlugin
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.camera.AnimationAdapter.OnAnimationButtonClicked
import com.mapbox.navigation.examples.core.databinding.LayoutActivityCameraBinding
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureActionListener
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
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
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            40.0 * pixelDensity,
            40.0 * pixelDensity,
            40.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    private val paddedFollowingEdgeInsets = EdgeInsets(
        0.0 * pixelDensity,
        40.0 * pixelDensity,
        120.0 * pixelDensity,
        40.0 * pixelDensity
    )

    private val notPaddedEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            0.0,
            0.0,
            0.0,
            0.0
        )
    }

    private var followingEdgeInsets = notPaddedEdgeInsets
        set(value) {
            field = value
            viewportDataSource.followingPadding = value
            viewportDataSource.evaluate()
        }

    private var lookAtPoint: Point? = null
        set(value) {
            field = value
            if (value != null) {
                poiSource.geometry(value)
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

            routeArrowAPI.addUpcomingManeuverArrow(routeProgress).apply {
                ifNonNull(routeArrowView, mapboxMap.getStyle()) { view, style ->
                    view.renderManeuverUpdate(style, this)
                }
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
                startSimulation(routes[0])
                viewportDataSource.onRouteChanged(routes.first())
                viewportDataSource.overviewPadding = overviewEdgeInsets
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
                navigationCamera.requestNavigationCameraToIdle()
            }
        }
    }

    private val onIndicatorPositionChangedListener = object : OnIndicatorPositionChangedListener {
        override fun onIndicatorPositionChanged(point: Point) {
            routeLineAPI?.updateTraveledRouteLine(point)
        }
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.getLocationComponentPlugin().apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MapboxCameraAnimationsActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            enabled = true
        }

        initNavigation()

        val debugger = MapboxNavigationViewportDataSourceDebugger(
            this,
            binding.mapView
        ).apply {
            enabled = true
        }
        viewportDataSource = MapboxNavigationViewportDataSource(
            binding.mapView.getMapboxMap()
        )
        viewportDataSource.debugger = debugger
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.getCameraAnimationsPlugin(),
            viewportDataSource
        )
        navigationCamera.debugger = debugger
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
                        viewportDataSource
                            .options
                            .followingFrameOptions
                            .zoomUpdatesAllowed = false
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
            .build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
    }

    private fun initButtons() {
        binding.gravitateLeft.setOnClickListener {
            followingEdgeInsets = EdgeInsets(
                followingEdgeInsets.top,
                20.0,
                followingEdgeInsets.bottom,
                200.0 * pixelDensity
            )
            viewportDataSource.evaluate()
        }

        binding.gravitateRight.setOnClickListener {
            followingEdgeInsets = EdgeInsets(
                followingEdgeInsets.top,
                200.0 * pixelDensity,
                followingEdgeInsets.bottom,
                20.0
            )
            viewportDataSource.evaluate()
        }

        binding.gravitateTop.setOnClickListener {
            followingEdgeInsets = EdgeInsets(
                20.0,
                followingEdgeInsets.left,
                240.0 * pixelDensity,
                followingEdgeInsets.right
            )
            viewportDataSource.evaluate()
        }

        binding.gravitateBottom.setOnClickListener {
            followingEdgeInsets = EdgeInsets(
                240.0 * pixelDensity,
                followingEdgeInsets.left,
                20.0,
                followingEdgeInsets.right
            )
            viewportDataSource.evaluate()
        }
    }

    private fun initCameraListeners() {
        mapboxMap.addOnCameraChangeListener {
            this.runOnUiThread {
                if (navigationCamera.state == NavigationCameraState.FOLLOWING &&
                    mapboxMap.getCameraOptions().pitch!! < 0.1
                ) {
                    // this is added to avoid locking zoom level after scaling in top-down following
                    viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed = true
                }
                updateCameraChangeView()
            }
        }
    }

    private fun initNavigation() {
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

        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(1.0)
        mapboxReplayer.play()
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

    private fun initStyle() {
        mapboxMap.loadStyleUri(
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
                override fun onMapLoadError(mapLoadErrorType: MapLoadErrorType, msg: String) {
                    Log.e(
                        "CameraAnimationsAct",
                        "Error loading map - error type: $mapLoadErrorType, message: $msg"
                    )
                }
            }
        )
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
                followingEdgeInsets = if (followingEdgeInsets == paddedFollowingEdgeInsets) {
                    notPaddedEdgeInsets
                } else {
                    paddedFollowingEdgeInsets
                }
                viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed = true
                viewportDataSource.followingPadding = followingEdgeInsets
                viewportDataSource.evaluate()
                navigationCamera.requestNavigationCameraToFollowing()
            }
            AnimationType.Overview -> {
                viewportDataSource.overviewPadding = overviewEdgeInsets
                viewportDataSource.evaluate()
                navigationCamera.requestNavigationCameraToOverview()
            }
            AnimationType.ToPOI -> {
                navigationLocationProvider.lastLocation?.let {
                    val center = Point.fromLngLat(
                        it.longitude + 0.0123,
                        it.latitude + 0.0123
                    )
                    // workaround for https://github.com/mapbox/mapbox-maps-android/issues/177
                    // binding.mapView.getCameraAnimationsPlugin().flyTo(
                    binding.mapView.getCameraAnimationsPlugin().easeTo(
                        CameraOptions.Builder()
                            .padding(notPaddedEdgeInsets)
                            .center(center)
                            .bearing(0.0)
                            .zoom(14.0)
                            .pitch(0.0)
                            .build(),
                        MapAnimationOptions.mapAnimationOptions {
                            duration(1000L)
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
                        viewportDataSource.additionalPointsToFrameForFollowing(listOf(it))
                        viewportDataSource.followingBearingPropertyOverride(
                            TurfMeasurement.bearing(center, it)
                        )
                        viewportDataSource.evaluate()
                    }
                } else {
                    lookAtPoint = null
                    viewportDataSource.additionalPointsToFrameForFollowing(emptyList())
                    viewportDataSource.followingBearingPropertyOverride(null)
                    viewportDataSource.evaluate()
                }
            }
            AnimationType.RemoveRoute -> {
                mapboxNavigation.setRoutes(emptyList())
            }
        }
    }

    private fun findRoute(origin: Point, destination: Point) {
        Utils.vibrate(this)
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

        mapboxNavigation.requestRoutes(
            routeOptions,
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
