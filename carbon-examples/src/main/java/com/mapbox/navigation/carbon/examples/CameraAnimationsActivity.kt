package com.mapbox.navigation.carbon.examples

import android.Manifest.permission
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
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
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
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.route.RouteUrl
import com.mapbox.navigation.carbon.examples.AnimationAdapter.OnAnimationButtonClicked
import com.mapbox.navigation.carbon.examples.LocationPermissionHelper.Companion.areLocationPermissionsGranted
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigation.Companion.defaultNavigationOptionsBuilder
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfTransformation
import kotlinx.android.synthetic.main.layout_camera_animations.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt

class CameraAnimationsActivity: AppCompatActivity(), PermissionsListener, OnAnimationButtonClicked, OnMapLongClickListener {

    private var  locationComponent: LocationComponentPlugin? = null
    private lateinit var  mapboxMap: MapboxMap
    private lateinit var  mapCamera: CameraAnimationsPluginImpl
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var route: DirectionsRoute
    private val pointGeometries: MutableList<Point> = mutableListOf()
    private val permissionsHelper = LocationPermissionHelper(this)
    private val locationEngineCallback: MyLocationEngineCallback = MyLocationEngineCallback(this)

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
            .locationEngine(LocationEngineProvider.getBestLocationEngine(this))
            .build()
        mapboxNavigation = MapboxNavigation(navigationOptions)
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

    fun transitionToVehicleFollowing() {
        val location = locationComponent?.lastKnownLocation
        if (location != null) {
            val centerPoint = Point.fromLngLat(location.longitude, location.latitude)
            transitionFromLowZoomToHighZoom(centerPoint, location.bearing.toDouble(), 16.35, 40.0)
        }
    }

    fun transitionToRouteOverview(points: List<Point>, edgeInsets: EdgeInsets) {
        val location = locationComponent?.lastKnownLocation
        if (location != null) {
            val mutablePoints: MutableList<Point> = points.toMutableList()
            mutablePoints.add(Point.fromLngLat(location.longitude, location.latitude))
            val zoomAndCenter = getZoomLevelAndCenterCoordinate(points, 0.0, 0.0, edgeInsets)
            transitionFromHighZoomToLowZoom(zoomAndCenter.second, 0.0, zoomAndCenter.first, 0.0)
        }
    }

