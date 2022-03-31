package com.mapbox.navigation.qa_test_app.car

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMap

/**
 * Session class for the Mapbox Map sample app for Android Auto.
 */
@OptIn(MapboxExperimental::class)
class MapSession : Session() {
  private val carMapShowcase = CarMapShowcase()
  private var mapboxCarMap: MapboxCarMap? = null

  override fun onCreateScreen(intent: Intent): Screen {
    // The onCreate is guaranteed to be called before onCreateScreen. You can pass the
    // mapboxCarMap to other screens. Each screen can register and unregister observers.
    // This allows you to scope behaviors to sessions, screens, or events.
    val mapScreen = MapScreen(mapboxCarMap!!)

    return if (carContext.checkSelfPermission(ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
      carContext.getCarService(ScreenManager::class.java)
        .push(mapScreen)
      RequestPermissionScreen(carContext)
    } else mapScreen
  }

  override fun onCarConfigurationChanged(newConfiguration: Configuration) {
    carMapShowcase.loadMapStyle(carContext)
  }

  init {
    lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onCreate(owner: LifecycleOwner) {
        // The carContext is not initialized until onCreate. Initialize your object here
        // and then register any observers that should have a lifecycle for the entire
        // car session.
        mapboxCarMap = MapboxCarMap(MapInitOptions(carContext))
          .registerObserver(carMapShowcase)
          .registerObserver(CarMapWidgets())
      }

      override fun onDestroy(owner: LifecycleOwner) {
        mapboxCarMap?.clearObservers()
        mapboxCarMap = null
      }
    })
  }
}
