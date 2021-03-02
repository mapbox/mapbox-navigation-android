package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectEdgeLocation
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectLocation
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectMetadata
import com.mapbox.navigation.core.trip.model.eh.OpenLRStandard
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectEdgeLocation
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectLocation
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectProvider
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectType
import com.mapbox.navigation.core.trip.model.eh.mapToOpenLRStandard
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator

/**
 * [MapboxNavigation.roadObjectsStore] provides methods to get road objects metadata, add and remove
 * custom road objects.
 */
class RoadObjectsStore internal constructor(
    private val navigator: MapboxNativeNavigator,
) {

    /**
     * Returns mapping `road object id -> EHorizonObjectEdgeLocation` for all road objects
     * which are lying on the edge with given id.
     * @param edgeId
     */
    fun getRoadObjectsOnTheEdge(edgeId: Long): Map<String, EHorizonObjectEdgeLocation> {
        val roadObjects = mutableMapOf<String, EHorizonObjectEdgeLocation>()
        navigator.roadObjectsStore?.get(edgeId)?.forEach { (objectId, objectEdgeLocation) ->
            roadObjects[objectId] = objectEdgeLocation.mapToEHorizonObjectEdgeLocation()
        }

        return roadObjects
    }

    /**
     * Returns metadata of object with given id, if such object cannot be found returns null.
     * @param roadObjectId
     */
    fun getRoadObjectMetadata(roadObjectId: String): EHorizonObjectMetadata? {
        return navigator.roadObjectsStore?.getRoadObjectMetadata(roadObjectId)?.let {
            navigator.navigatorMapper.run {
                EHorizonObjectMetadata(
                    it.type.mapToEHorizonObjectType(),
                    it.provider.mapToEHorizonObjectProvider(),
                    getIncidentInfo(it.incident),
                    getTunnelInfo(it.tunnelInfo),
                    getBorderCrossingInfo(it.borderCrossingInfo),
                    getTollCollectionType(it.tollCollectionInfo),
                    getRestStopType(it.serviceAreaInfo),
                )
            }
        }
    }

    /**
     * Returns location of object with given id, if such object cannot be found returns null.
     * @param roadObjectId
     */
    fun getRoadObjectLocation(roadObjectId: String): EHorizonObjectLocation? {
        return navigator.roadObjectsStore?.getRoadObjectLocation(roadObjectId)
            ?.mapToEHorizonObjectLocation()
    }

    /**
     * Returns list of road object ids which are (partially) belong to `edgeIds`.
     * @param edgeIds list of edge ids
     *
     * @return list of road object ids
     */
    fun getRoadObjectIdsByEdgeIds(edgeIds: List<Long>): List<String> {
        return navigator.roadObjectsStore?.getRoadObjectIdsByEdgeIds(edgeIds) ?: emptyList()
    }

    /**
     * Adds road object to be tracked in electronic horizon. In case if object with such id already
     * exists updates it.
     * @param roadObjectId unique id of the object
     * @param openLRLocation road object location
     * @param openLRStandard standard used to encode openLRLocation
     */
    fun addCustomRoadObject(
        roadObjectId: String,
        openLRLocation: String,
        @OpenLRStandard.Type openLRStandard: String
    ) {
        navigator.openLRDecoder?.decode(
            listOf(openLRLocation),
            openLRStandard.mapToOpenLRStandard()
        ) { locations ->
            locations.first().value?.let { openLRLocation ->
                navigator.roadObjectsStore?.addCustomRoadObject(roadObjectId, openLRLocation)
            }
        }
    }

    /**
     * Removes road object(i.e. stops tracking it in electronic horizon)
     * @param roadObjectId of road object
     */
    fun removeCustomRoadObject(roadObjectId: String) {
        navigator.roadObjectsStore?.removeCustomRoadObject(roadObjectId)
    }
}
