package com.mapbox.navigation.base.internal.factory

import com.mapbox.navigation.base.trip.model.eh.EHorizonGraphPath
import com.mapbox.navigation.base.trip.model.eh.EHorizonGraphPosition
import com.mapbox.navigation.base.trip.model.eh.MatchableGeometry
import com.mapbox.navigation.base.trip.model.eh.MatchableOpenLr
import com.mapbox.navigation.base.trip.model.eh.MatchablePoint
import com.mapbox.navigation.base.trip.model.eh.mapToEHorizonEdgeMetadata
import com.mapbox.navigation.base.trip.model.eh.mapToEHorizonPosition
import com.mapbox.navigation.base.trip.model.eh.mapToNativeGraphPath
import com.mapbox.navigation.base.trip.model.eh.mapToNativeGraphPosition
import com.mapbox.navigation.base.trip.model.eh.mapToNativeMatchableGeometry
import com.mapbox.navigation.base.trip.model.eh.mapToNativeMatchableOpenLr
import com.mapbox.navigation.base.trip.model.eh.mapToNativeMatchablePoint
import com.mapbox.navigation.base.trip.model.eh.mapToRoadObjectDistance
import com.mapbox.navigation.base.trip.model.eh.mapToRoadObjectEdgeLocation
import com.mapbox.navigation.base.trip.model.eh.mapToRoadObjectEnterExitInfo
import com.mapbox.navigation.base.trip.model.eh.mapToRoadObjectPassInfo
import com.mapbox.navigator.RoadObjectEdgeLocation

/**
 * Internal factory to build EHorizon objects
 */
object EHorizonFactory {

    /**
     * Build RoadObjectEnterExitInfo
     */
    fun buildRoadObjectEnterExitInfo(enterExitInfo: com.mapbox.navigator.RoadObjectEnterExitInfo) =
        enterExitInfo.mapToRoadObjectEnterExitInfo()

    /**
     * Build RoadObjectPassInfo
     */
    fun buildRoadObjectPassInfo(passInfo: com.mapbox.navigator.RoadObjectPassInfo) =
        passInfo.mapToRoadObjectPassInfo()

    /**
     * Build EHorizonPosition
     */
    fun buildEHorizonPosition(position: com.mapbox.navigator.ElectronicHorizonPosition) =
        position.mapToEHorizonPosition()

    /**
     * Build RoadObjectDistance
     */
    fun buildRoadObjectDistance(distance: com.mapbox.navigator.RoadObjectDistance) =
        distance.mapToRoadObjectDistance()

    /**
     * Build EHorizonEdgeMetadata
     */
    fun buildEHorizonEdgeMetadata(edgeMetadata: com.mapbox.navigator.EdgeMetadata) =
        edgeMetadata.mapToEHorizonEdgeMetadata()

    /**
     * Build RoadObjectEdgeLocation
     */
    fun buildRoadObjectEdgeLocation(edgeLocation: RoadObjectEdgeLocation) =
        edgeLocation.mapToRoadObjectEdgeLocation()

    /**
     * Build native GraphPath
     */
    fun buildNativeGraphPath(graphPath: EHorizonGraphPath) = graphPath.mapToNativeGraphPath()

    /**
     * Build native GraphPosition
     */
    fun buildNativeGraphPosition(graphPosition: EHorizonGraphPosition) =
        graphPosition.mapToNativeGraphPosition()

    fun buildNativeMatchableOpenLr(
        matchable: MatchableOpenLr,
    ) = matchable.mapToNativeMatchableOpenLr()

    fun buildNativeMatchableGeometry(
        matchable: MatchableGeometry,
    ) = matchable.mapToNativeMatchableGeometry()

    fun buildNativeMatchablePoint(
        matchable: MatchablePoint,
    ) = matchable.mapToNativeMatchablePoint()
}
