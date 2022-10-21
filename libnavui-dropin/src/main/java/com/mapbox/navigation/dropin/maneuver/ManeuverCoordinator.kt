package com.mapbox.navigation.dropin.maneuver

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.EmptyBinder
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

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

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return combine(
            context.options.showManeuver,
            context.uiBinders.maneuver,
            store.select { it.navigation }
        ) { show, binder, navigationState ->
            if (show && navigationState == NavigationState.ActiveNavigation) {
                binder ?: ManeuverViewBinder(context)
            } else {
                EmptyBinder()
            }
        }
    }
}
