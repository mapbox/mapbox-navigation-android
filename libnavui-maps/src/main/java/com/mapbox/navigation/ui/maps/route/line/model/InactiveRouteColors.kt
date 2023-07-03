package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.ColorInt

internal data class InactiveRouteColors(
    @ColorInt val inactiveRouteLegLowCongestionColor: Int,
    @ColorInt val inactiveRouteLegModerateCongestionColor: Int,
    @ColorInt val inactiveRouteLegHeavyCongestionColor: Int,
    @ColorInt val inactiveRouteLegSevereCongestionColor: Int,
    @ColorInt val inactiveRouteLegUnknownCongestionColor: Int,
    @ColorInt val inactiveRouteLegClosureColor: Int,
    @ColorInt val inactiveRouteLegRestrictedRoadColor: Int,
) {

    /**
     * Applies colors from [MapboxRouteLineOptions].
     */
    constructor(options: MapboxRouteLineOptions) : this(
        inactiveRouteLegLowCongestionColor =
        options.resourceProvider.routeLineColorResources.inactiveRouteLegLowCongestionColor,
        inactiveRouteLegModerateCongestionColor =
        options
            .resourceProvider.routeLineColorResources.inactiveRouteLegModerateCongestionColor,
        inactiveRouteLegHeavyCongestionColor =
        options.resourceProvider.routeLineColorResources.inactiveRouteLegHeavyCongestionColor,
        inactiveRouteLegSevereCongestionColor =
        options.resourceProvider.routeLineColorResources.inactiveRouteLegSevereCongestionColor,
        inactiveRouteLegUnknownCongestionColor =
        options.resourceProvider.routeLineColorResources.inactiveRouteLegUnknownCongestionColor,
        inactiveRouteLegClosureColor =
        options.resourceProvider.routeLineColorResources.inactiveRouteLegClosureColor,
        inactiveRouteLegRestrictedRoadColor =
        options.resourceProvider.routeLineColorResources.inactiveRouteLegRestrictedRoadColor,
    )

    /**
     * Applies the same color for all congestion levels.
     */
    constructor(@ColorInt color: Int) : this(
        inactiveRouteLegLowCongestionColor = color,
        inactiveRouteLegModerateCongestionColor = color,
        inactiveRouteLegHeavyCongestionColor = color,
        inactiveRouteLegSevereCongestionColor = color,
        inactiveRouteLegUnknownCongestionColor = color,
        inactiveRouteLegClosureColor = color,
        inactiveRouteLegRestrictedRoadColor = color,
    )
}
