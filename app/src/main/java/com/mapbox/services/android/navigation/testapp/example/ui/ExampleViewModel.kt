package com.mapbox.services.android.navigation.testapp.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.location.Location
import android.preference.PreferenceManager
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.services.android.navigation.testapp.NavigationApplication.Companion.instance
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.example.ui.navigation.ExampleMilestoneEventListener
import com.mapbox.services.android.navigation.testapp.example.ui.navigation.ExampleOffRouteListener
import com.mapbox.services.android.navigation.testapp.example.ui.navigation.ExampleProgressChangeListener
import com.mapbox.services.android.navigation.testapp.example.ui.navigation.RouteFinder
import com.mapbox.services.android.navigation.ui.v5.camera.DynamicCamera
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationSpeechPlayer
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechPlayerProvider
import com.mapbox.services.android.navigation.ui.v5.voice.VoiceInstructionLoader
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import okhttp3.Cache
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
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
  val milestone: MutableLiveData<Milestone> = MutableLiveData()
  val destination: MutableLiveData<Point> = MutableLiveData()
  val geocode: MutableLiveData<GeocodingResponse> = MutableLiveData()

  var primaryRoute: DirectionsRoute? = null
  var collapsedBottomSheet: Boolean = false
  var isOffRoute: Boolean = false

  private val locationEngine: LocationEngine
  private val locationEngineCallback: ExampleLocationEngineCallback
  private val speechPlayer: NavigationSpeechPlayer
  private val navigation: MapboxNavigation

  private val accessToken: String = instance.resources.getString(R.string.mapbox_access_token)
  private val routeFinder: RouteFinder

  init {
    routeFinder = RouteFinder(this, routes, accessToken, retrieveOfflineVersionFromPreferences())
    // Initialize the location engine
    locationEngine = LocationEngineProvider.getBestLocationEngine(getApplication())
    locationEngineCallback = ExampleLocationEngineCallback(location)

    // Initialize navigation and pass the LocationEngine
    navigation = MapboxNavigation(getApplication(), accessToken)
    navigation.locationEngine = locationEngine

    // Initialize the speech player and pass to milestone event listener for instructions
    val english = US.language // TODO localization
    val cache = Cache(File(application.cacheDir, EXAMPLE_INSTRUCTION_CACHE),
        TEN_MEGABYTE_CACHE_SIZE)
    val voiceInstructionLoader = VoiceInstructionLoader(getApplication(), accessToken, cache)
    val speechPlayerProvider = SpeechPlayerProvider(getApplication(), english, true, voiceInstructionLoader)
    speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)
    navigation.addMilestoneEventListener(ExampleMilestoneEventListener(milestone, speechPlayer))
    navigation.addProgressChangeListener(ExampleProgressChangeListener(location, progress))
    navigation.addOffRouteListener(ExampleOffRouteListener(this))
  }

  override fun onCleared() {
    super.onCleared()
    shutdown()
  }

  fun activateLocationEngine() {
    requestLocation()
  }

  fun findRouteToDestination() {
    location.value?.let { location ->
      destination.value?.let { destination ->
        routeFinder.findRoute(location, destination)
      }
    }
  }

  fun updatePrimaryRoute(primaryRoute: DirectionsRoute) {
    this.primaryRoute = primaryRoute
  }

  fun canNavigate(): Boolean {
    return primaryRoute != null
  }

  fun startNavigation() {
    primaryRoute?.let {
      navigation.startNavigation(it)
      removeLocation()
    }
  }

  fun stopNavigation() {
    requestLocation()
    navigation.stopNavigation()
  }

  fun retrieveNavigation(): MapboxNavigation {
    return navigation
  }

  fun reverseGeocode(point: LatLng) {
    val reverseGeocode = MapboxGeocoding.builder()
        .accessToken(Mapbox.getAccessToken()!!)
        .query(Point.fromLngLat(point.longitude, point.latitude))
        .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
        .build()
    reverseGeocode.enqueueCall(object : Callback<GeocodingResponse> {
      override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
        geocode.value = response.body()
      }

      override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
        Timber.e(throwable, "Geocoding request failed")
      }
    })
  }

  fun refreshOfflineVersionFromPreferences() {
    val version = retrieveOfflineVersionFromPreferences()
    routeFinder.updateOfflineVersion(version)
  }

  private fun shutdown() {
    val cameraEngine = navigation.cameraEngine
    if (cameraEngine is DynamicCamera) {
      cameraEngine.clearMap()
    }
    navigation.onDestroy()
    speechPlayer.onDestroy()
    removeLocation()
  }

  @SuppressLint("MissingPermission")
  private fun requestLocation() {
    val request = buildEngineRequest()
    locationEngine.requestLocationUpdates(request, locationEngineCallback, null)
  }

  private fun removeLocation() {
    locationEngine.removeLocationUpdates(locationEngineCallback)
  }

  private fun retrieveOfflineVersionFromPreferences(): String {
    val context = getApplication<Application>()
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getString(context.getString(R.string.offline_version_key), "")
  }

  private fun buildEngineRequest(): LocationEngineRequest {
    return LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
        .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
        .build()
  }
}