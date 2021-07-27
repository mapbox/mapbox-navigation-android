package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityManeuverBinding
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.api.RoadShieldCallback
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.LoggerProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This activity demonstrates the usage of the [MapboxManeuverApi]. There is boiler plate
 * code for establishing basic navigation and a route simulator is used. The example assumes
 * that LOCATION permission has already been granted.
 *
 * The code specifically related to the maneuver component is commented in order to call
 * attention to its usage. Long press anywhere on the map to set a destination and trigger
 * navigation.
 */
class MapboxManeuverActivity : AppCompatActivity(), OnMapLongClickListener {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var binding: LayoutActivityManeuverBinding
    private lateinit var locationComponent: LocationComponentPlugin
    private var isNavigating = false

    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * The data in the view is formatted by default mapbox distance formatting implementation.
     */
    private val distanceFormatter: DistanceFormatterOptions by lazy {
        DistanceFormatterOptions.Builder(this).build()
    }

    /**
     * The [MapboxManeuverApi] consumes route progress and banner instructions data
     * and produces trip related data that is consumed by the [MapboxManeuverView] in the layout.
     */
    private val maneuverApi: MapboxManeuverApi by lazy {
        MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatter))
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder().build()
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routeArrowApi: MapboxRouteArrowApi by lazy {
        MapboxRouteArrowApi()
    }

    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(RouteArrowOptions.Builder(this).build())
    }

    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private val roadShieldCallback = RoadShieldCallback { _, shields, errors ->
        binding.maneuverView.renderManeuverShields(shields)
        errors.forEach { (id, error) ->
            LoggerProvider.logger.e(
                Tag("MbxManeuverActivity"),
                Message("id: $id -- error: ${error.url} - ${error.message}")
            )
        }
    }

    private val mapMatcherResultObserver = MapMatcherResultObserver { mapMatcherResult ->
        navigationLocationProvider.changePosition(
            mapMatcherResult.enhancedLocation,
            mapMatcherResult.keyPoints
        )
        if (isNavigating) {
            updateCamera(mapMatcherResult.enhancedLocation)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        renderManeuvers(maneuvers)

        routeArrowApi.addUpcomingManeuverArrow(routeProgress).apply {
            routeArrowView.renderManeuverUpdate(mapboxMap.getStyle()!!, this)
        }
    }

    private val routesObserver = RoutesObserver { routes ->
        if (routes.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                routeLineApi.setRoutes(
                    listOf(RouteLine(routes[0], null))
                ).apply {
                    routeLineView.renderRouteDrawData(mapboxMap.getStyle()!!, this)
                }
            }
            isNavigating = true
            startSimulation(routes[0])

            val maneuvers = maneuverApi.getManeuvers(routes.first())
            renderManeuvers(maneuvers)
        }
    }

    private fun renderManeuvers(maneuvers: Expected<ManeuverError, List<Maneuver>>) {
        maneuvers.onError { error ->
            LoggerProvider.logger.e(Tag("MbxManeuverActivity"), Message(error.errorMessage!!))
        }
        maneuvers.onValue {
            if (binding.maneuverView.visibility == INVISIBLE) {
                binding.maneuverView.visibility = VISIBLE
            }
            binding.maneuverView.renderManeuvers(maneuvers)
            maneuverApi.getRoadShields(it, roadShieldCallback)
        }
    }

    private fun init() {
        initNavigation()
        initStyle()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        val navigationOptions = NavigationOptions.Builder(this)
            .accessToken(getMapboxAccessTokenFromResources())
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()
        mapboxNavigation = MapboxNavigation(navigationOptions)
        mapboxNavigation.startTripSession()
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
        mapboxNavigation.registerLocationObserver(object : LocationObserver {
            override fun onRawLocationChanged(rawLocation: Location) {
                updateCamera(rawLocation)
                navigationLocationProvider.changePosition(
                    rawLocation,
                )
                mapboxNavigation.unregisterLocationObserver(this)
            }

            override fun onEnhancedLocationChanged(
                enhancedLocation: Location,
                keyPoints: List<Location>
            ) {
                //
            }
        })
        mapboxNavigation.registerMapMatcherResultObserver(mapMatcherResultObserver)
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            MAPBOX_STREETS
        ) {
            binding.mapView.gestures.addOnMapLongClickListener(this)
        }
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
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

    private fun findRoute(origin: Point, waypoints: List<Point>?, destination: Point) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinates(
                origin = origin,
                waypoints = waypoints,
                destination = destination
            )
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setRoutes(routes)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityManeuverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        init()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
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

    override fun onMapLongClick(point: Point): Boolean {
        ifNonNull(navigationLocationProvider.lastLocation) { currentLocation ->
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            findRoute(originPoint, null, point)
        }
        return false
    }
}
