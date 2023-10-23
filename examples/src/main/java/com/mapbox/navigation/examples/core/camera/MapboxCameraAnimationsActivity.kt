package com.mapbox.navigation.examples.core.camera

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimationsLifecycleListener
import com.mapbox.maps.plugin.animation.CameraAnimatorType
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.camera.AnimationAdapter.OnAnimationButtonClicked
import com.mapbox.navigation.examples.core.databinding.LayoutActivityCameraBinding
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.FollowingCameraFramingStrategy
import com.mapbox.navigation.ui.maps.camera.data.FollowingFrameOptions.FocalPoint
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
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
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MapboxCameraAnimationsActivity :
    AppCompatActivity(),
    OnAnimationButtonClicked,
    OnMapLongClickListener {

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
        0.0,
        0.0,
        120.0 * pixelDensity,
        0.0
    )

    private val notPaddedEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            0.0,
            0.0,
            0.0,
            0.0
        )
    }

    private var followingEdgeInsets = paddedFollowingEdgeInsets
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
    private var complexFollowingNorth = false
    private val poiLayer = CircleLayer("circle_layer", "circle_source")
        .circleColor(Color.RED)
        .circleRadius(10.0)
    private val poiSource = geoJsonSource("circle_source") {}.data("")

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

            lookAtPoint?.run {
                val point = Point.fromLngLat(
                    locationMatcherResult.enhancedLocation.longitude,
                    locationMatcherResult.enhancedLocation.latitude,
                )
                val bearing = TurfMeasurement.bearing(point, this)
                viewportDataSource.followingBearingPropertyOverride(bearing)
            }

            if (complexFollowingNorth) {
                viewportDataSource.options.followingFrameOptions.focalPoint = focalPointForBearing(
                    locationMatcherResult.enhancedLocation.bearing.toDouble()
                )
            }

            viewportDataSource.evaluate()
            if (locationMatcherResult.isTeleport) {
                navigationCamera.resetFrame()
            }
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

    private val onIndicatorPositionChangedListener =
        OnIndicatorPositionChangedListener { point ->
            routeLineAPI?.updateTraveledRouteLine(point)?.apply {
                ifNonNull(routeLineView, mapboxMap.getStyle()) { view, style ->
                    view.renderRouteLineUpdate(style, this)
                }
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
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
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
        viewportDataSource.options.followingFrameOptions.apply {
            // An example of maneuver exclusion from "pitch to 0 near maneuvers" updates.
            pitchNearManeuvers.excludedManeuvers = listOf(
                StepManeuver.FORK,
                StepManeuver.OFF_RAMP,
                StepManeuver.ARRIVE
            )
        }
        viewportDataSource.debugger = debugger
        navigationCamera = NavigationCamera(
            binding.mapView.getMapboxMap(),
            binding.mapView.camera,
            viewportDataSource
        )
        navigationCamera.debugger = debugger
        /* Alternative to the NavigationScaleGestureHandler
        mapView.getCameraAnimationsPlugin().addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )*/
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

        binding.mapView.camera.addCameraAnimationsLifecycleListener(object : CameraAnimationsLifecycleListener {
            override fun onAnimatorInterrupting(type: CameraAnimatorType, runningAnimator: ValueAnimator, runningAnimatorOwner: String?, newAnimator: ValueAnimator, newAnimatorOwner: String?) {
                Log.d("MapZoomingTest", "CameraAnimation.onAnimatorInterrupting($type, $runningAnimatorOwner, $newAnimatorOwner)")
            }

            override fun onAnimatorCancelling(type: CameraAnimatorType, animator: ValueAnimator, owner: String?) {
            }

            override fun onAnimatorEnding(type: CameraAnimatorType, animator: ValueAnimator, owner: String?) {
            }

            override fun onAnimatorStarting(type: CameraAnimatorType, animator: ValueAnimator, owner: String?) {
            }
        })
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
                    mapboxMap.cameraState.pitch < 0.1
                ) {
                    // this is added to avoid locking zoom level after scaling in top-down following
                    viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed = true
                }
                updateCameraChangeView()
            }
        }
    }

    private fun initNavigation() {
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        ).apply {
            registerLocationObserver(
                object : LocationObserver {

                    override fun onNewRawLocation(rawLocation: Location) {
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
        mapboxMap.loadStyleUri(
            MAPBOX_STREETS,
            { style ->
                binding.mapView.gestures.addOnMapLongClickListener(
                    this@MapboxCameraAnimationsActivity
                )
                style.addSource(poiSource)
                style.addLayer(poiLayer)
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    Log.e(
                        MapboxCameraAnimationsActivity::class.java.simpleName,
                        "Error loading map - error type: " +
                            "${eventData.type}, message: ${eventData.message}"
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
        mapboxMap.cameraState.let { currentMapCamera ->
            binding.cameraChangeViewState.text = "state: ${navigationCamera.state}"
            binding.cameraChangeViewLng.text = "lng: " +
                currentMapCamera.center.longitude().formatNumber()
            binding.cameraChangeViewLat.text =
                "lat: ${currentMapCamera.center.latitude().formatNumber()}"
            binding.cameraChangeViewZoom.text = "zoom: ${currentMapCamera.zoom.formatNumber()}"
            binding.cameraChangeViewBearing.text =
                "bearing: ${currentMapCamera.bearing.formatNumber()}"
            binding.cameraChangeViewPitch.text = "pitch: ${currentMapCamera.pitch.formatNumber()}"
            binding.cameraChangeViewPadding.text =
                """
                    |padding:
                    |  top: ${currentMapCamera.padding.top.formatNumber()}
                    |  left: ${currentMapCamera.padding.left.formatNumber()}
                    |  bottom: ${currentMapCamera.padding.bottom.formatNumber()}
                    |  right: ${currentMapCamera.padding.right.formatNumber()}
                """.trimMargin()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onButtonClicked(animationType: AnimationType) {
        viewportDataSource.options.followingFrameOptions.framingStrategy =
            FollowingCameraFramingStrategy.Default

        when (animationType) {
            AnimationType.Following, AnimationType.FastFollowing, AnimationType.FollowingNorth -> {
                followingEdgeInsets = paddedFollowingEdgeInsets
                viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed = true
                viewportDataSource.followingPadding = followingEdgeInsets
                if (animationType == AnimationType.FollowingNorth) {
                    viewportDataSource.apply {
                        followingBearingPropertyOverride(0.0)
                        followingPitchPropertyOverride(0.0)
                        // An example moving the user location puck to the screen center
                        viewportDataSource.options.followingFrameOptions.focalPoint =
                            navigationLocationProvider.lastLocation?.let {
                                focalPointForBearing(it.bearing.toDouble())
                            } ?: FocalPoint(0.5, 0.5)
                        // disable the maximizeViewableGeometryWhenPitchZero keep the puck centered
                        options.followingFrameOptions.maximizeViewableGeometryWhenPitchZero = false
                    }
                    complexFollowingNorth = true
                } else {
                    viewportDataSource.apply {
                        followingBearingPropertyOverride(null)
                        followingPitchPropertyOverride(null)
                        // An example moving the user location puck to the bottom edge (default)
                        options.followingFrameOptions.focalPoint = FocalPoint(0.5, 1.0)
                        options.followingFrameOptions.maximizeViewableGeometryWhenPitchZero = true
                    }
                    complexFollowingNorth = false
                }
                viewportDataSource.evaluate()
                if (animationType == AnimationType.Following) {
                    navigationCamera.requestNavigationCameraToFollowing()
                } else {
                    navigationCamera.requestNavigationCameraToFollowing(
                        stateTransitionOptionsBlock = {
                            maxDuration(750L)
                        },
                        frameTransitionOptionsBlock = {
                            maxDuration(333L)
                        }
                    )
                }
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
                    binding.mapView.camera.easeTo(
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
                    val center = mapboxMap.cameraState.center
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
                mapboxNavigation.setNavigationRoutes(emptyList())
            }
            AnimationType.CustomFollowing -> {
                followingEdgeInsets = paddedFollowingEdgeInsets
                viewportDataSource.apply {
                    followingPadding = followingEdgeInsets
                    options.followingFrameOptions.zoomUpdatesAllowed = true
                    options.followingFrameOptions.framingStrategy =
                        CustomFollowingCameraFramingStrategy()
                    evaluate()
                }
                navigationCamera.requestNavigationCameraToFollowing()
            }
        }
    }

    private fun findRoute(origin: Point, destination: Point) {
        Utils.vibrate(this)
        val routeOptions: RouteOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
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
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private fun Number?.formatNumber() = "%.8f".format(this)

    /**
     * Produces a [FocalPoint] that shifts the focal point of the camera to maximize the viewable area depending on bearing of the vehicle.
     */
    private fun focalPointForBearing(bearing: Double): FocalPoint {
        val bearingRad = Math.toRadians(bearing)
        val focalPointX = sin(-bearingRad) / 3.0 + 0.5
        val focalPointY = cos(bearingRad) / 3.0 + 0.5
        return FocalPoint(
            focalPointX,
            focalPointY
        )
    }
}
