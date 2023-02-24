package com.mapbox.navigation.examples.androidauto.car

import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.ActionStrip
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.action.MapboxScreenActionStripProvider
import com.mapbox.androidauto.deeplink.GeoDeeplinkNavigateAction
import com.mapbox.androidauto.freedrive.FreeDriveActionStrip
import com.mapbox.androidauto.map.MapboxCarMapLoader
import com.mapbox.androidauto.map.compass.CarCompassRenderer
import com.mapbox.androidauto.map.logo.CarLogoRenderer
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.androidauto.screenmanager.prepareExperimentalRoutePreviewScreen
import com.mapbox.androidauto.screenmanager.prepareScreens
import com.mapbox.maps.ContextMode
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapOptions
import com.mapbox.maps.applyDefaultParams
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.MapboxTripStarter
import com.mapbox.navigation.examples.androidauto.CarAppSyncComponent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MainCarSession : Session() {

    private val mapboxCarMapLoader = MapboxCarMapLoader()
    private val mapboxCarMap = MapboxCarMap().registerObserver(mapboxCarMapLoader)
    private val mapboxCarContext = MapboxCarContext(lifecycle, mapboxCarMap)
        .prepareScreens()
        .prepareExperimentalRoutePreviewScreen()
        .customize {
            // Use the actionStripProvider to customize the screen actions.
            actionsStripProvider = object : MapboxScreenActionStripProvider() {
                override fun getFreeDrive(screen: Screen): ActionStrip {
                    val actionsBuilder = FreeDriveActionStrip(screen)
                    return ActionStrip.Builder()
                        .addAction(actionsBuilder.buildSettingsAction())
                        .addAction(actionsBuilder.buildFeedbackAction())
                        .addAction(actionsBuilder.buildSearchAction())
                        .addAction(actionsBuilder.buildFavoritesAction())
                        .build()
                }
            }
        }
    private val mapboxTripStarter = MapboxTripStarter.getRegisteredInstance()

    init {
        MapboxNavigationApp.attach(this)

        // Decide how you want the car and app to interact. In this example, the car and app
        // are kept in sync where they essentially mirror each other.
        CarAppSyncComponent.getInstance().setCarSession(this)

        // Add BitmapWidgets to the map that will be shown whenever the map is visible.
        val logoSurfaceRenderer = CarLogoRenderer()
        val compassSurfaceRenderer = CarCompassRenderer()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                // You must give the MapboxCarMap an instance of the carContext.
                // The BitmapWidget needs to use ContextMode.SHARED to remove an optimization
                // that can crash or create artifacts.
                mapboxCarMap.setup(
                    carContext,
                    MapInitOptions(
                        context = carContext,
                        mapOptions = MapOptions.Builder()
                            .applyDefaultParams(carContext)
                            .contextMode(ContextMode.SHARED)
                            .build()
                    )
                )
                checkLocationPermissions()
                observeAutoDrive()
            }

            override fun onResume(owner: LifecycleOwner) {
                mapboxCarMap.registerObserver(logoSurfaceRenderer)
                mapboxCarMap.registerObserver(compassSurfaceRenderer)
            }

            override fun onPause(owner: LifecycleOwner) {
                mapboxCarMap.unregisterObserver(logoSurfaceRenderer)
                mapboxCarMap.unregisterObserver(compassSurfaceRenderer)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                mapboxCarMap.clearObservers()
            }
        })
    }

    // This logic is for you to decide. In this example the MapboxScreenManager.replaceTop is
    // declared in other logical places. At this point the screen key should be already set.
    override fun onCreateScreen(intent: Intent): Screen {
        val screenKey = MapboxScreenManager.current()?.key
        checkNotNull(screenKey) { "The screen key should be set before the Screen is requested." }
        return mapboxCarContext.mapboxScreenManager.createScreen(screenKey)
    }

    // Forward the CarContext to the MapboxCarMapLoader with the configuration changes.
    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        mapboxCarMapLoader.onCarConfigurationChanged(carContext)
    }

    // Handle the geo deeplink for voice activated navigation. This will handle the case when
    // you ask the head unit to "Navigate to coffee shop".
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (PermissionsManager.areLocationPermissionsGranted(carContext)) {
            GeoDeeplinkNavigateAction(mapboxCarContext).onNewIntent(intent)
        }
    }

    // Location permissions are required for this example. Check the state and replace the current
    // screen if there is not one already set.
    private fun checkLocationPermissions() {
        PermissionsManager.areLocationPermissionsGranted(carContext).also { isGranted ->
            val currentKey = MapboxScreenManager.current()?.key
            if (!isGranted) {
                MapboxScreenManager.replaceTop(MapboxScreen.NEEDS_LOCATION_PERMISSION)
            } else if (currentKey == null || currentKey == MapboxScreen.NEEDS_LOCATION_PERMISSION) {
                MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE)
            }
        }
    }

    // Enable auto drive. Open the app on the head unit and then execute the following from your
    // computer terminal.
    // adb shell dumpsys activity service com.mapbox.navigation.examples.androidauto.car.MainCarAppService AUTO_DRIVE
    private fun observeAutoDrive() {
        mapboxCarContext.mapboxNavigationManager.autoDriveEnabledFlow
            .filter { it }
            .onEach { mapboxTripStarter.enableReplayRoute() }
            .launchIn(lifecycleScope)
    }
}
