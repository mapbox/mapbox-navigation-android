package com.mapbox.navigation.core.trip.session.eh

import com.mapbox.navigation.base.internal.factory.EHorizonFactory
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectEdgeLocation
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator

/**
 * [MapboxNavigation.roadObjectsStore] provides methods to get road objects metadata, add and remove
 * custom road objects.
 */
class RoadObjectsStore internal constructor(
    private val navigator: MapboxNativeNavigator,
) {

    /**
     * Returns mapping `road object id -> RoadObjectEdgeLocation` for all road objects
     * which are lying on the edge with given id.
     * @param edgeId
     */
    fun getRoadObjectsOnTheEdge(edgeId: Long): Map<String, RoadObjectEdgeLocation> {
        val roadObjects = mutableMapOf<String, RoadObjectEdgeLocation>()
        navigator.roadObjectsStore.get(edgeId).entries
            .forEach { (objectId, objectEdgeLocation) ->
                roadObjects[objectId] =
                    EHorizonFactory.buildRoadObjectEdgeLocation(objectEdgeLocation)
            }

        return roadObjects
    }

    /**
     * Returns roadObject, if such object cannot be found returns null.
     * @param roadObjectId id of the road object
     */
    fun getRoadObject(roadObjectId: String): RoadObject? {
        return navigator.roadObjectsStore.getRoadObject(roadObjectId)?.let {
            RoadObjectFactory.buildRoadObject(it)
        }
    }

    /**
     * Returns list of road object ids which are (partially) belong to `edgeIds`.
     * @param edgeIds list of edge ids
     *
     * @return list of road object ids
     */
    fun getRoadObjectIdsByEdgeIds(edgeIds: List<Long>): List<String> {
        return navigator.roadObjectsStore.getRoadObjectIdsByEdgeIds(edgeIds)
    }

    /**
     * Adds road object to be tracked in electronic horizon. In case if object with such id already
     * exists updates it.
     * @param roadObject object to add
     */
    fun addCustomRoadObject(roadObject: RoadObject) {
        val nativeRoadObject = RoadObjectFactory.buildNativeRoadObject(roadObject)
        navigator.roadObjectsStore.addCustomRoadObject(nativeRoadObject)
    }

    /**
     * Removes custom road object (i.e. stops tracking it in electronic horizon)
     * @param roadObjectId id of the road object
     */
    fun removeCustomRoadObject(roadObjectId: String) {
        navigator.roadObjectsStore.removeCustomRoadObject(roadObjectId)
    }

    /**
     * Removes all custom road objects (i.e. stops tracking them in electronic horizon)
     */
    fun removeAllCustomRoadObjects() {
        navigator.roadObjectsStore.removeAllCustomRoadObjects()
    }

    /**
     * Returns a list of [UpcomingRoadObject]
     * @param distances a list of [RoadObjectDistanceInfo]
     */
    fun getUpcomingRoadObjects(
        distances: List<RoadObjectDistanceInfo>,
    ): List<UpcomingRoadObject> {
        val upcomingObjects = mutableListOf<UpcomingRoadObject>()
        distances.forEach {
            getRoadObject(it.roadObjectId)?.let { roadObject ->
                upcomingObjects.add(
                    RoadObjectFactory.buildUpcomingRoadObject(
                        roadObject,
                        it.distanceToStart,
                        it,
                    ),
                )
            }
        }

        return upcomingObjects
    }
}
