package com.mapbox.navigation.core.trip.session

import android.location.Location
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.speed.model.SpeedLimit

/**
 * Provides information about the status of the enhanced location updates generated
 * by the map matching engine of the Navigation SDK.
 *
 * @param enhancedLocation the best possible location update, snapped to the route or map matched to the road if possible.
 * @param keyPoints a list (can be empty) of predicted location points leading up to the target update.
 * The last point on the list (if not empty) is always equal to [enhancedLocation].
 * @param isOffRoad whether the SDK thinks that the user is off road, based on the [offRoadProbability].
 * @param offRoadProbability probability that the user is off road.
 * @param isTeleport returns true if map matcher changed its opinion about most probable path on last update.
 * In practice it means we don't need to animate puck movement from previous to current location
 * and just do an immediate transition instead.
 * @param speedLimit current speed limit during free drive and active navigation session.
 * In order to receive the speed limit make sure you add annotationsList with
 * DirectionsCriteria.ANNOTATION_MAXSPEED annotation to the route request.
 * @param roadEdgeMatchProbability when map matcher snaps to a road, this is the confidence in the chosen edge from all nearest edges.
 * @param zLevel [Int] current Z-level. Can be used to build a route from a proper level of a road.
 * @param road Road can be used to get information about the [Road] including name, shield name and shield url.
 * @param isDegradedMapMatching whether map matching was running in "degraded" mode, i.e. can have worse quality(usually happens due to the lack of map data).
 * In practice "degraded" mode means raw location in free drive and worse off-route experience in case of route set.
 */
class LocationMatcherResult internal constructor(
    val enhancedLocation: Location,
    val keyPoints: List<Location>,
    val isOffRoad: Boolean,
    val offRoadProbability: Float,
    val isTeleport: Boolean,
    val speedLimit: SpeedLimit?,
    val roadEdgeMatchProbability: Float,
    val zLevel: Int?,
    val road: Road,
    val isDegradedMapMatching: Boolean
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationMatcherResult

        if (enhancedLocation != other.enhancedLocation) return false
        if (keyPoints != other.keyPoints) return false
        if (isOffRoad != other.isOffRoad) return false
        if (offRoadProbability != other.offRoadProbability) return false
        if (isTeleport != other.isTeleport) return false
        if (speedLimit != other.speedLimit) return false
        if (roadEdgeMatchProbability != other.roadEdgeMatchProbability) return false
        if (road != other.road) return false
        if (isDegradedMapMatching != other.isDegradedMapMatching) return false
        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = enhancedLocation.hashCode()
        result = 31 * result + keyPoints.hashCode()
        result = 31 * result + isOffRoad.hashCode()
        result = 31 * result + offRoadProbability.hashCode()
        result = 31 * result + isTeleport.hashCode()
        result = 31 * result + speedLimit.hashCode()
        result = 31 * result + roadEdgeMatchProbability.hashCode()
        result = 31 * result + road.hashCode()
        result = 31 * result + isDegradedMapMatching.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "LocationMatcherResult(enhancedLocation=$enhancedLocation, " +
            "keyPoints=$keyPoints, isOffRoad=$isOffRoad, offRoadProbability=$offRoadProbability, " +
            "isTeleport=$isTeleport, speedLimit=$speedLimit, " +
            "roadEdgeMatchProbability=$roadEdgeMatchProbability, road=$road, " +
            "isDegradedMapMatching=$isDegradedMapMatching)"
    }
}
