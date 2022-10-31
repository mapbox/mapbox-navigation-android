package com.mapbox.navigation.qa_test_app.car

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.deeplink.GeoDeeplinkNavigateAction
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.map.compass.CarCompassSurfaceRenderer
import com.mapbox.androidauto.map.logo.CarLogoSurfaceRenderer
import com.mapbox.androidauto.notification.MapboxCarNotificationOptions
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.androidauto.screenmanager.prepareExperimentalScreens
import com.mapbox.androidauto.screenmanager.prepareScreens
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.qa_test_app.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(MapboxExperimental::class, ExperimentalPreviewMapboxNavigationAPI::class)
class MainCarSession : Session() {

    private val carMapLoader = MainCarMapLoader()
    private val mapboxCarMap = MapboxCarMap()
        .registerObserver(carMapLoader)

    private val mapboxCarContext = MapboxCarContext(lifecycle, mapboxCarMap)
        .prepareScreens()
        .prepareExperimentalScreens()
        .customize {
            notificationOptions = MapboxCarNotificationOptions.Builder()
                .startAppService(MainCarAppService::class.java)
                .build()
        }

    init {
        logAndroidAuto("MainCarSession constructor")
        val logoSurfaceRenderer = CarLogoSurfaceRenderer()
        val compassSurfaceRenderer = CarCompassSurfaceRenderer()
        MapboxNavigationApp.attach(lifecycleOwner = this)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onCreate")
                if (!MapboxNavigationApp.isSetup()) {
                    MapboxNavigationApp.setup(
                        NavigationOptions.Builder(carContext)
                            .accessToken(Utils.getMapboxAccessToken(carContext))
                            .build()
                    )
                }
                mapboxCarMap.setup(carContext, MapInitOptions(context = carContext))
                repeatWhenStarted {
                    if (hasLocationPermission()) {
                        startTripSession()
                    }
                }
            }

            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onResume")
                mapboxCarMap.registerObserver(logoSurfaceRenderer)
                mapboxCarMap.registerObserver(compassSurfaceRenderer)
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onPause")
                mapboxCarMap.unregisterObserver(logoSurfaceRenderer)
                mapboxCarMap.unregisterObserver(compassSurfaceRenderer)
            }

            override fun onStop(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onStop")
            }

            override fun onDestroy(owner: LifecycleOwner) {
                logAndroidAuto("MainCarSession onDestroy")
                mapboxCarMap.clearObservers()
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        logAndroidAuto("MainCarSession onCreateScreen")
        val firstScreenKey = if (hasLocationPermission()) {
            MapboxScreenManager.current()?.key ?: MapboxScreen.FREE_DRIVE
        } else {
            MapboxScreen.NEEDS_LOCATION_PERMISSION
        }
        return mapboxCarContext.mapboxScreenManager.createScreen(firstScreenKey)
    }

    @SuppressLint("MissingPermission")
    private fun startTripSession() {
        logAndroidAuto("MainCarSession startTripSession")
        MapboxNavigationApp.current()?.apply {
            if (getTripSessionState() != TripSessionState.STARTED) {
                startTripSession()
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        PermissionsManager.areLocationPermissionsGranted(carContext)

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        logAndroidAuto("onCarConfigurationChanged ${carContext.isDarkMode}")

        carMapLoader.updateMapStyle(carContext.isDarkMode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logAndroidAuto("onNewIntent $intent")

        GeoDeeplinkNavigateAction(mapboxCarContext).onNewIntent(intent)
    }
}

fun LifecycleOwner.repeatWhenStarted(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED, block) }
}
