package com.mapbox.navigation.examples.util

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.TileStore
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.clearRouteLine
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RouteDrawingActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var navigationLocationProvider: NavigationLocationProvider
    private lateinit var locationComponent: LocationComponentPlugin
    private lateinit var mapCamera: CameraAnimationsPlugin
    private lateinit var routeDrawingUtil: RouteDrawingUtil
    private var routeDrawingUtilEnabled = false

    private val routeColorResources: RouteLineColorResources by lazy {
        RouteLineColorResources.Builder()
            .restrictedRoadColor(Color.parseColor("#ffcc00"))
            .build()
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .restrictedRoadSectionScale(7.0)
            .routeLineColorResources(routeColorResources)
            .build()
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_route_drawing_activity)
        val tileStore = TileStore.create()
        val mapboxMapOptions = MapInitOptions(this)
        val resourceOptions = ResourceOptions.Builder()
            .accessToken(getMapboxAccessTokenFromResources())
            .assetPath(filesDir.absolutePath)
            .dataPath(filesDir.absolutePath + "/mbx.db")
            .tileStore(tileStore)
            .build()
        mapboxMapOptions.resourceOptions = resourceOptions
        mapView = MapView(this, mapboxMapOptions)
        val mapLayout = findViewById<RelativeLayout>(R.id.mapView_container)
        mapLayout.addView(mapView)
        navigationLocationProvider = NavigationLocationProvider()
        locationComponent = mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        mapCamera = getMapCamera()

        init()
    }

    private fun init() {
        initStyle()
        initLocation()
    }

    private fun initListeners() {
        findViewById<Button>(R.id.btnEnableLongPress).setOnClickListener {
            when (routeDrawingUtilEnabled) {
                false -> {
                    routeDrawingUtilEnabled = true
                    routeDrawingUtil.enable()
                    (it as Button).text = "Disable Long Press Map"
                }
                true -> {
                    routeDrawingUtilEnabled = false
                    routeDrawingUtil.disable()
                    (it as Button).text = "Enable Long Press Map"
                }
            }
        }

        findViewById<Button>(R.id.btnFetchRoute).setOnClickListener {
            routeDrawingUtil.fetchRoute(routeRequestCallback)
        }

        findViewById<Button>(R.id.btnRemoveLastPoint).setOnClickListener {
            routeDrawingUtil.removeLastPoint()
        }

        findViewById<Button>(R.id.btnClearPoints).setOnClickListener {
            routeDrawingUtil.clear()
            CoroutineScope(Dispatchers.Main).launch {
                routeLineApi.clearRouteLine().apply {
                    routeLineView.renderClearRouteLineValue(
                        mapView.getMapboxMap().getStyle()!!,
                        this
                    )
                }
            }
        }
    }

    private val routeRequestCallback: RouterCallback = object : RouterCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
            val routeLines = routes.map { RouteLine(it, null) }
            CoroutineScope(Dispatchers.Main).launch {
                routeLineApi.setRoutes(routeLines).apply {
                    routeLineView.renderRouteDrawData(mapView.getMapboxMap().getStyle()!!, this)
                }
            }
        }

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            Toast.makeText(
                this@RouteDrawingActivity,
                reasons.firstOrNull()?.message,
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
            Toast.makeText(
                this@RouteDrawingActivity,
                "Fetch Route Cancelled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocation() {
        val location = Location("").also {
            it.latitude = 37.975391
            it.longitude = -122.523667
        }

        val point = Point.fromLngLat(-122.523667, 37.975391)
        val cameraOptions = CameraOptions.Builder().center(point).zoom(14.0).build()
        mapView.getMapboxMap().setCamera(cameraOptions)
        navigationLocationProvider.changePosition(
            location,
            listOf(),
            null,
            null
        )

        LocationEngineProvider.getBestLocationEngine(this)
            .getLastLocation(
                object : LocationEngineCallback<LocationEngineResult> {
                    override fun onSuccess(result: LocationEngineResult?) {
                        result?.lastLocation?.let { location ->
                            val point = Point.fromLngLat(location.longitude, location.latitude)
                            val cameraOptions =
                                CameraOptions.Builder().center(point).zoom(14.0).build()
                            mapView.getMapboxMap().setCamera(cameraOptions)
                            navigationLocationProvider.changePosition(
                                location,
                                listOf(),
                                null,
                                null
                            )
                        }
                    }

                    override fun onFailure(exception: Exception) {
                        // Intentionally empty
                    }
                }
            )
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            routeDrawingUtil = RouteDrawingUtil(mapView)
            initListeners()
        }
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private fun getMapCamera(): CameraAnimationsPlugin {
        return mapView.camera
    }
}
