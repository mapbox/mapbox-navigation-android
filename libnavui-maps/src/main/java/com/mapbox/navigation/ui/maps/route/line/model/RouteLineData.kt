package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection

/**
 * Provides information needed to draw a route.
 *
 * @param featureCollection the routes geometry
 * @param dynamicData dynamic data to style the route line
 */
class RouteLineData internal constructor(
    val featureCollection: FeatureCollection,
    val dynamicData: RouteLineDynamicData,
    internal var copy: RouteLineDataCopy? = null
) {

    /**
     * @return a class with mutable values for replacing.
     */
    fun toMutableValue() = MutableRouteLineData(
        featureCollection,
        dynamicData
    )

    /**
     * Provides mutable information needed to draw a route.
     *
     * @param featureCollection the routes geometry
     * @param dynamicData dynamic data to style the route line
     */
    class MutableRouteLineData internal constructor(
        var featureCollection: FeatureCollection,
        var dynamicData: RouteLineDynamicData
    ) {

        /**
         * @return a RouteLineData
         */
        fun toImmutableValue() = RouteLineData(
            featureCollection,
            dynamicData
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineData

        if (featureCollection != other.featureCollection) return false
        if (dynamicData != other.dynamicData) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = featureCollection.hashCode()
        result = 31 * result + dynamicData.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineData(" +
            "featureCollection=$featureCollection, " +
            "dynamicData=$dynamicData" +
            ")"
    }
}