    private fun transitionFromLowZoomToHighZoom(center: Point, bearing: Double, zoomLevel: Double, pitch: Double) {
        val currentMapCamera = mapboxMap.getCameraOptions(null)
        val currentMapCameraCenter = currentMapCamera.center
        var screenDistanceFromMapCenterToLocation = 0.0
        if (currentMapCameraCenter != null) {
            val currentCenterScreenCoordinate = mapboxMap.pixelForCoordinate(Point.fromLngLat(currentMapCameraCenter.longitude(), currentMapCameraCenter.latitude()))
            val locationScreenCoordinate = mapboxMap.pixelForCoordinate(center)
            screenDistanceFromMapCenterToLocation = Math.hypot(currentCenterScreenCoordinate.x - locationScreenCoordinate.x, currentCenterScreenCoordinate.y - locationScreenCoordinate.y)
        }

        val currentMapCameraZoomLevel = currentMapCamera.zoom
        var zoomLevelDelta = 0.0
        if (currentMapCameraZoomLevel != null) {
            zoomLevelDelta = Math.abs(zoomLevel - currentMapCameraZoomLevel)
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
        val bearing = CameraBearingAnimator.create(bearing) {
            startDelay = bearingDelay.toLong()
            duration = bearingDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val pitch = CameraPitchAnimator.create(pitch) {
            startDelay = pitchAndAnchorDelay.toLong()
            duration = pitchAndAnchorDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        val startPadding = getEdgeInsetsFromScreenCenterOffset(mapboxMap.getSize(), android.graphics.Point(0, 0))
        val endPadding = getEdgeInsetsFromScreenCenterOffset(mapboxMap.getSize(), android.graphics.Point(0, 300))

        val padding = CameraPaddingAnimator.create(CameraAnimator.StartValue(startPadding), endPadding) {
            startDelay = pitchAndAnchorDelay.toLong()
            duration = pitchAndAnchorDuration.toLong()
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        mapCamera.registerAnimators(center, zoom, bearing, pitch, padding)
        val set = AnimatorSet()
        set.playTogether(center, zoom, bearing, pitch, padding)
        set.start()

        Toast
            .makeText(this@CameraAnimationsActivity,
                "transitionFromLowZoomToHighZoom",
                Toast.LENGTH_SHORT).show()
    }

    private fun transitionFromHighZoomToLowZoom(center: Point, bearing: Double, zoomLevel: Double, pitch: Double) {
        val center = CameraCenterAnimator.create(center) {
            duration = 1800
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val zoom = CameraZoomAnimator.create(zoomLevel) {
            startDelay = 0
            duration = 1800
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val bearing = CameraBearingAnimator.create(bearing) {
            startDelay = 600
            duration = 1800
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }
        val pitch = CameraPitchAnimator.create(pitch) {
            startDelay = 0
            duration = 1200
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        val startPadding = getEdgeInsetsFromScreenCenterOffset(mapboxMap.getSize(), android.graphics.Point(0, 300))
        val endPadding = getEdgeInsetsFromScreenCenterOffset(mapboxMap.getSize(), android.graphics.Point(0, 0))

        val padding = CameraPaddingAnimator.create(CameraAnimator.StartValue(startPadding), endPadding) {
            startDelay = 0
            duration = 1200
            interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
        }

        mapCamera.registerAnimators(center, zoom, bearing, pitch, padding)
        val set = AnimatorSet()
        set.playTogether(center, zoom, bearing, pitch, padding)
        set.start()

        Toast
            .makeText(this@CameraAnimationsActivity,
                "transitionFromHighZoomToLowZoom",
                Toast.LENGTH_SHORT).show()
    }

    private fun getEdgeInsetsFromScreenCenterOffset(mapSize: Size, centerOffset: android.graphics.Point = android.graphics.Point(0,0)): EdgeInsets {
        val mapCenterScreenCoordinate = ScreenCoordinate((mapSize.width / 2).toDouble(), (mapSize.height / 2).toDouble())
        return EdgeInsets(mapCenterScreenCoordinate.y + centerOffset.y, mapCenterScreenCoordinate.x + centerOffset.x, mapCenterScreenCoordinate.y - centerOffset.y, mapCenterScreenCoordinate.x - centerOffset.x)
    }

    private fun getZoomLevelAndCenterCoordinate(points: List<Point>, bearing: Double, pitch: Double, edgeInsets: EdgeInsets): Pair<Double, Point> {
        val mapInsetWidth = mapboxMap.getSize().width - edgeInsets.left - edgeInsets.right
        val mapInsetHeight = mapboxMap.getSize().height - edgeInsets.top - edgeInsets.bottom

        val widthForMinPitch = mapInsetWidth
        val widthForMaxPitch = mapInsetHeight * 2
        val widthDelta = widthForMaxPitch - widthForMinPitch
        val widthWithPitchEffect = widthForMinPitch + ((pitch / 40.0) * widthDelta)
        val heightWithPitchEffect = mapInsetHeight + (mapInsetHeight * Math.sin(pitch * Math.PI / 180.0) * 1.25)

        val currentBearing = mapboxMap.getCameraOptions(null).bearing
        val bearingDelta = shortestRotation(currentBearing ?: bearing, bearing)

        val screenCoordinatesForPoints = points.map { mapboxMap.pixelForCoordinate(Point.fromLngLat(it.longitude(), it.latitude())) }
        val centerScreenCoordinate = getCenterScreenCoordinate(screenCoordinatesForPoints)
        val rotatedPixels = screenCoordinatesForPoints.map {rotate(it, centerScreenCoordinate, -bearingDelta)}

        val screenCoordinatesBbox = getBboxFromScreenCoordinates(rotatedPixels)

        var pointsFromRotatedPixels = screenCoordinatesBbox?.map { mapboxMap.coordinateForPixel(it) }

        if (pointsFromRotatedPixels != null) {
            val rotatedBbox = getBboxFromPoints(pointsFromRotatedPixels)!!
            val zl = getCoordinateBoundsZoomLevel(rotatedBbox, Size(widthWithPitchEffect.toFloat(), heightWithPitchEffect.toFloat()))

            return Pair(zl, getCenterPoint(pointsFromRotatedPixels))
        }


//            val widthDistance = TurfMeasurement.distance(coordinatesFromScreenPointBbox[0], coordinatesFromScreenPointBbox[1]) * 1000.0
//            val heightDistance = TurfMeasurement.distance(coordinatesFromScreenPointBbox[1], coordinatesFromScreenPointBbox[2]) * 1000.0
//            val coordinateScreenPointsBboxSizeInMeters = Size(widthDistance.toFloat(), heightDistance.toFloat())
//            val bboxNorth = TurfMeasurement.destination(centerCoordinateOfBbox, coordinateScreenPointsBboxSizeInMeters.height / 2.0, 0.0, TurfConstants.UNIT_METERS)
//            val bboxSouth = TurfMeasurement.destination(centerCoordinateOfBbox, coordinateScreenPointsBboxSizeInMeters.height / 2.0, 180.0, TurfConstants.UNIT_METERS)
//            val bboxWest = TurfMeasurement.destination(centerCoordinateOfBbox, coordinateScreenPointsBboxSizeInMeters.width / 2.0, -90.0, TurfConstants.UNIT_METERS)
//            val bboxEast = TurfMeasurement.destination(centerCoordinateOfBbox, coordinateScreenPointsBboxSizeInMeters.width / 2.0, 90.0, TurfConstants.UNIT_METERS)
//            val rotatedBbox = listOf(Point.fromLngLat(bboxWest.longitude(), bboxNorth.latitude()), Point.fromLngLat(bboxEast.longitude(), bboxNorth.latitude()), Point.fromLngLat(bboxEast.longitude(), bboxSouth.latitude()), Point.fromLngLat(bboxWest.longitude(), bboxSouth.latitude()))


//        }

        return Pair(2.0, Point.fromLngLat(0.0,0.0))
    }

    private fun shortestRotation(from: Double, to: Double): Double {
        return (to - from + 540) % 360 - 180
    }

    private fun rotate(pixel: ScreenCoordinate, around: ScreenCoordinate, angle: Double): ScreenCoordinate {
        val centerPixel = around
        val radians = angle * Math.PI / 180
        val rx = Math.cos(radians) * (pixel.x-centerPixel.x) - Math.sin(radians) * (pixel.y-centerPixel.y) + centerPixel.x
        val ry = Math.sin(radians) * (pixel.x-centerPixel.x) + Math.cos(radians) * (pixel.y-centerPixel.y) + centerPixel.y
        return ScreenCoordinate(rx, ry)
    }

    private fun getBboxFromScreenCoordinates(pixels: List<ScreenCoordinate>): List<ScreenCoordinate>? {
        val ys: List<Double> = pixels.map {it.y}
        val xs: List<Double> = pixels.map {it.x}
        val ysMax: Double? = ys.maxOrNull()
        val xsMin: Double? = xs.minOrNull()
        val ysMin: Double? = ys.minOrNull()
        val xsMax: Double? = xs.maxOrNull()
        if (ysMax != null && xsMin != null && ysMin != null && xsMax != null) {
            val tl = ScreenCoordinate(xsMin, ysMin)
            val tr = ScreenCoordinate(xsMax, ysMin)
            val br = ScreenCoordinate(xsMax, ysMax)
            val bl = ScreenCoordinate(xsMin, ysMax)
            return listOf(tl, tr, br, bl)
        }
        return null
    }

    private fun getCenterScreenCoordinate(pixels: List<ScreenCoordinate>): ScreenCoordinate {
        val avgX = pixels.map {it.x}.reduce {acc, it -> acc + it} / pixels.size
        val avgY = pixels.map {it.y}.reduce {acc, it -> acc + it} / pixels.size
        return ScreenCoordinate(avgX, avgY)
    }

    private fun getBboxFromPoints(points: List<Point>): List<Point>? {
        val lats: List<Double> = points.map {it.latitude()}
        val lngs: List<Double> = points.map {it.longitude()}
        val latsMax = lats.maxOrNull()
        val lngsMin = lngs.minOrNull()
        val latsMin = lats.minOrNull()
        val lngsMax = lngs.maxOrNull()
        if (latsMax != null && lngsMin != null && latsMin != null && lngsMax != null) {
            val nw = Point.fromLngLat(lngsMin, latsMax)
            val ne = Point.fromLngLat(lngsMax, latsMax)
            val se = Point.fromLngLat(lngsMax, latsMin)
            val sw = Point.fromLngLat(lngsMin, latsMin)
            return listOf(nw, ne, se, sw)
        }
        return null
    }

    private fun getCenterPoint(points: List<Point>): Point {
        var avgLng = 0.0
        var avgLat = 0.0
        if (points.size > 0) {
            avgLat = points.map { it.latitude() }.reduce { acc, it -> acc + it } / points.size
            avgLng = points.map { it.longitude() }.reduce { acc, it -> acc + it } / points.size
        }
        return Point.fromLngLat(avgLng, avgLat)
    }

    private fun getCoordinateBoundsZoomLevel(bounds: List<Point>, fitToSize: Size): Double {
        val bbox = getBboxFromPoints(bounds)
        if (bbox != null) {
            val nw = bbox[1]
            val se = bbox[3]
            val latFraction = (latRad(nw.latitude()) - latRad(se.latitude())) / Math.PI

            val lngDiff = nw.longitude() - se.longitude()
            val lngFraction = ( if (lngDiff < 0) (lngDiff + 360) else lngDiff) / 360

            val latZoom = zoom (fitToSize.height.toDouble(), 512.0, latFraction)
            val lngZoom = zoom (fitToSize.width.toDouble(), 512.0, lngFraction)

//            return Math.min(latZoom, 21.0)
            return Math.min(Math.min(latZoom, lngZoom), 21.0)
        }

        return 12.0
    }

    private fun latRad(lat: Double): Double {
        val sinVal = Math.sin(lat * Math.PI / 180)
        val radX2 = Math.log((1 + sinVal) / (1 - sinVal)) / 2
        return Math.max(Math.min(radX2, Math.PI), -Math.PI) / 2
    }

    private fun zoom(displayDimensionSize: Double, tileSize: Double, fraction: Double): Double {
        return Math.log(displayDimensionSize / tileSize / fraction) / ln(2.0)
    }

    override fun onButtonClicked(animationType: AnimationType) {
        when (animationType) {
            AnimationType.Animation1 -> {
                transitionToVehicleFollowing()
            }
            AnimationType.Animation2 -> {
                transitionToRouteOverview(pointGeometries, EdgeInsets(20.0,20.0,20.0,20.0))
            }
            AnimationType.Animation3 -> {
                Toast
                    .makeText(this@CameraAnimationsActivity,
                        "Animation3",
                        Toast.LENGTH_SHORT).show()
            }
            AnimationType.Animation4 -> {
                Toast
                    .makeText(this@CameraAnimationsActivity,
                        "Animation4",
                        Toast.LENGTH_SHORT).show()
            }
            AnimationType.Animation5 -> {
                Toast
                    .makeText(this@CameraAnimationsActivity,
                        "Animation5",
                        Toast.LENGTH_SHORT).show()
            }
            AnimationType.Animation6 -> {
                Toast
                    .makeText(this@CameraAnimationsActivity,
                        "Animation6",
                        Toast.LENGTH_SHORT).show()
            }
            AnimationType.Animation7 -> {
                Toast
                    .makeText(this@CameraAnimationsActivity,
                        "Animation7",
                        Toast.LENGTH_SHORT).show()
            }
            AnimationType.Animation8 -> {
                Toast
                    .makeText(this@CameraAnimationsActivity,
                        "Animation8",
                        Toast.LENGTH_SHORT).show()
            }
            AnimationType.Animation9 -> {
                Toast
                    .makeText(this@CameraAnimationsActivity,
                        "Animation9",
                        Toast.LENGTH_SHORT).show()
            }
            AnimationType.Animation10 -> {
                Toast
                    .makeText(this@CameraAnimationsActivity,
                        "Animation10",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findRoute(origin: Point, destination: Point) {
        val routeOptions: RouteOptions = RouteOptions.builder()
            .applyDefaultParams()
            .accessToken(getMapboxAccessTokenFromResources())
            .coordinates(Arrays.asList(origin, destination))
            .alternatives(false)
            .geometries(RouteUrl.GEOMETRY_POLYLINE)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .build()

        mapboxNavigation.requestRoutes(
            routeOptions,
            routesReqCallback
        )
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            route = routes[0]
            // All the geometries are added to this list [pointGeometries]
            pointGeometries.addAll(PolylineUtils.decode(route.geometry()!!, 5))
            transitionToRouteOverview(pointGeometries, EdgeInsets(20.0,20.0,20.0,20.0))
            Timber.d("some message: -------------------------------------------------------------------------")
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
