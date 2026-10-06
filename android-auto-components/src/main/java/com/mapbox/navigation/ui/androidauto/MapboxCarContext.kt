package com.mapbox.navigation.ui.androidauto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.navigation.NavigationManager
import androidx.lifecycle.Lifecycle
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.internal.context.MapboxCarContextOwner
import com.mapbox.navigation.ui.androidauto.internal.context.attachWhileCreated
import com.mapbox.navigation.ui.androidauto.internal.context.mapboxCarNavigationService
import com.mapbox.navigation.ui.androidauto.internal.context.mapboxCarService
import com.mapbox.navigation.ui.androidauto.navigation.MapboxCarNavigationManager
import com.mapbox.navigation.ui.androidauto.notification.ActiveGuidanceExtenderUpdater
import com.mapbox.navigation.ui.androidauto.notification.FreeDriveExtenderUpdater
import com.mapbox.navigation.ui.androidauto.notification.IdleExtenderUpdater
import com.mapbox.navigation.ui.androidauto.notification.MapboxCarNotification
import com.mapbox.navigation.ui.androidauto.notification.MapboxCarNotificationOptions
import com.mapbox.navigation.ui.androidauto.placeslistonmap.PlacesListOnMapProvider
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequest
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.settings.MapboxCarStorage

/**
 * This is the entry point for Mapbox Navigation in Android Auto. Create this object and then you
 * have access to services and options for customization.
 *
 * The [lifecycle] must come from a [Session.getLifecycle] or [Screen.getLifecycle] at this time.
 *
 * Create this object and register the screen factories (for example with `prepareScreens`) before
 * the [lifecycle] is [Lifecycle.State.CREATED], for example as a property of your [Session].
 * Screen events from a previous [Session] are replayed to the [MapboxScreenManager] when the
 * [lifecycle] is created; events for screens without a registered factory at that moment are
 * ignored.
 *
 * @param lifecycle used to access the [CarContext].
 * @param mapboxCarMap controls the Mapbox car map surface.
 */
class MapboxCarContext(
    /**
     * Gives access to the [Lifecycle] that owns the [CarContext].
     */
    val lifecycle: Lifecycle,
    /**
     * [MapboxCarMap] controls the Mapbox car map surface
     */
    val mapboxCarMap: MapboxCarMap,
) {
    private val carContextOwner = MapboxCarContextOwner(lifecycle)

    /**
     * Options available for customizing Mapbox Android Auto Navigation.
     */
    val options: MapboxCarOptions = MapboxCarOptions()

    /**
     * Gives access to the [CarContext]. Throws an [IllegalStateException] when accessed before the
     * lifecycle is [Lifecycle.State.CREATED]
     */
    val carContext: CarContext by mapboxCarService("CarContext") {
        carContextOwner.carContext()
    }

    /**
     * Control the screens shown with the [MapboxScreenManager].
     */
    val mapboxScreenManager = MapboxScreenManager(carContextOwner)

    /**
     * Integrates Mapbox with the car libraries [NavigationManager]. Gives access to the auto
     * drive state. Throws an [IllegalStateException] when accessed before the lifecycle is
     * [Lifecycle.State.CREATED].
     *
     * @see MapboxCarNavigationManager
     * @see NavigationManager
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    val mapboxNavigationManager by mapboxCarNavigationService("MapboxCarNavigationManager") {
        MapboxCarNavigationManager(carContext) {
            stopNavigationFromCarHost(MapboxNavigationApp.current())
        }
    }

    /**
     * See the [MapboxCarNotificationOptions] for customization.
     *
     * @see MapboxCarNotification
     */
    internal val mapboxNotification by mapboxCarNavigationService("MapboxCarNotification") {
        MapboxCarNotification(
            { options.notificationOptions },
            carContext,
            IdleExtenderUpdater(carContext),
            FreeDriveExtenderUpdater(carContext),
            ActiveGuidanceExtenderUpdater(carContext),
        )
    }

    /**
     * Access to persistent storage. Throws an [IllegalStateException] when accessed before
     * the [lifecycle] is [Lifecycle.State.CREATED].
     */
    val mapboxCarStorage by mapboxCarService("MapboxCarStorage") {
        MapboxCarStorage(carContext)
    }

    /**
     * Control and access the route preview. It can be read at any time, and it is attached to
     * [MapboxNavigationApp] while the [lifecycle] is [Lifecycle.State.CREATED], so every screen
     * that requests a route preview shares one attachment. Do not register or unregister it with
     * [MapboxNavigationApp] yourself: unregistering it detaches it for every screen.
     */
    val routePreviewRequest = attachWhileCreated(CarRoutePreviewRequest(options))

    // This is internal because it surfaces search objects which will likely change.
    internal var geoDeeplinkPlacesProvider: PlacesListOnMapProvider? = null

    /**
     * Allows you to define values used by the Mapbox Android Auto Navigation SDK.
     */
    fun customize(action: MapboxCarOptions.Customization.() -> Unit) = apply {
        val customization = MapboxCarOptions.Customization().apply(action)
        options.applyCustomization(customization)
    }

    internal fun stopNavigationFromCarHost(mapboxNavigation: MapboxNavigation?) {
        mapboxNavigation?.setNavigationRoutes(emptyList())
        if (MapboxScreenManager.current()?.key != MapboxScreen.NAVIGATION) {
            MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE)
        }
    }
}
