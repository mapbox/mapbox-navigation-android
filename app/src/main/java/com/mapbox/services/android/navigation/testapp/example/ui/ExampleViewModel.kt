package com.mapbox.services.android.navigation.testapp.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.preference.PreferenceManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.navigation.base.extensions.applyDefaultParams
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.extensions.ifNonNull
import com.mapbox.navigation.base.network.ReplayRouteLocationEngine
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader
import com.mapbox.services.android.navigation.testapp.NavigationApplication.Companion.instance
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.example.ui.navigation.ExampleOffRouteListener
import com.mapbox.services.android.navigation.testapp.example.ui.navigation.ExampleProgressChangeListener
import com.mapbox.services.android.navigation.testapp.example.ui.navigation.RouteFinder
import okhttp3.Cache
import java.io.File
import java.util.Locale.US

private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000
private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 500
private const val EXAMPLE_INSTRUCTION_CACHE = "example-navigation-instruction-cache"
private const val TEN_MEGABYTE_CACHE_SIZE: Long = 10 * 1024 * 1024

class ExampleViewModel(application: Application) : AndroidViewModel(application) {

    val location: MutableLiveData<Location> = MutableLiveData()
    val routes: MutableLiveData<List<DirectionsRoute>> = MutableLiveData()
    val progress: MutableLiveData<RouteProgress> = MutableLiveData()
    val destination: MutableLiveData<Point> = MutableLiveData()

    var primaryRoute: DirectionsRoute? = null
    var isOffRoute: Boolean = false

    private val locationEngine: LocationEngine by lazy(LazyThreadSafetyMode.NONE) {
        LocationEngineProvider.getBestLocationEngine(getApplication())
    }
    private val locationEngineCallback: ExampleLocationEngineCallback by lazy(LazyThreadSafetyMode.NONE) {
        ExampleLocationEngineCallback(location)
    }
    private val speechPlayer: NavigationSpeechPlayer
    private var navigation: MapboxNavigation

    private val accessToken: String = instance.resources.getString(R.string.mapbox_access_token)
    private val routeFinder: RouteFinder
    private val progressChangeListener: ExampleProgressChangeListener

    init {
        navigation =
            MapboxNavigation(getApplication(), accessToken, locationEngine = locationEngine)

        routeFinder = RouteFinder(
            this, routes, { navigation }, accessToken
        )

        // Initialize the speech player and pass to milestone event listener for instructions
        val english = US.language // TODO localization
        val cache = Cache(
            File(application.cacheDir, EXAMPLE_INSTRUCTION_CACHE),
            TEN_MEGABYTE_CACHE_SIZE
        )
        val voiceInstructionLoader = VoiceInstructionLoader(getApplication(), accessToken, cache)
        val speechPlayerProvider =
            SpeechPlayerProvider(getApplication(), english, true, voiceInstructionLoader)
        speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)
        progressChangeListener = ExampleProgressChangeListener(location, progress).also { routeProgressChangeListener ->
            navigation.registerRouteProgressObserver(routeProgressChangeListener)
            navigation.registerLocationObserver(routeProgressChangeListener)
        }
        navigation.registerOffRouteObserver(ExampleOffRouteListener(this))
    }

    override fun onCleared() {
        super.onCleared()
        shutdown()
    }

    fun activateLocationEngine() {
        requestLocation()
    }

    fun findRouteToDestination() {
        ifNonNull(location.value, destination.value) { location, destination ->
            navigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultParams()
                    .accessToken(getApplication<Application>().getString(R.string.mapbox_access_token))
                    .coordinates(
                        Point.fromLngLat(location.longitude, location.latitude),
                        destination = destination
                    )
                    .build()
            )
            routeFinder.findRoute(location, destination)
        }
    }

    fun updatePrimaryRoute(primaryRoute: DirectionsRoute) {
        this.primaryRoute = primaryRoute
    }

    fun canNavigate(): Boolean {
        return primaryRoute != null
    }

    @SuppressLint("MissingPermission")
    fun startNavigation() {
        primaryRoute?.let { primaryRoute ->
            navigation = when (shouldSimulateRoute()) {
                true -> {
                    val replayRouteLocationEngine = ReplayRouteLocationEngine()
                    replayRouteLocationEngine.assign(primaryRoute)
                    MapboxNavigation(
                        getApplication(),
                        accessToken,
                        locationEngine = replayRouteLocationEngine
                    )
                }
                false -> {
                    MapboxNavigation(getApplication(), accessToken, locationEngine = locationEngine)
                }
            }
            navigation.setRoutes(listOf(primaryRoute))
            navigation.startTripSession()
            removeLocation()
        }
    }

    private fun shouldSimulateRoute(): Boolean {
        val context = getApplication<Application>()
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.simulate_route_key), false)
    }

    fun stopNavigation() {
        requestLocation()
        navigation.stopTripSession()
    }

    fun retrieveNavigation(): MapboxNavigation {
        return navigation
    }

    private fun shutdown() {
        // val cameraEngine = navigation.cameraEngine
        // if (cameraEngine is DynamicCamera) {
        //     cameraEngine.clearMap()
        // }
        navigation.onDestroy()
        speechPlayer.onDestroy()
        removeLocation()
        navigation.unregisterRouteProgressObserver(progressChangeListener)
        navigation.unregisterLocationObserver(progressChangeListener)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        val request = buildEngineRequest()
        locationEngine.requestLocationUpdates(request, locationEngineCallback, null)
    }

    private fun removeLocation() {
        locationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private fun buildEngineRequest(): LocationEngineRequest {
        return LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
            .build()
    }
}
