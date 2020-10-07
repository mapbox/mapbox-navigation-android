package com.mapbox.navigation.base.trip.model

import java.util.LinkedList

/**
 * An Electronic Horizon is a probable path (or paths) of a vehicle within the road graph which is
 * used to surface metadata about the underlying edges of the graph for a certain distance in front
 * of the vehicle thus extending the user's perspective beyond the “visible” horizon.
 *
 * Mapbox Electronic Horizon correlates the vehicle’s location to the road graph and broadcasts
 * updates to the Electronic Horizon as the vehicle’s position and trajectory change.
 *
 * In Active Guidance state, the user-selected route and its metadata are used as the path for the
 * Electronic Horizon. In a Free Drive state there is no active route selected, Mapbox Electronic
 * Horizon will determine the most probable path from the vehicle’s current location.
 * For both states Active Guidance and Free Drive, the Electronic Horizon and its metadata are
 * exposed via the same interface as described below.
 *
 * We represent the road network ahead of us as a tree of edges. Each intersection has outbound
 * edges and each edge has probability of transition to another edge as well as metadata which
 * can be used to implement sophisticated features on top of it.
 *
 * The EHorizon is a simple tree structure that can be navigated easily by traversing the Edges and
 * their outgoing connections. This can be done by simply looping over the Edges or using a Visitor.
 * For common cases, a number of utilities have been added which are described below.
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * @param start [Edge]
 */
class EHorizon private constructor(
    val start: Edge
) {

    /**
     * Get the current edge given the position
     *
     * @return current Edge given the position
     */
    fun current(position: EHorizonPosition): Edge {
        val visitedEdges = LinkedList<Edge>()
        visitedEdges.push(start)
        while (visitedEdges.isNotEmpty()) {
            val current = visitedEdges.poll()
            if (current.id != position.edgeId) {
                val mppEdges = current.out.filter { e -> e.level == 0.toByte() }
                visitedEdges.addAll(mppEdges)
            } else {
                return current
            }
        }
        throw IllegalArgumentException("No edge with id ${position.edgeId}")
    }

    /**
     * Get the MPP starting at the current [EHorizonPosition]
     *
     * @return the Edges starting at the current edge
     */
    fun mpp(position: EHorizonPosition): List<Edge> {
        return this.current(position).collect { edge: Edge ->
            if (edge.level == 0.toByte()) {
                edge
            } else {
                null
            }
        }
    }

    /**
     * Get the MPP starting at the first Edge in the [EHorizon]
     *
     * @return entire MPP from the [EHorizon] as a list of ordered Edges
     */
    fun mpp(): List<Edge> {
        return this.start.collect { edge: Edge ->
            if (edge.level == 0.toByte()) {
                edge
            } else {
                null
            }
        }
    }

    /**
     * @return the builder that created the [EHorizon]
     */
    fun toBuilder(): Builder = Builder().apply {
        start(start)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizon

        if (start != other.start) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return start.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizon(" +
            "start=$start" +
            ")"
    }

    /**
     * Builder for [EHorizon].
     */
    class Builder {

        private var start: Edge = Edge.Builder().build()

        /**
         * Defines the start [Edge]
         */
        fun start(start: Edge): Builder =
            apply { this.start = start }

        /**
         * Build the [EHorizon]
         */
        fun build(): EHorizon {
            return EHorizon(
                start = start
            )
        }
    }
}
