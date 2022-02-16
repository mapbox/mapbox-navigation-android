package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.extensions.flowTripSessionState
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Coordinator for navigation guidance.
 * This is the top panel for a portrait view.
 */
internal class GuidanceCoordinator(
    private val navigationViewContext: DropInNavigationViewContext,
    guidanceLayout: ViewGroup
) : UICoordinator<ViewGroup>(guidanceLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return combine(
            flowTripSessionState(), flowRoutesUpdated().map { it.routes }
        ) { tripSessionState, routes ->
            when {
                tripSessionState == TripSessionState.STARTED && routes.isNotEmpty() -> {
                    navigationViewContext.uiBinders.maneuver
                }
                else -> {
                    EmptyBinder()
                }
            }
        }
    }
}
