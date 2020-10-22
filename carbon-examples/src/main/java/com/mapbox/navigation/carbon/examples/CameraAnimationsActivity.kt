package com.mapbox.navigation.carbon.examples

import android.Manifest.permission
import android.animation.Animator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.*
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorChangeListener
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.GesturesPluginImpl
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.getGesturesPlugin
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions
import com.mapbox.maps.plugin.location.LocationComponentConstants
import com.mapbox.maps.plugin.location.LocationComponentPlugin
import com.mapbox.maps.plugin.location.modes.RenderMode
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.carbon.examples.AnimationAdapter.OnAnimationButtonClicked
import com.mapbox.navigation.carbon.examples.LocationPermissionHelper.Companion.areLocationPermissionsGranted
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigation.Companion.defaultNavigationOptionsBuilder
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.camera.*
import com.mapbox.navigation.ui.route.NavigationMapRoute
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMisc
import kotlinx.android.synthetic.main.layout_camera_animations.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.max

class CameraAnimationsActivity :
    AppCompatActivity(),
    PermissionsListener,
    OnAnimationButtonClicked,
    OnMapLongClickListener,
    OnMoveListener,
    RouteProgressObserver,
    MapboxCameraStateChangeObserver {

    private var locationComponent: LocationComponentPlugin? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var navigationMapRoute: NavigationMapRoute
    private lateinit var route: DirectionsRoute
    private lateinit var navigationStateTransitionProvider: NavigationStateTransitionProvider
    private val navigationCameraOptions = NavigationCameraOptions.Builder().build()
    private val replayRouteMapper = ReplayRouteMapper()
    private val mapboxReplayer = MapboxReplayer()
    private val fullRoutePoints: MutableList<Point> = mutableListOf()
    private val permissionsHelper = LocationPermissionHelper(this)
    private val locationEngineCallback: MyLocationEngineCallback = MyLocationEngineCallback(this)
    private var previousCameraState: MapboxCameraState = MapboxCameraState.IDLE

    private var cameraState = MapboxCameraState.IDLE
        set(value) {
            field = value
            updateCameraChangeView()
        }
    private var completeRoutePoints: List<List<List<Point>>> = emptyList()
    private var remainingPointsOnCurrentStep: List<Point> = emptyList()
    private var remainingPointsOnRoute: List<Point> = emptyList()

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            Timber.d("raw location %s", rawLocation.toString())
        }

        override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
            if (keyPoints.isEmpty()) {
                updateLocation(enhancedLocation)
            } else {
                updateLocation(keyPoints)
            }
        }
    }

    private fun updateLocation(location: Location) {
        updateLocation(listOf(location))
    }

    private fun updateLocation(locations: List<Location>) {
        getLocationComponent()!!.forceLocationUpdate(locations, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_camera_animations)
        mapboxMap = mapView.getMapboxMap()
        mapView.getGesturesPlugin().addOnMoveListener(this)
        locationComponent = getLocationComponent()
        navigationStateTransitionProvider = MapboxNavigationStateTransition(
            mapView,
            MapboxNavigationCameraTransition(mapView)
        )

        if (areLocationPermissionsGranted(this)) {
            requestPermissionIfNotGranted(permission.WRITE_EXTERNAL_STORAGE)
        } else {
            permissionsHelper.requestLocationPermissions(this)
        }
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        routeProgress.currentLegProgress?.let { currentLegProgress ->
            currentLegProgress.currentStepProgress?.let { currentStepProgress ->
                val currentStepFullPoints = currentStepProgress.stepPoints ?: emptyList()
                var distanceTraveledOnStepKM = max(
                    currentStepProgress.distanceTraveled / 1000.0, 0.0)
                val fullDistanceOfCurrentStepKM = max(
                    (currentStepProgress.distanceRemaining
                        + currentStepProgress.distanceTraveled) / 1000.0,
                    0.0)
                if (distanceTraveledOnStepKM > fullDistanceOfCurrentStepKM) distanceTraveledOnStepKM = 0.0

                try {
                    remainingPointsOnCurrentStep = TurfMisc.lineSliceAlong(
                        LineString.fromLngLats(currentStepFullPoints),
                        distanceTraveledOnStepKM,
                        fullDistanceOfCurrentStepKM, TurfConstants.UNIT_KILOMETERS
                    ).coordinates()
                } catch (e: TurfException) {
                    return
                }

                val currentLegPoints = completeRoutePoints[currentLegProgress.legIndex]
                val remainingStepsAfterCurrentStep = if (currentStepProgress.stepIndex < currentLegPoints.size) currentLegPoints.slice(currentStepProgress.stepIndex + 1 until currentLegPoints.size - 1) else emptyList()
                val remainingPointsAfterCurrentStep = remainingStepsAfterCurrentStep.flatten()
                remainingPointsOnRoute = listOf(remainingPointsOnCurrentStep, remainingPointsAfterCurrentStep).flatten()

                updateCameraTracking()
            }
        }
    }

    private fun updateCameraTracking() {
        if (cameraState == MapboxCameraState.FOLLOWING) {
            updateMapFrameForFollowing()
        } else if (cameraState == MapboxCameraState.ROUTE_OVERVIEW) {
            updateMapFrameForOverview()
        }
    }

    private fun updateMapFrameForFollowing() {
        locationComponent?.lastKnownLocation?.let {
            navigationStateTransitionProvider.updateMapFrameForFollowing(NavigationStateTransitionToFollowingOptions.Builder(
                it, remainingPointsOnCurrentStep
            ).apply {
                padding(navigationCameraOptions.edgeInsets)
                maxZoom(navigationCameraOptions.maxZoom)
                pitch(navigationCameraOptions.followingPitch)
            }.build()).start()
        }
    }

    private fun updateMapFrameForOverview() {
        locationComponent?.lastKnownLocation?.let {
            navigationStateTransitionProvider.updateMapFrameForOverview(NavigationStateTransitionToRouteOverviewOptions.Builder(
                it, remainingPointsOnRoute
            ).apply {
                padding(navigationCameraOptions.edgeInsets)
                maxZoom(navigationCameraOptions.maxZoom)
                pitch(navigationCameraOptions.overviewPitch)
            }.build()).start()
        }
    }

    private fun init() {
        initAnimations()
        initNavigation()
        initStyle()
        initCameraListeners()
    }

    private fun initCameraListeners() {
        getMapCamera().apply {
            addCameraCenterChangeListener(object : CameraAnimatorChangeListener<Point> {
                override fun onChanged(updatedValue: Point) {
                    updateCameraChangeView()
                }
            })
            addCameraZoomChangeListener(object : CameraAnimatorChangeListener<Double> {
                override fun onChanged(updatedValue: Double) {
                    updateCameraChangeView()
                }
            })
            addCameraBearingChangeListener(object : CameraAnimatorChangeListener<Double> {
                override fun onChanged(updatedValue: Double) {
                    updateCameraChangeView()
                }
            })
            addCameraPitchChangeListener(object : CameraAnimatorChangeListener<Double> {
                override fun onChanged(updatedValue: Double) {
                    updateCameraChangeView()
                }
            })
            addCameraPaddingChangeListener(object : CameraAnimatorChangeListener<EdgeInsets> {
                override fun onChanged(updatedValue: EdgeInsets) {
                    updateCameraChangeView()
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        val navigationOptions = defaultNavigationOptionsBuilder(this, getMapboxAccessTokenFromResources())
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()
        mapboxNavigation = MapboxNavigation(navigationOptions).apply {
            registerRouteProgressObserver(this@CameraAnimationsActivity)
            registerLocationObserver(locationObserver)
        }

        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(1.0)
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = replayRouteMapper.mapDirectionsRouteLegAnnotation(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxNavigation.startTripSession()
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(MAPBOX_STREETS, object : Style.OnStyleLoaded {
            override fun onStyleLoaded(style: Style) {
                initializeLocationComponent(style)
                mapboxNavigation.navigationOptions.locationEngine.getLastLocation(locationEngineCallback)
                getGesturesPlugin()?.addOnMapLongClickListener(this@CameraAnimationsActivity)
                navigationMapRoute = NavigationMapRoute.Builder(mapView, mapboxMap, this@CameraAnimationsActivity)
                    .withBelowLayer(LocationComponentConstants.FOREGROUND_LAYER)
                    .withMapboxNavigation(mapboxNavigation)
                    .withVanishRouteLineEnabled(true)
                    .build()
            }
        }, object : OnMapLoadErrorListener {
            override fun onMapLoadError(mapViewLoadError: MapLoadError, msg: String) {
                Timber.e("Error loading map: %s", mapViewLoadError.name)
            }
        })
    }

    private fun initAnimations() {
        val adapter = AnimationAdapter(this, this)
        val manager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        animationsList.layoutManager = manager
        animationsList.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    private fun updateCameraChangeView() {
        mapboxMap.getCameraOptions(null).let { currentMapCamera ->
            cameraChangeView_state.text = "state: $cameraState"
            cameraChangeView_lng.text = "lng: ${currentMapCamera.center?.longitude().toString().format(6)}"
            cameraChangeView_lat.text = "lat: ${currentMapCamera.center?.latitude().toString().format(6)}"
            cameraChangeView_zoom.text = "zoom: ${currentMapCamera.zoom.toString().format("%.2f")}"
            cameraChangeView_bearing.text = "bearing: ${currentMapCamera.bearing.toString().format(2)}"
            cameraChangeView_pitch.text = "pitch: ${currentMapCamera.pitch.toString().format(2)}"
            cameraChangeView_anchor.text = "anchor: t: ${currentMapCamera.padding?.top.toString().format(1)} l: ${currentMapCamera.padding?.left.toString().format(1)} b: ${currentMapCamera.padding?.bottom.toString().format(1)} r: ${currentMapCamera.padding?.right.toString().format(1)}"
        }
    }

    @SuppressLint("MissingPermission")
    override fun onButtonClicked(animationType: AnimationType) {
        when (animationType) {
            AnimationType.Following -> {
                previousCameraState = MapboxCameraState.FOLLOWING
                requestCameraToFollowing()
            }
            AnimationType.Overview -> {
                previousCameraState = MapboxCameraState.ROUTE_OVERVIEW
                requestCameraToRouteOverview()
            }
            AnimationType.Recenter -> {
                if (previousCameraState == MapboxCameraState.FOLLOWING) {
                    requestCameraToFollowing()
                } else if (previousCameraState == MapboxCameraState.ROUTE_OVERVIEW) {
                    requestCameraToRouteOverview()
                }
            }
        }
    }

    private fun requestCameraToFollowing() {
        if (cameraState == MapboxCameraState.TRANSITION_TO_FOLLOWING
            || cameraState == MapboxCameraState.FOLLOWING) {
            return
        }

        navigationStateTransitionProvider.transitionToVehicleFollowing(NavigationStateTransitionToFollowingOptions.Builder(
            locationComponent?.lastKnownLocation!!,
            remainingPointsOnCurrentStep).apply {
            pitch(navigationCameraOptions.followingPitch)
            padding(navigationCameraOptions.edgeInsets)
            maxZoom(navigationCameraOptions.maxZoom)
        }.build()).apply { addListener(toFollowingTransitionAnimatorListener) }.start()
    }

    private fun requestCameraToRouteOverview() {
        if (cameraState == MapboxCameraState.TRANSITION_TO_ROUTE_OVERVIEW
            || cameraState == MapboxCameraState.ROUTE_OVERVIEW) {
            return
        }

        navigationStateTransitionProvider.transitionToRouteOverview(NavigationStateTransitionToRouteOverviewOptions.Builder(
            locationComponent?.lastKnownLocation!!,
            remainingPointsOnRoute).apply {
            pitch(navigationCameraOptions.followingPitch)
            padding(navigationCameraOptions.edgeInsets)
            maxZoom(navigationCameraOptions.maxZoom)
        }.build()).apply { addListener(toRouteOverviewTransitionAnimatorListener) }.start()
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

        mapboxNavigation.requestRoutes(
            routeOptions,
            routesReqCallback
        )
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            route = routes[0]
            processRouteInfo()
            // Clear all the existing geometries first
            fullRoutePoints.clear()
            fullRoutePoints.addAll(PolylineUtils.decode(route.geometry()!!, 6))
            navigationMapRoute.addRoute(route)
            previousCameraState = MapboxCameraState.ROUTE_OVERVIEW
            remainingPointsOnRoute = fullRoutePoints
            requestCameraToRouteOverview()
            startSimulation(route)
            Toast
                .makeText(this@CameraAnimationsActivity,
                    "routesReqCallback",
                    Toast.LENGTH_SHORT).show()
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {

        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {

        }
    }

    override fun onMapLongClick(point: Point): Boolean {
        locationComponent?.let { locComp ->
            val currentLocation = locComp.lastKnownLocation
            if (currentLocation != null) {
                val originPoint = Point.fromLngLat(currentLocation.longitude, currentLocation.latitude)
                findRoute(originPoint, point)
            }
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.getGesturesPlugin().removeOnMoveListener(this)
        mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    private fun initializeLocationComponent(style: Style) {
        val activationOptions = LocationComponentActivationOptions.builder(this, style)
            .useDefaultLocationEngine(false) //SBNOTE: I think this should be false eventually
            .build()
        locationComponent?.let {
            it.activateLocationComponent(activationOptions)
            it.enabled = true
            it.renderMode = RenderMode.GPS
        }
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private fun getLocationComponent(): LocationComponentPlugin? {
        return mapView.getPlugin(LocationComponentPlugin::class.java)
    }

    private fun getMapCamera(): CameraAnimationsPlugin {
        return mapView.getCameraAnimationsPlugin()
    }

    private fun getGesturesPlugin(): GesturesPluginImpl? {
        return mapView.getPlugin(GesturesPluginImpl::class.java)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "This app needs location and storage permissions in order to show its functionality.", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when {
            requestCode == LOCATION_PERMISSIONS_REQUEST_CODE -> {
                permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
            grantResults.isNotEmpty() -> {
                init()
            }
            else -> {
                Toast.makeText(this, "You didn't grant storage and/or location permissions.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            requestPermissionIfNotGranted(permission.WRITE_EXTERNAL_STORAGE)
        } else {
            Toast.makeText(this, "Uou didn't grant location permissions.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("SameParameterValue")
    private fun requestPermissionIfNotGranted(permission: String) {
        val permissionsNeeded: MutableList<String> = ArrayList()
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(permission)
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 10)
        } else {
            init()
        }
    }

    private fun processRouteInfo() {
        completeRoutePoints = route.legs()?.map { routeLeg ->
            routeLeg.steps()?.map { legStep ->
                legStep.geometry()?.let { geometry ->
                    PolylineUtils.decode(geometry, 6).toList()
                } ?: emptyList()
            } ?: emptyList()
        } ?: emptyList()
    }

    companion object {
        class MyLocationEngineCallback(activity: CameraAnimationsActivity) : LocationEngineCallback<LocationEngineResult> {
            private val activityRef = WeakReference(activity)

            override fun onSuccess(result: LocationEngineResult?) {
                val activity = activityRef.get()
                activity?.locationComponent?.let { locComponent ->
                    val location = result?.lastLocation
                    location?.let { loc ->
                        val point = Point.fromLngLat(loc.longitude, loc.latitude)
                        val cameraOptions = CameraOptions.Builder().center(point).zoom(13.0).build()
                        activity.mapboxMap.jumpTo(cameraOptions)
                        locComponent.forceLocationUpdate(location)
                    } ?: Timber.e("Location from the result is null")
                } ?: Timber.e("Location Component cannot be null")
            }

            override fun onFailure(exception: Exception) {
                Timber.i(exception)
            }
        }
    }

    override fun onMapboxCameraStateChange(mapboxCameraState: MapboxCameraState) {
        updateCameraChangeView()
    }

    override fun onMove(detector: MoveGestureDetector) {
        cameraState = MapboxCameraState.IDLE
    }

    override fun onMoveBegin(detector: MoveGestureDetector) {
    }

    override fun onMoveEnd(detector: MoveGestureDetector) {
    }

    private val toFollowingTransitionAnimatorListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {
            cameraState = MapboxCameraState.TRANSITION_TO_FOLLOWING
        }

        override fun onAnimationEnd(animation: Animator?) {
            cameraState = MapboxCameraState.FOLLOWING
        }

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationRepeat(animation: Animator?) {
        }
    }

    private val toRouteOverviewTransitionAnimatorListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {
            cameraState = MapboxCameraState.TRANSITION_TO_ROUTE_OVERVIEW
        }

        override fun onAnimationEnd(animation: Animator?) {
            cameraState = MapboxCameraState.ROUTE_OVERVIEW
        }

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationRepeat(animation: Animator?) {
        }
    }
}

enum class MapboxCameraState {
    IDLE,
    TRANSITION_TO_FOLLOWING,
    FOLLOWING,
    TRANSITION_TO_ROUTE_OVERVIEW,
    ROUTE_OVERVIEW
}

interface MapboxCameraStateChangeObserver {
    fun onMapboxCameraStateChange(mapboxCameraState: MapboxCameraState)
}
