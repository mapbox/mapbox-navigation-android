package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.trip.model.eh.EHorizonObject
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectDistanceInfo
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

    fun onRoadObjectEnter(eHorizonObject: EHorizonObject)

    fun onRoadObjectExit(eHorizonObject: EHorizonObject)

    fun onRoadObjectAdded(eHorizonObject: EHorizonObject)

    fun onRoadObjectUpdated(eHorizonObject: EHorizonObject)

    fun onRoadObjectRemoved(eHorizonObject: EHorizonObject)
}
