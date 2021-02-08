package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectEdgeLocation
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectLocation
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectMetadata
import com.mapbox.navigation.core.trip.model.eh.OpenLRStandard

interface EHorizonObjectsStore {
    /**
     * Returns mapping `road object id -> EHorizonObjectEdgeLocation` for all road objects
     * which are lying on the edge with given id.
     * @param edgeId
     */
    fun getRoadObjectsOnTheEdge(edgeId: Long): Map<String, EHorizonObjectEdgeLocation>

    /**
     * Returns metadata of object with given id, if such object cannot be found returns null.
     * @param roadObjectId
     */
    fun getRoadObjectMetadata(roadObjectId: String): EHorizonObjectMetadata?

    /**
     * Returns location of object with given id, if such object cannot be found returns null.
     * @param roadObjectId
     */
    fun getRoadObjectLocation(roadObjectId: String): EHorizonObjectLocation?

    /**
     * Returns list of road object ids which are (partially) belong to `edgeIds`.
     * @param edgeIds list of edge ids
     *
     * @return list of road object ids
     */
    fun getRoadObjectIdsByEdgeIds(edgeIds: List<Long>): List<String>

    /**
     * Adds road object to be tracked in electronic horizon. In case if object with such id already
     * exists updates it.
     * @param roadObjectId unique id of the object
     * @param openLRLocation road object location
     * @param standard standard used to encode openLRLocation
     */
    fun addCustomRoadObject(roadObjectId: String, openLRLocation: String, standard: OpenLRStandard)

    /**
     * Removes road object(i.e. stops tracking it in electronic horizon)
     * @param roadObjectId of road object
     */
    fun removeCustomRoadObject(roadObjectId: String)
}
