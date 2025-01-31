package com.mapbox.navigation.base.trip.model.eh

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * The record represents a piece of data which is required to match one OpenLR.
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
 *
 * @param roadObjectId unique id of the object
 * @param openLRLocation road object location
 * @param openLRStandard standard used to encode openLRLocation
 */
class MatchableOpenLr(
    val roadObjectId: String,
    val openLRLocation: String,
    @OpenLRStandard.Type val openLRStandard: String,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MatchableOpenLr

        if (roadObjectId != other.roadObjectId) return false
        if (openLRLocation != other.openLRLocation) return false
        return openLRStandard == other.openLRStandard
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = roadObjectId.hashCode()
        result = 31 * result + openLRLocation.hashCode()
        result = 31 * result + openLRStandard.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MatchableOpenLr(" +
            "roadObjectId='$roadObjectId', " +
            "openLRLocation='$openLRLocation', " +
            "openLRStandard='$openLRStandard'" +
            ")"
    }
}

/**
 * The record represents the raw data which could be matched to the road graph.
 * Might be used to match:
 * - gantries, with exactly two coordinates
 * - lines, with two or more coordinates
 * - polygons, with three or more coordinates
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
 *
 * @param roadObjectId unique id of the object
 * @param coordinates list of points representing the geometry
 */
class MatchableGeometry(
    val roadObjectId: String,
    val coordinates: List<Point>,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MatchableGeometry

        if (roadObjectId != other.roadObjectId) return false
        return coordinates == other.coordinates
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = roadObjectId.hashCode()
        result = 31 * result + coordinates.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MatchableGeometry(roadObjectId='$roadObjectId', coordinates=$coordinates)"
    }
}

/**
 * The record represents a raw point which could be matched to the road graph.
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
 *
 * @param roadObjectId unique id of the object
 * @param point point representing the object
 * @param bearing optional bearing in degrees from the North.
 * Describes the direction of riding for the edge where provided point is going to be matched.
 */
class MatchablePoint(
    val roadObjectId: String,
    val point: Point,
    val bearing: Double? = null,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MatchablePoint

        if (roadObjectId != other.roadObjectId) return false
        if (point != other.point) return false
        return bearing.safeCompareTo(other.bearing)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = roadObjectId.hashCode()
        result = 31 * result + point.hashCode()
        result = 31 * result + (bearing?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MatchablePoint(roadObjectId='$roadObjectId', point=$point, bearing=$bearing)"
    }
}
