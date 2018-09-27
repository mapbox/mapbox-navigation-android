package com.mapbox.services.android.navigation.testapp.example.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.location.Location
import android.preference.PreferenceManager
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.services.android.navigation.testapp.NavigationApplication
import com.mapbox.services.android.navigation.testapp.NavigationApplication.Companion.instance
import com.mapbox.services.android.navigation.testapp.R
import com.mapbox.services.android.navigation.testapp.example.ui.navigation.*
import com.mapbox.services.android.navigation.testapp.example.ui.offline.OfflineFileLoader
import com.mapbox.services.android.navigation.testapp.example.ui.offline.OfflineFilesLoadedCallback
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationSpeechPlayer
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechPlayerProvider
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.Locale.US

class ExampleViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    const val ONE_SECOND_INTERVAL = 1000
  }

  val location: MutableLiveData<Location> = MutableLiveData()
  val route: MutableLiveData<DirectionsRoute> = MutableLiveData()
  val progress: MutableLiveData<RouteProgress> = MutableLiveData()
  val milestone: MutableLiveData<Milestone> = MutableLiveData()
  val destination: MutableLiveData<Point> = MutableLiveData()
  val geocode: MutableLiveData<GeocodingResponse> = MutableLiveData()
  var collapsedBottomSheet: Boolean = false

  private val locationEngine: LocationEngine
  private val locationEngineListener: ExampleLocationEngineListener
  private val speechPlayer: NavigationSpeechPlayer
  private val navigation: MapboxNavigation
  private val routeFinder: ExampleRouteFinder
  private val accessToken: String = instance.resources.getString(R.string.mapbox_access_token)

  init {
    // Initialize the location engine
    val locationEngineProvider = LocationEngineProvider(getApplication())
    locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable()
    locationEngineListener = ExampleLocationEngineListener(locationEngine, location)
    locationEngine.addLocationEngineListener(locationEngineListener)
    locationEngine.priority = LocationEnginePriority.HIGH_ACCURACY
    locationEngine.fastestInterval = ONE_SECOND_INTERVAL

    // Initialize the speech player and pass to milestone event listener for instructions
    val english = US.language // TODO localization
    val speechPlayerProvider = SpeechPlayerProvider(getApplication(), english, true, accessToken)
    speechPlayer = NavigationSpeechPlayer(speechPlayerProvider)

    // Initialize navigation and pass the LocationEngine
    navigation = MapboxNavigation(getApplication(), accessToken)
    navigation.locationEngine = locationEngine
    navigation.addMilestoneEventListener(ExampleMilestoneEventListener(milestone, speechPlayer))
    navigation.addProgressChangeListener(ExampleProgressChangeListener(location, progress))
    navigation.addOffRouteListener(ExampleOffRouteListener(this))

    // For fetching new routes
    routeFinder = ExampleRouteFinder(navigation, route, accessToken)
  }

  fun activateLocationEngine() {
    locationEngine.activate()
  }

  fun loadOfflineFiles(callback: OfflineFilesLoadedCallback) {
    OfflineFileLoader(navigation, callback)
  }

  fun findRouteToDestination() {
    location.value?.let { location ->
      destination.value?.let { destination ->
        if (isOfflineEnabled()) {
          routeFinder.findOfflineRoute(location, destination)
        } else {
          routeFinder.findRoute(location, destination)
        }
      }
    }
  }

  fun startNavigationWith(route: DirectionsRoute) {
    navigation.startNavigation(route)
    removeLocationEngineListener()
  }

  fun stopNavigation() {
    addLocationEngineListener()
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

      override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
        Timber.e(t, "Geocoding request failed")
      }
    })
  }

  fun onDestroy() {
    navigation.onDestroy()
    speechPlayer.onDestroy()
    removeLocationEngineListener()
  }

  private fun addLocationEngineListener() {
    locationEngine.addLocationEngineListener(locationEngineListener)
  }

  private fun removeLocationEngineListener() {
    locationEngine.removeLocationEngineListener(locationEngineListener)
  }

  private fun isOfflineEnabled(): Boolean {
    val offlineKey = getApplication<NavigationApplication>().getString(R.string.offline_preference_key)
    val preferences = PreferenceManager.getDefaultSharedPreferences(getApplication())
    return preferences.getBoolean(offlineKey, true)
  }
}