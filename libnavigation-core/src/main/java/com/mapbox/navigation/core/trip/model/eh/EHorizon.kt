package com.mapbox.navigation.core.trip.model.eh

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
 * @param start [EHorizonEdge]
 */
class EHorizon internal constructor(
    val start: EHorizonEdge
) {

    /**
     * Get the current edge given the position
     *
     * @return current Edge given the position
     */
    fun current(position: EHorizonPosition): EHorizonEdge {
        val visitedEdges = LinkedList<EHorizonEdge>()
        visitedEdges.push(start)
        while (visitedEdges.isNotEmpty()) {
            val current = visitedEdges.poll()!!
            if (current.id != position.eHorizonGraphPosition.edgeId) {
                val mppEdges = current.out.filter { it.isMpp() }
                visitedEdges.addAll(mppEdges)
            } else {
                return current
            }
        }
        throw IllegalArgumentException("No edge with id ${position.eHorizonGraphPosition.edgeId}")
    }

    /**
     * Get the MPP starting at the first Edge in the [EHorizon]
     *
     * @return entire MPP from the [EHorizon] as a list of ordered Edges
     */
    fun mpp(): List<List<EHorizonEdge>> {
        return mpp(start)
    }

    /**
     * Get the MPP starting at the current [EHorizonPosition]
     *
     * @return the Edges starting at the current edge
     */
    fun mpp(position: EHorizonPosition): List<List<EHorizonEdge>> {
        return mpp(current(position))
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

    private fun mpp(start: EHorizonEdge): List<List<EHorizonEdge>> {
        // if it is not MPP return empty list
        if (!start.isMpp()) return emptyList()
        if (start.out.isEmpty()) return listOf(listOf(start))
        val result = LinkedList<LinkedList<EHorizonEdge>>()
        // traverse through MPP nodes until a tip is found
        val visitedEdges = LinkedList(start.out.filter { c -> c.isMpp() })
        while (visitedEdges.isNotEmpty()) {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            val current: EHorizonEdge = visitedEdges.poll()
            // check if is a tip of MPP
            if (current.out.isEmpty()) {
                val path = LinkedList<EHorizonEdge>()
                var b: EHorizonEdge? = current
                // trace back and form mpp
                while (b != null) {
                    path.push(b)
                    if (b == start) break
                    b = b.parent
                }
                result.add(path)
            } else {
                // push all MPP children to stack
                visitedEdges.addAll(current.out.filter { c -> c.isMpp() })
            }
        }
        return result
    }
}
