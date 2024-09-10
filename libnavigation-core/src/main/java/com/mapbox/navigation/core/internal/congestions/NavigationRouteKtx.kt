package com.mapbox.navigation.core.internal.congestions

import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.route.overriddenTraffic
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress

internal fun NavigationRoute.getOverriddenTrafficForProgress(
    legProgress: RouteLegProgress,
): CongestionNumericOverride? {
    return this.overriddenTraffic?.takeIf {
        it.legIndex == legProgress.legIndex &&
            legProgress.geometryIndex >= it.startIndex &&
            legProgress.geometryIndex < it.startIndex + it.length &&
            it.originalCongestionNumeric != null
    }
}
