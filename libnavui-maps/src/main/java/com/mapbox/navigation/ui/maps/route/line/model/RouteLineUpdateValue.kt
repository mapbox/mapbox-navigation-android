package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Represents data for updating the appearance of the route lines.
 *
 * @param primaryRouteLineDynamicData the data describing the primary route line
 * @param alternativeRouteLinesDynamicData the data describing alternative route lines
 */
class RouteLineUpdateValue internal constructor(
    val primaryRouteLineDynamicData: RouteLineDynamicData,
    val alternativeRouteLinesDynamicData: List<RouteLineDynamicData>
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineUpdateValue

        if (primaryRouteLineDynamicData != other.primaryRouteLineDynamicData) return false
        if (alternativeRouteLinesDynamicData != other.alternativeRouteLinesDynamicData) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = primaryRouteLineDynamicData.hashCode()
        result = 31 * result + alternativeRouteLinesDynamicData.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineUpdateValue(" +
            "primaryRouteLineDynamicData=$primaryRouteLineDynamicData, " +
            "alternativeRouteLinesDynamicData=$alternativeRouteLinesDynamicData" +
            ")"
    }
}
