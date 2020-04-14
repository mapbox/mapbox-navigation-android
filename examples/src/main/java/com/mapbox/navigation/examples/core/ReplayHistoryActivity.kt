package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.history.CustomEventMapper
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayHistoryLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import com.mapbox.navigation.core.replay.history.ReplayHistoryPlayer
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset.forName
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.android.synthetic.main.activity_trip_service.mapView
import kotlinx.android.synthetic.main.replay_history_example_activity_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class ReplayHistoryActivity : AppCompatActivity() {

    private var navigationContext: ReplayNavigationContext? = null

    // This is needed to update the location component with enhanced location while navigating
    // TODO replace with a recommended way to deal with the issue
    private var isNavigating = false

    // You choose your loading mechanism. Use Coroutines, ViewModels, RxJava, Threads, etc..
    private var loadNavigationJob: Job? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.replay_history_example_activity_layout)
        mapView.onCreate(savedInstanceState)

        getNavigationAsync {
            navigationContext = it
            it.onNavigationReady()
        }
    }

    private fun getNavigationAsync(callback: (ReplayNavigationContext) -> Unit) {
        loadNavigationJob = CoroutineScope(Dispatchers.Main).launch {
            // Load map and style on Main dispatchers
            val deferredMapboxWithStyle = async { loadMapWithStyle() }

            // Load and replay history on IO dispatchers
            val deferredEvents = async(Dispatchers.IO) { loadReplayHistory() }
            val replayEvents = deferredEvents.await()
            val replayHistoryPlayer = ReplayHistoryPlayer()
                .pushEvents(replayEvents)
            if (!isActive) return@launch

            val locationEngine = ReplayHistoryLocationEngine(replayHistoryPlayer)

            // Await the map and we're ready for navigation
            val mapboxNavigation = createMapboxNavigation(locationEngine)
            val (mapboxMap, style) = deferredMapboxWithStyle.await()
            if (!isActive) return@launch

            initLocationComponent(locationEngine, style, mapboxMap)

            val navigationMapboxMap = NavigationMapboxMap(mapView, mapboxMap)
            val navigationContext = ReplayNavigationContext(
                locationEngine,
                mapboxMap,
                style,
                mapboxNavigation,
                navigationMapboxMap,
                replayHistoryPlayer
            )
            callback(navigationContext)
        }
    }

    private suspend fun loadMapWithStyle(): Pair<MapboxMap, Style> = suspendCoroutine { cont ->
        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                cont.resume(Pair(mapboxMap, style))
            }
        }
    }

    private suspend fun loadReplayHistory(): List<ReplayEventBase> = suspendCoroutine { cont ->
        val replayHistoryMapper = ReplayHistoryMapper(Gson(), ReplayCustomEventMapper())
        val rideHistoryExample = loadHistoryJsonFromAssets(this@ReplayHistoryActivity, "replay-history-activity.json")
        val replayEvents = replayHistoryMapper.mapToReplayEvents(rideHistoryExample)
        cont.resume(replayEvents)
    }

    private fun createMapboxNavigation(locationEngine: LocationEngine): MapboxNavigation {
        val accessToken = Utils.getMapboxAccessToken(this)
        val mapboxNavigationOptions = MapboxNavigation.defaultNavigationOptions(
            this, accessToken)

        return MapboxNavigation(
            applicationContext,
            accessToken,
            mapboxNavigationOptions,
            locationEngine
        )
    }

    /**
     * After the map, style, and replay history is all loaded. Connect the view.
     */
    private fun ReplayNavigationContext.onNavigationReady() {
        navigationMapboxMap.addProgressChangeListener(mapboxNavigation)

        mapboxNavigation.registerLocationObserver(locationObserver)

        replayHistoryPlayer.playFirstLocation()
        mapboxMap.addOnMapLongClickListener { latLng ->
            selectMapLocation(latLng)
            true
        }

        replayHistoryPlayer.observeReplayEvents {
            it.forEach { event ->
                when (event) {
                    is ReplayEventInitialRoute -> {
                        event.coordinates.lastOrNull()?.let { latLng ->
                            selectMapLocation(latLng)
                        }
                    }
                }
            }
        }

        playReplay.setOnClickListener {
            replayHistoryPlayer.play(this@ReplayHistoryActivity)
        }
    }

    private fun ReplayNavigationContext.selectMapLocation(latLng: LatLng) {
        mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
            mapboxNavigation.requestRoutes(
                RouteOptions.builder().applyDefaultParams()
                    .accessToken(Utils.getMapboxAccessToken(applicationContext))
                    .coordinates(originLocation.toPoint(), null, latLng.toPoint())
                    .alternatives(true)
                    .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                    .build(),
                routesReqCallback
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun ReplayNavigationContext.startNavigation() {
        isNavigating = true

        if (mapboxNavigation.getRoutes().isNotEmpty()) {
            navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.GPS)
            navigationMapboxMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
            navigationMapboxMap.startCamera(mapboxNavigation.getRoutes()[0])
        }
        mapboxNavigation.startTripSession()
    }

    @SuppressLint("RestrictedApi")
    private fun initLocationComponent(locationEngine: LocationEngine, loadedMapStyle: Style, mapboxMap: MapboxMap) {
        initLocationEngine(locationEngine)

        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
        mapboxMap.locationComponent.let { locationComponent ->
            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationEngine(locationEngine)
                    .build()

            locationComponent.activateLocationComponent(locationComponentActivationOptions)
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS
        }
    }

    private fun initLocationEngine(locationEngine: LocationEngine) {
        val ignoredForReplayEngineRequest = LocationEngineRequest
            .Builder(0).build()
        locationEngine.requestLocationUpdates(
            ignoredForReplayEngineRequest,
            locationListenerCallback,
            null
        )
        locationEngine.getLastLocation(locationListenerCallback)
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
            if (routes.isNotEmpty()) {
                navigationContext?.navigationMapboxMap?.drawRoute(routes[0])
                navigationContext?.startNavigation()
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadNavigationJob?.cancelChildren()
        navigationContext?.apply {
            locationEngine.removeLocationUpdates(locationListenerCallback)
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            replayHistoryPlayer.finish()
            mapboxNavigation.stopTripSession()
            mapboxNavigation.onDestroy()
        }
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private val locationListenerCallback: LocationEngineCallback<LocationEngineResult> =
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                if (isNavigating) return

                result.lastLocation?.let {
                    navigationContext?.mapboxMap?.locationComponent?.forceLocationUpdate(it)
                }
            }

            override fun onFailure(exception: Exception) {
                Timber.i(exception)
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
            if (!isNavigating) return

            if (keyPoints.isNotEmpty()) {
                navigationContext?.mapboxMap?.locationComponent?.forceLocationUpdate(keyPoints, true)
            } else {
                navigationContext?.mapboxMap?.locationComponent?.forceLocationUpdate(enhancedLocation)
            }
        }
    }
}

