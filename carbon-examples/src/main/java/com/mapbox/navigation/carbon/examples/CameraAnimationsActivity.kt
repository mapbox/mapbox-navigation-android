package com.mapbox.navigation.carbon.examples

import android.Manifest.permission
import android.animation.Animator
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.*
import com.mapbox.maps.MapboxMap.OnMapLoadErrorListener
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImpl
import com.mapbox.maps.plugin.animation.animator.*
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.gesture.GesturePluginImpl
import com.mapbox.maps.plugin.gesture.OnMapLongClickListener
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions
import com.mapbox.maps.plugin.location.LocationComponentPlugin
import com.mapbox.maps.plugin.location.modes.RenderMode
import com.mapbox.maps.plugin.style.layers.addLayerBelow
import com.mapbox.maps.plugin.style.layers.generated.lineLayer
import com.mapbox.maps.plugin.style.layers.properties.generated.LineCap
import com.mapbox.maps.plugin.style.layers.properties.generated.LineJoin
import com.mapbox.maps.plugin.style.sources.addSource
import com.mapbox.maps.plugin.style.sources.generated.GeojsonSource
import com.mapbox.maps.plugin.style.sources.generated.geojsonSource
import com.mapbox.maps.plugin.style.sources.getSource
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.carbon.examples.AnimationAdapter.OnAnimationButtonClicked
import com.mapbox.navigation.carbon.examples.LocationPermissionHelper.Companion.areLocationPermissionsGranted
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigation.Companion.defaultNavigationOptionsBuilder
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import kotlinx.android.synthetic.main.layout_camera_animations.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

enum class CameraState(val id_string: String) {
    FREE("Free"),
    FOLLOWING("Following"),
    OVERVIEW("Overview"),
    TRANSITION_TO_FOLLOWING("To_following"),
    TRANSITION_TO_OVERVIEW("To_overview")
}

class CameraAnimationsActivity: AppCompatActivity(), PermissionsListener, OnAnimationButtonClicked, OnMapLongClickListener {

