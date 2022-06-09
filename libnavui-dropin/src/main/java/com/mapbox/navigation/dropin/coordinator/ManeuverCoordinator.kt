package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.dropin.component.maneuver.ManeuverViewBinder
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Coordinator for navigation guidance.
 * This is the top panel for a portrait view.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class ManeuverCoordinator(
    private val context: NavigationViewContext,
    guidanceLayout: ViewGroup
) : UICoordinator<ViewGroup>(guidanceLayout) {
    private val store = context.store

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return store.select { it.navigation }
            .flatMapLatest { navigationState ->
                context.uiBinders.maneuver.map {
                    if (navigationState == NavigationState.ActiveNavigation) {
                        it ?: ManeuverViewBinder(
                            context.mapStyleLoader.loadedMapStyle,
                            context.styles.maneuverViewOptions,
                        )
                    } else {
                        EmptyBinder()
                    }
                }
            }
    }
}
