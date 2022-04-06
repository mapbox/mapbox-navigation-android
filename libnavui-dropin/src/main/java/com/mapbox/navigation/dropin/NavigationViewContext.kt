package com.mapbox.navigation.dropin

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapView
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationState
import com.mapbox.navigation.dropin.component.marker.MapMarkerFactory
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import com.mapbox.navigation.dropin.util.BitmapMemoryCache
import com.mapbox.navigation.dropin.util.BitmapMemoryCache.Companion.MB_IN_BYTES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

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
) {
    val mapView = MutableStateFlow<MapView?>(null)

    val uiBinders = ViewBinder()
    val options = NavigationViewOptions(context)

    val dispatch: (action: Any) -> Unit = { action ->
        when (action) {
            is RoutesAction -> viewModel.routesViewModel.invoke(action)
            is DestinationAction -> viewModel.destinationViewModel.invoke(action)
        }
    }

    val destinationState: StateFlow<DestinationState> get() = viewModel.destinationViewModel.state

    val mapStyleLoader = MapStyleLoader(context, options)

    fun mapAnnotationFactory() = MapMarkerFactory(
        context,
        BitmapMemoryCache(4 * MB_IN_BYTES)
    )

    fun applyBinderCustomization(action: ViewBinderCustomization.() -> Unit) {
        val customization = ViewBinderCustomization().apply(action)
        uiBinders.applyCustomization(customization)
    }

    fun applyOptionsCustomization(action: ViewOptionsCustomization.() -> Unit) {
        val customization = ViewOptionsCustomization().apply(action)
        options.applyCustomization(customization)
    }
}

/**
 * Helper extension to map [UIBinder] inside a [UICoordinator].
 * Uses a distinct by class to prevent refreshing views of the same type of [UIBinder].
 */
internal fun <T : UIBinder> NavigationViewContext.flowUiBinder(
    selector: (value: ViewBinder) -> StateFlow<T>,
    mapper: suspend (value: T) -> T = { it }
): Flow<T> {
    return selector(this.uiBinders).map(mapper)
}
