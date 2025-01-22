package com.mapbox.navigation.ui.maps.route.line.model

import android.graphics.Color
import androidx.annotation.ColorInt
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData

/**
 * Contains data related to a route distance traveled and a traffic congestion color.
 */
internal class RouteLineExpressionData(
    offset: Double,
    val congestionValue: String,
    val segmentColorType: SegmentColorType,
    legIndex: Int,
) : ExpressionOffsetData(offset, legIndex) {

    override fun <T : ExpressionOffsetData> copyWithNewOffset(newOffset: Double): T {
        return RouteLineExpressionData(
            offset = newOffset,
            congestionValue = congestionValue,
            segmentColorType = segmentColorType,
            legIndex = legIndex,
        ) as T
    }

    fun copyWithNewSegmentColorType(
        newSegmentColorType: SegmentColorType,
    ): RouteLineExpressionData {
        return RouteLineExpressionData(
            offset = offset,
            congestionValue = congestionValue,
            segmentColorType = newSegmentColorType,
            legIndex = legIndex,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as RouteLineExpressionData

        if (congestionValue != other.congestionValue) return false
        if (segmentColorType != other.segmentColorType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + congestionValue.hashCode()
        result = 31 * result + segmentColorType.hashCode()
        return result
    }

    override fun toString(): String {
        return "RouteLineExpressionData(" +
            "congestionValue=$congestionValue, " +
            "segmentColorProvider=$segmentColorType, " +
            "offset=$offset, " +
            "legIndex=$legIndex" +
            ")"
    }
}

internal enum class SegmentColorType {
    PRIMARY_DEFAULT,
    PRIMARY_LOW_CONGESTION,
    PRIMARY_MODERATE_CONGESTION,
    PRIMARY_HEAVY_CONGESTION,
    PRIMARY_SEVERE_CONGESTION,
    PRIMARY_UNKNOWN_CONGESTION,
    PRIMARY_CASING,
    PRIMARY_CLOSURE,
    PRIMARY_RESTRICTED,
    TRAVELED,
    TRAVELED_CASING,

    INACTIVE_DEFAULT,
    INACTIVE_LOW_CONGESTION,
    INACTIVE_MODERATE_CONGESTION,
    INACTIVE_HEAVY_CONGESTION,
    INACTIVE_SEVERE_CONGESTION,
    INACTIVE_UNKNOWN_CONGESTION,
    INACTIVE_CASING,
    INACTIVE_CLOSURE,
    INACTIVE_RESTRICTED,

    ALTERNATIVE_DEFAULT,
    ALTERNATIVE_LOW_CONGESTION,
    ALTERNATIVE_MODERATE_CONGESTION,
    ALTERNATIVE_HEAVY_CONGESTION,
    ALTERNATIVE_SEVERE_CONGESTION,
    ALTERNATIVE_UNKNOWN_CONGESTION,
    ALTERNATIVE_CASING,
    ALTERNATIVE_CLOSURE,
    ALTERNATIVE_RESTRICTED,

    TRANSPARENT,

    ;

    @ColorInt
    fun getColor(data: RouteLineViewOptionsData): Int {
        val colorResources = data.routeLineColorResources
        return when (this) {
            PRIMARY_DEFAULT -> colorResources.routeDefaultColor
            PRIMARY_LOW_CONGESTION -> colorResources.routeLowCongestionColor
            PRIMARY_MODERATE_CONGESTION -> colorResources.routeModerateCongestionColor
            PRIMARY_HEAVY_CONGESTION -> colorResources.routeHeavyCongestionColor
            PRIMARY_SEVERE_CONGESTION -> colorResources.routeSevereCongestionColor
            PRIMARY_UNKNOWN_CONGESTION -> colorResources.routeUnknownCongestionColor
            PRIMARY_CASING -> colorResources.routeCasingColor
            PRIMARY_CLOSURE -> colorResources.routeClosureColor
            PRIMARY_RESTRICTED -> colorResources.restrictedRoadColor
            TRAVELED -> colorResources.routeLineTraveledColor
            TRAVELED_CASING -> colorResources.routeLineTraveledCasingColor
            INACTIVE_DEFAULT -> colorResources.inActiveRouteLegsColor
            INACTIVE_LOW_CONGESTION -> colorResources.inactiveRouteLegLowCongestionColor
            INACTIVE_MODERATE_CONGESTION -> colorResources.inactiveRouteLegModerateCongestionColor
            INACTIVE_HEAVY_CONGESTION -> colorResources.inactiveRouteLegHeavyCongestionColor
            INACTIVE_SEVERE_CONGESTION -> colorResources.inactiveRouteLegSevereCongestionColor
            INACTIVE_UNKNOWN_CONGESTION -> colorResources.inactiveRouteLegUnknownCongestionColor
            INACTIVE_CASING -> colorResources.inactiveRouteLegCasingColor
            INACTIVE_CLOSURE -> colorResources.inactiveRouteLegClosureColor
            INACTIVE_RESTRICTED -> colorResources.inactiveRouteLegRestrictedRoadColor
            ALTERNATIVE_DEFAULT -> colorResources.alternativeRouteDefaultColor
            ALTERNATIVE_LOW_CONGESTION -> colorResources.alternativeRouteLowCongestionColor
            ALTERNATIVE_MODERATE_CONGESTION ->
                colorResources.alternativeRouteModerateCongestionColor
            ALTERNATIVE_HEAVY_CONGESTION -> colorResources.alternativeRouteHeavyCongestionColor
            ALTERNATIVE_SEVERE_CONGESTION -> colorResources.alternativeRouteSevereCongestionColor
            ALTERNATIVE_UNKNOWN_CONGESTION -> colorResources.alternativeRouteUnknownCongestionColor
            ALTERNATIVE_CASING -> colorResources.alternativeRouteCasingColor
            ALTERNATIVE_CLOSURE -> colorResources.alternativeRouteClosureColor
            ALTERNATIVE_RESTRICTED -> colorResources.alternativeRouteRestrictedRoadColor
            TRANSPARENT -> Color.TRANSPARENT
        }
    }
}
