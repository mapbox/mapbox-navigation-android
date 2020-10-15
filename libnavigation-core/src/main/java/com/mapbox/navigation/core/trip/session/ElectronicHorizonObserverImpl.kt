package com.mapbox.navigation.core.trip.session

import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.EHorizon
import com.mapbox.navigation.base.trip.model.EHorizonPosition
import com.mapbox.navigation.base.trip.model.EHorizonResultType
import com.mapbox.navigation.base.trip.model.Edge
import com.mapbox.navigation.base.trip.model.FunctionalRoadClass.MOTORWAY
import com.mapbox.navigation.base.trip.model.FunctionalRoadClass.PRIMARY
import com.mapbox.navigation.base.trip.model.FunctionalRoadClass.RESIDENTIAL
import com.mapbox.navigation.base.trip.model.FunctionalRoadClass.SECONDARY
import com.mapbox.navigation.base.trip.model.FunctionalRoadClass.SERVICE_OTHER
import com.mapbox.navigation.base.trip.model.FunctionalRoadClass.TERTIARY
import com.mapbox.navigation.base.trip.model.FunctionalRoadClass.TRUNK
import com.mapbox.navigation.base.trip.model.FunctionalRoadClass.UNCLASSIFIED
import com.mapbox.navigation.base.trip.model.NameInfo
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
        return EHorizon.Builder().start(start.mapToEdge()).build()
    }

    private fun ElectronicHorizonEdge.mapToEdge(): Edge {
        return Edge.Builder()
            .id(id)
            .level(level)
            .probability(probability)
            .heading(heading)
            .length(length)
            .out(mapOut(out))
            .frc(frc.mapToFRC())
            .wayId(wayId)
            .positiveDirection(positiveDirection)
            .speed(speed)
            .ramp(ramp)
            .motorway(motorway)
            .bridge(bridge)
            .tunnel(tunnel)
            .toll(toll)
            .names(mapNames(names))
            .curvature(curvature)
            .geometry(mapGeometry(geometry))
            .speedLimit(speedLimit)
            .laneCount(laneCount)
            .meanElevation(meanElevation)
            .countryCode(countryCode)
            .stateCode(stateCode)
            .build()
    }

    private fun mapOut(out: List<ElectronicHorizonEdge>): List<Edge> {
        val outEdges = mutableListOf<Edge>()
        for (electronicHorizonEdge in out) {
            outEdges.add(electronicHorizonEdge.mapToEdge())
        }
        return outEdges.toList()
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
        return NameInfo.Builder()
            .name(name)
            .shielded(shielded)
            .build()
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
        return EHorizonPosition.Builder()
            .edgeId(edgeId)
            .percentAlong(percentAlong)
            .build()
    }
}
