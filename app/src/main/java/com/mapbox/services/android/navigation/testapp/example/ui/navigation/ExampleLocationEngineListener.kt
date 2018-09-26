package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.annotation.SuppressLint
import android.arch.lifecycle.MutableLiveData
import android.location.Location
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener

class ExampleLocationEngineListener(private val locationEngine: LocationEngine,
                                    private val location: MutableLiveData<Location>): LocationEngineListener {

  override fun onLocationChanged(location: Location?) {
    this.location.value = location
  }

  @SuppressLint("MissingPermission")
  override fun onConnected() {
    locationEngine.requestLocationUpdates()

    if (locationEngine.lastLocation != null) {
      onLocationChanged(locationEngine.lastLocation)
    }
  }
}