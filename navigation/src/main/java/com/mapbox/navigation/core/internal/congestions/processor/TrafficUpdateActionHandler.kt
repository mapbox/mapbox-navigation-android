package com.mapbox.navigation.core.internal.congestions.processor

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction

internal fun interface TrafficUpdateActionHandler<T : TrafficUpdateAction> {
    fun handleAction(
        action: T,
    ): NavigationRoute?
}
