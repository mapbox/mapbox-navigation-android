package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.speedlimit.SpeedLimitViewBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinator for showing speed limit.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class SpeedLimitCoordinator(
    val navigationViewContext: NavigationViewContext,
    speedLimitLayout: ViewGroup
) : UICoordinator<ViewGroup>(speedLimitLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return navigationViewContext.uiBinders.speedLimit.map {
            it ?: SpeedLimitViewBinder(navigationViewContext)
        }
    }
}
