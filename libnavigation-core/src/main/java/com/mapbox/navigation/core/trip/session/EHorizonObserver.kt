package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectDistanceInfo
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectEnterExitInfo
import com.mapbox.navigation.core.trip.model.eh.EHorizonPosition

/**
 * Electronic horizon listener. Callbacks are fired in the order specified.
 * onPositionUpdated might be called multiple times after the other callbacks until a new change to
 * the horizon occurs.
 */
interface EHorizonObserver {

    /**
     * This callback might be called multiple times when the position changes.
     * @param position current electronic horizon position(map matched position + e-horizon tree)
     * @param distances map road object id -> EHorizonObjectDistanceInfo for upcoming road objects
     *
     */
    fun onPositionUpdated(
        position: EHorizonPosition,
        distances: Map<String, EHorizonObjectDistanceInfo>
    )

    /**
     * Called when entry to line-like(i.e. which has length != null) road object was detected
     * @param objectEnterExitInfo contains info related to the object
     */
    fun onRoadObjectEnter(objectEnterExitInfo: EHorizonObjectEnterExitInfo)

    /**
     * Called when exit from line-like(i.e. which has length != null) road object was detected
     * @param objectEnterExitInfo contains info related to the object
     */
    fun onRoadObjectExit(objectEnterExitInfo: EHorizonObjectEnterExitInfo)

    /**
     * This callback is fired whenever road object is added
     * @param roadObjectId id of the object
     */
    fun onRoadObjectAdded(roadObjectId: String)

    /**
     * This callback is fired whenever road object is updated
     * @param roadObjectId id of the object
     */
    fun onRoadObjectUpdated(roadObjectId: String)

    /**
     * This callback is fired whenever road object is removed
     * @param roadObjectId id of the object
     */
    fun onRoadObjectRemoved(roadObjectId: String)
}
