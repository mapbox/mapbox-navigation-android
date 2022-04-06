package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.maneuver.ManeuverViewBinder
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Coordinator for navigation guidance.
 * This is the top panel for a portrait view.
 */
internal class ManeuverCoordinator(
    private val context: NavigationViewContext,
    guidanceLayout: ViewGroup
) : UICoordinator<ViewGroup>(guidanceLayout) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return context
            .viewModel
            .navigationStateViewModel
            .state
            .flatMapLatest { state ->
                context.uiBinders.maneuver.map {
                    if (state == NavigationState.ActiveNavigation) {
                        it ?: ManeuverViewBinder(context.mapStyleLoader.loadedMapStyle)
                    } else {
                        EmptyBinder()
                    }
                }
            }
    }
}
