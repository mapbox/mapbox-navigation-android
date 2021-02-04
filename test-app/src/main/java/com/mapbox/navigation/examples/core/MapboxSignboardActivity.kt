package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.Vibrator
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapLoadError
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.GesturesPluginImpl
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions
import com.mapbox.maps.plugin.location.LocationPluginImpl
import com.mapbox.maps.plugin.location.LocationUpdate
import com.mapbox.maps.plugin.location.modes.RenderMode
import com.mapbox.navigation.base.internal.route.RouteUrl
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.util.Slackline
import com.mapbox.navigation.examples.util.Utils.getMapboxAccessToken
import com.mapbox.navigation.ui.base.api.signboard.SignboardApi
import com.mapbox.navigation.ui.base.api.signboard.SignboardReadyCallback
import com.mapbox.navigation.ui.base.model.signboard.SignboardState
import com.mapbox.navigation.ui.maps.signboard.api.MapboxSignboardApi
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import kotlinx.android.synthetic.main.layout_activity_signboard.*
import java.lang.ref.WeakReference
import java.util.Objects

class MapboxSignboardActivity : AppCompatActivity(), OnMapLongClickListener {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapCamera: CameraAnimationsPlugin
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var signboardApi: SignboardApi
    private var locationComponent: LocationPluginImpl? = null

    private val mapboxReplayer = MapboxReplayer()
    private val replayRouteMapper = ReplayRouteMapper()
    private val slackLine = Slackline(this)
    private val locationEngineCallback = MyLocationEngineCallback(this)
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private val signboardCallback: SignboardReadyCallback = object : SignboardReadyCallback {
        override fun onAvailable(state: SignboardState.Signboard.Available) {
            signboardView.render(SignboardState.Show)
            signboardView.render(state)
        }

        override fun onUnavailable(state: SignboardState.Signboard.Empty) {
            signboardView.render(SignboardState.Hide)
            signboardView.render(state)
        }

        override fun onError(state: SignboardState.Signboard.Error) {
            signboardView.render(SignboardState.Hide)
            signboardView.render(state)
        }
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                startNavigation.visibility = VISIBLE
            }
        }
        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {}
        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {}
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {}
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

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                startSimulation(routes[0])
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            routeProgress.bannerInstructions?.let { instruction ->
                signboardApi.generateSignboard(instruction, signboardCallback)
            }
        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_signboard)
        mapboxMap = mapView.getMapboxMap()
        locationComponent = getLocationComponent()
        mapCamera = getMapCamera()
        init()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        }
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

    override fun onMapLongClick(point: Point): Boolean {
        vibrate()
        startNavigation.visibility = GONE
        locationComponent?.let { it ->
            ifNonNull(it.lastKnownLocation) {
                val or = Point.fromLngLat(-3.5870, 40.5719)
                val de = Point.fromLngLat(-3.607835, 40.551486)
                findRoute(or, de)
                // findRoute(Point.fromLngLat(it.longitude, it.latitude), point)
            }
        }
        return false
    }

    private fun init() {
        initNavigation()
        initStyle()
        slackLine.initialize(mapView, mapboxNavigation)
        initListeners()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
        signboardApi = MapboxSignboardApi(getMapboxRouteAccessToken(this))
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            MAPBOX_STREETS,
            { style: Style ->
                initializeLocationComponent(style)
                mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                    locationEngineCallback
                )
                getGesturePlugin()!!.addOnMapLongClickListener(this)
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapViewLoadError: MapLoadError, msg: String) {
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        startNavigation.setOnClickListener {
            locationComponent?.let {
                it.renderMode = RenderMode.GPS
            }
            mapboxNavigation.startTripSession()
            startNavigation.visibility = GONE
        }
    }

    private fun findRoute(origin: Point, destination: Point) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .baseUrl("https://api-valhalla-route-staging.tilestream.net/")
                .user("directions-team")
                .profile(RouteUrl.PROFILE_DRIVING_TRAFFIC)
                .geometries(RouteUrl.GEOMETRY_POLYLINE6)
                .requestUuid("")
                .accessToken(Objects.requireNonNull(getMapboxRouteAccessToken(this)))
                .coordinates(listOf(origin, destination))
                .alternatives(true)
                .build(),
            routesReqCallback
        )
    }

    @SuppressLint("MissingPermission")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100L, DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100L)
        }
    }

    private fun getLocationComponent(): LocationPluginImpl? {
        return mapView.getPlugin(LocationPluginImpl::class.java)
    }

    private fun getMapCamera(): CameraAnimationsPlugin {
        return mapView.getCameraAnimationsPlugin()
    }

    private fun getGesturePlugin(): GesturesPluginImpl? {
        return mapView.getPlugin(GesturesPluginImpl::class.java)
    }

    private fun getMapboxAccessTokenFromResources(): String? {
        return getMapboxAccessToken(this)
    }

    private fun initializeLocationComponent(style: Style) {
        val activationOptions = LocationComponentActivationOptions.builder(this, style)
            .useDefaultLocationEngine(false)
            .build()
        locationComponent?.let {
            it.activateLocationComponent(activationOptions)
            it.enabled = true
            it.renderMode = RenderMode.COMPASS
        }
    }

    private fun updateLocation(location: Location) {
        updateLocation(listOf(location))
    }

    private fun updateLocation(locations: List<Location>) {
        val location = locations[0]
        locationComponent?.forceLocationUpdate(LocationUpdate(location, null, null))

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

    private class MyLocationEngineCallback constructor(
        activity: MapboxSignboardActivity
    ) : LocationEngineCallback<LocationEngineResult> {

        private val activityRef: WeakReference<MapboxSignboardActivity> = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            ifNonNull(result.lastLocation, activityRef.get()) { loc, act ->
                val point = Point.fromLngLat(loc.longitude, loc.latitude)
                val cameraOptions = CameraOptions.Builder().center(point).zoom(13.0).build()
                act.mapboxMap.jumpTo(cameraOptions)
                act.locationComponent?.forceLocationUpdate(LocationUpdate(loc))
            }
        }

        override fun onFailure(exception: Exception) {}
    }

    /**
     * Returns the Mapbox access token set in the app resources.
     *
     * @param context The [Context] of the [android.app.Activity] or [android.app.Fragment].
     * @return The Mapbox access token or null if not found.
     */
    private fun getMapboxRouteAccessToken(context: Context): String {
        val tokenResId = context.resources
            .getIdentifier("mapbox_route_token", "string", context.packageName)
        return if (tokenResId != 0) {
            context.getString(tokenResId)
        } else {
            throw RuntimeException("mapbox_route_token needed (see code comments for details)")
        }
    }
}
