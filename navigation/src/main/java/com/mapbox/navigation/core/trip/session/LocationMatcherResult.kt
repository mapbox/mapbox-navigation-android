package com.mapbox.navigation.core.trip.session

import com.mapbox.common.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.speed.model.SpeedLimitInfo
import com.mapbox.navigation.core.trip.session.location.CorrectedLocationData

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
 * @param speedLimitInfo current speed limit during free drive and active navigation session.
 * In order to receive the speed limit make sure you add annotationsList with
 * DirectionsCriteria.ANNOTATION_MAXSPEED annotation to the route request.
 * @param roadEdgeMatchProbability when map matcher snaps to a road, this is the confidence in the chosen edge from all nearest edges.
 * @param zLevel [Int] current Z-level. Can be used to build a route from a proper level of a road.
 * @param road Road can be used to get information about the [Road] including name, shield name and shield url.
 * @param isDegradedMapMatching whether map matching was running in "degraded" mode, i.e. can have worse quality(usually happens due to the lack of map data).
 * @param inTunnel value indicating whether the current location is in a tunnel.
 * In practice "degraded" mode means raw location in free drive and worse off-route experience in case of route set.
 * @param correctedLocationData corrected GPS location data, the result of corrections applied
 * to the input location, if any. Users still need to use [enhancedLocation].
 * @param isAdasDataAvailable flag indicating whether ADAS data available for the current location:
 * - Null if ADAS cache is OFF, e. g. neither ADASIS nor EH enabled
 * - True if ADAS tiles are loaded for the current location
 * - False if ADAS cache is ON, but no tiles around
 */
@OptIn(ExperimentalMapboxNavigationAPI::class)
class LocationMatcherResult internal constructor(
    val enhancedLocation: Location,
    val keyPoints: List<Location>,
    val isOffRoad: Boolean,
    val offRoadProbability: Float,
    val isTeleport: Boolean,
    val speedLimitInfo: SpeedLimitInfo,
    val roadEdgeMatchProbability: Float,
    val zLevel: Int?,
    val road: Road,
    val isDegradedMapMatching: Boolean,
    val inTunnel: Boolean,
    @ExperimentalMapboxNavigationAPI
    val correctedLocationData: CorrectedLocationData?,
    @ExperimentalMapboxNavigationAPI
    val isAdasDataAvailable: Boolean?,
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
        if (speedLimitInfo != other.speedLimitInfo) return false
        if (roadEdgeMatchProbability != other.roadEdgeMatchProbability) return false
        if (road != other.road) return false
        if (isDegradedMapMatching != other.isDegradedMapMatching) return false
        if (inTunnel != other.inTunnel) return false
        if (correctedLocationData != other.correctedLocationData) return false
        if (isAdasDataAvailable != other.isAdasDataAvailable) return false
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
        result = 31 * result + speedLimitInfo.hashCode()
        result = 31 * result + roadEdgeMatchProbability.hashCode()
        result = 31 * result + road.hashCode()
        result = 31 * result + isDegradedMapMatching.hashCode()
        result = 31 * result + inTunnel.hashCode()
        result = 31 * result + correctedLocationData.hashCode()
        result = 31 * result + isAdasDataAvailable.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "LocationMatcherResult(enhancedLocation=$enhancedLocation, " +
            "keyPoints=$keyPoints, isOffRoad=$isOffRoad, offRoadProbability=$offRoadProbability, " +
            "isTeleport=$isTeleport, speedLimitInfo=$speedLimitInfo, " +
            "roadEdgeMatchProbability=$roadEdgeMatchProbability, road=$road, " +
            "isDegradedMapMatching=$isDegradedMapMatching, " +
            "inTunnel=$inTunnel, " +
            "correctedLocationData=$correctedLocationData, " +
            "isAdasDataAvailable=$isAdasDataAvailable" +
            ")"
    }
}
