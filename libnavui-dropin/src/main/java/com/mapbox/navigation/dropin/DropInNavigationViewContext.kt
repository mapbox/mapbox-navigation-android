package com.mapbox.navigation.dropin

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationState
import com.mapbox.navigation.dropin.component.marker.MapMarkerFactory
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import com.mapbox.navigation.dropin.util.BitmapMemoryCache
import com.mapbox.navigation.dropin.util.BitmapMemoryCache.Companion.MB_IN_BYTES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * This context is a top level data object for [DropInNavigationView].
 *
 * If your data should survive orientation changes, place it inside [DropInNavigationViewModel].
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DropInNavigationViewContext(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val viewModel: DropInNavigationViewModel,
) {
    val uiBinders = NavigationUIBinders()
    val options = NavigationViewOptions(context)

    val dispatch: (action: Any) -> Unit = { action ->
        when (action) {
            is RoutesAction -> viewModel.routesViewModel.invoke(action)
            is DestinationAction -> viewModel.destinationViewModel.invoke(action)
        }
    }

    val destinationState: StateFlow<DestinationState> get() = viewModel.destinationViewModel.state

    //region Builders & Factories

    fun mapAnnotationFactory() = MapMarkerFactory(
        context,
        BitmapMemoryCache(4 * MB_IN_BYTES)
    )

    //endregion

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
internal fun <T : UIBinder> DropInNavigationViewContext.flowUiBinder(
    selector: (value: NavigationUIBinders) -> StateFlow<T>,
    mapper: suspend (value: T) -> T = { it }
): Flow<T> {
    return selector(this.uiBinders).map(mapper)
}
