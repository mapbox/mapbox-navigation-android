package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapLoadError
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxMapOptions
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.getGesturesPlugin
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions
import com.mapbox.maps.plugin.location.LocationPluginImpl
import com.mapbox.maps.plugin.location.LocationUpdate
import com.mapbox.maps.plugin.location.getLocationPlugin
import com.mapbox.maps.plugin.location.modes.RenderMode
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.internal.route.RouteUrl
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.util.Slackline
import com.mapbox.navigation.ui.base.model.tripprogress.DistanceRemainingFormatter
import com.mapbox.navigation.ui.base.model.tripprogress.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.base.model.tripprogress.PercentDistanceTraveledFormatter
import com.mapbox.navigation.ui.base.model.tripprogress.TimeRemainingFormatter
import com.mapbox.navigation.ui.base.model.tripprogress.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import kotlinx.android.synthetic.main.trip_progress_activity_layout.*
import java.lang.ref.WeakReference

class TripProgressActivity : AppCompatActivity(), OnMapLongClickListener {

    private val slackline = Slackline(this)
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var locationComponent: LocationPluginImpl
    private lateinit var mapCamera: CameraAnimationsPlugin
    private lateinit var mapboxNavigation: MapboxNavigation
    private val mapboxReplayer = MapboxReplayer()
    private lateinit var tripProgressApiApi: MapboxTripProgressApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.trip_progress_activity_layout)
        addMap()
        mapboxMap = mapView.getMapboxMap()
        locationComponent = getLocationComponent()
        mapCamera = getMapCamera()
        init()

        tripProgressApiApi = MapboxTripProgressApi(getTripProgressFormatter())
    }

    private fun getLocationComponent(): LocationPluginImpl {
        return mapView.getLocationPlugin()
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

    private fun addMap() {
        val mapboxMapOptions = MapboxMapOptions(this, resources.displayMetrics.density, null)
        val resourceOptions = ResourceOptions.Builder()
            .accessToken(getMapboxAccessTokenFromResources())
            .assetPath(filesDir.absolutePath)
            .cachePath(filesDir.absolutePath + "/mbx.db")
            .cacheSize(100000000L) // 100 MB
            .tileStorePath(filesDir.absolutePath + "/maps_tile_store/")
            .build()
        mapboxMapOptions.resourceOptions = resourceOptions
        mapView = MapView(this, mapboxMapOptions)
        mapView_container.addView(mapView)
    }

    private fun init() {
        initNavigation()
        initStyle()
        slackline.initialize(mapView, mapboxNavigation)
        initListeners()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this@TripProgressActivity)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            MAPBOX_STREETS,
            Style.OnStyleLoaded { style: Style ->
                initializeLocationComponent(style)
                mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                    locationEngineCallback
                )
                mapView.getGesturesPlugin().addOnMapLongClickListener(this)
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapLoadError: MapLoadError, msg: String) {
                    Log.e(
                        TripProgressActivity::class.java.simpleName,
                        "Error loading map: " + mapLoadError.name
                    )
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        startNavigation.setOnClickListener {
            locationComponent.renderMode = RenderMode.GPS
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.startTripSession()
            startNavigation.visibility = View.GONE
            tripProgressView.visibility = View.VISIBLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100L)
        }
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private fun getMapCamera(): CameraAnimationsPlugin {
        return mapView.getCameraAnimationsPlugin()
    }

    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            Log.d(
                TripProgressActivity::class.java.simpleName,
                "raw location " + rawLocation.toString()
            )
        }

        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (keyPoints.isEmpty()) {
                updateLocation(enhancedLocation)
            } else {
                updateLocation(keyPoints)
            }
        }
    }

    private fun initializeLocationComponent(style: Style) {
        val activationOptions = LocationComponentActivationOptions.builder(this, style)
            .useDefaultLocationEngine(false)
            .build()
        locationComponent.activateLocationComponent(activationOptions)
        locationComponent.enabled = true
        locationComponent.renderMode = RenderMode.COMPASS
    }

    private fun updateLocation(location: Location) {
        updateLocation(listOf(location))
    }

    private fun updateLocation(locations: List<Location>) {
        val location = locations[0]
        val locationUpdate = LocationUpdate(location, null, null)
        locationComponent.forceLocationUpdate(locationUpdate)

        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration = 1500L
        mapCamera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .pitch(45.0)
                .zoom(17.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    override fun onMapLongClick(point: Point): Boolean {
        vibrate()
        startNavigation.visibility = View.GONE

        val currentLocation = mapView.getLocationPlugin().lastKnownLocation
        if (currentLocation != null) {
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            findRoute(originPoint, point)
        }
        return false
    }

    private fun findRoute(origin: Point?, destination: Point?) {
        val routeOptions = RouteOptions.builder()
            .baseUrl(RouteUrl.BASE_URL)
            .user(RouteUrl.PROFILE_DEFAULT_USER)
            .profile(RouteUrl.PROFILE_DRIVING_TRAFFIC)
            .geometries(RouteUrl.GEOMETRY_POLYLINE6)
            .requestUuid("")
            .accessToken(getMapboxAccessTokenFromResources())
            .coordinates(listOf(origin, destination))
            .alternatives(true)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            routesReqCallback
        )
    }

    private val routesReqCallback: RoutesRequestCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                startNavigation.visibility = View.VISIBLE
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Log.e(
                TripProgressActivity::class.java.simpleName,
                "route request failure " + throwable.toString()
            )
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Log.d(TripProgressActivity::class.java.simpleName, "route request canceled")
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            tripProgressApiApi.getTripProgress(routeProgress).let { update ->
                tripProgressView.render(update)
            }
        }
    }

    private val locationEngineCallback = MyLocationEngineCallback(WeakReference(this))

    private class MyLocationEngineCallback(
        private val activityRef: WeakReference<TripProgressActivity>
    ) : LocationEngineCallback<LocationEngineResult> {

        override fun onSuccess(result: LocationEngineResult?) {
            val location = result!!.lastLocation
            val activity = activityRef.get()
            if (location != null && activity != null) {
                val point = Point.fromLngLat(location.longitude, location.latitude)
                val cameraOptions = CameraOptions.Builder().center(point).zoom(13.0).build()
                activity.mapboxMap.jumpTo(cameraOptions)
                activity.locationComponent.forceLocationUpdate(location)
            }
        }

        override fun onFailure(exception: Exception) {
            Log.i(TripProgressActivity::class.java.simpleName, exception.toString())
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
