package com.mapbox.navigation.base.trip.model.eh

/**
 * Edge metadata
 *
 * @param heading heading when starting to move along the edge.
 * The value is in degrees in the range [0, 360)
 * @param length the Edge's length in meters
 * @param functionRoadClass the edge's [RoadClass]
 * @param speedLimit max speed of the edge (speed limit) in m/s
 * @param speed average speed along the edge in m/s
 * @param ramp is the edge a ramp?
 * @param motorway is the edge a motorway?
 * @param bridge is the edge a bridge?
 * @param tunnel is the edge a tunnel?
 * @param toll is the edge a toll road?
 * @param names an array of road names
 * @param laneCount the number of lanes on the edge (does not change mid-edge)
 * @param meanElevation mean elevation along the edge in meters
 * @param curvature binned number denoting the curvature degree of the edge (0-15)
 * @param countryCodeIso3 ISO 3166-1 alpha-3 country code
 * @param countryCodeIso2 the edge's country code (ISO-2 format)
 * @param stateCode a state inside a country (ISO 3166-2)
 * @param isRightHandTraffic true if in the current place/state right-hand traffic is used
 * @param isOneWay true if current edge is one-way.
 * false if left-hand.
 * @param roadSurface type of the road surface.
 */
class EHorizonEdgeMetadata internal constructor(
    val heading: Double,
    val length: Double,
    val functionRoadClass: String,
    val speedLimit: Double?,
    val speed: Double,
    val ramp: Boolean,
    val motorway: Boolean,
    val bridge: Boolean,
    val tunnel: Boolean,
    val toll: Boolean,
    val names: List<RoadName>,
    val laneCount: Byte?,
    val meanElevation: Double?,
    val curvature: Byte,
    val countryCodeIso3: String?,
    val countryCodeIso2: String?,
    val stateCode: String?,
    val isRightHandTraffic: Boolean,
    val isOneWay: Boolean,
    @RoadSurface.Type val roadSurface: String
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonEdgeMetadata

        if (heading != other.heading) return false
        if (length != other.length) return false
        if (functionRoadClass != other.functionRoadClass) return false
        if (speedLimit != other.speedLimit) return false
        if (speed != other.speed) return false
        if (ramp != other.ramp) return false
        if (motorway != other.motorway) return false
        if (bridge != other.bridge) return false
        if (tunnel != other.tunnel) return false
        if (toll != other.toll) return false
        if (names != other.names) return false
        if (laneCount != other.laneCount) return false
        if (meanElevation != other.meanElevation) return false
        if (curvature != other.curvature) return false
        if (countryCodeIso3 != other.countryCodeIso3) return false
        if (countryCodeIso2 != other.countryCodeIso2) return false
        if (stateCode != other.stateCode) return false
        if (isRightHandTraffic != other.isRightHandTraffic) return false
        if (isOneWay != other.isOneWay) return false
        if (roadSurface != other.roadSurface) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = heading.hashCode()
        result = 31 * result + length.hashCode()
        result = 31 * result + functionRoadClass.hashCode()
        result = 31 * result + (speedLimit?.hashCode() ?: 0)
        result = 31 * result + speed.hashCode()
        result = 31 * result + ramp.hashCode()
        result = 31 * result + motorway.hashCode()
        result = 31 * result + bridge.hashCode()
        result = 31 * result + tunnel.hashCode()
        result = 31 * result + toll.hashCode()
        result = 31 * result + names.hashCode()
        result = 31 * result + (laneCount ?: 0)
        result = 31 * result + (meanElevation?.hashCode() ?: 0)
        result = 31 * result + curvature
        result = 31 * result + (countryCodeIso3?.hashCode() ?: 0)
        result = 31 * result + (countryCodeIso2?.hashCode() ?: 0)
        result = 31 * result + (stateCode?.hashCode() ?: 0)
        result = 31 * result + isRightHandTraffic.hashCode()
        result = 31 * result + isOneWay.hashCode()
        result = 31 * result + roadSurface.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonEdgeMetadata(" +
            "heading=$heading, " +
            "length=$length, " +
            "functionRoadClass='$functionRoadClass', " +
            "speedLimit=$speedLimit, " +
            "speed=$speed, " +
            "ramp=$ramp, " +
            "motorway=$motorway, " +
            "bridge=$bridge, " +
            "tunnel=$tunnel, " +
            "toll=$toll, " +
            "names=$names, " +
            "laneCount=$laneCount, " +
            "meanElevation=$meanElevation, " +
            "curvature=$curvature, " +
            "countryCodeIso3=$countryCodeIso3, " +
            "countryCodeIso2=$countryCodeIso2, " +
            "stateCode=$stateCode, " +
            "isRightHandTraffic=$isRightHandTraffic, " +
            "isOneWay=$isOneWay, " +
            "roadSurface=$roadSurface" +
            ")"
    }
}
