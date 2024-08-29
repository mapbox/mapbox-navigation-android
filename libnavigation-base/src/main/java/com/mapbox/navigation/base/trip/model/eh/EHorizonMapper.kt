package com.mapbox.navigation.base.trip.model.eh

import com.mapbox.navigation.base.internal.extensions.toMapboxShield
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPosition
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.GantryDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.Gate
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.LineDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.PointDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.PolygonDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.SubGraphDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.location.GantryLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.OpenLRLineLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.OpenLRPointLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.PointLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.PolygonLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.PolylineLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.RouteAlertLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.SubgraphLocation
import com.mapbox.navigator.EdgeMetadata
import com.mapbox.navigator.ElectronicHorizon
import com.mapbox.navigator.ElectronicHorizonEdge
import com.mapbox.navigator.ElectronicHorizonPosition
import com.mapbox.navigator.ElectronicHorizonResultType
import com.mapbox.navigator.FunctionalRoadClass
import com.mapbox.navigator.GraphPath
import com.mapbox.navigator.GraphPosition
import com.mapbox.navigator.MatchableGeometry
import com.mapbox.navigator.MatchableOpenLr
import com.mapbox.navigator.MatchablePoint
import com.mapbox.navigator.MatchedRoadObjectLocation
import com.mapbox.navigator.Position
import com.mapbox.navigator.RoadObjectDistance
import com.mapbox.navigator.RoadObjectDistanceInfo
import com.mapbox.navigator.RoadObjectEdgeLocation
import com.mapbox.navigator.RoadObjectEnterExitInfo
import com.mapbox.navigator.RoadObjectPassInfo
import com.mapbox.navigator.RoadObjectProvider
import com.mapbox.navigator.RoadObjectType
import com.mapbox.navigator.RoadSurface
import com.mapbox.navigator.SubgraphEdge
import com.mapbox.navigator.match.openlr.OpenLR
import com.mapbox.navigator.match.openlr.Orientation
import com.mapbox.navigator.match.openlr.SideOfRoad

private typealias SDKRoadObjectType =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

internal typealias SDKRoadObjectDistanceInfo =
    com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo

internal typealias SDKRoadObjectProvider =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider

internal typealias SDKRoadObjectEdgeLocation =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectEdgeLocation

internal typealias SDKRoadObjectEnterExitInfo =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectEnterExitInfo

internal typealias SDKRoadObjectPassInfo =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPassInfo

internal typealias SDKOpenLRSideOfRoad =
    com.mapbox.navigation.base.trip.model.roadobject.location.OpenLRSideOfRoad

internal typealias SDKOpenLROrientation =
    com.mapbox.navigation.base.trip.model.roadobject.location.OpenLROrientation

internal typealias SDKRoadSurface =
    com.mapbox.navigation.base.trip.model.eh.RoadSurface

internal typealias SDKSubgraphEdge =
    com.mapbox.navigation.base.trip.model.roadobject.location.SubgraphEdge

internal typealias SDKMatchableOpenLr =
    com.mapbox.navigation.base.trip.model.eh.MatchableOpenLr

internal typealias SDKMatchableGeometry =
    com.mapbox.navigation.base.trip.model.eh.MatchableGeometry

internal typealias SDKMatchablePoint =
    com.mapbox.navigation.base.trip.model.eh.MatchablePoint

/**
 * Map the ElectronicHorizonPosition.
 */
internal fun ElectronicHorizonPosition.mapToEHorizonPosition() =
    EHorizonPosition(
        position().mapToEHorizonGraphPosition(),
        tree().mapToEHorizon(),
        type().mapToEHorizonResultType(),
    )

/**
 * Map the RoadObjectEnterExitInfo.
 */
internal fun RoadObjectEnterExitInfo.mapToRoadObjectEnterExitInfo() =
    SDKRoadObjectEnterExitInfo(
        roadObjectId,
        enterFromStartOrExitFromEnd,
        type.mapToRoadObjectType(),
    )

