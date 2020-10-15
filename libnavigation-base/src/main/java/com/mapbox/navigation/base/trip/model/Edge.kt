package com.mapbox.navigation.base.trip.model

import com.mapbox.geojson.LineString

/**
 * Basic Edge class
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * @param id the Edge identifier
 * @param level the level of the Edge (0 being the mpp, 1 branches of the mpp,
 * 2 branches of level 1 branches, etc)
 * @param probability the probability of the Edge in percentage
 * @param heading the bearing in degrees clockwise at the start of the edge
 * @param length the Edge's length in meters
 * @param out the outgoing Edges
 * @param frc the edge's functional road class
 * @param wayId the edge's way ID
 * @param positiveDirection specifies if the geometry is in the forward or
 * backwards direction
 * @param speed the Edge's average speed
 * @param ramp is the edge a ramp?
 * @param motorway is the edge a motorway?
 * @param bridge is the edge a bridge?
 * @param tunnel is the edge a tunnel?
 * @param toll is the edge a toll road?
 * @param names the edge's names
 * @param curvature the edge's curvature
 * @param geometry optional geometry if requested
 * @param speedLimit the Edge's max speed
 * @param laneCount the edge's lane counts
 * @param meanElevation the edge's mean elevation
 * @param countryCode the edge's country code
 * @param stateCode the edge's state code
 */
