package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.matching.v5.MapboxMapMatching
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionState.STARTED
import com.mapbox.navigation.core.trip.session.TripSessionState.STOPPED
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.Utils.PRIMARY_ROUTE_BUNDLE_KEY
import com.mapbox.navigation.examples.utils.Utils.getRouteFromBundle
import com.mapbox.navigation.examples.utils.extensions.ifNonNull
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_basic_navigation_layout.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * This activity shows how to fetch DirectionsRoute with MapMatching
 *
 */
@SuppressLint("MissingPermission")
class MapMatchingActivity : AppCompatActivity(), OnMapReadyCallback {

    private companion object {
        private const val INTERVAL_MILLISECONDS = 1000L
        private const val FASTEST_INTERVAL_MILLISECONDS = 500L
    }

    private var mapboxNavigation: MapboxNavigation? = null
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var mapInstanceState: Bundle? = null
    private var directionsRoute: DirectionsRoute? = null
    private lateinit var localLocationEngine: LocationEngine
    private val locationEngineCallback = MyLocationEngineCallback(this)
    private var destination: Point? = null
    private var locationComponent: LocationComponent? = null
    private val rerouteController = TestRerouteController()
    private lateinit var directionsBuilder: MapboxDirections.Builder

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_matching)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        localLocationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, Utils.getMapboxAccessToken(this))
            .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions).apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
            setRerouteController(rerouteController)
        }

        initDirectionsBuilder()
        initListeners()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(this@MapMatchingActivity, it)
                        .useDefaultLocationEngine(false)
                        .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }
            mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
            navigationMapboxMap = NavigationMapboxMap.Builder(mapView, mapboxMap, this)
                .vanishRouteLineEnabled(true)
                .build().apply {
                    setCamera(DynamicCamera(mapboxMap))
                    addProgressChangeListener(mapboxNavigation!!)
                }
            mapInstanceState?.let { state ->
                navigationMapboxMap?.restoreStateFrom(state)
            }
        }

        mapboxMap.addOnMapLongClickListener { destination ->
            this.destination = destination.toPoint()
            locationComponent?.lastKnownLocation?.let { origin ->
                fetchRouteWithDirectionsApi(origin.toPoint(), destination.toPoint())
            }
            true
        }
    }

    private fun initDirectionsBuilder() {
        directionsBuilder = MapboxDirections.builder()
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .continueStraight(true)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .steps(true)
            .accessToken(Utils.getMapboxAccessToken(applicationContext))
            .enableRefresh(false)
    }

    // MapMatching needs a list of coordinates to build a DirectionsRoute. That list should be provided by a customer.
    // For testing purposes we use DirectionsApi to fetch a DirectionsRoute.
    // We take coordinates of each maneuver, pass the list to MapMatching and get a final DirectionsRoute.
    private fun fetchRouteWithDirectionsApi(origin: Point, destination: Point) {
        directionsBuilder
            .origin(origin)
            .destination(destination)
            .build()
            .enqueueCall(
                object : Callback<DirectionsResponse> {
                    override fun onResponse(
                        call: Call<DirectionsResponse>,
                        response: Response<DirectionsResponse>
                    ) {
                        Timber.d("DirectionsAPI request succeeded")
                        response.body()?.routes()?.let { routes ->
                            if (routes.isNotEmpty()) {
                                val coordinates = mutableListOf<Point>()
                                routes[0].legs()?.forEach { leg ->
                                    leg.steps()?.forEach { step ->
                                        step.maneuver().location().run {
                                            coordinates.add(
                                                Point.fromLngLat(longitude(), latitude())
                                            )
                                        }
                                    }
                                }

                                locationComponent?.lastKnownLocation?.let {
                                    coordinates.add(0, origin)
                                    coordinates.add(destination)

                                    fetchRoute(coordinates)
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                        Timber.e("DirectionsAPI route request failure %s", t.toString())
                    }
                }
            )
    }

    private fun fetchRoute(coordinates: List<Point>) {
        Timber.d("MapMatching request with ${coordinates.size} coordinates.")

        val mapMatching = MapboxMapMatching.builder()
            .accessToken(Utils.getMapboxAccessToken(applicationContext))
            .coordinates(coordinates)
            .waypointIndices(0, coordinates.size - 1)
            .steps(true)
            .bannerInstructions(true)
            .voiceInstructions(true)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .build()

        mapMatching.enqueueCall(
            object : Callback<MapMatchingResponse> {
                override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
                    Timber.e("MapMatching request failure %s", t.toString())
                }

                override fun onResponse(
                    call: Call<MapMatchingResponse>,
                    response: Response<MapMatchingResponse>
                ) {
                    Timber.d("MapMatching request succeeded")

                    val route = response.body()?.matchings()?.get(0)?.toDirectionRoute()
                    if (route != null) {
                        if (directionsRoute == null) {
                            startNavigation.visibility = View.VISIBLE
                        }
                        directionsRoute = route
                        mapboxNavigation?.setRoutes(listOf(route))
                        navigationMapboxMap?.drawRoute(route)
                    } else {
                        startNavigation.visibility = View.GONE
                    }
                }
            }
        )
    }

    fun initListeners() {
        startNavigation.setOnClickListener {
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation?.let {
                navigationMapboxMap?.addProgressChangeListener(it)
                it.getRoutes().firstOrNull()?.let { route ->
                    navigationMapboxMap?.startCamera(route)
                }
                it.startTripSession()
            }

            startNavigation.visibility = View.GONE
            stopLocationUpdates()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.run {
            unregisterTripSessionStateObserver(tripSessionStateObserver)
            stopTripSession()
            onDestroy()
        }
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        navigationMapboxMap?.saveStateWith(outState)
        mapView.onSaveInstanceState(outState)

        // This is not the most efficient way to preserve the route on a device rotation.
        // This is here to demonstrate that this event needs to be handled in order to
        // redraw the route line after a rotation.
        directionsRoute?.let {
            outState.putString(PRIMARY_ROUTE_BUNDLE_KEY, it.toJson())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        mapInstanceState = savedInstanceState
        directionsRoute = getRouteFromBundle(savedInstanceState)
    }

    fun startLocationUpdates() {
        val request = LocationEngineRequest.Builder(INTERVAL_MILLISECONDS)
            .setFastestInterval(FASTEST_INTERVAL_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()

        localLocationEngine.requestLocationUpdates(
            request,
            locationEngineCallback,
            Looper.getMainLooper()
        )

        if (directionsRoute == null) {
            localLocationEngine.getLastLocation(locationEngineCallback)
        }
    }

    private fun stopLocationUpdates() {
        localLocationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                STARTED -> stopLocationUpdates()
                STOPPED -> {
                    startLocationUpdates()
                    navigationMapboxMap?.hideRoute()
                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private fun updateCameraOnNavigationStateChange(navigationStarted: Boolean) {
        navigationMapboxMap?.apply {
            if (navigationStarted) {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                updateLocationLayerRenderMode(RenderMode.GPS)
            } else {
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                updateLocationLayerRenderMode(RenderMode.COMPASS)
            }
        }
    }

    private inner class MyLocationEngineCallback(activity: MapMatchingActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            activityRef.get()?.locationComponent?.forceLocationUpdate(result.lastLocation)
        }

        override fun onFailure(exception: java.lang.Exception) {
            Timber.i(exception)
        }
    }

    private inner class TestRerouteController : RerouteController {
        override val state = RerouteState.Idle

        override fun reroute(routesCallback: RerouteController.RoutesCallback) {
            Timber.d("TestRerouteController reroute")

            ifNonNull(
                locationComponent?.lastKnownLocation,
                destination
            ) { location, destination ->
                fetchRouteWithDirectionsApi(location.toPoint(), destination)
            }
        }

        override fun interrupt() {
            // do nothing
        }

        override fun registerRerouteStateObserver(
            rerouteStateObserver: RerouteController.RerouteStateObserver
        ): Boolean {
            throw Exception("Not yet implemented")
        }

        override fun unregisterRerouteStateObserver(
            rerouteStateObserver: RerouteController.RerouteStateObserver
        ): Boolean {
            throw Exception("Not yet implemented")
        }
    }
}
