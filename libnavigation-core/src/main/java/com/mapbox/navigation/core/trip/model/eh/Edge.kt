package com.mapbox.navigation.core.trip.model.eh

import com.mapbox.geojson.LineString

/**
 * Basic Edge class
 *
 * @param id identifier of the directed edge (it's unique)
 * @param level the level of the Edge (0 being the mpp, 1 branches of the mpp,
 * 2 branches of level 1 branches, etc)
 * @param probability the probability for this edge in percentage to be taken by the driver.
 * The probabilities of all outgoing edges on a single intersection sum up to 1.
 * @param heading heading when starting to move along the edge.
 * The value is in degrees in the range [0, 360)
 * @param length the Edge's length in meters
 * @param out the outgoing Edges
 * @param parent the parent Edge
 * @param functionRoadClass the edge's [FunctionalRoadClass]
 * @param speed average speed along the edge in m/s
 * @param ramp is the edge a ramp?
 * @param motorway is the edge a motorway?
 * @param bridge is the edge a bridge?
 * @param tunnel is the edge a tunnel?
 * @param toll is the edge a toll road?
 * @param names an array of road names
 * @param curvature binned number denoting the curvature degree of the edge (0-15)
 * @param geometry optional geometry if requested
 * @param speedLimit max speed of the edge (speed limit) in m/s
 * @param laneCount the number of lanes on the edge (does not change mid-edge)
 * @param meanElevation mean elevation along the edge in meters
 * @param countryCode ISO 3166-1 alpha-3 country code
 * @param stateCode a state inside a country (ISO 3166-2)
 */
class Edge internal constructor(
    val id: Long,
    val level: Byte,
    val probability: Double,
    val heading: Double,
    val length: Double,
    val out: List<Edge>,
    var parent: Edge?,
    val functionRoadClass: String,
    val speed: Double,
    val ramp: Boolean,
    val motorway: Boolean,
    val bridge: Boolean,
    val tunnel: Boolean,
    val toll: Boolean,
    val names: List<NameInfo>,
    val curvature: Byte,
    val geometry: LineString?,
    val speedLimit: Double?,
    val laneCount: Byte?,
    val meanElevation: Double?,
    val countryCode: String?,
    val stateCode: String?
) {

    /**
     * @return true if the Edge is the most probable path (MPP), false if not
     */
    fun isMpp(): Boolean {
        return level == 0.toByte()
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Edge

        if (id != other.id) return false
        if (level != other.level) return false
        if (probability != other.probability) return false
        if (heading != other.heading) return false
        if (length != other.length) return false
        if (out != other.out) return false
        if (parent != other.parent) return false
        if (functionRoadClass != other.functionRoadClass) return false
        if (speed != other.speed) return false
        if (ramp != other.ramp) return false
        if (motorway != other.motorway) return false
        if (bridge != other.bridge) return false
        if (tunnel != other.tunnel) return false
        if (toll != other.toll) return false
        if (names != other.names) return false
        if (curvature != other.curvature) return false
        if (geometry != other.geometry) return false
        if (speedLimit != other.speedLimit) return false
        if (laneCount != other.laneCount) return false
        if (meanElevation != other.meanElevation) return false
        if (countryCode != other.countryCode) return false
        if (stateCode != other.stateCode) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + level.hashCode()
        result = 31 * result + probability.hashCode()
        result = 31 * result + heading.hashCode()
        result = 31 * result + length.hashCode()
        result = 31 * result + out.hashCode()
        result = 31 * result + parent.hashCode()
        result = 31 * result + functionRoadClass.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + ramp.hashCode()
        result = 31 * result + motorway.hashCode()
        result = 31 * result + bridge.hashCode()
        result = 31 * result + tunnel.hashCode()
        result = 31 * result + toll.hashCode()
        result = 31 * result + names.hashCode()
        result = 31 * result + curvature.hashCode()
        result = 31 * result + geometry.hashCode()
        result = 31 * result + speedLimit.hashCode()
        result = 31 * result + laneCount.hashCode()
        result = 31 * result + meanElevation.hashCode()
        result = 31 * result + countryCode.hashCode()
        result = 31 * result + stateCode.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Edge(" +
            "id=$id, " +
            "level=$level, " +
            "probability=$probability, " +
            "heading=$heading, " +
            "length=$length, " +
            "out=$out, " +
            "parent=${parent?.id}, " +
            "functionRoadClass=$functionRoadClass, " +
            "speed=$speed, " +
            "ramp=$ramp, " +
            "motorway=$motorway, " +
            "bridge=$bridge, " +
            "tunnel=$tunnel, " +
            "toll=$toll, " +
            "names=$names, " +
            "curvature=$curvature, " +
            "geometry=$geometry, " +
            "speedLimit=$speedLimit, " +
            "laneCount=$laneCount, " +
            "meanElevation=$meanElevation, " +
            "countryCode=$countryCode, " +
            "stateCode=$stateCode" +
            ")"
    }
}
