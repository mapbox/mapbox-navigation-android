@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LoggingLevel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.logD
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.fasterroute.FasterRouteOptions
import com.mapbox.navigation.core.fasterroute.NewFasterRoute
import com.mapbox.navigation.core.fasterroute.NewFasterRouteObserver
import com.mapbox.navigation.core.fasterroute.createFasterRoutes
import com.mapbox.navigation.core.internal.fasterroute.RecordRouteObserverResults
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesRequestCallback
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.qa_test_app.databinding.AlternativeRouteActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.utils.Utils.getMapboxAccessToken
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AlternativeRouteActivity : AppCompatActivity(), OnMapLongClickListener {

    private companion object {
        private const val TAG = "AlternativeRouteActivity"
    }

    private val routeClickPadding = Utils.dpToPx(30f)
    private val navigationLocationProvider = NavigationLocationProvider()
    private val mapboxReplayer = MapboxReplayer()
    private val binding: AlternativeRouteActivityLayoutBinding by lazy {
        AlternativeRouteActivityLayoutBinding.inflate(layoutInflater)
    }

    private val mapCamera: CameraAnimationsPlugin by lazy {
        binding.mapView.camera
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder().build()
    }

    private val routeLineOptions: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label-navigation")
            .withVanishingRouteLineEnabled(true)
            .displayRestrictedRoadSections(true)
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(routeLineOptions)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(routeLineOptions)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private val mapboxNavigation: MapboxNavigation by requireMapboxNavigation(
        onResumedObserver = object : MapboxNavigationObserver {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.registerLocationObserver(locationObserver)
                mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.registerRoutesObserver(recordRoutesObserver)
                mapboxNavigation.registerRoutesObserver(routesObserver)
                mapboxNavigation.registerRouteAlternativesObserver(alternativesObserver)
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
                mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.unregisterLocationObserver(locationObserver)
                mapboxNavigation.unregisterRoutesObserver(recordRoutesObserver)
                mapboxNavigation.unregisterRoutesObserver(routesObserver)
                mapboxNavigation.unregisterRouteAlternativesObserver(alternativesObserver)
            }
        },
        onInitialize = this::initNavigation
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
        setContentView(binding.root)
        initStyle()
        initListeners()
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private val fasterRoutes by lazy {
        mapboxNavigation.createFasterRoutes(
            FasterRouteOptions.Builder().build()
        )
    }

    override fun onResume() {
        super.onResume()
        @OptIn(ExperimentalMapboxNavigationAPI::class)
        fasterRoutes.registerNewFasterRouteObserver(fasterRouteObserver)
    }

    override fun onPause() {
        super.onPause()
        @OptIn(ExperimentalMapboxNavigationAPI::class)
        fasterRoutes.unregisterNewFasterRouteObserver(fasterRouteObserver)
    }

    private fun initNavigation() {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessToken(this))
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .routeAlternativesOptions(
                    RouteAlternativesOptions.Builder()
                        .intervalMillis(TimeUnit.SECONDS.toMillis(30))
                        .build()
                )
                .build()
        )

        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            enabled = true
        }

        mapboxReplayer.pushEvents(
            ReplayEventLocation(
                11.574758,48.150672,
                provider = "me",
                0.0,
                null,
                null,
                null,
                null,
            ).let { listOf<ReplayEventBase>(ReplayEventUpdateLocation(0.0, it)) })
        mapboxReplayer.playbackSpeed(3.0)
        mapboxReplayer.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxReplayer.finish()
    }

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            Log.d(TAG, "raw location $rawLocation")
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(locationMatcherResult.enhancedLocation)
        }
    }

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(0L)
        mapCamera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .zoom(13.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        binding.mapView.getMapboxMap().loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE
        ) {
            mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                object : LocationEngineCallback<LocationEngineResult> {
                    override fun onSuccess(result: LocationEngineResult) {
                        result.lastLocation?.let {
                            navigationLocationProvider.changePosition(it)
                            updateCamera(it)
                        }
                    }

                    override fun onFailure(exception: Exception) {}
                }
            )
            binding.mapView.gestures.addOnMapLongClickListener(this)
        }
    }

    override fun onMapLongClick(point: Point): Boolean {
        vibrate()

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

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        routeLineApi.updateTraveledRouteLine(point).let {
            routeLineView.renderRouteLineUpdate(
                binding.mapView.getMapboxMap().getStyle()!!,
                it
            )
        }
    }

    /**
     * Example of how to handle route alternatives during navigation.
     */
    private val alternativesObserver = object : NavigationRouteAlternativesObserver {
        override fun onRouteAlternatives(
            routeProgress: RouteProgress,
            alternatives: List<NavigationRoute>,
            routerOrigin: RouterOrigin
        ) {
            // Set the alternatives suggested
            val updatedRoutes = mutableListOf<NavigationRoute>()
            updatedRoutes.add(routeProgress.navigationRoute)
            updatedRoutes.addAll(alternatives)
            mapboxNavigation.setNavigationRoutes(updatedRoutes)
        }

        override fun onRouteAlternativesError(error: RouteAlternativesError) {
            Toast.makeText(
                this@AlternativeRouteActivity,
                "error (check logs)",
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "$error")
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            binding.mapView.getMapboxMap().getStyle()?.apply {
                routeLineView.renderRouteLineUpdate(this, result)
            }
        }
    }

    private fun findRoute(origin: Point?, destination: Point?) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .alternatives(true)
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

    @SuppressLint("MissingPermission")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100L)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        binding.startNavigation.setOnClickListener {
            mapboxNavigation.startTripSession()
            binding.startNavigation.visibility = View.GONE
            binding.requestAlternatives.visibility = View.VISIBLE
        }
        binding.requestAlternatives.setOnClickListener {
            binding.requestAlternatives.isEnabled = false
            mapboxNavigation.requestAlternativeRoutes(object :
                NavigationRouteAlternativesRequestCallback {
                override fun onRouteAlternativeRequestFinished(
                    routeProgress: RouteProgress,
                    alternatives: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    binding.requestAlternatives.isEnabled = true
                }

                override fun onRouteAlternativesRequestError(error: RouteAlternativesError) {
                    binding.requestAlternatives.isEnabled = true
                }
            })
        }

        binding.mapView.gestures.addOnMapClickListener(mapClickListener)
    }

    private val recordRoutesObserver = RecordRouteObserverResults { mapboxNavigation }
    private val routesObserver = RoutesObserver { result ->
        CoroutineScope(Dispatchers.Main).launch {
            routeLineApi.setNavigationRoutes(
                newRoutes = result.navigationRoutes,
                alternativeRoutesMetadata = mapboxNavigation.getAlternativeMetadataFor(
                    result.navigationRoutes
                )
            ).apply {
                routeLineView.renderRouteDrawData(
                    binding.mapView.getMapboxMap().getStyle()!!,
                    this
                )
            }
        }
    }

    private val fasterRouteObserver = NewFasterRouteObserver { newFasterRoute: NewFasterRoute ->
        val message = "faster route found: ${mapboxNavigation.getAlternativeMetadataFor(newFasterRoute.fasterRoute)?.alternativeId} is faster then primary by ${newFasterRoute.fasterThanPrimary}"
        logD("faster route", message)
    }

    private val mapClickListener = OnMapClickListener {
        CoroutineScope(Dispatchers.Main).launch {
            val result = routeLineApi.findClosestRoute(
                it,
                binding.mapView.getMapboxMap(),
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
                mapboxNavigation.setRoutes(reOrderedRoutes)
            }
        }
        false
    }
}