/**
 * Map the RoadObjectEnterExitInfo.
 */
internal fun RoadObjectPassInfo.mapToRoadObjectPassInfo() =
    SDKRoadObjectPassInfo(
        roadObjectId,
        type.mapToRoadObjectType(),
    )

/**
 * Map the RoadObjectDistance.
 */
internal fun RoadObjectDistance.mapToRoadObjectDistance(): SDKRoadObjectDistanceInfo {
    return distanceInfo.mapToRoadObjectDistanceInfo(
        roadObjectId,
        type.mapToRoadObjectType(),
    )
}

/**
 * Map the RoadObjectDistanceInfo.
 */
internal fun RoadObjectDistanceInfo.mapToRoadObjectDistanceInfo(
    roadObjectId: String,
    roadObjectType: Int,
):
    SDKRoadObjectDistanceInfo {
    return when {
        isGantryDistanceInfo -> GantryDistanceInfo(
            roadObjectId,
            roadObjectType,
            gantryDistanceInfo.distance,
        )
        isLineDistanceInfo -> with(lineDistanceInfo) {
            LineDistanceInfo(
                roadObjectId,
                roadObjectType,
                distanceToEntry,
                distanceToExit,
                distanceToEnd,
                entryFromStart,
                length,
            )
        }
        isPointDistanceInfo -> PointDistanceInfo(
            roadObjectId,
            roadObjectType,
            pointDistanceInfo.distance,
        )
        isPolygonDistanceInfo -> with(polygonDistanceInfo) {
            val entrances = mapToGates(entrances)
            val exits = mapToGates(exits)
            PolygonDistanceInfo(
                roadObjectId,
                roadObjectType,
                entrances,
                exits,
                inside,
            )
        }
        isSubGraphDistanceInfo -> with(subGraphDistanceInfo) {
            val entrances = mapToGates(entrances)
            val exits = mapToGates(exits)
            SubGraphDistanceInfo(
                roadObjectId,
                roadObjectType,
                entrances,
                exits,
                inside,
            )
        }
        else -> throw IllegalArgumentException("Unsupported distance info type.")
    }
}

internal fun MatchedRoadObjectLocation.mapToRoadObjectLocation(): RoadObjectLocation {
    return when {
        isMatchedGantryLocation -> {
            GantryLocation(
                matchedGantryLocation.positions.mapToRoadObjectPositions(),
                matchedGantryLocation.shape,
            )
        }
        isMatchedPointLocation -> {
            val position = matchedPointLocation.position.mapToRoadObjectPosition()
            PointLocation(position, position.coordinate)
        }
        isMatchedPolygonLocation -> {
            PolygonLocation(
                matchedPolygonLocation.entries.mapToRoadObjectPositions(),
                matchedPolygonLocation.exits.mapToRoadObjectPositions(),
                matchedPolygonLocation.shape,
            )
        }
        isMatchedPolylineLocation -> {
            PolylineLocation(
                matchedPolylineLocation.path.mapToEHorizonGraphPath(),
                matchedPolylineLocation.shape,
            )
        }
        isOpenLRLineLocation -> {
            OpenLRLineLocation(
                openLRLineLocation.path.mapToEHorizonGraphPath(),
                openLRLineLocation.shape,
            )
        }
        isOpenLRPointAlongLineLocation -> {
            OpenLRPointLocation(
                openLRPointAlongLineLocation.position.mapToEHorizonGraphPosition(),
                openLRPointAlongLineLocation.coordinate,
                openLRPointAlongLineLocation.sideOfRoad.mapToSideOfRoad(),
                openLRPointAlongLineLocation.orientation.mapToOrientation(),
            )
        }
        isRouteAlertLocation -> {
            RouteAlertLocation(routeAlertLocation.shape)
        }
        isMatchedSubgraphLocation -> {
            SubgraphLocation(
                matchedSubgraphLocation.enters.mapToRoadObjectPositions(),
                matchedSubgraphLocation.exits.mapToRoadObjectPositions(),
                matchedSubgraphLocation.edges.mapToSubgraphEdges(),
                matchedSubgraphLocation.shape,
            )
        }
        else -> throw IllegalArgumentException("Unsupported object location type.")
    }
}

