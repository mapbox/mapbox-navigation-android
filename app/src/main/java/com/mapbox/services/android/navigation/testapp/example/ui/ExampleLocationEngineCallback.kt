package com.mapbox.services.android.navigation.testapp.example.ui

import android.arch.lifecycle.MutableLiveData
import android.location.Location
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import timber.log.Timber

class ExampleLocationEngineCallback(private val location: MutableLiveData<Location>):
    LocationEngineCallback<LocationEngineResult> {

  override fun onSuccess(result: LocationEngineResult) {
    location.value = result.lastLocation
  }

  override fun onFailure(exception: Exception) {
    Timber.e(exception)
  }
}