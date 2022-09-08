package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
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
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityJunctionBinding
import com.mapbox.navigation.examples.util.Utils.getMapboxAccessToken
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.guidance.junction.api.MapboxJunctionApi
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import com.mapbox.navigation.ui.maps.guidance.junction.view.MapboxJunctionView
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
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
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            updateCamera(locationMatcherResult.enhancedLocation)
        }
    }

    private val routesObserver = RoutesObserver { result ->
        if (result.routes.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                routeLineApi.setRoutes(listOf(RouteLine(result.routes[0], null))).apply {
                    routeLineView.renderRouteDrawData(mapboxMap.getStyle()!!, this)
                }
            }
            startSimulation(result.routes[0])
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        routeArrowApi.addUpcomingManeuverArrow(routeProgress).apply {
            routeArrowView.renderManeuverUpdate(mapboxMap.getStyle()!!, this)
        }
    }

    private val bannerInstructionsObserver = BannerInstructionsObserver { bannerInstructions ->
        // The junction component is driven by banner instructions updates.
        // Passing the instructions to the MapboxJunctionApi generates the data
        // for updating the view.
        junctionApi.generateJunction(bannerInstructions, junctionCallback)
    }

    private fun init() {
        initNavigation()
        initStyle()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxRouteAccessToken(this))
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
        mapboxNavigation.startTripSession()
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(MAPBOX_STREETS) { style ->
            routeLineView.initializeLayers(style)
            binding.mapView.gestures.addOnMapLongClickListener(this)
        }
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
            Toast.makeText(this, "Missing mapbox_access_token_junction", Toast.LENGTH_LONG).show()
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
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        junctionApi.cancelAll()
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
    }

    /**
     * Coordinates containing `subType = JCT`
     * 139.7745686, 35.677573;139.784915, 35.680960
     * https://api.mapbox.com/guidance-views/v1/709948800/jct/CA075101?arrow_ids=CA07510E
     *
     * Coordinates containing `subType` = SAPA`
     * 137.76136788022933, 34.83891088143494;137.75220947550804, 34.840924660770725
     * https://api.mapbox.com/guidance-views/v1/709948800/sapa/SA117201?arrow_ids=SA11720A
     *
     * Coordinates containing `subType` = CITYREAL`
     * 139.68153626083233, 35.66812853462302;139.68850488593154, 35.66099697148769
     * https://api.mapbox.com/guidance-views/v1/709948800/cityreal/13c00282_o40d?arrow_ids=13c00282_o41a
     *
     * Coordinates containing `subType` = TOLLBRANCH`
     * 137.02725, 35.468588;137.156787, 35.372602
     * https://api.mapbox.com/guidance-views/v1/709948800/tollbranch/CR896101?arrow_ids=CR89610A
     *
     * Coordinates containing `subType` = AFTERTOLL`
     * 141.4223967090212, 43.07693368987961;141.42118630948409, 43.07604662044662
     * https://api.mapbox.com/guidance-views/v1/709948800/aftertoll/HW00101805?arrow_ids=HW00101805_1
     *
     * Coordinates containing `subType` = EXPRESSWAY_ENTRANCE`
     * 139.724088, 35.672885; 139.630359, 35.626416
     * https://api.mapbox.com/guidance-views/v1/709948800/entrance/13i00015_o10d?arrow_ids=13i00015_o11a
     *
     * Coordinates containing `subType` = EXPRESSWAY_EXIT`
     * 135.324023, 34.715952;135.296332, 34.711387
     * https://api.mapbox.com/guidance-views/v1/709948800/exit/28o00022_o20d?arrow_ids=28o00022_o21a
     */
    override fun onMapLongClick(point: Point): Boolean {
        ifNonNull(navigationLocationProvider.lastLocation) {
            val or = Point.fromLngLat(141.4223967090212, 43.07693368987961)
            val de = Point.fromLngLat(141.42118630948409, 43.07604662044662)
            findRoute(or, de)
        }
        return false
    }
}
