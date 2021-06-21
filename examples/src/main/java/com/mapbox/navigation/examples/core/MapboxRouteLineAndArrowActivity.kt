package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.delegates.listeners.eventdata.MapLoadErrorType
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityRoutelineExampleBinding
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import java.lang.ref.WeakReference

/**
 * This class demonstrates the usage of the route line and route arrow API's. There is
 * boiler plate code for a basic navigation experience. The turn by turn navigation is simulated.
 * The route line and arrow specific code is indicated with inline comments.
 */
class MapboxRouteLineAndArrowActivity : AppCompatActivity(), OnMapLongClickListener {
    private val routeClickPadding = Utils.dpToPx(30f)
    private val ONE_HUNDRED_MILLISECONDS = 100
    private val mapboxReplayer = MapboxReplayer()
    private val replayRouteMapper = ReplayRouteMapper()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private val viewBinding: LayoutActivityRoutelineExampleBinding by lazy {
        LayoutActivityRoutelineExampleBinding.inflate(layoutInflater)
    }

    private val mapboxMap: MapboxMap by lazy {
        viewBinding.mapView.getMapboxMap()
    }

    private val navigationLocationProvider by lazy {
        NavigationLocationProvider()
    }

    private val locationComponent by lazy {
        viewBinding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
    }

    private val mapboxNavigation by lazy {
        MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
    }

    private val mapCamera by lazy {
        viewBinding.mapView.camera
    }

