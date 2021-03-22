@file:JvmName("EHorizonMapper")

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

private typealias SDKRoadObjectType =
    com.mapbox.navigation.core.trip.model.roadobject.RoadObjectType

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
                type.mapToRoadObjectType()
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
                type.mapToRoadObjectType()
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
internal fun RoadObjectType.mapToRoadObjectType(): Int {
    return when (this) {
        RoadObjectType.INCIDENT -> SDKRoadObjectType.INCIDENT
        RoadObjectType.TOLL_COLLECTION_POINT -> SDKRoadObjectType.TOLL_COLLECTION
        RoadObjectType.BORDER_CROSSING -> SDKRoadObjectType.COUNTRY_BORDER_CROSSING
        RoadObjectType.TUNNEL_ENTRANCE -> SDKRoadObjectType.TUNNEL_ENTRANCE
        RoadObjectType.TUNNEL_EXIT -> SDKRoadObjectType.TUNNEL_EXIT
        RoadObjectType.RESTRICTED_AREA_ENTRANCE -> SDKRoadObjectType.RESTRICTED_AREA_ENTRANCE
        RoadObjectType.RESTRICTED_AREA_EXIT -> SDKRoadObjectType.RESTRICTED_AREA_EXIT
        RoadObjectType.SERVICE_AREA -> SDKRoadObjectType.REST_STOP
        RoadObjectType.BRIDGE_ENTRANCE -> SDKRoadObjectType.BRIDGE_ENTRANCE
        RoadObjectType.BRIDGE_EXIT -> SDKRoadObjectType.BRIDGE_EXIT
        RoadObjectType.CUSTOM -> SDKRoadObjectType.CUSTOM
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
        "USA",
        stateCode,
        isRightHandTraffic
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
