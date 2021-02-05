package com.mapbox.navigation.core.trip.model.eh

import com.mapbox.navigator.ElectronicHorizon
import com.mapbox.navigator.ElectronicHorizonEdge
import com.mapbox.navigator.ElectronicHorizonPosition
import com.mapbox.navigator.ElectronicHorizonResultType
import com.mapbox.navigator.GraphPosition
import com.mapbox.navigator.RoadObjectDistanceInfo
import com.mapbox.navigator.RoadObjectEnterExitInfo
import com.mapbox.navigator.RoadObjectLocation
import com.mapbox.navigator.RoadObjectProvider
import com.mapbox.navigator.RoadObjectType

/**
 * Map the ElectronicHorizonPosition.
 */
fun ElectronicHorizonPosition.mapToEHorizonPosition(): EHorizonPosition {
    return EHorizonPosition(
        this.position().mapToEHorizonGraphPosition(),
        this.tree().mapToEHorizon(),
        this.type().mapToEHorizonResultType()
    )
}

/**
 * Map the RoadObjectEnterExitInfo.
 */
fun RoadObjectEnterExitInfo.mapToEHorizonObjectEnterExitInfo(): EHorizonObjectEnterExitInfo {
    return EHorizonObjectEnterExitInfo(
        this.roadObjectId,
        this.enterFromStartOrExitFromEnd,
        this.type.mapToEHorizonObjectType()
    )
}

/**
 * Map the RoadObjectDistanceInfo.
 */
fun RoadObjectDistanceInfo.mapToEHorizonObjectDistanceInfo(): EHorizonObjectDistanceInfo {
    return EHorizonObjectDistanceInfo(
        this.distanceToEntry,
        this.distanceToEnd,
        this.entryFromStart,
        this.length,
        this.type.mapToEHorizonObjectType()
    )
}

/**
 * Map the ElectronicHorizonPosition.
 */
fun RoadObjectLocation.mapToEHorizonObjectLocation(): EHorizonObjectLocation {
    return EHorizonObjectLocation(
        edges,
        percentAlongBegin,
        percentAlongEnd
    )
}

/**
 * Map the RoadObjectType.
 */
fun RoadObjectType.mapToEHorizonObjectType(): String {
    return when (this) {
        RoadObjectType.INCIDENT -> EHorizonObjectType.INCIDENT
        RoadObjectType.CUSTOM -> EHorizonObjectType.CUSTOM
    }
}

/**
 * Map the RoadObjectType.
 */
fun RoadObjectProvider.mapToEHorizonObjectProvider(): String {
    return when (this) {
        RoadObjectProvider.MAPBOX -> EHorizonObjectProvider.MAPBOX
        RoadObjectProvider.CUSTOM -> EHorizonObjectProvider.CUSTOM
    }
}

/**
 * Map the electronic horizon graph.
 */
private fun ElectronicHorizon.mapToEHorizon(): EHorizon {
    return EHorizon(this.start.mapToEdge(null))
}

/**
 * Recursively map each edge of the graph.
 */
private fun ElectronicHorizonEdge.mapToEdge(parent: Edge?): Edge {
    val outgoingEdges = mutableListOf<Edge>()
    val edge = Edge(
        id,
        level,
        probability,
        outgoingEdges,
        parent
    )
    // Recursively map the outgoing edges
    out.forEach { outgoingEdges.add(it.mapToEdge(edge)) }
    return edge
}

/**
 * Map the ElectronicHorizonResultType.
 */
private fun ElectronicHorizonResultType.mapToEHorizonResultType(): String {
    return when (this) {
        ElectronicHorizonResultType.INITIAL -> EHorizonResultType.INITIAL
        ElectronicHorizonResultType.UPDATE -> EHorizonResultType.UPDATE
    }
}



/**
 * Map the GraphPosition.
 */
private fun GraphPosition.mapToEHorizonGraphPosition(): EHorizonGraphPosition {
    return EHorizonGraphPosition(
        this.edgeId,
        this.percentAlong
    )
}
