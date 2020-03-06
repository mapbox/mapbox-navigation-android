package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.options.Endpoint
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter
import com.mapbox.navigation.route.onboard.network.HttpClient
import com.mapbox.navigator.RouterParams
import com.mapbox.navigator.TileEndpointConfiguration
import kotlinx.android.synthetic.main.activity_free_drive.*
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

class FreeDriveActivityKt : AppCompatActivity(), OnMapReadyCallback {

    private val locationEngineCallback = MyLocationEngineCallback(this)
    private val restartSessionEventChannel = Channel<RestartTripSessionAction>(1)

    private var mapboxMap: MapboxMap? = null
    private var locationComponent: LocationComponent? = null

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var localLocationEngine: LocationEngine

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_free_drive)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        localLocationEngine = LocationEngineProvider.getBestLocationEngine(applicationContext)

        setupMapboxNavigation()
    }

    private fun setupMapboxNavigation() {
        val file = File(
                Environment.getExternalStoragePublicDirectory("Offline").absolutePath,
                "2019_04_13-00_00_11"
        )
        val fileTiles = File(file, "tiles")
        val config = MapboxOnboardRouterConfig(
                fileTiles.absolutePath,
                null,
                null,
                null,
                Endpoint(
                        "https://api-routing-tiles-staging.tilestream.net",
                        "2019_04_13-00_00_11",
                        Utils.getMapboxAccessToken(this),
                        "MapboxNavigationNative"
                )
        )

        val tileDir = File(config.tilePath, MapboxOnboardRouter.TILES_DIR_NAME)
        if (!tileDir.exists()) {
            tileDir.mkdirs()
        }
        val routerParams = RouterParams(
                tileDir.absolutePath,
                config.inMemoryTileCache,
                config.mapMatchingSpatialCache,
                config.threadsCount,
                config.endpoint?.let {
                    TileEndpointConfiguration(
                            it.host,
                            it.version,
                            it.token,
                            it.userAgent,
                            ""
                    )
                }
        )
        val httpClient = HttpClient()

//         TODO: This code change NavigatorConfig values for weak devices
        /*val navigatorNativeStub = MapboxNativeNavigatorImpl.getInstance(
                routerParams = routerParams,
                httpClient = httpClient
        )
        val navigatorConfig = navigatorNativeStub.getConfig()
        navigatorNativeStub.shutdown()
        navigatorConfig.mppMaxRequestTimeMs = 500
        navigatorConfig.ehMaxRequestTimeMs = 500

        MapboxNativeNavigatorImpl.initInstance(
                routerParams = routerParams,
                httpClient = httpClient,
                config = navigatorConfig
        )
         */

        val navigatorNative = MapboxNativeNavigatorImpl.getInstance(
                routerParams = routerParams,
                httpClient = httpClient
        )

        mapboxNavigation = MapboxNavigation(
                applicationContext,
                Utils.getMapboxAccessToken(this),
                navigationOptions = MapboxNavigation.defaultNavigationOptions(this, Utils.getMapboxAccessToken(this)),
                navigatorNative = navigatorNative
        )
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))

        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            locationComponent = mapboxMap.locationComponent.apply {
                activateLocationComponent(
                        LocationComponentActivationOptions.builder(this@FreeDriveActivityKt, style)
                                .useDefaultLocationEngine(false)
                                .build()
                )
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }

            mapboxNavigation.startTripSession()
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            Timber.d("raw location %s", rawLocation.toString())
        }

        override fun onEnhancedLocationChanged(
                enhancedLocation: Location,
                keyPoints: List<Location>
        ) {
            if (keyPoints.isNotEmpty()) {
                locationComponent?.forceLocationUpdate(keyPoints, true)
            } else {
                locationComponent?.forceLocationUpdate(enhancedLocation)
            }
            Timber.d("enhanced location %s", enhancedLocation)
            Timber.d("enhanced keyPoints %s", keyPoints)
        }
    }

    private fun startLocationUpdates() {
        val request = LocationEngineRequest.Builder(1000L)
                .setFastestInterval(500L)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .build()
        try {
            localLocationEngine.requestLocationUpdates(
                    request,
                    locationEngineCallback,
                    Looper.getMainLooper()
            )
            localLocationEngine.getLastLocation(locationEngineCallback)
        } catch (exception: SecurityException) {
            Timber.e(exception)
        }
    }

    private fun stopLocationUpdates() {
        localLocationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    stopLocationUpdates()
                }
                TripSessionState.STOPPED -> {
                    startLocationUpdates()
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView.onStart()

        restartSessionEventChannel.poll()?.also {
            mapboxNavigation.startTripSession()
        }

        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()

        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        stopLocationUpdates()

        if (mapboxNavigation.getRoutes().isEmpty() && mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
            // use this to kill the service and hide the notification when going into the background in the Free Drive state,
            // but also ensure to restart Free Drive when coming back from background by using the channel
            mapboxNavigation.stopTripSession()
            restartSessionEventChannel.offer(RestartTripSessionAction)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
        restartSessionEventChannel.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private class MyLocationEngineCallback(activity: FreeDriveActivityKt) :
            LocationEngineCallback<LocationEngineResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                activityRef.get()?.locationComponent?.forceLocationUpdate(it)
            }
        }

        override fun onFailure(exception: Exception) {
        }
    }

    private object RestartTripSessionAction
}