internal fun List<Position>.mapToRoadObjectPositions(): List<RoadObjectPosition> =
    map { it.mapToRoadObjectPosition() }

internal fun Position.mapToRoadObjectPosition(): RoadObjectPosition {
    return RoadObjectPosition(
        position.mapToEHorizonGraphPosition(),
        coordinate,
    )
}

internal fun Map<Long, SubgraphEdge>.mapToSubgraphEdges(): Map<Long, SDKSubgraphEdge> {
    val edges = mutableMapOf<Long, SDKSubgraphEdge>()
    this.entries.forEach {
        edges[it.key] = it.value.mapToSubgraphEdge()
    }

    return edges
}

internal fun SubgraphEdge.mapToSubgraphEdge() =
    SDKSubgraphEdge(
        id,
        innerEdgeIds,
        outerEdgeIds,
        shape,
        length,
    )

/**
 * Map the RoadObjectType.
 */
internal fun RoadObjectType.mapToRoadObjectType(): Int {
    return when (this) {
        RoadObjectType.INCIDENT -> SDKRoadObjectType.INCIDENT
        RoadObjectType.TOLL_COLLECTION_POINT -> SDKRoadObjectType.TOLL_COLLECTION
        RoadObjectType.BORDER_CROSSING -> SDKRoadObjectType.COUNTRY_BORDER_CROSSING
        RoadObjectType.TUNNEL -> SDKRoadObjectType.TUNNEL
        RoadObjectType.RESTRICTED_AREA -> SDKRoadObjectType.RESTRICTED_AREA
        RoadObjectType.SERVICE_AREA -> SDKRoadObjectType.REST_STOP
        RoadObjectType.BRIDGE -> SDKRoadObjectType.BRIDGE
        RoadObjectType.CUSTOM -> SDKRoadObjectType.CUSTOM
        RoadObjectType.RAILWAY_CROSSING -> SDKRoadObjectType.RAILWAY_CROSSING
        RoadObjectType.IC -> SDKRoadObjectType.IC
        RoadObjectType.JCT -> SDKRoadObjectType.JCT
        RoadObjectType.NOTIFICATION -> SDKRoadObjectType.NOTIFICATION
        RoadObjectType.MERGING_AREA -> SDKRoadObjectType.MERGING_AREA
    }
}

internal fun SideOfRoad.mapToSideOfRoad(): Int {
    return when (this) {
        SideOfRoad.BOTH -> SDKOpenLRSideOfRoad.BOTH
        SideOfRoad.LEFT -> SDKOpenLRSideOfRoad.LEFT
        SideOfRoad.RIGHT -> SDKOpenLRSideOfRoad.RIGHT
        SideOfRoad.ON_ROAD_OR_UNKNOWN -> SDKOpenLRSideOfRoad.ON_ROAD_OR_UNKNOWN
    }
}

internal fun Orientation.mapToOrientation(): Int {
    return when (this) {
        Orientation.BOTH -> SDKOpenLROrientation.BOTH
        Orientation.NO_ORIENTATION_OR_UNKNOWN ->
            SDKOpenLROrientation.NO_ORIENTATION_OR_UNKNOWN
        Orientation.WITH_LINE_DIRECTION -> SDKOpenLROrientation.WITH_LINE_DIRECTION
        Orientation.AGAINST_LINE_DIRECTION -> SDKOpenLROrientation.AGAINST_LINE_DIRECTION
    }
}

/**
 * Map the RoadObjectProvider.
 */
internal fun RoadObjectProvider.mapToRoadObjectProvider(): String {
    return when (this) {
        RoadObjectProvider.MAPBOX -> SDKRoadObjectProvider.MAPBOX
        RoadObjectProvider.CUSTOM -> SDKRoadObjectProvider.CUSTOM
    }
}

/**
 * Map the RoadObjectEdgeLocation.
 */
