package com.mapbox.navigation.core.trip.model.eh

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.ElectronicHorizon
import com.mapbox.navigator.ElectronicHorizonEdge
import com.mapbox.navigator.ElectronicHorizonResultType
import com.mapbox.navigator.FRC
import com.mapbox.navigator.GraphPosition
import com.mapbox.navigator.RoadNameInfo

/**
 * This class is responsible for converting nav native into
 * navigation sdk domain objects.
 */
internal object EHorizonMapper {

    /**
     * Map the result type.
     */
    fun mapToEHorizonResultType(electronicHorizonResultType: ElectronicHorizonResultType): String {
        return when (electronicHorizonResultType) {
            ElectronicHorizonResultType.INITIAL -> EHorizonResultType.INITIAL
            ElectronicHorizonResultType.UPDATE -> EHorizonResultType.UPDATE
        }
    }

    /**
     * Map the electronic horizon graph.
     */
    fun mapToEHorizon(electronicHorizon: ElectronicHorizon): EHorizon {
        val startEdge = electronicHorizon.start.mapToEdge(null)
        return EHorizon(startEdge)
    }

    /**
     * Map the electronic horizon graph position.
     */
    fun mapToEHorizonPosition(position: GraphPosition): EHorizonPosition {
        return EHorizonPosition(
            position.edgeId,
            position.percentAlong,
        )
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
            heading,
            length,
            outgoingEdges,
            parent,
            mapToFunctionalRoadClass(frc),
            speed,
            ramp,
            motorway,
            bridge,
            tunnel,
            toll,
            mapNames(names),
            curvature,
            mapGeometry(geometry),
            speedLimit,
            laneCount,
            meanElevation,
            countryCode,
            stateCode
        )
        // Recursively map the outgoing edges
        out.forEach { outgoingEdges.add(it.mapToEdge(edge)) }
        return edge
    }

    fun mapToFunctionalRoadClass(frc: FRC): String {
        return when (frc) {
            FRC.MOTORWAY -> FunctionalRoadClass.MOTORWAY
            FRC.TRUNK -> FunctionalRoadClass.TRUNK
            FRC.PRIMARY -> FunctionalRoadClass.PRIMARY
            FRC.SECONDARY -> FunctionalRoadClass.SECONDARY
            FRC.TERTIARY -> FunctionalRoadClass.TERTIARY
            FRC.UNCLASSIFIED -> FunctionalRoadClass.UNCLASSIFIED
            FRC.RESIDENTIAL -> FunctionalRoadClass.RESIDENTIAL
            FRC.SERVICE_OTHER -> FunctionalRoadClass.SERVICE_OTHER
        }
    }

    private fun mapNames(names: List<RoadNameInfo>): List<NameInfo> {
        val namesNamesInfo = mutableListOf<NameInfo>()
        for (roadNameInfo in names) {
            namesNamesInfo.add(roadNameInfo.mapToNameInfo())
        }
        return namesNamesInfo.toList()
    }

    private fun RoadNameInfo.mapToNameInfo(): NameInfo {
        return NameInfo(
            name,
            shielded
        )
    }

    private fun mapGeometry(geometry: List<Point>?): LineString? {
        return ifNonNull(geometry) { coordinates ->
            LineString.fromLngLats(coordinates)
        }
    }
}