class Edge private constructor(
    val id: Long,
    val level: Byte,
    val probability: Double,
    val heading: Double,
    val length: Double,
    val out: List<Edge>,
    val frc: String,
    val wayId: String,
    val positiveDirection: Boolean,
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
     * For more control on how to visit the Edge tree, a visitor inheriting from [EdgeVisitor]
     * may be used.
     */
    fun <R> accept(visitor: EdgeVisitor<R>): R {
        return visitor.visit(this)
    }

    /**
     * When starting from an arbitrary Edge, collecting edges that match a certain criterium is
     * aided by a helper function on Edge that takes a lambda as a filter and/or transformation
     * and returns a list of matches.
     *
     * Returning null from the selection function excludes it from the results.
     */
    fun <R> collect(fn: (edge: Edge) -> R?): List<R> {
        class Visitor : EdgeVisitor<List<R>> {

            override fun visit(edge: Edge): List<R> {
                val result: R? = fn(edge)
                return if (result != null) {
                    listOf(result)
                } else {
                    emptyList()
                } + edge.out.flatMap { outEdge -> outEdge.accept(this) }
            }
        }
        return this.accept(Visitor())
    }

    /**
     * @return the builder that created the [NameInfo]
     */
    fun toBuilder(): Builder = Builder().apply {
        id(id)
        level(level)
        probability(probability)
        heading(heading)
        length(length)
        out(out)
        frc(frc)
        wayId(wayId)
        positiveDirection(positiveDirection)
        speed(speed)
        ramp(ramp)
        motorway(motorway)
        bridge(bridge)
        tunnel(tunnel)
        toll(toll)
        names(names)
        curvature(curvature)
        geometry(geometry)
        speedLimit(speedLimit)
        laneCount(laneCount)
        meanElevation(meanElevation)
        countryCode(countryCode)
        stateCode(stateCode)
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
        if (frc != other.frc) return false
        if (wayId != other.wayId) return false
        if (positiveDirection != other.positiveDirection) return false
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
        result = 31 * result + frc.hashCode()
        result = 31 * result + wayId.hashCode()
        result = 31 * result + positiveDirection.hashCode()
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
            "frc=$frc, " +
            "wayId=$wayId, " +
            "positiveDirection=$positiveDirection, " +
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

    /**
     * Builder for [NameInfo].
     */
    class Builder {

        private var id: Long = 0
        private var level: Byte = 0.toByte()
        private var probability: Double = 0.0
        private var heading: Double = 0.0
        private var length: Double = 0.0
        private var out: List<Edge> = emptyList()
        private var frc: String = ""
        private var wayId: String = ""
        private var positiveDirection: Boolean = true
        private var speed: Double = 0.0
        private var ramp: Boolean = false
        private var motorway: Boolean = false
        private var bridge: Boolean = false
        private var tunnel: Boolean = false
        private var toll: Boolean = false
        private var names: List<NameInfo> = emptyList()
        private var curvature: Byte = 0.toByte()
        private var geometry: LineString? = null
        private var speedLimit: Double? = null
        private var laneCount: Byte? = null
        private var meanElevation: Double? = null
        private var countryCode: String? = null
        private var stateCode: String? = null

        /**
         * Defines the Edge identifier
         */
        fun id(id: Long): Builder =
            apply { this.id = id }

        /**
         * Defines the level of the Edge
         */
        fun level(level: Byte): Builder =
            apply { this.level = level }

        /**
         * Defines the probability of the Edge in percentage
         */
        fun probability(probability: Double): Builder =
            apply { this.probability = probability }

        /**
         * Defines the Edge's length in meters
         */
        fun length(length: Double): Builder =
            apply { this.length = length }

        /**
         * Defines the bearing in degrees clockwise at the start of the edge
         */
        fun heading(heading: Double): Builder =
            apply { this.heading = heading }

        /**
         * Defines the outgoing Edges
         */
        fun out(out: List<Edge>): Builder =
            apply { this.out = out }

        /**
         * Defines the edge's functional road class
         */
        fun frc(frc: String): Builder =
            apply { this.frc = frc }

        /**
         * Defines the edge's way ID
         */
        fun wayId(wayId: String): Builder =
            apply { this.wayId = wayId }

        /**
         * Specifies if the geometry is in the forward or backwards direction
         */
        fun positiveDirection(positiveDirection: Boolean): Builder =
            apply { this.positiveDirection = positiveDirection }

        /**
         * Defines the Edge's average speed
         */
        fun speed(speed: Double): Builder =
            apply { this.speed = speed }

        /**
         * Defines if the edge is a ramp
         */
        fun ramp(ramp: Boolean): Builder =
            apply { this.ramp = ramp }

        /**
         * Defines if the edge is a motorway
         */
        fun motorway(motorway: Boolean): Builder =
            apply { this.motorway = motorway }

        /**
         * Defines if the edge is a bridge
         */
        fun bridge(bridge: Boolean): Builder =
            apply { this.bridge = bridge }

        /**
         * Defines if the edge is a tunnel
         */
        fun tunnel(tunnel: Boolean): Builder =
            apply { this.tunnel = tunnel }

        /**
         * Defines if the edge is a toll
         */
        fun toll(toll: Boolean): Builder =
            apply { this.toll = toll }

        /**
         * Defines the edge's names
         */
        fun names(names: List<NameInfo>): Builder =
            apply { this.names = names }

        /**
         * Defines the edge's curvature
         */
        fun curvature(curvature: Byte): Builder =
            apply { this.curvature = curvature }

        /**
         * Defines the edge's geometry
         */
        fun geometry(geometry: LineString?): Builder =
            apply { this.geometry = geometry }

        /**
         * Defines the Edge's max speed
         */
        fun speedLimit(speedLimit: Double?): Builder =
            apply { this.speedLimit = speedLimit }

        /**
         * Defines the edge's lane counts
         */
        fun laneCount(laneCount: Byte?): Builder =
            apply { this.laneCount = laneCount }

        /**
         * Defines the edge's mean elevation
         */
        fun meanElevation(meanElevation: Double?): Builder =
            apply { this.meanElevation = meanElevation }

        /**
         * Defines the edge's country code
         */
        fun countryCode(countryCode: String?): Builder =
            apply { this.countryCode = countryCode }

        /**
         * Defines the edge's state code
         */
        fun stateCode(stateCode: String?): Builder =
            apply { this.stateCode = stateCode }

        /**
         * Build the [NameInfo]
         */
        fun build(): Edge {
            return Edge(
                id = id,
                level = level,
                probability = probability,
                heading = heading,
                length = length,
                out = out,
                frc = frc,
                wayId = wayId,
                positiveDirection = positiveDirection,
                speed = speed,
                ramp = ramp,
                motorway = motorway,
                bridge = bridge,
                tunnel = tunnel,
                toll = toll,
                names = names,
                curvature = curvature,
                geometry = geometry,
                speedLimit = speedLimit,
                laneCount = laneCount,
                meanElevation = meanElevation,
                countryCode = countryCode,
                stateCode = stateCode
            )
        }
    }
}
