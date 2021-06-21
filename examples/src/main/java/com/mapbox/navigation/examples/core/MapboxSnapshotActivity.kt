package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
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
import com.mapbox.navigation.examples.core.databinding.LayoutActivitySnapshotBinding
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
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
import com.mapbox.navigation.ui.maps.snapshotter.api.MapboxSnapshotterApi
import com.mapbox.navigation.ui.maps.snapshotter.model.MapboxSnapshotterOptions
import com.mapbox.navigation.ui.maps.snapshotter.model.SnapshotError
import com.mapbox.navigation.ui.maps.snapshotter.model.SnapshotValue
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This activity demonstrates the usage of the [MapboxSnapshotterApi]. There is boiler plate
 * code for establishing basic navigation and a route simulator is used. The example assumes
 * that LOCATION permission has already been granted.
 *
 * The code specifically related to the snapshot component is commented in order to call
 * attention to its usage. The example uses a predefined location pair to demonstrate snapshot.
 * Long press anywhere on the map to use predefined coordinates and trigger navigation.
 *
 * Note: A snapshot is generated at every intersection that contains a signboard.
 * Hence, a special access token is required to get access to signboards in directions response.
 */
class MapboxSnapshotActivity : AppCompatActivity(), OnMapLongClickListener {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapCamera: CameraAnimationsPlugin
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var binding: LayoutActivitySnapshotBinding
    private lateinit var locationComponent: LocationComponentPlugin

    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     *
     */
    private val snapshotOptions: MapboxSnapshotterOptions by lazy {
        val density = resources.displayMetrics.density
        MapboxSnapshotterOptions.Builder(applicationContext)
            .edgeInsets(EdgeInsets(250.0 * density, 0.0 * density, 0.0 * density, 0.0 * density))
            .styleUri("mapbox://styles/mapbox-map-design/ckkfnaak605mv17pgmmxgmlvd")
            .build()
    }

    /**
     *
     */
    private val snapshotApi: MapboxSnapshotterApi by lazy {
        MapboxSnapshotterApi(this, mapboxMap, snapshotOptions, binding.mapView)
    }

    /**
     * The result of invoking [MapboxSnapshotterApi.generateSnapshot] is returned as a callback
     * containing either a success in the form of [SnapshotValue] or failure in the form of
     * [SnapshotError].
     */
    private val snapshotConsumer =
        MapboxNavigationConsumer<Expected<SnapshotError, SnapshotValue>> { value ->
            // The data obtained must be rendered by [MapboxSnapshotView]
            binding.snapshotView.render(value)
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
            // The snapshot component is driven by route progress updates.
            // Passing the route progress to the MapboxSnapshotterApi generates the data
            // for updating the view.
            snapshotApi.generateSnapshot(routeProgress, snapshotConsumer)
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
            MAPBOX_STREETS
        ) {
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
                .accessToken(getMapboxRouteAccessToken(this))
                .coordinatesList(listOf(origin, destination))
                .build(),
            object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    mapboxNavigation.setRoutes(routes)
                }

                override fun onRoutesRequestFailure(
                    throwable: Throwable,
                    routeOptions: RouteOptions
                ) {
                    // no impl
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    // no impl
                }
            }
        )
    }

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
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

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    /**
     * Uses a specific access token required for the route request to send snapshots in the response.
     * If the specific access token is not present, it defaults to access token required to render
     * Maps.
     *
     * @param context The [Context] of the [android.app.Activity] or [android.app.Fragment].
     * @return The Mapbox access token or null if not found.
     */
    private fun getMapboxRouteAccessToken(context: Context): String {
        val tokenResId = context.resources
            .getIdentifier("mapbox_access_token_snapshot", "string", context.packageName)
        return if (tokenResId != 0) {
            context.getString(tokenResId)
        } else {
            getMapboxAccessTokenFromResources()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Look at LayoutActivitySnapshotBinding to see the view component in the
        // activity's layout.
        binding = LayoutActivitySnapshotBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        mapCamera = binding.mapView.camera
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
            val or = Point.fromLngLat(-3.5870, 40.5719)
            val de = Point.fromLngLat(-3.607835, 40.551486)
            findRoute(or, de)
        }
        return false
    }
}
