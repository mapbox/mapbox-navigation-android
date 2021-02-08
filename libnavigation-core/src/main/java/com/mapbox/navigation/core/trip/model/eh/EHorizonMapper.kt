package com.mapbox.navigation.core.trip.model.eh

import com.mapbox.navigator.EdgeMetadata
import com.mapbox.navigator.ElectronicHorizon
import com.mapbox.navigator.ElectronicHorizonEdge
import com.mapbox.navigator.ElectronicHorizonPosition
import com.mapbox.navigator.ElectronicHorizonResultType
import com.mapbox.navigator.FunctionalRoadClass
import com.mapbox.navigator.GraphPosition
import com.mapbox.navigator.RoadObjectDistanceInfo
import com.mapbox.navigator.RoadObjectEdgeLocation
import com.mapbox.navigator.RoadObjectEnterExitInfo
import com.mapbox.navigator.RoadObjectLocation
import com.mapbox.navigator.RoadObjectProvider
import com.mapbox.navigator.RoadObjectType
import com.mapbox.navigator.Standard

/**
 * Map the ElectronicHorizonPosition.
 */
internal fun ElectronicHorizonPosition.mapToEHorizonPosition(): EHorizonPosition {
    return EHorizonPosition(
        position().mapToEHorizonGraphPosition(),
        tree().mapToEHorizon(),
        type().mapToEHorizonResultType()
    )
}

/**
 * Map the RoadObjectEnterExitInfo.
 */
internal fun RoadObjectEnterExitInfo.mapToEHorizonObjectEnterExitInfo():
    EHorizonObjectEnterExitInfo {
        return EHorizonObjectEnterExitInfo(
            roadObjectId,
            enterFromStartOrExitFromEnd,
            type.mapToEHorizonObjectType()
        )
    }

/**
 * Map the RoadObjectDistanceInfo.
 */
internal fun RoadObjectDistanceInfo.mapToEHorizonObjectDistanceInfo(): EHorizonObjectDistanceInfo {
    return EHorizonObjectDistanceInfo(
        distanceToEntry,
        distanceToEnd,
        entryFromStart,
        length,
        type.mapToEHorizonObjectType()
    )
}

/**
 * Map the ElectronicHorizonPosition.
 */
internal fun RoadObjectLocation.mapToEHorizonObjectLocation(): EHorizonObjectLocation {
    return EHorizonObjectLocation(
        edges,
        percentAlongBegin,
        percentAlongEnd
    )
}

/**
 * Map the RoadObjectType.
 */
internal fun RoadObjectType.mapToEHorizonObjectType(): String {
    return when (this) {
        RoadObjectType.INCIDENT -> EHorizonObjectType.INCIDENT
        RoadObjectType.CUSTOM -> EHorizonObjectType.CUSTOM
    }
}

/**
 * Map the RoadObjectProvider.
 */
internal fun RoadObjectProvider.mapToEHorizonObjectProvider(): String {
    return when (this) {
        RoadObjectProvider.MAPBOX -> EHorizonObjectProvider.MAPBOX
        RoadObjectProvider.CUSTOM -> EHorizonObjectProvider.CUSTOM
    }
}

/**
 * Map the RoadObjectEdgeLocation.
 */
internal fun RoadObjectEdgeLocation.mapToEHorizonObjectEdgeLocation(): EHorizonObjectEdgeLocation {
    return EHorizonObjectEdgeLocation(
        percentAlongBegin,
        percentAlongEnd
    )
}

/**
 * Map the EdgeMetadata.
 */
internal fun EdgeMetadata.mapToEHorizonEdgeMetadata(): EHorizonEdgeMetadata {
    return EHorizonEdgeMetadata(
        heading,
        length,
        frc.mapToRoadClass(),
        speedLimit,
        speed,
        ramp,
        motorway,
        bridge,
        tunnel,
        toll,
        mapNames(names),
        laneCount,
        meanElevation,
        curvature,
        countryCode,
        stateCode
    )
}

internal fun OpenLRStandard.mapToStandard(): Standard {
    return when (this) {
        OpenLRStandard.TOM_TOM -> Standard.TOM_TOM
        OpenLRStandard.TPEG -> Standard.TPEG
    }
}

private fun FunctionalRoadClass.mapToRoadClass(): String {
    return when (this) {
        FunctionalRoadClass.MOTORWAY -> RoadClass.MOTORWAY
        FunctionalRoadClass.TRUNK -> RoadClass.TRUNK
        FunctionalRoadClass.PRIMARY -> RoadClass.PRIMARY
        FunctionalRoadClass.SECONDARY -> RoadClass.SECONDARY
        FunctionalRoadClass.TERTIARY -> RoadClass.TERTIARY
        FunctionalRoadClass.UNCLASSIFIED -> RoadClass.UNCLASSIFIED
        FunctionalRoadClass.RESIDENTIAL -> RoadClass.RESIDENTIAL
        FunctionalRoadClass.SERVICE_OTHER -> RoadClass.SERVICE_OTHER
    }
}

private fun mapNames(names: List<com.mapbox.navigator.RoadName>): List<RoadName> {
    val namesNames = mutableListOf<RoadName>()
    names.forEach {
        namesNames.add(it.mapToRoadName())
    }
    return namesNames.toList()
}

private fun com.mapbox.navigator.RoadName.mapToRoadName(): RoadName {
    return RoadName(
        name,
        shielded
    )
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
private fun ElectronicHorizonEdge.mapToEdge(parent: EHorizonEdge?): EHorizonEdge {
    val outgoingEdges = mutableListOf<EHorizonEdge>()
    val edge = EHorizonEdge(
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
        edgeId,
        percentAlong
    )
}
