package com.mapbox.navigation.qa_test_app.view.base

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
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
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityBaseNavigationBinding
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
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
import com.mapbox.navigation.ui.shield.model.RouteShieldCallback
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseNavigationActivity : AppCompatActivity(), OnMapLongClickListener {

    private lateinit var baseBinding: LayoutActivityBaseNavigationBinding

    val mapView: MapView get() = baseBinding.mapView
    val camera: CameraAnimationsPlugin get() = mapView.camera
    val lastLocation get() = navigationLocationProvider.lastLocation

    lateinit var mapboxNavigation: MapboxNavigation

    var isNavigating = false
        private set

    private lateinit var locationComponent: LocationComponentPlugin

    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()

    private val distanceFormatter: DistanceFormatterOptions by lazy {
        DistanceFormatterOptions.Builder(this).build()
    }

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

    private val roadShieldCallback = RouteShieldCallback { shieldResult ->
        baseBinding.maneuverView.renderManeuverWith(shieldResult)
    }

    lateinit var navigationCamera: NavigationCamera
    lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private val locationObserver = object : LocationObserver {

        override fun onNewRawLocation(rawLocation: Location) {}

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
            viewportDataSource.evaluate()

            navigationLocationProvider.changePosition(
                locationMatcherResult.enhancedLocation,
                locationMatcherResult.keyPoints,
            )
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        renderManeuvers(maneuvers)

        routeArrowApi.addUpcomingManeuverArrow(routeProgress).apply {
            routeArrowView.renderManeuverUpdate(mapView.getMapboxMap().getStyle()!!, this)
        }
    }

    private val routesObserver = RoutesObserver { result ->
        if (result.routes.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                routeLineApi.setRoutes(
                    listOf(RouteLine(result.routes[0], null))
                ).apply {
                    routeLineView.renderRouteDrawData(mapView.getMapboxMap().getStyle()!!, this)
                }
            }
            startSimulation(result.routes[0])

            val maneuvers = maneuverApi.getManeuvers(result.routes.first())
            renderManeuvers(maneuvers)
        }
    }

    //region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseBinding = LayoutActivityBaseNavigationBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)
        locationComponent = baseBinding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        init()
    }

    override fun onStart() {
        super.onStart()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        maneuverApi.cancel()
        mapboxReplayer.finish()
        mapboxNavigation.onDestroy()
    }

    //endregion

    //region Actions

    override fun onMapLongClick(point: Point): Boolean {
        ifNonNull(navigationLocationProvider.lastLocation) { currentLocation ->

            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )

            findRoute(originPoint, point)
        }
        return false
    }

    //endregion

    /**
     * Subclasses must implement this method and return content view that will be displayed over the MapView.
     *
     * Content view will be constrained as following:
     *     app:layout_constraintTop_toBottomOf="@+id/maneuverView"
     *     app:layout_constraintBottom_toBottomOf="parent"
     *     app:layout_constraintStart_toStartOf="parent"
     *     app:layout_constraintEnd_toEndOf="parent"
     */
    abstract fun onCreateContentView(): View?

    protected open fun onStyleLoaded(style: Style) {
        routeLineView.initializeLayers(style)
        baseBinding.mapView.gestures.addOnMapLongClickListener(this)
    }

    // --

    private fun renderManeuvers(maneuvers: Expected<ManeuverError, List<Maneuver>>) {
        maneuvers.onError { error ->
            logE(error.errorMessage!!, "BaseNavigationActivity")
        }
        maneuvers.onValue {
            if (baseBinding.maneuverView.visibility == View.INVISIBLE) {
                baseBinding.maneuverView.visibility = View.VISIBLE
            }
            baseBinding.maneuverView.renderManeuvers(maneuvers)
            maneuverApi.getRoadShields(
                DirectionsCriteria.PROFILE_DEFAULT_USER,
                NavigationStyles.NAVIGATION_DAY_STYLE_ID,
                getMapboxAccessToken("mapbox_access_token"),
                it,
                roadShieldCallback
            )
        }
    }

    private fun init() {
        initNavigation()
        initStyle()
        initUi()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        val navigationOptions = NavigationOptions.Builder(this)
            .accessToken(getMapboxAccessToken("mapbox_access_token_signboard"))
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()

        mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
        mapboxNavigation.startTripSession()

        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
        mapboxNavigation.onFirstRawLocation { rawLocation ->
            updateCamera(rawLocation)
            navigationLocationProvider.changePosition(rawLocation)
        }
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxReplayer.pushRealLocation(this, 0.0)

        // initialize the location puck
        mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    this@BaseNavigationActivity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
        }

        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapView.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.camera,
            viewportDataSource
        )
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
        }
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.followingPadding = followingPadding
        }
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapView.getMapboxMap()
            .loadStyleUri(NavigationStyles.NAVIGATION_DAY_STYLE, this::onStyleLoaded)
    }

    private fun initUi() {
        onCreateContentView()?.also { contentView ->
            baseBinding.content.addView(
                contentView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            )
        }
    }

    private fun MapboxNavigation.onFirstRawLocation(action: (rawLocation: Location) -> Unit) {
        registerLocationObserver(object : LocationObserver {
            override fun onNewRawLocation(rawLocation: Location) {
                action(rawLocation)
                unregisterLocationObserver(this)
            }

            override fun onNewLocationMatcherResult(
                locationMatcherResult: LocationMatcherResult,
            ) = Unit
        })
    }

    private fun getMapboxAccessToken(resourceName: String): String {
        val tokenResId = resources
            .getIdentifier(resourceName, "string", packageName)
        return if (tokenResId != 0) {
            getString(tokenResId)
        } else {
            getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
        }
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()

        isNavigating = true
        navigationCamera.requestNavigationCameraToFollowing()
    }

    private fun findRoute(origin: Point, destination: Point) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            .coordinates(
                origin = origin,
                destination = destination
            )
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
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
        baseBinding.mapView.camera.easeTo(
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

    private val Number.dp: Double get() = toDouble() * Resources.getSystem().displayMetrics.density

    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0.dp,
            40.0.dp,
            120.0.dp,
            40.0.dp
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0.dp,
            380.0.dp,
            20.0.dp,
            20.0.dp
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0.dp,
            40.0.dp,
            150.0.dp,
            40.0.dp
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0.dp,
            380.0.dp,
            110.0.dp,
            40.0.dp
        )
    }
}