    // RouteLine: Route line related colors can be customized via the RouteLineColorResources.
    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder().build()
    }

    // RouteLine: Various route line related options can be customized here including applying
    // route line color customizations. If using the default colors the RouteLineColorResources
    // does not need to be set as seen here, the defaults will be used internally by the builder.
    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    // RouteLine: Additional route line options are available through the MapboxRouteLineOptions.
    // Notice here the withRouteLineBelowLayerId option. The map is made up of layers. In this
    // case the route line will be placed below the "road-label" layer which is a good default
    // for the most common Mapbox navigation related maps. You should consider if this should be
    // changed for your use case especially if you are using a custom map style.
    //
    // Also noteworthy is the 'withVanishingRouteLineEnabled' option. This feature will change
    // the color of the route line behind the puck during navigation. The color can be customized
    // using the RouteLineColorResources::routeLineTraveledColor and
    // RouteLineColorResources::routeLineTraveledCasingColor options. The color options support
    // an alpha value to render the line transparent which is the default.
    //
    // To use the vanishing route line feature it is also necessary to register an
    // OnIndicatorPositionChangedListener and a RouteProgressObserver. There may be reasons to use
    // a RouteProgressObserver even if you are not using the vanishing route line feature.
    // The OnIndicatorPositionChangedListener is only useful and required when enabling the
    // vanishing route line feature.
    //
    // Examples are below.
    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .withVanishingRouteLineEnabled(true)
            .build()
    }

    // RouteLine: This class is responsible for rendering route line related mutations generated
    // by the MapboxRouteLineApi class.
    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    // RouteLine: This class is responsible for generating route line related data which must be
    // rendered by the MapboxRouteLineView class in order to visualize the route line on the map.
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    // RouteArrow: This class is responsible for generating data related to maneuver arrows. The
    // data generated must be rendered by the MapboxRouteArrowView in order to apply mutations to
    // the map.
    private val routeArrowApi: MapboxRouteArrowApi by lazy {
        MapboxRouteArrowApi()
    }

    // RouteArrow: Customization of the maneuver arrow(s) can be done using the
    // RouteArrowOptions. Here the above layer ID is used to determine where in the map layer
    // stack the arrows appear. Above the layer of the route traffic line is being used here. Your
    // use case may necessitate adjusting this to a different layer position.
    private val routeArrowOptions by lazy {
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
            .build()
    }

    // RouteArrow: This class is responsible for rendering the arrow related mutations generated
    // by the MapboxRouteArrowApi class.
    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(routeArrowOptions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        init()
    }

    private fun init() {
        initNavigation()
        initStyle()
        initListeners()
        locationComponent.locationPuck = LocationPuck2D(
            null,
            ContextCompat.getDrawable(
                this@MapboxRouteLineAndArrowActivity,
                R.drawable.mapbox_navigation_puck_icon
            ),
            null,
            null
        )
    }

    // RouteLine: This is one way to keep the route(s) appearing on the map in sync with
    // MapboxNavigation. When this observer is called the route data is used to draw route(s)
    // on the map.
    private val routesObserver: RoutesObserver = RoutesObserver { routes ->
        // RouteLine: wrap the DirectionRoute objects and pass them
        // to the MapboxRouteLineApi to generate the data necessary to draw the route(s)
        // on the map.
        val routeLines = routes.map { RouteLine(it, null) }
        routeLineApi.setRoutes(
            routeLines
        ) { value ->
            // RouteLine: The MapboxRouteLineView expects a non-null reference to the map style.
            // the data generated by the call to the MapboxRouteLineApi above must be rendered
            // by the MapboxRouteLineView in order to visualize the changes on the map.
            mapboxMap.getStyle()?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }

    // RouteLine: This listener is necessary only when enabling the vanishing route line feature
    // which changes the color of the route line behind the puck during navigation. If this
    // option is set to `false` (the default) in MapboxRouteLineOptions then it is not necessary
    // to use this listener.
    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeLineView.renderVanishingRouteLineUpdateValue(this, result)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        // RouteLine: This line is only necessary if the vanishing route line feature
        // is enabled.
        routeLineApi.updateWithRouteProgress(routeProgress)

        // RouteArrow: The next maneuver arrows are driven by route progress events.
        // Generate the next maneuver arrow update data and pass it to the view class
        // to visualize the updates on the map.
        val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeArrowView.renderManeuverUpdate(this, arrowUpdate)
        }
    }

    // RouteLine: Below is a demonstration of selecting different routes. On a map click, a call
    // will be made to look for a route line on the map based on the map touch point. If a route is
    // found and it is not already the primary route, the selected route will designated the primary
    // route and MapboxNavigation will be updated.
    private val mapClickListener = OnMapClickListener { point ->
        mapboxMap.getStyle()?.apply {
            // Since this listener is reacting to all map touches, if the primary and alternative
            // routes aren't visible it's assumed the touch isn't related to selecting an
            // alternative route.
            val primaryLineVisibility = routeLineView.getPrimaryRouteVisibility(this)
            val alternativeRouteLinesVisibility = routeLineView.getAlternativeRoutesVisibility(this)
            if (
                primaryLineVisibility == Visibility.VISIBLE &&
                alternativeRouteLinesVisibility == Visibility.VISIBLE
            ) {
                routeLineApi.findClosestRoute(
                    point,
                    mapboxMap,
                    routeClickPadding
                ) { result ->
                    result.onValue { value ->
                        if (value.route != routeLineApi.getPrimaryRoute()) {
                            val reOrderedRoutes = routeLineApi.getRoutes()
                                .filter { it != value.route }
                                .toMutableList()
                                .also {
                                    it.add(0, value.route)
                                }
                            mapboxNavigation.setRoutes(reOrderedRoutes)
                        }
                    }
                }
            }
        }

        false
    }

    private fun initNavigation() {
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)

        // The lines below are related to the navigation simulator.
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(1.5)
        mapboxReplayer.play()
    }

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

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
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

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS,
            { style: Style ->
                // Get the last known location and move the map to that location.
                mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                    locationEngineCallback
                )
                viewBinding.mapView.gestures.addOnMapLongClickListener(this)
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapLoadError: MapLoadErrorType, message: String) {
                    Log.e(
                        MapboxRouteLineAndArrowActivity::class.java.simpleName,
                        "Error loading map: " + mapLoadError.name
                    )
                }
            }
        )
    }

    override fun onMapLongClick(point: Point): Boolean {
        vibrate()
        viewBinding.startNavigation.visibility = View.GONE
        val currentLocation = navigationLocationProvider.lastLocation
        if (currentLocation != null) {
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            findRoute(originPoint, point)
            viewBinding.routeLoadingProgressBar.visibility = View.VISIBLE
        }
        return false
    }

    fun findRoute(origin: Point?, destination: Point?) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .accessToken(getMapboxAccessTokenFromResources())
            .coordinatesList(listOf(origin, destination))
            .alternatives(true)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            routesReqCallback
        )
    }

    private val routesReqCallback: RoutesRequestCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            mapboxNavigation.setRoutes(routes)
            if (routes.isNotEmpty()) {
                viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
                viewBinding.startNavigation.visibility = View.VISIBLE
            }
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            viewBinding.routeLoadingProgressBar.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    ONE_HUNDRED_MILLISECONDS.toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(ONE_HUNDRED_MILLISECONDS.toLong())
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListeners() {
        viewBinding.startNavigation.setOnClickListener {
            val route = mapboxNavigation.getRoutes().firstOrNull()
            if (route != null) {
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.startTripSession()
                viewBinding.startNavigation.visibility = View.INVISIBLE
                locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)

                // RouteLine: Hiding the alternative routes when navigation starts.
                mapboxMap.getStyle()?.apply {
                    routeLineView.hideAlternativeRoutes(this)
                }

                startSimulation(route)
            }
        }
        viewBinding.mapView.gestures.addOnMapClickListener(mapClickListener)
    }

    // Starts the navigation simulator
    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData: List<ReplayEventBase> = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        mapboxReplayer.play()
    }

    override fun onStart() {
        super.onStart()
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        viewBinding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        locationComponent.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        viewBinding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewBinding.mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        viewBinding.mapView.onLowMemory()
    }

    private val locationEngineCallback = MyLocationEngineCallback(this)

    private class MyLocationEngineCallback(activity: MapboxRouteLineAndArrowActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef: WeakReference<MapboxRouteLineAndArrowActivity> by lazy {
            WeakReference(activity)
        }

        override fun onSuccess(result: LocationEngineResult?) {
            val location = result?.lastLocation
            val activity = activityRef.get()
            if (location != null && activity != null) {
                val point = Point.fromLngLat(location.longitude, location.latitude)
                val cameraOptions = CameraOptions.Builder().center(point).zoom(13.0).build()
                activity.mapboxMap.setCamera(cameraOptions)
                activity.navigationLocationProvider.changePosition(location, listOf(), null, null)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }
}
