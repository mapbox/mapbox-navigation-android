package com.mapbox.androidauto.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.androidauto.ActiveGuidanceState
import com.mapbox.androidauto.ArrivalState
import com.mapbox.androidauto.CarAppState
import com.mapbox.androidauto.FreeDriveState
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.RoutePreviewState
import com.mapbox.androidauto.car.feedback.ui.CarGridFeedbackScreen
import com.mapbox.androidauto.car.permissions.NeedsLocationPermissionsScreen
import com.mapbox.androidauto.car.placeslistonmap.PlacesListOnMapScreen
import com.mapbox.androidauto.car.settings.CarSettingsScreen
import com.mapbox.androidauto.deeplink.GeoDeeplinkNavigateAction
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * This is a top level screen manager for a session. Some screens are associated with the
 * [CarAppState] and whenever the state changes, the screen changes. This makes it possible for the
 * app, such as an Activity, to change the car screen with [MapboxCarApp.updateCarAppState].
 *
 * Some screens do not represent an app state, but rather a user action. For example,
 * the [CarSettingsScreen] and [CarGridFeedbackScreen] can be displayed without changing
 * [CarAppState]. The [CarAppState] takes priority over the secondary screens.
 *
 * Usage: Use the [MapboxNavigationApp] to register the screen manager to a [Session] lifecycle. As
 * long as the [MapboxScreenManager] is attached, the car screen will update with [CarAppState].
 *
 * @param mainCarContext MainCarContext
 * @param screenProvider overridable screen provider
 */
@ExperimentalPreviewMapboxNavigationAPI
@MapboxExperimental
class MapboxScreenManager(
    private val mainCarContext: MainCarContext,
    private val screenProvider: MapboxScreenProvider = MapboxScreenProvider(mainCarContext),
) : UIComponent() {

    private val screenManager by lazy {
        mainCarContext.carContext.getCarService(ScreenManager::class.java)
    }

    /**
     * Restore the app to [MapboxCarApp.carAppState]. If the app is in [ActiveGuidanceState],
     * the user will expect the head unit to be in [ActiveGuidanceState] once the car is
     * connected.
     *
     * When location permissions have not been accepted, the [NeedsLocationPermissionsScreen] is
     * returned.
     */
    fun currentScreen(): Screen = carAppStateScreen(MapboxCarApp.carAppState.value)

    /**
     * Given an [Intent] from a [Session]. This will parse the data and search for places.
     * The results will be shown on the [PlacesListOnMapScreen], where the user can select one of
     * the results.
     *
     * @param intent provided by [Session.onNewIntent]
     */
    fun handleNewIntent(intent: Intent) {
        val carContext = mainCarContext.carContext
        val screenManager = carContext.getCarService(ScreenManager::class.java)
        if (hasLocationPermission()) {
            if (intent.action == CarContext.ACTION_NAVIGATE) {
                val screen = GeoDeeplinkNavigateAction(mainCarContext).onNewIntent(intent)
                    ?: return
                screenManager.push(screen)
            }
        } else {
            replaceTop(NeedsLocationPermissionsScreen(carContext))
        }
    }

    /**
     * Replace the top screen with a new screen. When the [CarAppState] changes, the
     * [MapboxScreenProvider] will provide a screen to this function. The [CarAppState] will not
     * be bound, when this manager is detached from [MapboxNavigationApp].
     *
     * For example, you can disable the [MapboxScreenManager] with [MapboxNavigationApp.detach] or
     * [MapboxNavigationApp.unregisterObserver]. This function helps you manually update the
     * screen, even when the screen manager is detached.
     */
    fun replaceTop(screen: Screen) {
        val currentTop = screenManager.top
        if (currentTop.javaClass != screen.javaClass) {
            logAndroidAuto("MainScreenManager screen change ${screen.javaClass.simpleName}")
            with(screenManager) {
                popToRoot()
                push(screen)
                currentTop.finish()
            }
        }
    }

    private fun carAppStateScreen(carAppState: CarAppState): Screen {
        if (!hasLocationPermission()) {
            return screenProvider.needsLocationPermission()
        }
        return when (carAppState) {
            is FreeDriveState -> screenProvider.freeDriveScreen()
            is RoutePreviewState -> screenProvider.routePreviewScreen(carAppState)
            is ActiveGuidanceState -> screenProvider.activeGuidanceScreen()
            is ArrivalState -> screenProvider.arrivalScreen()
        }
    }

    private fun hasLocationPermission(): Boolean =
        PermissionsManager.areLocationPermissionsGranted(mainCarContext.carContext)

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            MapboxCarApp.carAppState
                .distinctUntilChangedBy { it.javaClass }
                .map { carAppStateScreen(it) }
                .collect { screen -> replaceTop(screen) }
        }
    }
}
