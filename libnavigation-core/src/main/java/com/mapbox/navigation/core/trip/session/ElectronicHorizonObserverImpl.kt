package com.mapbox.navigation.core.trip.session

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.trip.model.eh.EHorizon
import com.mapbox.navigation.core.trip.model.eh.EHorizonPosition
import com.mapbox.navigation.core.trip.model.eh.EHorizonResultType
import com.mapbox.navigation.core.trip.model.eh.Edge
import com.mapbox.navigation.core.trip.model.eh.FunctionalRoadClass.MOTORWAY
import com.mapbox.navigation.core.trip.model.eh.FunctionalRoadClass.PRIMARY
import com.mapbox.navigation.core.trip.model.eh.FunctionalRoadClass.RESIDENTIAL
import com.mapbox.navigation.core.trip.model.eh.FunctionalRoadClass.SECONDARY
import com.mapbox.navigation.core.trip.model.eh.FunctionalRoadClass.SERVICE_OTHER
import com.mapbox.navigation.core.trip.model.eh.FunctionalRoadClass.TERTIARY
import com.mapbox.navigation.core.trip.model.eh.FunctionalRoadClass.TRUNK
import com.mapbox.navigation.core.trip.model.eh.FunctionalRoadClass.UNCLASSIFIED
import com.mapbox.navigation.core.trip.model.eh.NameInfo
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.ElectronicHorizon
import com.mapbox.navigator.ElectronicHorizonEdge
import com.mapbox.navigator.ElectronicHorizonObserver
import com.mapbox.navigator.ElectronicHorizonResultType
import com.mapbox.navigator.FRC
import com.mapbox.navigator.GraphPosition
import com.mapbox.navigator.RoadNameInfo
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

internal class ElectronicHorizonObserverImpl(
    private val jobController: JobControl
) : ElectronicHorizonObserver() {

    val eHorizonObservers = CopyOnWriteArraySet<EHorizonObserver>()
    var currentHorizon: EHorizon? = null
    var currentType: String? = null
    var currentPosition: EHorizonPosition? = null

    override fun onElectronicHorizonUpdated(
        horizon: ElectronicHorizon,
        type: ElectronicHorizonResultType
    ) {
        val horizon = horizon.mapToEHorizon()
        val type = type.convertResultType()
        jobController.scope.launch {
            currentHorizon = horizon
            currentType = type
            eHorizonObservers.forEach {
                it.onElectronicHorizonUpdated(
                    horizon,
                    type
                )
            }
        }
    }

    override fun onPositionUpdated(position: GraphPosition) {
        val position = position.mapToEHorizonPosition()
        jobController.scope.launch {
            eHorizonObservers.forEach {
                currentPosition = position
                it.onPositionUpdated(position)
            }
        }
    }

    private fun ElectronicHorizon.mapToEHorizon(): EHorizon {
        return EHorizon(start.mapToEdge())
    }

    private fun ElectronicHorizonEdge.mapToEdge(parent: Edge? = null): Edge {
        val futureOut = mutableListOf<Edge>()
        val edge = Edge(
            id,
            level,
            probability,
            heading,
            length,
            futureOut,
            parent,
            frc.mapToFRC(),
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
        for (e in out) {
            futureOut.add(e.mapToEdge(edge))
        }
        return edge
    }

    private fun FRC.mapToFRC(): String {
        return when (this) {
            FRC.MOTORWAY -> MOTORWAY
            FRC.TRUNK -> TRUNK
            FRC.PRIMARY -> PRIMARY
            FRC.SECONDARY -> SECONDARY
            FRC.TERTIARY -> TERTIARY
            FRC.UNCLASSIFIED -> UNCLASSIFIED
            FRC.RESIDENTIAL -> RESIDENTIAL
            FRC.SERVICE_OTHER -> SERVICE_OTHER
        }
    }

    private fun RoadNameInfo.mapToNameInfo(): NameInfo {
        return NameInfo(
            name,
            shielded
        )
    }

    private fun mapNames(names: List<RoadNameInfo>): List<NameInfo> {
        val namesNamesInfo = mutableListOf<NameInfo>()
        for (roadNameInfo in names) {
            namesNamesInfo.add(roadNameInfo.mapToNameInfo())
        }
        return namesNamesInfo.toList()
    }

    private fun mapGeometry(geometry: List<Point>?): LineString? {
        return ifNonNull(geometry) { coordinates ->
            LineString.fromLngLats(coordinates)
        }
    }

    private fun ElectronicHorizonResultType.convertResultType(): String {
        return when (this) {
            ElectronicHorizonResultType.INITIAL -> EHorizonResultType.INITIAL
            ElectronicHorizonResultType.UPDATE -> EHorizonResultType.UPDATE
        }
    }

    private fun GraphPosition.mapToEHorizonPosition(): EHorizonPosition {
        return EHorizonPosition(
            edgeId,
            percentAlong,
        )
    }
}
