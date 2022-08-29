package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
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
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityExtenedReplayBinding
import com.mapbox.navigation.examples.util.RouteDrawingUtil
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setNavigationRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapboxReplayExtendedActivity : AppCompatActivity(), OnMapLongClickListener {

    private lateinit var binding: LayoutActivityExtenedReplayBinding
    private val mapboxReplayer = MapboxReplayer()
    private val replayLocationEngine: ReplayLocationEngine by lazy {
        ReplayLocationEngine(mapboxReplayer)
    }
    private val navigationLocationProvider = NavigationLocationProvider()


    private lateinit var simulationRouteLineView: MapboxRouteLineView
    private lateinit var simulationRouteLineAPI: MapboxRouteLineApi

    private lateinit var routeLineAPI: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView

    private lateinit var mapboxNavigation: MapboxNavigation

    private var onInitLocation = false
    private var setRoutesState: SetRoutesState = SetRoutesState.NOT_SET

    companion object {
        private const val SIMULATION_LAYER_ID = "SIMULATION_LAYER_ID"
        private const val SIMULATION_SOURCE_ID = "SIMULATION_SOURCE_ID"
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            if (!onInitLocation) {
                updateCamera(locationMatcherResult.enhancedLocation)
                onInitLocation = true
            }

        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        routeLineAPI.updateWithRouteProgress(routeProgress) { result ->
            binding.mapView.getMapboxMap().getStyle()?.apply {
                routeLineView.renderRouteLineUpdate(this, result)
            }
        }
    }

    private val routesObserver = RoutesObserver { result ->
        CoroutineScope(Dispatchers.Main).launch {
            routeLineAPI.setNavigationRoutes(
                listOf(result.navigationRoutes[0])
            ).apply {
                routeLineView.renderRouteDrawData(
                    binding.mapView.getMapboxMap().getStyle()!!, this
                )
            }
        }
    }

    private val offRouteObserver = OffRouteObserver {
        val msg = if (it){
            "You're OFF ROUTE!!!"
        } else {
            "You've backed to the route"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LayoutActivityExtenedReplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRouteLinesApi()
        initStyle()

        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxAccessToken(this))
                .locationEngine(replayLocationEngine)
                .build()
        )
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerOffRouteObserver(offRouteObserver)

        mapboxNavigation.startTripSession()
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            routeLineView.initializeLayers(style)
            simulationRouteLineView.initializeLayers(style)
            binding.mapView.gestures.addOnMapLongClickListener(this)

            geoJsonSource(SIMULATION_SOURCE_ID).bindTo(style)
            LineLayer(SIMULATION_LAYER_ID, SIMULATION_SOURCE_ID)
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineColor(Color.YELLOW)
                .lineWidth(4.0)
                .bindTo(style)
        }

        // initialize the location puck
        binding.mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@MapboxReplayExtendedActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
    }

    private fun initRouteLinesApi() {
        val mapboxSimulationRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(
                RouteLineResources.Builder()
                    .routeLineColorResources(
                        RouteLineColorResources.Builder()
                            .routeDefaultColor(Color.YELLOW)
                            .build()
                    ).build()
            )
            .withRouteLineBelowLayerId("road-label-navigation")
            .build()
        simulationRouteLineAPI = MapboxRouteLineApi(mapboxSimulationRouteLineOptions)
        simulationRouteLineView = MapboxRouteLineView(mapboxSimulationRouteLineOptions)

        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label")
            .withVanishingRouteLineEnabled(true)
            .build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

    }

    override fun onDestroy() {
        super.onDestroy()

        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterOffRouteObserver(offRouteObserver)
        mapboxNavigation.onDestroy()
    }

    override fun onMapLongClick(point: Point): Boolean {
        ifNonNull(navigationLocationProvider.lastLocation) { currentLocation ->
            findRoute(currentLocation.toPoint(), point)
        }
        return false
    }

    private fun handleSetRoutes(routes: List<NavigationRoute>) {
//        startSimulation(routes.first())
        setRoutesState = when (setRoutesState) {
            SetRoutesState.NOT_SET -> {
                mapboxNavigation.setNavigationRoutes(routes)
                SetRoutesState.ON_SET_TO_NAVIGATOR
            }
            SetRoutesState.ON_SET_TO_NAVIGATOR -> {
                startSimulation(routes.first())
                SetRoutesState.ON_SET_TO_SIMULATOR
            }
            SetRoutesState.ON_SET_TO_SIMULATOR -> return
        }
    }


    private fun startSimulation(route: NavigationRoute) {
        CoroutineScope(Dispatchers.Main).launch {
            ifNonNull(binding.mapView.getMapboxMap().getStyle()) { style ->
                style.getSourceAs<GeoJsonSource>(SIMULATION_SOURCE_ID)?.feature(
                    Feature.fromGeometry(
                        LineString.fromPolyline(route.directionsRoute.geometry()!!, 6)
                    )
                )
            }
        }

        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route.directionsRoute)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    private fun findRoute(origin: Point, destination: Point) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .coordinatesList(listOf(origin, destination))
                .layersList(listOf(mapboxNavigation.getZLevel(), null))
                .alternatives(false)
                .annotationsList(
                    listOf(
                        DirectionsCriteria.ANNOTATION_CONGESTION,
                        DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                    )
                )
                .build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    handleSetRoutes(routes)
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

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        binding.mapView.camera.easeTo(
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

    private enum class SetRoutesState {
        NOT_SET,
        ON_SET_TO_NAVIGATOR,
        ON_SET_TO_SIMULATOR,
    }
}