    private var  locationComponent: LocationComponentPlugin? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapCamera: CameraAnimationsPluginImpl
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var route: DirectionsRoute
    private lateinit var lineSource: GeojsonSource
    private val replayRouteMapper = ReplayRouteMapper()
    private val mapboxReplayer = MapboxReplayer()
    private val pointGeometries: MutableList<Point> = mutableListOf()
    private val permissionsHelper = LocationPermissionHelper(this)
    private val locationEngineCallback: MyLocationEngineCallback = MyLocationEngineCallback(this)

    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private var cameraState: CameraState = CameraState.FREE

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            Timber.d("raw location %s", rawLocation.toString());
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
        updateLocation(Arrays.asList(location))
    }

    private fun updateLocation(locations: List<Location>) {
        getLocationComponent()!!.forceLocationUpdate(locations, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_camera_animations)
        mapboxMap = mapView.getMapboxMap()
        locationComponent = getLocationComponent()
        mapCamera = getMapCamera()

        if (areLocationPermissionsGranted(this)) {
            requestPermissionIfNotGranted(permission.WRITE_EXTERNAL_STORAGE)
        } else {
            permissionsHelper.requestLocationPermissions(this)
        }
    }

    private fun init() {
        initAnimations()
        initNavigation()
        initStyle()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        val navigationOptions = defaultNavigationOptionsBuilder(this, getMapboxAccessTokenFromResources())
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()
        mapboxNavigation = MapboxNavigation(navigationOptions).apply {
            registerRouteProgressObserver(replayProgressObserver)
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
        mapboxNavigation?.startTripSession()
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(MAPBOX_STREETS, object: Style.OnStyleLoaded {
            override fun onStyleLoaded(style: Style) {
                initializeLocationComponent(style)
                mapboxNavigation.navigationOptions.locationEngine.getLastLocation(locationEngineCallback)
                getGesturePlugin()?.addOnMapLongClickListener(this@CameraAnimationsActivity)
            }
        }, object: OnMapLoadErrorListener {
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

    fun transitionToVehicleFollowing(points: List<Point>, edgeInsets: EdgeInsets) {
        cameraState = CameraState.TRANSITION_TO_FOLLOWING
        updateCameraChangeView()
        val location = locationComponent?.lastKnownLocation
        if (location != null) {
            val centerPoint = Point.fromLngLat(location.longitude, location.latitude)
            val yOffset = mapboxMap.getSize().height / 2 - edgeInsets.bottom
            transitionFromLowZoomToHighZoom(centerPoint, location.bearing.toDouble(), 16.35, 40.0, ScreenCoordinate(0.0, yOffset)) {
                cameraState = CameraState.FOLLOWING
                updateCameraChangeView()
            }
        }
    }

    fun transitionToRouteOverview(points: List<Point>, edgeInsets: EdgeInsets) {
        cameraState = CameraState.TRANSITION_TO_OVERVIEW
        updateCameraChangeView()
        val location = locationComponent?.lastKnownLocation
        if (location != null) {
            val mutablePoints: MutableList<Point> = points.toMutableList()
            mutablePoints.add(Point.fromLngLat(location.longitude, location.latitude))
            val zoomAndCenter = getZoomLevelAndCenterCoordinate(points, 0.0, 0.0, edgeInsets)
            val currentMapCamera = mapboxMap.getCameraOptions(null)
            if (currentMapCamera.zoom ?: 2.0 < zoomAndCenter.first) {
                transitionFromLowZoomToHighZoom(zoomAndCenter.second, 0.0, zoomAndCenter.first, 0.0, ScreenCoordinate(0.0, 0.0)) {
                    cameraState = CameraState.OVERVIEW
                    updateCameraChangeView()
                }
            } else {
                transitionFromHighZoomToLowZoom(zoomAndCenter.second, 0.0, zoomAndCenter.first, 0.0) {
                    cameraState = CameraState.OVERVIEW
                    updateCameraChangeView()
                }
            }
        }
    }

    private var timeofLastCameraChangeViewUpdate: Date = Date()

    private fun updateCameraChangeView() {
        val newDate = Date()
        val timeSinceLastUpdate = newDate.time - timeofLastCameraChangeViewUpdate.time
        if (timeSinceLastUpdate < 16) {
            return
        }
        val currentMapCamera = mapboxMap.getCameraOptions(null)
        cameraChangeView_state.text = "state: ${cameraState.id_string}"
        cameraChangeView_lng.text = "lng: ${currentMapCamera.center?.longitude().toString().format(6) ?: "null"}"
        cameraChangeView_lat.text = "lat: ${currentMapCamera.center?.latitude().toString().format(6) ?: "null"}"
        cameraChangeView_zoom.text = "zoom: ${currentMapCamera.zoom.toString().format("%.2f") ?: "null"}"
        cameraChangeView_bearing.text = "bearing: ${currentMapCamera.bearing.toString().format(2) ?: "null"}"
        cameraChangeView_pitch.text = "pitch: ${currentMapCamera.pitch.toString().format(2) ?: "null"}"
        cameraChangeView_anchor.text = "anchor: t: ${currentMapCamera.padding?.top.toString().format(1) ?: "null"} l: ${currentMapCamera.padding?.left.toString().format(1) ?: "null"} b: ${currentMapCamera.padding?.bottom.toString().format(1) ?: "null"} r: ${currentMapCamera.padding?.right.toString().format(1) ?: "null"}"
        timeofLastCameraChangeViewUpdate = newDate
    }

    private fun transitionFromLowZoomToHighZoom(center: Point, bearing: Double, zoomLevel: Double, pitch: Double, anchorOffset: ScreenCoordinate, onComplete: () -> Unit) {
        val currentMapCamera = mapboxMap.getCameraOptions(null)
        val currentMapCameraCenter = currentMapCamera.center
        var screenDistanceFromMapCenterToLocation = 0.0
        if (currentMapCameraCenter != null) {
            val currentCenterScreenCoordinate = mapboxMap.pixelForCoordinate(Point.fromLngLat(currentMapCameraCenter.longitude(), currentMapCameraCenter.latitude()))
            val locationScreenCoordinate = mapboxMap.pixelForCoordinate(center)
            screenDistanceFromMapCenterToLocation = Math.hypot(currentCenterScreenCoordinate.x - locationScreenCoordinate.x, currentCenterScreenCoordinate.y - locationScreenCoordinate.y)
        }

        var bearingShortestRotation = bearing
        if (currentMapCamera.bearing != null) {
            bearingShortestRotation = currentMapCamera.bearing!! + shortestRotation(currentMapCamera.bearing!!, bearing)
        }

        val currentMapCameraZoomLevel = currentMapCamera.zoom
        var zoomLevelDelta = 0.0
        if (currentMapCameraZoomLevel != null) {
            zoomLevelDelta = Math.abs(zoomLevel - currentMapCameraZoomLevel)
        }

        var currentPadding = EdgeInsets(0.0,0.0,0.0,0.0)
        if (currentMapCamera.padding != null) {
            currentPadding = currentMapCamera.padding!!
        }

        val centerAnimationRate = 300.0
        val centerDuration = Math.min((screenDistanceFromMapCenterToLocation / centerAnimationRate) * 1000.0, 3000.0)

        val zoomAnimationRate = 2.0
        val zoomDelay = centerDuration * 0.3
        val zoomDuration = Math.min((zoomLevelDelta / zoomAnimationRate) * 1000.0, 3000.0)

        val bearingAnimationRate = 100.0
        val bearingDuration = 1800.0
        val bearingDelay = Math.max(zoomDelay + zoomDuration - bearingDuration , 0.0)

        val pitchAndAnchorDuration = 1200.0
        val pitchAndAnchorDelay = Math.max(zoomDelay + zoomDuration - pitchAndAnchorDuration + 100 , 0.0)

        val center = CameraCenterAnimator.create(center) {
            duration = centerDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val zoom = CameraZoomAnimator.create(zoomLevel) {
            startDelay = zoomDelay.toLong()
            duration = zoomDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val bearing = CameraBearingAnimator.create(bearingShortestRotation) {
            startDelay = bearingDelay.toLong()
            duration = bearingDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val pitch = CameraPitchAnimator.create(pitch) {
            startDelay = pitchAndAnchorDelay.toLong()
            duration = pitchAndAnchorDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        val startPadding = currentPadding
        val endPadding = getEdgeInsetsFromScreenCenterOffset(mapboxMap.getSize(), anchorOffset)

        val padding = CameraPaddingAnimator.create(CameraAnimator.StartValue(startPadding), endPadding) {
            startDelay = pitchAndAnchorDelay.toLong()
            duration = pitchAndAnchorDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        mapCamera.cancelAllAnimators()
        mapCamera.registerAnimators(center, zoom, bearing, pitch, padding)
        mapCamera.addCameraCenterChangeListener(object : CameraCenterAnimator.ChangeListener {
            override fun onChanged(updatedValue: Point) {
                updateCameraChangeView()
            }
        })
        val set = AnimatorSet()
        set.playTogether(center, zoom, bearing, pitch, padding)
        set.start()
        set.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                onComplete()
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {

            }

        })

        Toast
            .makeText(this@CameraAnimationsActivity,
                "transitionFromLowZoomToHighZoom",
                Toast.LENGTH_SHORT).show()
    }

    private fun transitionFromHighZoomToLowZoom(center: Point, bearing: Double, zoomLevel: Double, pitch: Double, onComplete: () -> Unit) {
        val currentMapCamera = mapboxMap.getCameraOptions(null)

        var bearingShortestRotation = bearing
        if (currentMapCamera.bearing != null) {
            bearingShortestRotation = currentMapCamera.bearing!! + shortestRotation(currentMapCamera.bearing!!, bearing)
        }

        var currentPadding = EdgeInsets(0.0,0.0,0.0,0.0)
        if (currentMapCamera.padding != null) {
            currentPadding = currentMapCamera.padding!!
        }

        val center = CameraCenterAnimator.create(center) {
            startDelay = 800
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val zoom = CameraZoomAnimator.create(zoomLevel) {
            startDelay = 0
            duration = 1800
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val bearing = CameraBearingAnimator.create(bearingShortestRotation) {
            startDelay = 600
            duration = 1200
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val pitch = CameraPitchAnimator.create(pitch) {
            startDelay = 0
            duration = 1000
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        val startPadding = currentPadding
        val endPadding = getEdgeInsetsFromScreenCenterOffset(mapboxMap.getSize(), ScreenCoordinate(0.0, 0.0))

        val padding = CameraPaddingAnimator.create(CameraAnimator.StartValue(startPadding), endPadding) {
            startDelay = 0
            duration = 1200
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        mapCamera.cancelAllAnimators()
        mapCamera.registerAnimators(center, zoom, bearing, pitch, padding)
        mapCamera.addCameraCenterChangeListener(object : CameraCenterAnimator.ChangeListener {
            override fun onChanged(updatedValue: Point) {
                updateCameraChangeView()
            }
        })
        val set = AnimatorSet()
        set.playTogether(center, zoom, bearing, pitch, padding)
        set.start()
        set.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                onComplete()
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {

            }

        })

        Toast
            .makeText(this@CameraAnimationsActivity,
                "transitionFromHighZoomToLowZoom",
                Toast.LENGTH_SHORT).show()
    }

    private fun shortestRotation(from: Double, to: Double): Double {
        return (to - from + 540) % 360 - 180
    }

    private fun getEdgeInsetsFromScreenCenterOffset(mapSize: Size, centerOffset: ScreenCoordinate = ScreenCoordinate(0.0,0.0)): EdgeInsets {
        val mapCenterScreenCoordinate = ScreenCoordinate((mapSize.width / 2).toDouble(), (mapSize.height / 2).toDouble())
        return EdgeInsets(mapCenterScreenCoordinate.y + centerOffset.y, mapCenterScreenCoordinate.x + centerOffset.x, mapCenterScreenCoordinate.y - centerOffset.y, mapCenterScreenCoordinate.x - centerOffset.x)
    }

    private fun getZoomLevelAndCenterCoordinate(points: List<Point>, bearing: Double, pitch: Double, edgeInsets: EdgeInsets): Pair<Double, Point> {
        val cam = mapboxMap.cameraForCoordinates(points, edgeInsets, bearing, pitch)

        if (cam != null && cam.zoom != null && cam.center != null) {
            return Pair(cam.zoom!! - 0.2, cam.center!!)
        }
        return Pair(2.0, Point.fromLngLat(0.0,0.0))
    }

    @SuppressLint("MissingPermission")
    override fun onButtonClicked(animationType: AnimationType) {
        when (animationType) {
            AnimationType.Following -> {
                transitionToVehicleFollowing(pointGeometries, EdgeInsets(12.0,12.0,12.0,12.0))
            }
            AnimationType.Overview -> {
                transitionToRouteOverview(pointGeometries, EdgeInsets(12.0,12.0,12.0,12.0))
            }
            AnimationType.Recenter -> {
                // Recenter animation
            }
        }
    }

    /**
     * Add data to the map once the GeoJSON has been loaded
     *
     * @param featureCollection returned GeoJSON FeatureCollection from the Directions API route request
     */
    private fun initData(style: Style, featureCollection: FeatureCollection) {
        initSources(style, featureCollection)
        initLinePath(style)
    }

    private fun initSources(style: Style, featureCollection: FeatureCollection) {
        lineSource = geojsonSource("line-source-id") {
            featureCollection(featureCollection)
        }
        style.addSource(lineSource)
    }

    private fun initLinePath(style: Style) {
        style.addLayerBelow(
            lineLayer("line-layer-id", "line-source-id") {
                lineColor("#F13C6E")
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
                lineWidth(4.0)
            },
            below = "road-label"
        )
    }

    private fun findRoute(origin: Point, destination: Point) {
        val routeOptions: RouteOptions = RouteOptions.builder()
            .applyDefaultParams()
            .accessToken(getMapboxAccessTokenFromResources())
            .coordinates(Arrays.asList(origin, destination))
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
            // Clear all the existing geometries first
            pointGeometries.clear()
            pointGeometries.addAll(PolylineUtils.decode(route.geometry()!!, 6))
            mapView.getMapboxMap().getStyle(object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    val lSource = style.getSource("line-source-id")
                    route.geometry()?.let { geometry ->
                        if (lSource != null) {
                            // update line source with new set of geometries
                            lineSource.geometry(LineString.fromLngLats(pointGeometries))
                        } else {
                            initData(
                                style,
                                FeatureCollection.fromFeature(
                                    Feature.fromGeometry(
                                        LineString.fromLngLats(pointGeometries)
                                    )
                                )
                            )
                        }
                        startSimulation(route)
                        transitionToRouteOverview(pointGeometries, EdgeInsets(40.0,40.0,40.0,40.0))
                    }
                }
            })
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
            it.renderMode = RenderMode.COMPASS
        }
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private fun getLocationComponent(): LocationComponentPlugin? {
        return mapView.getPlugin(LocationComponentPlugin::class.java)
    }

    private fun getMapCamera(): CameraAnimationsPluginImpl {
        return mapView.getCameraAnimationsPlugin()
    }

    private fun getGesturePlugin(): GesturePluginImpl? {
        return mapView.getPlugin(GesturePluginImpl::class.java)
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

    private fun requestPermissionIfNotGranted(permission: String) {
        val permissionsNeeded: MutableList<String> = ArrayList()
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(permission)
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 10)
        } else {
            init()
        }
    }

    companion object {
        class MyLocationEngineCallback(activity: CameraAnimationsActivity): LocationEngineCallback<LocationEngineResult> {
            private val activityRef = WeakReference<CameraAnimationsActivity>(activity)

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
}
