package com.mapbox.navigation.core.trip.model.eh

import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.EdgeMetadata
import com.mapbox.navigator.ElectronicHorizon
import com.mapbox.navigator.ElectronicHorizonEdge
import com.mapbox.navigator.ElectronicHorizonPosition
import com.mapbox.navigator.ElectronicHorizonResultType
import com.mapbox.navigator.FunctionalRoadClass
import com.mapbox.navigator.GraphPath
import com.mapbox.navigator.GraphPosition
import com.mapbox.navigator.RoadObjectDistanceInfo
import com.mapbox.navigator.RoadObjectEdgeLocation
import com.mapbox.navigator.RoadObjectEnterExitInfo
import com.mapbox.navigator.RoadObjectLocation
import com.mapbox.navigator.RoadObjectProvider
import com.mapbox.navigator.RoadObjectType
import com.mapbox.navigator.Standard
import kotlinx.coroutines.withContext

/**
 * Map the ElectronicHorizonPosition.
 */
internal suspend fun ElectronicHorizonPosition.mapToEHorizonPosition(): EHorizonPosition {
    return withContext(ThreadController.IODispatcher) {
        EHorizonPosition(
            position().mapToEHorizonGraphPosition(),
            tree().mapToEHorizon(),
            type().mapToEHorizonResultType()
        )
    }
}

/**
 * Map the RoadObjectEnterExitInfo.
 */
internal suspend fun RoadObjectEnterExitInfo.mapToEHorizonObjectEnterExitInfo():
    EHorizonObjectEnterExitInfo {
        return withContext(ThreadController.IODispatcher) {
            EHorizonObjectEnterExitInfo(
                roadObjectId,
                enterFromStartOrExitFromEnd,
                type.mapToEHorizonObjectType()
            )
        }
    }

/**
 * Map the RoadObjectDistanceInfo.
 */
internal suspend fun RoadObjectDistanceInfo.mapToEHorizonObjectDistanceInfo():
    EHorizonObjectDistanceInfo {
        return withContext(ThreadController.IODispatcher) {
            EHorizonObjectDistanceInfo(
                distanceToEntry,
                distanceToEnd,
                entryFromStart,
                length,
                type.mapToEHorizonObjectType()
            )
        }
    }

/**
 * Map the ElectronicHorizonPosition.
 */
internal fun RoadObjectLocation.mapToEHorizonObjectLocation(): EHorizonObjectLocation {
    return EHorizonObjectLocation(
        path?.mapToEHorizonGraphPath(),
        position?.mapToEHorizonGraphPosition()
    )
}

/**
 * Map the RoadObjectType.
 */
internal fun RoadObjectType.mapToEHorizonObjectType(): String {
    return when (this) {
        RoadObjectType.INCIDENT -> EHorizonObjectType.INCIDENT
        RoadObjectType.TOLL_COLLECTION_POINT -> EHorizonObjectType.TOLL_COLLECTION_POINT
        RoadObjectType.BORDER_CROSSING -> EHorizonObjectType.BORDER_CROSSING
        RoadObjectType.TUNNEL_ENTRANCE -> EHorizonObjectType.TUNNEL_ENTRANCE
        RoadObjectType.TUNNEL_EXIT -> EHorizonObjectType.TUNNEL_EXIT
        RoadObjectType.RESTRICTED_AREA_ENTRANCE -> EHorizonObjectType.RESTRICTED_AREA_ENTRANCE
        RoadObjectType.RESTRICTED_AREA_EXIT -> EHorizonObjectType.RESTRICTED_AREA_EXIT
        RoadObjectType.SERVICE_AREA -> EHorizonObjectType.SERVICE_AREA
        RoadObjectType.BRIDGE_ENTRANCE -> EHorizonObjectType.BRIDGE_ENTRANCE
        RoadObjectType.BRIDGE_EXIT -> EHorizonObjectType.BRIDGE_EXIT
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

internal fun String.mapToOpenLRStandard(): Standard {
    return when (this) {
        OpenLRStandard.TOM_TOM -> Standard.TOM_TOM
        else -> throw Exception("Invalid OpenLRStandard.")
    }
}

/**
 * Map the EHorizonGraphPath.
 */
internal fun EHorizonGraphPath.mapToGraphPath(): GraphPath {
    return GraphPath(
        edges,
        percentAlongBegin,
        percentAlongEnd,
        length
    )
}

/**
 * Map the EHorizonGraphPosition.
 */
internal fun EHorizonGraphPosition.mapToGraphPosition(): GraphPosition {
    return GraphPosition(
        edgeId,
        percentAlong
    )
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
    return EHorizon(this.start.mapToEdge())
}

/**
 * Recursively map each edge of the graph.
 */
private fun ElectronicHorizonEdge.mapToEdge(): EHorizonEdge {
    val outgoingEdges = mutableListOf<EHorizonEdge>()
    val edge = EHorizonEdge(
        id,
        level,
        probability,
        outgoingEdges,
    )
    // Recursively map the outgoing edges
    out.forEach { outgoingEdges.add(it.mapToEdge()) }
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

/**
 * Map the GraphPath.
 */
private fun GraphPath.mapToEHorizonGraphPath(): EHorizonGraphPath {
    return EHorizonGraphPath(
        edges,
        percentAlongBegin,
        percentAlongEnd,
        length
    )
}
