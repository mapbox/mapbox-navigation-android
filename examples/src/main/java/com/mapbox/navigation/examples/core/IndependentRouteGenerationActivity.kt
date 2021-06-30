package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityIndependentRouteGenerationBinding
import com.mapbox.navigation.examples.util.RouteLineUtil
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter

class IndependentRouteGenerationActivity : AppCompatActivity() {

    private lateinit var binding: LayoutActivityIndependentRouteGenerationBinding
    private val routeLine = RouteLineUtil(this)
    private lateinit var mapboxMap: MapboxMap
    private val navigationLocationProvider = NavigationLocationProvider()
    private lateinit var locationComponent: LocationComponentPlugin
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var circleManager: CircleAnnotationManager
    private lateinit var lineManager: PolylineAnnotationManager
    private val mapboxReplayer = MapboxReplayer()

    private val tripProgressApi: MapboxTripProgressApi by lazy {
        MapboxTripProgressApi(tripProgressFormatter)
    }

    private val tripProgressFormatter: TripProgressUpdateFormatter by lazy {
        val distanceFormatterOptions =
            DistanceFormatterOptions.Builder(this).build()
        TripProgressUpdateFormatter.Builder(this)
            .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
            .timeRemainingFormatter(TimeRemainingFormatter(this))
            .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(this))
            .build()
    }

    private var routeRequestId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityIndependentRouteGenerationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapboxMap = binding.mapView.getMapboxMap()
        circleManager = binding.mapView.annotations
            .createCircleAnnotationManager(binding.mapView, null)
        lineManager = binding.mapView.annotations
            .createPolylineAnnotationManager(binding.mapView, null)
        locationComponent = binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        initStyle()
        initNavigation()
        routeLine.initialize(binding.mapView, mapboxNavigation)
    }

    private fun initStyle() {
        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            binding.mapView.gestures.addOnMapLongClickListener(
                mapLongClickListener
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxAccessToken(this))
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
        mapboxNavigation.startTripSession()
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
    }

    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private val locationObserver = object : LocationObserver {
        private var initialUpdateDone = false

        override fun onRawLocationChanged(rawLocation: Location) {}
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (!initialUpdateDone) {
                updateCamera(
                    Point.fromLngLat(enhancedLocation.longitude, enhancedLocation.latitude)
                )
                initialUpdateDone = true
            }

            navigationLocationProvider.changePosition(
                enhancedLocation,
                keyPoints,
            )
        }
    }

    private val routesObserver = RoutesObserver { routes ->
        if (routes.isNotEmpty()) {
            startSimulation(routes[0])
            binding.tripProgressView.visibility = View.VISIBLE
        } else {
            mapboxReplayer.stop()
            binding.tripProgressView.visibility = View.GONE
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        tripProgressApi.getTripProgress(routeProgress).let { update ->
            binding.tripProgressView.render(update)
        }
    }

    private val mapLongClickListener = OnMapLongClickListener { point ->
        Utils.vibrate(this@IndependentRouteGenerationActivity)

        val currentLocation = navigationLocationProvider.lastLocation
        if (currentLocation != null) {
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            findRoute(originPoint, point)
        }
        true
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    private fun updateCamera(point: Point) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        binding.mapView.camera.flyTo(
            CameraOptions.Builder()
                .center(point)
                .bearing(0.0)
                .pitch(0.0)
                .zoom(14.0)
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    private fun findRoute(origin: Point, destination: Point) {
        val currentRouteRequestId = routeRequestId
        if (currentRouteRequestId != null) {
            mapboxNavigation.cancelRouteRequest(currentRouteRequestId)
        } else {
            clearRouteSelectionUi()
        }

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .accessToken(Utils.getMapboxAccessToken(this))
            .coordinatesList(listOf(origin, destination))
            .alternatives(false)
            .build()
        routeRequestId = mapboxNavigation.requestRoutes(
            routeOptions,
            object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    lineManager.create(
                        PolylineAnnotationOptions.fromFeature(
                            Feature.fromGeometry(
                                LineString.fromPolyline(routes[0].geometry()!!, 6)
                            )
                        )!!
                    )

                    binding.routeSelection.visibility = View.VISIBLE
                    binding.acceptRoute.setOnClickListener {
                        mapboxNavigation.setRoutes(routes)
                        clearRouteSelectionUi()
                    }
                    binding.rejectRoute.setOnClickListener {
                        updateCamera(origin)
                        clearRouteSelectionUi()
                    }

                    routeRequestId = null
                    updateCamera(destination)
                }

                override fun onRoutesRequestFailure(
                    throwable: Throwable,
                    routeOptions: RouteOptions
                ) {
                    Log.e(
                        "RouteGenerationActivity",
                        "route request failed:\n" + throwable.message
                    )
                    Toast.makeText(
                        this@IndependentRouteGenerationActivity,
                        "Route request failed.",
                        Toast.LENGTH_LONG
                    ).show()
                    routeRequestId = null
                    clearRouteSelectionUi()
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    Toast.makeText(
                        this@IndependentRouteGenerationActivity,
                        """Route request "$routeRequestId" canceled.""",
                        Toast.LENGTH_LONG
                    ).show()
                    routeRequestId = null
                    clearRouteSelectionUi()
                }
            }
        )

        circleManager.create(
            CircleAnnotationOptions.fromFeature(Feature.fromGeometry(destination))!!
        )
    }

    private fun clearRouteSelectionUi() {
        binding.routeSelection.visibility = View.GONE
        circleManager.deleteAll()
        lineManager.deleteAll()
    }

    override fun onStart() {
        super.onStart()
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}
