package com.mapbox.navigation.dropin.navigationview

import android.content.Context
import androidx.core.graphics.Insets
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.dropin.ViewBinderCustomization
import com.mapbox.navigation.dropin.ViewOptionsCustomization
import com.mapbox.navigation.dropin.ViewStyleCustomization
import com.mapbox.navigation.dropin.infopanel.InfoPanelBehavior
import com.mapbox.navigation.dropin.maneuver.ManeuverBehavior
import com.mapbox.navigation.dropin.map.MapClickBehavior
import com.mapbox.navigation.dropin.map.MapStyleLoader
import com.mapbox.navigation.dropin.map.MapViewOwner
import com.mapbox.navigation.dropin.map.marker.MapMarkerFactory
import com.mapbox.navigation.dropin.speedlimit.SpeedInfoBehavior
import com.mapbox.navigation.dropin.util.BitmapMemoryCache
import com.mapbox.navigation.dropin.util.BitmapMemoryCache.Companion.MB_IN_BYTES
import com.mapbox.navigation.ui.app.internal.SharedApp
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.routefetch.RouteOptionsProvider
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.utils.internal.Provider
import com.mapbox.navigation.ui.utils.internal.getValue
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This context is a top level data object for [NavigationView] which gives access to [context]
 * and [NavigationViewModel]. The lifecycle of this object is attached to the lifecycle of
 * [NavigationView] and will destroy and recreate upon configuration changes.
 *
 * If your data should survive configuration changes, place it inside
 * [NavigationViewModel].
 */
internal class NavigationViewContext(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val viewModel: NavigationViewModel,
    storeProvider: Provider<Store> = Provider { SharedApp.store }
) {
    val store by storeProvider

    val systemBarsInsets = MutableStateFlow(Insets.NONE)

    val uiBinders = NavigationViewBinder()
    val styles = NavigationViewStyles(context)
    val options = NavigationViewOptions(context)
    val maneuverBehavior = ManeuverBehavior()
    val infoPanelBehavior = InfoPanelBehavior()
    val mapViewOwner = MapViewOwner()
    val mapStyleLoader = MapStyleLoader(context, options)
    val mapClickBehavior = MapClickBehavior()
    val speedInfoBehavior = SpeedInfoBehavior()
    val listenerRegistry by lazy {
        NavigationViewListenerRegistry(
            store,
            maneuverBehavior,
            infoPanelBehavior,
            mapClickBehavior,
            speedInfoBehavior,
            lifecycleOwner.lifecycleScope
        )
    }
    val locationProvider = NavigationLocationProvider()
    val routeOptionsProvider = RouteOptionsProvider()

    fun mapAnnotationFactory() = MapMarkerFactory(
        context,
        BitmapMemoryCache(4 * MB_IN_BYTES)
    )

    fun applyBinderCustomization(action: ViewBinderCustomization.() -> Unit) {
        val customization = ViewBinderCustomization(this).apply(action)
        uiBinders.applyCustomization(customization)
    }

    fun applyStyleCustomization(action: ViewStyleCustomization.() -> Unit) {
        val customization = ViewStyleCustomization().apply(action)
        styles.applyCustomization(customization)
    }

    fun applyOptionsCustomization(action: ViewOptionsCustomization.() -> Unit) {
        val customization = ViewOptionsCustomization().apply(action)
        options.applyCustomization(customization)
    }
}
