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
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.MapboxCarNavigationManager
import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.car.map.widgets.compass.CarCompassSurfaceRenderer
import com.mapbox.androidauto.car.map.widgets.logo.CarLogoSurfaceRenderer
import com.mapbox.androidauto.deeplink.GeoDeeplinkNavigateAction
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.notification.CarNotificationInterceptor
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
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

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, MapboxExperimental::class)
class MainCarSession : Session() {

    private var mapboxCarContext: MapboxCarContext? = null
    private lateinit var mapboxNavigationManager: MapboxCarNavigationManager
    private val mapboxCarMap = MapboxCarMap()
    private val carMapStyleLoader = MainCarMapLoader()
    private val notificationInterceptor by lazy {
        CarNotificationInterceptor(carContext, MainCarAppService::class.java)
    }

    init {
        logAndroidAuto("MainCarSession constructor")
        MapboxCarApp.setup()
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
                mapboxCarMap.registerObserver(carMapStyleLoader)
                val mapboxCarContext = MapboxCarContext(
                    carContext = carContext,
                    lifecycleOwner = this@MainCarSession,
                    mapboxCarMap = mapboxCarMap,
                ).also {
                    this@MainCarSession.mapboxCarContext = it
                }
                mapboxCarContext.mapboxScreenManager.prepareScreens(mapboxCarContext)
                mapboxNavigationManager = MapboxCarNavigationManager(carContext)
                MapboxNavigationApp.registerObserver(mapboxNavigationManager)
                MapboxNavigationApp.registerObserver(notificationInterceptor)

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
                MapboxNavigationApp.unregisterObserver(mapboxNavigationManager)
                MapboxNavigationApp.unregisterObserver(notificationInterceptor)
                mapboxCarMap.clearObservers()
                mapboxCarContext = null
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
        return mapboxCarContext!!.mapboxScreenManager.createScreen(firstScreenKey)
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

        carMapStyleLoader.updateMapStyle(carContext.isDarkMode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logAndroidAuto("onNewIntent $intent")

        GeoDeeplinkNavigateAction(mapboxCarContext!!).onNewIntent(intent)
    }
}

fun LifecycleOwner.repeatWhenStarted(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED, block) }
}
