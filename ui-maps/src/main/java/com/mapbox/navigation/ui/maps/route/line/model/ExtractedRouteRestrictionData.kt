package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Represents restricted route data extracted from a [DirectionsRoute]
 *
 * @param isInRestrictedSection if true this section of the route is designated as restricted.
 */
internal class ExtractedRouteRestrictionData(
    offset: Double,
    val isInRestrictedSection: Boolean = false,
    legIndex: Int = 0,
) : ExpressionOffsetData(offset, legIndex) {

    override fun <T : ExpressionOffsetData> copyWithNewOffset(newOffset: Double): T {
        return ExtractedRouteRestrictionData(
            offset = newOffset,
            isInRestrictedSection = isInRestrictedSection,
            legIndex = legIndex,
        ) as T
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as ExtractedRouteRestrictionData

        if (isInRestrictedSection != other.isInRestrictedSection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isInRestrictedSection.hashCode()
        return result
    }

    override fun toString(): String {
        return "ExtractedRouteRestrictionData(" +
            "isInRestrictedSection=$isInRestrictedSection, " +
            "offset=$offset, " +
            "legIndex=$legIndex" +
            ")"
    }
}
