package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.navigation.base.trip.model.eh.EHorizon
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfoType

/**
 * Holds the road objects and the distance to the start of the object.
 * There are two sources of road objects: active route and the electronic horizon.
 * Road objects coming from the electronic horizon will also have [RoadObjectDistanceInfo]
 * Available [RoadObjectDistanceInfo] types are:
 * - [RoadObjectDistanceInfoType.GANTRY]
 * - [RoadObjectDistanceInfoType.LINE]
 * - [RoadObjectDistanceInfoType.POINT]
 * - [RoadObjectDistanceInfoType.POLYGON]
 * - [RoadObjectDistanceInfoType.SUB_GRAPH]
 *
 * @param roadObjectProvider initializer function for the [roadObject] property
 * @param distanceToStartProvider initializer function for the [distanceToStart] property
 * @param distanceInfoProvider initializer function for the [distanceInfo] property
 */
class UpcomingRoadObject internal constructor(
    roadObjectProvider: () -> RoadObject,
    distanceToStartProvider: () -> Double?,
    distanceInfoProvider: () -> RoadObjectDistanceInfo?,
) {
    /**
     * The Road Object instance.
     */
    val roadObject: RoadObject by lazy { roadObjectProvider() }

    /**
     * Remaining distance to the start of the object.
     *
     * This value will be negative after passing the start of the object and until we cross the finish
     * point of the [RoadObject]s geometry for objects that are on the actively navigated route,
     * but it will be zero for [EHorizon] objects. It will be null if couldn't be determined.
     */
    val distanceToStart: Double? by lazy { distanceToStartProvider() }

    /**
     * Provides extra distance details for the road objects. It will be non-null
     * for objects coming from the electronic horizon and null for objects that are on the current route
     * that we are actively navigating on.
     */
    val distanceInfo: RoadObjectDistanceInfo? by lazy { distanceInfoProvider() }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpcomingRoadObject

        if (roadObject != other.roadObject) return false
        if (distanceToStart != other.distanceToStart) return false
        if (distanceInfo != other.distanceInfo) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = roadObject.hashCode()
        result = 31 * result + distanceToStart.hashCode()
        result = 31 * result + distanceInfo.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "UpcomingRoadObject(" +
            "roadObject=$roadObject, " +
            "distanceToStart=$distanceToStart, " +
            "distanceInfo=$distanceInfo" +
            ")"
    }
}
