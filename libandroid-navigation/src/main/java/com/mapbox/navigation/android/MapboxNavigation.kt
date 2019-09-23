package com.mapbox.navigation.android

import android.app.Application
import android.content.Context
import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.android.internal.InternalMetricsReporter
import com.mapbox.navigation.api.Navigation
import com.mapbox.navigation.api.NavigationMetricsDelegate
import com.mapbox.navigation.api.NavigationSession

object MapboxNavigation: Navigation {
  override var location: Location?
    get() = TODO("not implemented")
    set(value) {}

  data class Options(
    val offlineEnabled: Boolean = true
  )

  @Volatile
  var options: Options = Options()
  @Volatile
  var metricsDeleagate: NavigationMetricsDelegate = InternalMetricsReporter

  lateinit var applicationContext: Context
  lateinit var mapboxToken: String

  @JvmStatic
  fun init(application: Application, mapboxToken: String) {
    this.applicationContext = application.applicationContext
    this.mapboxToken = mapboxToken
  }

  override fun startNavigation(directionsRoute: DirectionsRoute, listener: NavigationSession.Listener): NavigationSession {
    TODO("not implemented")
  }
}