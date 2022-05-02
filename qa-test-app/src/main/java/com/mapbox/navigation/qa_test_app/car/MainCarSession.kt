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
import com.mapbox.androidauto.MapboxCarNavigationManager
import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.MainScreenManager
import com.mapbox.androidauto.car.map.widgets.compass.CarCompassSurfaceRenderer
import com.mapbox.androidauto.car.map.widgets.logo.CarLogoSurfaceRenderer
import com.mapbox.androidauto.car.permissions.NeedsLocationPermissionsScreen
import com.mapbox.androidauto.deeplink.GeoDeeplinkNavigateAction
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, MapboxExperimental::class)
class MainCarSession : Session() {

    private var mainCarContext: MainCarContext? = null
    private lateinit var mainScreenManager: MainScreenManager
    private lateinit var mapboxCarMap: MapboxCarMap
    private lateinit var mapboxNavigationManager: MapboxCarNavigationManager
    private val carMapStyleLoader = MainCarMapLoader()

    init {
        val logoSurfaceRenderer = CarLogoSurfaceRenderer()
        val compassSurfaceRenderer = CarCompassSurfaceRenderer()
        logAndroidAuto("MainCarSession constructor")
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

                mapboxCarMap = MapboxCarMap(MapInitOptions(context = carContext))
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
                mapboxCarMap.unregisterObserver(carMapStyleLoader)
                mainCarContext = null
            }
        })

        MapboxNavigationApp.attach(lifecycleOwner = this)
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
                    ifNonNull(mainCarContext) {
                        GeoDeeplinkNavigateAction(it, lifecycle).onNewIntent(intent)
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
