package com.mapbox.navigation.examples.util

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.manifesta.ManifestaAPI
import com.mapbox.navigation.examples.manifesta.model.domain.LocationCollection
import com.mapbox.navigation.examples.manifesta.model.entity.LocationCollectionEntity
import com.mapbox.navigation.examples.manifesta.model.entity.ManifestaLocation
import com.mapbox.navigation.examples.manifesta.model.entity.ManifestaUser
import com.mapbox.navigation.examples.manifesta.view.LocationCollectionSelectedConsumer
import com.mapbox.navigation.examples.manifesta.view.LocationsListDialog
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.clearRouteLine
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class RouteDrawingActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var navigationLocationProvider: NavigationLocationProvider
    private lateinit var locationComponent: LocationComponentPlugin
    private lateinit var mapCamera: CameraAnimationsPlugin
    private lateinit var routeDrawingUtil: RouteDrawingUtil
    private var routeDrawingUtilEnabled = false

    private val manifestAPI by lazy {
        object : ManifestaAPI {}
    }

    private val routeColorResources: RouteLineColorResources by lazy {
        RouteLineColorResources.Builder()
            .restrictedRoadColor(Color.parseColor("#ffcc00"))
            .build()
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
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
            .withRouteLineBelowLayerId("road-label-navigation")
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
        populateUserSpinner()
        initSaveLocationsButton()
        showLocationCollections()
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
            clearAllPoints()
        }
    }

    private fun clearAllPoints() {
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
            NavigationStyles.NAVIGATION_DAY_STYLE
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

    private var selectedUser: ManifestaUser? = null
    private fun populateUserSpinner() {
        findViewById<Spinner>(R.id.usersSpinner)?.apply {
            val spinnerRef = this
            CoroutineScope(Dispatchers.Main).launch {
                val users = manifestAPI.getAllUsers().getValueOrElse { listOf() }.also {
                    selectedUser = it.firstOrNull()
                }
                spinnerRef.adapter = ArrayAdapter(this@RouteDrawingActivity, R.layout.user_spinner_layout, users)
                spinnerRef.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {}
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        selectedUser = users[p2]
                    }
                }
            }
        }
    }

    private fun initSaveLocationsButton() {
        findViewById<ImageButton>(R.id.btnSaveLocations)?.setOnClickListener {
            if (routeDrawingUtil.touchPoints.isEmpty()) {
                Toast.makeText(this@RouteDrawingActivity, "There are no locations to save.", Toast.LENGTH_SHORT).show()
            } else {
                val input = EditText(this).also { editText ->
                    editText.hint = "Collection Name"
                    editText.inputType = InputType.TYPE_CLASS_TEXT
                }

                AlertDialog.Builder(this).apply {
                    this.setPositiveButton("Save", DialogInterface.OnClickListener { dialog, _ ->
                        val loc = LocationCollection(
                            UUID.randomUUID().toString().replace("-", ""),
                            input.text.toString(),
                            routeDrawingUtil.touchPoints.map { point ->
                                ManifestaLocation(
                                    UUID.randomUUID().toString().replace("-", ""),
                                    position = point
                                )
                            }
                        )

                        selectedUser?.let { currentUser ->
                            CoroutineScope(Dispatchers.Main).launch {
                                manifestAPI.storeLocationCollection(currentUser.id, loc)
                            }
                        }

                        dialog.dismiss()
                    })

                    this.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, _ ->
                        dialog.cancel()
                    })
                }.also { dlgBuilder ->
                    dlgBuilder.setView(input)
                    dlgBuilder.setTitle("Save Location Collection")
                    dlgBuilder.create().also { dlg ->
                        dlg.show()
                    }
                }
            }
        }
    }

    private fun showLocationCollections() {
        var locationCollectionDlg: LocationsListDialog? = null
        val itemSelected: LocationCollectionSelectedConsumer = {
            locationCollectionDlg?.dismiss()
            loadLocationCollection(it)
        }
        findViewById<ImageButton>(R.id.btnShowLocationCollections)?.setOnClickListener {
            locationCollectionDlg = LocationsListDialog(itemSelected).also {
                it.show(supportFragmentManager, "LocationCollectionChooser")
            }
            CoroutineScope(Dispatchers.Main).launch {
                manifestAPI.getLocationCollectionsShallow().getValueOrElse { listOf() }.apply {
                    locationCollectionDlg?.setLocationCollections(this)
                }
            }
        }
    }

    private fun loadLocationCollection(locColl: LocationCollectionEntity) {
        clearAllPoints()
        CoroutineScope(Dispatchers.Main).launch {

            manifestAPI.getLocations(locColl.locations).apply {
                if (this.isNotEmpty()) {
                    val cameraOptions = CameraOptions.Builder().center(this.first().position).zoom(14.0).build()
                    mapView.getMapboxMap().setCamera(cameraOptions)
                }

                this.forEach { loc ->
                    routeDrawingUtil.addPoint(loc.position)
                }
            }
        }
    }
}