private data class ReplayNavigationContext(
    val locationEngine: LocationEngine,
    val mapboxMap: MapboxMap,
    val style: Style,
    val mapboxNavigation: MapboxNavigation,
    val navigationMapboxMap: NavigationMapboxMap,
    val replayHistoryPlayer: ReplayHistoryPlayer
)

private fun loadHistoryJsonFromAssets(context: Context, fileName: String): String {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        String(buffer, forName("UTF-8"))
    } catch (e: IOException) {
        Timber.e(e, "Your history file failed to open $fileName")
        throw e
    }
}

private class ReplayCustomEventMapper : CustomEventMapper {
    override fun invoke(eventType: String, event: LinkedTreeMap<*, *>): ReplayEventBase? {
        return when (eventType) {
            "start_transit" -> ReplayEventStartTransit(
                eventTimestamp = event["event_timestamp"] as Double,
                properties = event["properties"] as Double)
            "initial_route" -> {
                val properties = event["properties"] as LinkedTreeMap<*, *>
                val routeOptions = properties["routeOptions"] as LinkedTreeMap<*, *>
                val coordinates = routeOptions["coordinates"] as List<List<Double>>
                val coordinatesLatLng = coordinates.map { LatLng(it[1], it[0]) }
                ReplayEventInitialRoute(
                    eventTimestamp = event["event_timestamp"] as Double,
                    coordinates = coordinatesLatLng
                )
            }
            else -> null
        }
    }
}

private data class ReplayEventStartTransit(
    override val eventTimestamp: Double,
    val properties: Double
) : ReplayEventBase

private data class ReplayEventInitialRoute(
    override val eventTimestamp: Double,
    val coordinates: List<LatLng>
) : ReplayEventBase
