package com.mapbox.navigation.core.internal.congestions.processor

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction
import com.mapbox.navigation.core.internal.congestions.speed.restoreTraffic

/**
 * Uses [TrafficUpdateAction.RestoreTraffic.congestionNumericOverride] to restore initial
 * congestion for the given [TrafficUpdateAction.RestoreTraffic.route]
 */
internal class RestoreTrafficUpdateActionHandler :
    TrafficUpdateActionHandler<TrafficUpdateAction.RestoreTraffic> {
    override fun handleAction(
        action: TrafficUpdateAction.RestoreTraffic,
    ): NavigationRoute {
        return restoreTraffic(
            action.route,
            action.congestionNumericOverride,
        )
    }
}
