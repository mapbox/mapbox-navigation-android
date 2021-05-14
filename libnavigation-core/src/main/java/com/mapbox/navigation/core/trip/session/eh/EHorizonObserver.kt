package com.mapbox.navigation.core.trip.session.eh

import com.mapbox.navigation.base.trip.model.eh.EHorizonPosition
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectEnterExitInfo
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPassInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo

/**
 * Electronic horizon listener. Callbacks are fired in the order specified.
 * onPositionUpdated might be called multiple times after the other callbacks until a new change to
 * the horizon occurs.
 */
interface EHorizonObserver {

    /**
     * This callback might be called multiple times when the position changes.
     * @param position current electronic horizon position (map matched position + e-horizon tree)
     * @param distances a list of [RoadObjectDistanceInfo] for upcoming road objects
     *
     */
    fun onPositionUpdated(
        position: EHorizonPosition,
        distances: List<RoadObjectDistanceInfo>
    )

    /**
     * Called when entry to line-like (i.e. which has length != null) road object was detected
     * @param objectEnterExitInfo contains info related to the object
     */
    fun onRoadObjectEnter(objectEnterExitInfo: RoadObjectEnterExitInfo)

    /**
     * Called when exit from line-like (i.e. which has length != null) road object was detected
     * @param objectEnterExitInfo contains info related to the object
     */
    fun onRoadObjectExit(objectEnterExitInfo: RoadObjectEnterExitInfo)

    /**
     * Called when the object is passed.
     * @param objectPassInfo contains info related to the object
     */
    fun onRoadObjectPassed(objectPassInfo: RoadObjectPassInfo)

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
