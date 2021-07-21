package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityJunctionBinding
import com.mapbox.navigation.examples.util.Utils.getMapboxAccessToken
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.guidance.junction.api.MapboxJunctionApi
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import com.mapbox.navigation.ui.maps.guidance.junction.view.MapboxJunctionView
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This activity demonstrates the usage of the [MapboxJunctionApi]. There is boiler plate
 * code for establishing basic navigation and a route simulator is used. The example assumes
 * that LOCATION permission has already been granted.
 *
 * The code specifically related to the junction component is commented in order to call
 * attention to its usage. The example uses a predefined location pair to demonstrate junction.
 * Long press anywhere on the map to use predefined coordinates and trigger navigation.
 *
 * Note: A special access token is required to get access to junctions in directions response.
 */
class MapboxJunctionActivity : AppCompatActivity(), OnMapLongClickListener {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var binding: LayoutActivityJunctionBinding
    private lateinit var locationComponent: LocationComponentPlugin

    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * The [MapboxJunctionApi] consumes banner instructions data and produces junctions related
     * data that is consumed by the [MapboxJunctionView] in the view layout.
     */
    private val junctionApi: MapboxJunctionApi by lazy {
        MapboxJunctionApi(getMapboxRouteAccessToken(this))
    }

    /**
     * The result of invoking [MapboxJunctionApi.generateJunction] is returned as a callback
     * containing either a success in the form of [JunctionValue] or failure in the form of
     * [JunctionError].
     */
    private val junctionCallback =
        MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>> { value ->
            // The data obtained must be rendered by [MapboxJunctionView]
            binding.junctionView.render(value)
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

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {}
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            navigationLocationProvider.changePosition(
                enhancedLocation,
                keyPoints,
            )
            updateCamera(enhancedLocation)
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    routeLineApi.setRoutes(
                        listOf(RouteLine(routes[0], null))
                    ).apply {
                        routeLineView.renderRouteDrawData(mapboxMap.getStyle()!!, this)
                    }
                }
                startSimulation(routes[0])
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            routeArrowApi.addUpcomingManeuverArrow(routeProgress).apply {
                routeArrowView.renderManeuverUpdate(mapboxMap.getStyle()!!, this)
            }
        }
    }

    private val bannerInstructionsObserver = object : BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            // The junction component is driven by banner instructions updates.
            // Passing the instructions to the MapboxJunctionApi generates the data
            // for updating the view.
            junctionApi.generateJunction(bannerInstructions, junctionCallback)
        }
    }

    private fun init() {
        initNavigation()
        initStyle()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
        mapboxNavigation.startTripSession()
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    binding.mapView.gestures.addOnMapLongClickListener(this@MapboxJunctionActivity)
                }
            }
        )
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

    private fun findRoute(origin: Point, destination: Point) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(this)
                .accessToken(getMapboxRouteAccessToken(this))
                .coordinatesList(listOf(origin, destination))
                .alternatives(true)
                .build(),
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

    private fun getMapboxAccessTokenFromResources(): String {
        return getMapboxAccessToken(this)
    }

    /**
     * Uses a specific access token required for the route request to send junctions in the response.
     * If the specific access token is not present, it defaults to access token required to render
     * Maps.
     *
     * @param context The [Context] of the [android.app.Activity] or [android.app.Fragment].
     * @return The Mapbox access token or null if not found.
     */
    private fun getMapboxRouteAccessToken(context: Context): String {
        val tokenResId = context.resources
            .getIdentifier("mapbox_access_token_junction", "string", context.packageName)
        return if (tokenResId != 0) {
            context.getString(tokenResId)
        } else {
            getMapboxAccessTokenFromResources()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Look at LayoutActivityJunctionBinding to see the view component in the
        // activity's layout.
        binding = LayoutActivityJunctionBinding.inflate(layoutInflater)
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
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionsObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
        }
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

    override fun onMapLongClick(point: Point): Boolean {
        ifNonNull(navigationLocationProvider.lastLocation) {
            val or = Point.fromLngLat(139.7745686, 35.677573)
            val de = Point.fromLngLat(139.784915, 35.680960)
            findRoute(or, de)
        }
        return false
    }
}
