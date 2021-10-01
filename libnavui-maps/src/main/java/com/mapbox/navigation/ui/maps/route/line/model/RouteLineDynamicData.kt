package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Provides information needed to draw a route.
 *
 * @param baseExpressionProvider expression used to style the base of the line
 * @param casingExpressionProvider expression used to style the case of the line
 * @param trafficExpressionProvider expression used to style the congestion colors on the line
 * @param restrictedSectionExpressionProvider expression used to style the restricted sections on the line
 */
class RouteLineDynamicData internal constructor(
    val baseExpressionProvider: RouteLineExpressionProvider,
    val casingExpressionProvider: RouteLineExpressionProvider,
    val trafficExpressionProvider: RouteLineExpressionProvider?,
    val restrictedSectionExpressionProvider: RouteLineExpressionProvider?
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineDynamicData

        if (baseExpressionProvider != other.baseExpressionProvider) return false
        if (casingExpressionProvider != other.casingExpressionProvider) return false
        if (trafficExpressionProvider != other.trafficExpressionProvider) return false
        if (restrictedSectionExpressionProvider != other.restrictedSectionExpressionProvider)
            return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = baseExpressionProvider.hashCode()
        result = 31 * result + casingExpressionProvider.hashCode()
        result = 31 * result + (trafficExpressionProvider?.hashCode() ?: 0)
        result = 31 * result + (restrictedSectionExpressionProvider?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineDynamicData(" +
            "baseExpressionProvider=$baseExpressionProvider, " +
            "casingExpressionProvider=$casingExpressionProvider, " +
            "trafficExpressionProvider=$trafficExpressionProvider, " +
            "restrictedSectionExpressionProvider=$restrictedSectionExpressionProvider" +
            ")"
    }
}
