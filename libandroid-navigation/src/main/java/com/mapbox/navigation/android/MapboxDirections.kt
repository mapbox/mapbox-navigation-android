package com.mapbox.navigation.android

import android.app.Application
import android.content.Context
import com.mapbox.navigation.api.Directions
import com.mapbox.navigation.api.DirectionsSession
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute

object MapboxDirections: Directions {
  data class Options(
    val offlineEnabled: Boolean = true
  )

  lateinit var applicationContext: Context
  lateinit var mapboxToken: String

  @Volatile
  var options: Options = Options()

  @JvmStatic
  fun init(application: Application, mapboxToken: String) {
    this.applicationContext = application.applicationContext
    this.mapboxToken = mapboxToken
  }

  override fun getDirections(routeRequest: NavigationRoute, listener: DirectionsSession.Listener): DirectionsSession {
    TODO("Wraps hybrid router calls")
  }
}