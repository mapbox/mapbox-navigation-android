package com.mapbox.navigation.ui.maps.route.line.model

internal data class InactiveRouteColors(
    val inactiveRouteLegLowCongestionColorType: SegmentColorType,
    val inactiveRouteLegModerateCongestionColorType: SegmentColorType,
    val inactiveRouteLegHeavyCongestionColorType: SegmentColorType,
    val inactiveRouteLegSevereCongestionColorType: SegmentColorType,
    val inactiveRouteLegUnknownCongestionColorType: SegmentColorType,
    val inactiveRouteLegClosureColorType: SegmentColorType,
    val inactiveRouteLegRestrictedRoadColorType: SegmentColorType,
) {

    /**
     * Applies colors from [MapboxRouteLineOptions].
     */
    constructor() : this(
        inactiveRouteLegLowCongestionColorType =
        SegmentColorType.INACTIVE_LOW_CONGESTION,
        inactiveRouteLegModerateCongestionColorType =
        SegmentColorType.INACTIVE_MODERATE_CONGESTION,
        inactiveRouteLegHeavyCongestionColorType =
        SegmentColorType.INACTIVE_HEAVY_CONGESTION,
        inactiveRouteLegSevereCongestionColorType =
        SegmentColorType.INACTIVE_SEVERE_CONGESTION,
        inactiveRouteLegUnknownCongestionColorType =
        SegmentColorType.INACTIVE_UNKNOWN_CONGESTION,
        inactiveRouteLegClosureColorType =
        SegmentColorType.INACTIVE_CLOSURE,
        inactiveRouteLegRestrictedRoadColorType =
        SegmentColorType.INACTIVE_RESTRICTED,
    )

    /**
     * Applies the same color for all congestion levels.
     */
    constructor(colorType: SegmentColorType) : this(
        inactiveRouteLegLowCongestionColorType = colorType,
        inactiveRouteLegModerateCongestionColorType = colorType,
        inactiveRouteLegHeavyCongestionColorType = colorType,
        inactiveRouteLegSevereCongestionColorType = colorType,
        inactiveRouteLegUnknownCongestionColorType = colorType,
        inactiveRouteLegClosureColorType = colorType,
        inactiveRouteLegRestrictedRoadColorType = colorType,
    )
}
