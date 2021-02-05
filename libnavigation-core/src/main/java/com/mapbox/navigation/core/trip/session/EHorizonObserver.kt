package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectDistanceInfo
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectEnterExitInfo
import com.mapbox.navigation.core.trip.model.eh.EHorizonPosition

interface EHorizonObserver {

    /**
     * This callback is fired whenever the location on the EHorizon changes.
     * This will always relate to the EHorizon previously conveyed through
     * onElectronicHorizonUpdated. The location contains the edge and the progress along that edge
     * in the EHorizon.
     *
     * @see EHorizonPosition
     */
    fun onPositionUpdated(
        position: EHorizonPosition,
        distances: Map<String, EHorizonObjectDistanceInfo>
    )

    fun onRoadObjectEnter(objectEnterExitInfo: EHorizonObjectEnterExitInfo)

    fun onRoadObjectExit(objectEnterExitInfo: EHorizonObjectEnterExitInfo)

    fun onRoadObjectAdded(roadObjectId: String)

    fun onRoadObjectUpdated(roadObjectId: String)

    fun onRoadObjectRemoved(roadObjectId: String)
}
