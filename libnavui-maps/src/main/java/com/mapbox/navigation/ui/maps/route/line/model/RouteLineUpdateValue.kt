package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Represents data for updating the appearance of the route lines.
 *
 * @param primaryRouteLineDynamicData the data describing the primary route line
 * @param alternativeRouteLinesDynamicData the data describing alternative route lines
 * @param routeLineMaskingLayerDynamicData the data describing the masking layers
 */
class RouteLineUpdateValue internal constructor(
    val primaryRouteLineDynamicData: RouteLineDynamicData,
    val alternativeRouteLinesDynamicData: List<RouteLineDynamicData>,
    val routeLineMaskingLayerDynamicData: RouteLineDynamicData? = null
) {

    /**
     * @return a class with mutable values for replacing.
     */
    fun toMutableValue() = MutableRouteLineUpdateValue(
        primaryRouteLineDynamicData,
        alternativeRouteLinesDynamicData,
        routeLineMaskingLayerDynamicData
    )

    /**
     * Represents the mutable data for updating the appearance of the route lines.
     *
     * @param primaryRouteLineDynamicData the data describing the primary route line
     * @param alternativeRouteLinesDynamicData the data describing alternative route lines
     * @param routeLineMaskingLayerDynamicData the data describing the masking layers
     */
    class MutableRouteLineUpdateValue internal constructor(
        var primaryRouteLineDynamicData: RouteLineDynamicData,
        var alternativeRouteLinesDynamicData: List<RouteLineDynamicData>,
        var routeLineMaskingLayerDynamicData: RouteLineDynamicData? = null
    ) {

        /**
         * @return a RouteLineUpdateValue
         */
        fun toImmutableValue() = RouteLineUpdateValue(
            primaryRouteLineDynamicData,
            alternativeRouteLinesDynamicData,
            routeLineMaskingLayerDynamicData
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineUpdateValue

        if (primaryRouteLineDynamicData != other.primaryRouteLineDynamicData) return false
        if (alternativeRouteLinesDynamicData != other.alternativeRouteLinesDynamicData) return false
        if (routeLineMaskingLayerDynamicData != other.routeLineMaskingLayerDynamicData) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = primaryRouteLineDynamicData.hashCode()
        result = 31 * result + alternativeRouteLinesDynamicData.hashCode()
        result = 31 * result + routeLineMaskingLayerDynamicData.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineUpdateValue(" +
            "primaryRouteLineDynamicData=$primaryRouteLineDynamicData, " +
            "alternativeRouteLinesDynamicData=$alternativeRouteLinesDynamicData," +
            "routeLineMaskingLayerDynamicData=$routeLineMaskingLayerDynamicData" +
            ")"
    }
}
