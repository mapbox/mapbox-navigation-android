package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
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
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.routealternatives.RouteAlternativesObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.qa_test_app.databinding.AppLifecycleActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.utils.Utils.getMapboxAccessToken
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AlternativeRouteActiv"

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AppLifecycleActivity : AppCompatActivity(), OnMapLongClickListener {

    private val binding: AppLifecycleActivityLayoutBinding by lazy {
        AppLifecycleActivityLayoutBinding.inflate(layoutInflater)
    }

    private val mapCamera: CameraAnimationsPlugin by lazy { binding.mapView.camera }

    private val replayInteractor = ReplayInteractor()
    private val locationInteractor = LocationInteractor()
    private val continuousRoutesInteractor = ContinuousRoutesInteractor()
    private lateinit var routesInteractor: RoutesInteractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupNavigation()
        setupMap()
    }

    override fun onResume() {
        super.onResume()
        routesInteractor = RoutesInteractor(binding.mapView)
        MapboxNavigationApp.registerObserver(routesInteractor)
    }

    override fun onPause() {
        super.onPause()
        MapboxNavigationApp.unregisterObserver(routesInteractor)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detach and unregister the listeners.
        MapboxNavigationApp.detach(this)
            .unregisterObserver(replayInteractor)
            .unregisterObserver(locationInteractor)
            .unregisterObserver(continuousRoutesInteractor)
    }

    private fun setupNavigation() {
        // 1. MapboxNavigationApp.setup with your NavigationOptions
        // 2. MapboxNavigationApp.attach with the LifecycleOwner
        // 3. Register MapboxNavigationObservers to observe data streams
        val navigationOptions = NavigationOptions.Builder(this)
            .accessToken(getMapboxAccessToken(this))
            .build()
        MapboxNavigationApp.setup(navigationOptions)
            .attach(this)
            .registerObserver(locationInteractor)
            .registerObserver(continuousRoutesInteractor)
            .registerObserver(replayInteractor)

        locationInteractor.locationLiveData.observe(this) {
            updateCamera(it)
        }
    }

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        mapCamera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .zoom(15.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    private fun setupMap() {
        binding.mapView.getMapboxMap().loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE
        ) {
            binding.mapView.gestures.addOnMapLongClickListener(this)
        }

        binding.mapView.location.apply {
            setLocationProvider(locationInteractor.navigationLocationProvider)
            enabled = true
        }

        binding.startNavigation.setOnClickListener {
            binding.startNavigation.visibility = View.GONE
            replayInteractor.startSimulation()
        }

        binding.mapView.gestures.addOnMapClickListener(mapClickListener)
    }

    override fun onMapLongClick(point: Point): Boolean {
        vibrate()
        val currentLocation = locationInteractor.navigationLocationProvider.lastLocation
        if (currentLocation != null) {
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            routesInteractor.findRoute(originPoint, point)
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100L)
        }
    }

    private val mapClickListener = OnMapClickListener {
        routesInteractor.selectRoute(it)
        false
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class ReplayInteractor : MapboxNavigationObserver {
    private val replayRouteMapper = ReplayRouteMapper()
    private lateinit var replayProgressObserver: ReplayProgressObserver

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        replayProgressObserver = ReplayProgressObserver(mapboxNavigation.mapboxReplayer)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.mapboxReplayer.pushRealLocation(
            mapboxNavigation.navigationOptions.applicationContext,
            0.0
        )
        mapboxNavigation.mapboxReplayer.playbackSpeed(1.5)
        mapboxNavigation.mapboxReplayer.play()
    }

    fun startSimulation() = MapboxNavigationApp.current()?.apply {
        val route = MapboxNavigationApp.current()?.getRoutes()?.get(0)
        checkNotNull(route) { "Current route should not be null" }
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        mapboxReplayer.play()
        startReplayTripSession()
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.mapboxReplayer.finish()
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class LocationInteractor : MapboxNavigationObserver {
    val navigationLocationProvider = NavigationLocationProvider()

    val locationLiveData = MutableLiveData<Location>()

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            Log.d(TAG, "raw location $rawLocation")
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints
            )
            locationLiveData.value = locationMatcherResult.enhancedLocation
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        val locationEngine = mapboxNavigation.navigationOptions.locationEngine
        locationEngine.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                result.lastLocation?.let {
                    navigationLocationProvider.changePosition(it, emptyList())
                    locationLiveData.value = it
                }
            }
            override fun onFailure(exception: Exception) {}
        })

        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class RoutesInteractor(val mapView: MapView) : MapboxNavigationObserver {
    private val routeClickPadding = Utils.dpToPx(30f)

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder().build()
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(mapView.context)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label-navigation")
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routesObserver = RoutesObserver { result ->
        val routelines = result.routes.map { RouteLine(it, null) }
        CoroutineScope(Dispatchers.Main).launch {
            routeLineApi.setRoutes(routelines).apply {
                routeLineView.renderRouteDrawData(
                    mapView.getMapboxMap().getStyle()!!,
                    this
                )
            }
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRoutesObserver(routesObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        routeLineApi.cancel()
        routeLineView.cancel()
    }

    fun selectRoute(point: Point) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = routeLineApi.findClosestRoute(
                point,
                mapView.getMapboxMap(),
                routeClickPadding
            )

            val routeFound = result.value?.route
            if (routeFound != null && routeFound != routeLineApi.getPrimaryRoute()) {
                val reOrderedRoutes = routeLineApi.getRoutes()
                    .filter { it != routeFound }
                    .toMutableList()
                    .also {
                        it.add(0, routeFound)
                    }
                MapboxNavigationApp.current()?.setRoutes(reOrderedRoutes)
            }
        }
    }

    fun findRoute(origin: Point?, destination: Point) {
        val mapboxNavigation: MapboxNavigation = MapboxNavigationApp.current() ?: return
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(mapView.context)
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .alternatives(true)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setRoutes(routes.reversed())
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
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class ContinuousRoutesInteractor : MapboxNavigationObserver {

    private val routeAlternativesObserver =
        RouteAlternativesObserver { routeProgress, alternatives, _ ->
            val updatedRoutes = mutableListOf<DirectionsRoute>()
            updatedRoutes.add(routeProgress.route)
            updatedRoutes.addAll(alternatives)

            MapboxNavigationApp.current()?.apply {
                setRoutes(updatedRoutes)
                requestAlternativeRoutes()
            }
        }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRouteAlternativesObserver(routeAlternativesObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteAlternativesObserver(routeAlternativesObserver)
    }
}
