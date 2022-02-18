package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Coordinator for the speed limit view.
 * This will include camera, location puck, and route line.
 */
internal class SpeedLimitCoordinator(
    navigationViewContext: DropInNavigationViewContext,
    speedLimitLayout: ViewGroup
) : UICoordinator<ViewGroup>(speedLimitLayout) {

    val stateFlow = MutableStateFlow(navigationViewContext.uiBinders.speedLimit)

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return stateFlow
    }
}
