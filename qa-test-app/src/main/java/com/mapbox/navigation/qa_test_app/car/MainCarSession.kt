package com.mapbox.navigation.qa_test_app.car

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.MapboxCarNavigationManager
import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.MainScreenManager
import com.mapbox.androidauto.car.map.widgets.compass.CarCompassSurfaceRenderer
import com.mapbox.androidauto.car.map.widgets.logo.CarLogoSurfaceRenderer
import com.mapbox.androidauto.car.permissions.NeedsLocationPermissionsScreen
import com.mapbox.androidauto.deeplink.GeoDeeplinkNavigateAction
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.notification.CarNotificationInterceptor
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

    private var mainCarContext: MainCarContext? = null
    private lateinit var mainScreenManager: MainScreenManager
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
                mapboxNavigationManager = MapboxCarNavigationManager(carContext)
                MapboxNavigationApp.registerObserver(mapboxNavigationManager)

                mapboxCarMap.setup(carContext, MapInitOptions(context = carContext))
                mapboxCarMap.registerObserver(carMapStyleLoader)
                val mainCarContext = MainCarContext(carContext, mapboxCarMap)
                    .also { mainCarContext = it }
                val mainScreenManager = MainScreenManager(mainCarContext)
                    .also { mainScreenManager = it }
                repeatWhenStarted {
                    if (hasLocationPermission()) {
                        startTripSession(mainCarContext)
                        mainScreenManager.observeCarAppState()
                    }
                }
                MapboxNavigationApp.registerObserver(notificationInterceptor)
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
                mainCarContext = null
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        logAndroidAuto("MainCarSession onCreateScreen")

        return when (hasLocationPermission()) {
            true -> mainScreenManager.currentScreen()
            false -> NeedsLocationPermissionsScreen(carContext)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTripSession(mainCarContext: MainCarContext) {
        mainCarContext.apply {
            logAndroidAuto("MainCarSession startTripSession")
            if (mapboxNavigation.getTripSessionState() != TripSessionState.STARTED) {
                mapboxNavigation.startTripSession()
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

        val currentScreen: Screen = when (hasLocationPermission()) {
            true -> {
                if (intent.action == CarContext.ACTION_NAVIGATE) {
                    mainCarContext?.let { context ->
                        GeoDeeplinkNavigateAction(context, lifecycle).onNewIntent(intent)
                    }
                } else {
                    null
                }
            }
            false -> NeedsLocationPermissionsScreen(carContext)
        } ?: mainScreenManager.currentScreen()
        carContext.getCarService(ScreenManager::class.java).push(currentScreen)
    }
}

fun LifecycleOwner.repeatWhenStarted(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED, block) }
}
