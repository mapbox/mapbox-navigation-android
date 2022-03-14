package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.flowUiBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Coordinator for navigation guidance.
 * This is the top panel for a portrait view.
 */
internal class ManeuverCoordinator(
    private val navigationViewContext: DropInNavigationViewContext,
    guidanceLayout: ViewGroup
) : UICoordinator<ViewGroup>(guidanceLayout) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return navigationViewContext
            .viewModel
            .navigationStateViewModel
            .state
            .flatMapLatest { state ->
                navigationViewContext.flowUiBinder {
                    if (state == NavigationState.ActiveNavigation) it.maneuver else EmptyBinder()
                }
            }
    }
}
