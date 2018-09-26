package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.arch.lifecycle.MutableLiveData
import android.location.Location
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

class ExampleProgressChangeListener(private val location: MutableLiveData<Location>,
                                    private val progress: MutableLiveData<RouteProgress>): ProgressChangeListener {

  override fun onProgressChange(location: Location, progress: RouteProgress) {
    this.location.value = location
    this.progress.value = progress
  }
}