internal fun RoadObjectEdgeLocation.mapToRoadObjectEdgeLocation(): SDKRoadObjectEdgeLocation {
    return SDKRoadObjectEdgeLocation(
        percentAlongBegin,
        percentAlongEnd,
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
        countryCodeIso3,
        countryCodeIso2,
        stateCode,
        isRightHandTraffic,
        isOneway,
        surface.mapToRoadSurface(),
        isUrban,
    )
}

internal fun String.mapToOpenLRStandard(): com.mapbox.navigator.match.openlr.Standard {
    return when (this) {
        OpenLRStandard.TOM_TOM -> com.mapbox.navigator.match.openlr.Standard.TOM_TOM
        OpenLRStandard.TPEG -> com.mapbox.navigator.match.openlr.Standard.TPEG
        else -> throw Exception("Invalid OpenLRStandard.")
    }
}

/**
 * Map the EHorizonGraphPath.
 */
internal fun EHorizonGraphPath.mapToNativeGraphPath(): GraphPath {
    return GraphPath(
        edges,
        percentAlongBegin,
        percentAlongEnd,
        length,
    )
}

/**
 * Map the EHorizonGraphPosition.
 */
internal fun EHorizonGraphPosition.mapToNativeGraphPosition(): GraphPosition {
    return GraphPosition(
        edgeId,
        percentAlong,
    )
}

internal fun SDKMatchableOpenLr.mapToNativeMatchableOpenLr(): MatchableOpenLr {
    return MatchableOpenLr(
        OpenLR(openLRLocation, openLRStandard.mapToOpenLRStandard()),
        roadObjectId,
    )
}

internal fun SDKMatchableGeometry.mapToNativeMatchableGeometry(): MatchableGeometry {
    return MatchableGeometry(
        roadObjectId,
        coordinates,
    )
}

internal fun SDKMatchablePoint.mapToNativeMatchablePoint(): MatchablePoint {
    return MatchablePoint(
        roadObjectId,
        point,
        bearing,
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

private fun mapNames(names: List<com.mapbox.navigator.RoadName>): List<RoadComponent> {
    val roadComponents = mutableListOf<RoadComponent>()
    names.forEach {
        roadComponents.add(it.mapToRoadComponent())
    }
    return roadComponents.toList()
}

private fun com.mapbox.navigator.RoadName.mapToRoadComponent(): RoadComponent {
    return RoadComponent(
        text,
        language,
        shield.toMapboxShield(),
        imageBaseUrl,
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
        ElectronicHorizonResultType.NOT_AVAILABLE -> EHorizonResultType.NOT_AVAILABLE
    }
}

/**
 * Map the GraphPosition.
 */
private fun GraphPosition.mapToEHorizonGraphPosition(): EHorizonGraphPosition {
    return EHorizonGraphPosition(
        edgeId,
        percentAlong,
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
        length,
    )
}

private fun mapToGates(gates: List<com.mapbox.navigator.Gate>): List<Gate> {
    return gates.map { it.mapToGate() }.toList()
}

private fun com.mapbox.navigator.Gate.mapToGate(): Gate {
    return Gate(
        id,
        position.mapToRoadObjectPosition(),
        probability,
        distance,
    )
}

private fun RoadSurface.mapToRoadSurface(): String =
    when (this) {
        RoadSurface.PAVED_SMOOTH -> SDKRoadSurface.PAVED_SMOOTH
        RoadSurface.PAVED -> SDKRoadSurface.PAVED
        RoadSurface.PAVED_ROUGH -> SDKRoadSurface.PAVED_ROUGH
        RoadSurface.COMPACTED -> SDKRoadSurface.COMPACTED
        RoadSurface.DIRT -> SDKRoadSurface.DIRT
        RoadSurface.GRAVEL -> SDKRoadSurface.GRAVEL
        RoadSurface.PATH -> SDKRoadSurface.PATH
        RoadSurface.IMPASSABLE -> SDKRoadSurface.IMPASSABLE
    }
