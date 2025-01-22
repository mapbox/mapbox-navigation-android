package com.mapbox.navigation.core.internal.congestions.processor

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.congestions.model.CongestionRangeGroup
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction

internal class CommonTrafficUpdateActionHandler(
    private val congestionRangeGroup: CongestionRangeGroup,
    private val increaseTrafficHandler:
        TrafficUpdateActionHandler<TrafficUpdateAction.IncreaseTraffic> =
            IncreaseTrafficUpdateActionHandler(congestionRangeGroup),
    private val decreaseTrafficHandler:
        TrafficUpdateActionHandler<TrafficUpdateAction.DecreaseTraffic> =
            DecreaseTrafficUpdateActionHandler(congestionRangeGroup),
    private val restoreTrafficHandler:
        TrafficUpdateActionHandler<TrafficUpdateAction.RestoreTraffic> =
            RestoreTrafficUpdateActionHandler(),
) : TrafficUpdateActionHandler<TrafficUpdateAction> {
    override fun handleAction(
        action: TrafficUpdateAction,
    ): NavigationRoute? {
        return when (action) {
            is TrafficUpdateAction.DecreaseTraffic ->
                decreaseTrafficHandler.handleAction(action)

            is TrafficUpdateAction.IncreaseTraffic ->
                increaseTrafficHandler.handleAction(action)

            is TrafficUpdateAction.RestoreTraffic ->
                restoreTrafficHandler.handleAction(action)

            is TrafficUpdateAction.AccumulatingLowSpeed,
            TrafficUpdateAction.NoAction,
            -> {
                null
            }
        }
    }
}
