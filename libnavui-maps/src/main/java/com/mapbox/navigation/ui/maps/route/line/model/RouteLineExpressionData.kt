package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.ColorInt

/**
 * Contains data related to a route distance traveled and a traffic congestion color.
 *
 * @param segmentColor a color for a segment of a route line
 */
internal class RouteLineExpressionData(
    offset: Double,
    val congestionValue: String,
    @ColorInt val segmentColor: Int,
    legIndex: Int,
) : ExpressionOffsetData(offset, legIndex) {

    override fun <T : ExpressionOffsetData> copyWithNewOffset(newOffset: Double): T {
        return RouteLineExpressionData(
            offset = newOffset,
            congestionValue = congestionValue,
            segmentColor = segmentColor,
            legIndex = legIndex,
        ) as T
    }

    fun copyWithNewSegmentColor(newSegmentColor: Int): RouteLineExpressionData {
        return RouteLineExpressionData(
            offset = offset,
            congestionValue = congestionValue,
            segmentColor = newSegmentColor,
            legIndex = legIndex,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as RouteLineExpressionData

        if (congestionValue != other.congestionValue) return false
        if (segmentColor != other.segmentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + congestionValue.hashCode()
        result = 31 * result + segmentColor
        return result
    }

    override fun toString(): String {
        return "RouteLineExpressionData(" +
            "congestionValue=$congestionValue, " +
            "segmentColor=$segmentColor, " +
            "offset=$offset, " +
            "legIndex=$legIndex" +
            ")"
    }
}
