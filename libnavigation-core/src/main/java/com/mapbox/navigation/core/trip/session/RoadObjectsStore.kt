package com.mapbox.navigation.core.trip.session

import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
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
import com.mapbox.navigator.OpenLRLocation

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
            listOf(openLRLocation), openLRStandard.mapToOpenLRStandard()
        ) { addObjectToStore(roadObjectId, it) }
    }

    /**
     * Matches given polylines to graph and add it for object tracking.
     * Polyline should define valid path on graph,
     * i.e. it should be possible to drive this path according to traffic rules.
     * In case of error(if there are no tiles in cache, decoding failed etc) object wont be added.
     */
    @ExperimentalEHorizonAPI
    fun addCustomPolylinesObject(roadObjectId: String, polylines: List<List<Point>>) {
        navigator.openLRDecoder?.decodePolylines(polylines) { addObjectToStore(roadObjectId, it) }
    }

    /**
     * Matches given polygons to graph and add it for object tracking.
     * "Matching" here means we try to find all intersections of polygon with the road graph
     * and track distances to those intersections as distance to polygon.
     * In case of error(if there are no tiles in cache, decoding failed etc) object wont be added.
     */
    @ExperimentalEHorizonAPI
    fun addCustomPolygonsObject(roadObjectId: String, polygons: List<List<Point>>) {
        navigator.openLRDecoder?.decodePolygons(polygons) { addObjectToStore(roadObjectId, it) }
    }

    /**
     * Matches given gantries(i.e. polylines orthogonal to the road) to the graph and add it
     * for object tracking.
     * "Matching" here means we try to find all intersections of gantry with road graph
     * and track distances to those intersections as distance to gantry.
     * In case of error(if there are no tiles in cache, decoding failed etc) object wont be added.
     */
    @ExperimentalEHorizonAPI
    fun addCustomGantriesObject(roadObjectId: String, gantries: List<List<Point>>) {
        navigator.openLRDecoder?.decodeGantries(gantries) { addObjectToStore(roadObjectId, it) }
    }

    /**
     * Matches given points to road graph and add it for object tracking.
     * In case of error(if there are no tiles in cache, decoding failed etc) object wont be added.
     */
    @ExperimentalEHorizonAPI
    fun addCustomPointsObject(roadObjectId: String, points: List<Point>) {
        navigator.openLRDecoder?.decodePoints(points) { addObjectToStore(roadObjectId, it) }
    }

    /**
     * Removes road object(i.e. stops tracking it in electronic horizon)
     * @param roadObjectId of road object
     */
    fun removeCustomRoadObject(roadObjectId: String) {
        navigator.roadObjectsStore?.removeCustomRoadObject(roadObjectId)
    }

    private fun addObjectToStore(
        roadObjectId: String,
        locations: List<Expected<OpenLRLocation, String>>
    ) {
        locations.first().value?.let { openLRLocation ->
            navigator.roadObjectsStore?.addCustomRoadObject(roadObjectId, openLRLocation)
        }
    }
}
