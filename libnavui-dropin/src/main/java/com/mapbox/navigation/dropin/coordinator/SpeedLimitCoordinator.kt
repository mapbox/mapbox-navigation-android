package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.flowUiBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow

/**
 * Coordinator for the speed limit view.
 * This will include camera, location puck, and route line.
 */
internal class SpeedLimitCoordinator(
    val navigationViewContext: DropInNavigationViewContext,
    speedLimitLayout: ViewGroup
) : UICoordinator<ViewGroup>(speedLimitLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return navigationViewContext.flowUiBinder { it.speedLimit }
    }
}
