package com.mapbox.navigation.core.trip.session

import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.navigator.getBorderCrossingInfo
import com.mapbox.navigation.core.navigator.getIncidentInfo
import com.mapbox.navigation.core.navigator.getRestStopType
import com.mapbox.navigation.core.navigator.getRoadObject
import com.mapbox.navigation.core.navigator.getTollCollectionType
import com.mapbox.navigation.core.navigator.getTunnelInfo
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectDistanceInfo
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectEdgeLocation
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectLocation
import com.mapbox.navigation.core.trip.model.eh.EHorizonObjectMetadata
import com.mapbox.navigation.core.trip.model.eh.OpenLRStandard
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectEdgeLocation
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectLocation
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonObjectProvider
import com.mapbox.navigation.core.trip.model.eh.mapToGraphPath
import com.mapbox.navigation.core.trip.model.eh.mapToGraphPosition
import com.mapbox.navigation.core.trip.model.eh.mapToOpenLRStandard
import com.mapbox.navigation.core.trip.model.eh.mapToRoadObjectType
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.OpenLRLocation

/**
 * [MapboxNavigation.roadObjectsStore] provides methods to get road objects metadata, add and remove
 * custom road objects.
 */
class RoadObjectsStore internal constructor(
    private val navigator: MapboxNativeNavigator,
    private val logger: Logger
) {
    private val TAG = Tag("MbxRoadObjectsStore")

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
            EHorizonObjectMetadata(
                it.type.mapToRoadObjectType(),
                it.provider.mapToEHorizonObjectProvider(),
                getIncidentInfo(it.incident),
                getTunnelInfo(it.tunnelInfo),
                getBorderCrossingInfo(it.borderCrossingInfo),
                getTollCollectionType(it.tollCollectionInfo),
                getRestStopType(it.serviceAreaInfo),
            )
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
     * Matches given polyline to graph and adds it for object tracking.
     * Polyline should define valid path on graph,
     * i.e. it should be possible to drive this path according to traffic rules.
     * In case of error (if there are no tiles in cache, decoding failed, etc.) object won't be added.
     *
     * @param roadObjectId unique id of the object
     * @param polyline polyline representing the object
     */
    @ExperimentalMapboxNavigationAPI
    fun addCustomPolylineObject(roadObjectId: String, polyline: List<Point>) {
        navigator.openLRDecoder?.decodePolylines(listOf(polyline)) {
            addObjectToStore(roadObjectId, it)
        }
    }

    /**
     * Matches given polygon to graph and adds it for object tracking.
     * "Matching" here means we try to find all intersections of polygon with the road graph
     * and track distances to those intersections as distance to polygon.
     * In case of error (if there are no tiles in cache, decoding failed, etc.) object won't be added.
     *
     * @param roadObjectId unique id of the object
     * @param polygon polygon representing the object
     */
    @ExperimentalMapboxNavigationAPI
    fun addCustomPolygonObject(roadObjectId: String, polygon: List<Point>) {
        navigator.openLRDecoder?.decodePolygons(listOf(polygon)) {
            addObjectToStore(roadObjectId, it)
        }
    }

    /**
     * Matches given gantry (i.e. polyline orthogonal to the road) to the graph and adds it
     * for object tracking.
     * "Matching" here means we try to find all intersections of gantry with road graph
     * and track distances to those intersections as distance to gantry.
     * In case of error (if there are no tiles in cache, decoding failed, etc.) object won't be added.
     *
     * @param roadObjectId unique id of the object
     * @param gantry gantry representing the object
     */
    @ExperimentalMapboxNavigationAPI
    fun addCustomGantryObject(roadObjectId: String, gantry: List<Point>) {
        navigator.openLRDecoder?.decodeGantries(listOf(gantry)) {
            addObjectToStore(roadObjectId, it)
        }
    }

    /**
     * Matches given point to road graph and adds it for object tracking.
     * In case of error (if there are no tiles in cache, decoding failed, etc.) object won't be added.
     *
     * @param roadObjectId unique id of the object
     * @param point point representing the object
     */
    @ExperimentalMapboxNavigationAPI
    fun addCustomPointObject(roadObjectId: String, point: Point) {
        navigator.openLRDecoder?.decodePoints(listOf(point)) { addObjectToStore(roadObjectId, it) }
    }

    /**
     * Removes road object(i.e. stops tracking it in electronic horizon)
     * @param roadObjectId of road object
     */
    fun removeCustomRoadObject(roadObjectId: String) {
        navigator.roadObjectsStore?.removeCustomRoadObject(roadObjectId)
    }

    /**
     * Returns a list of [UpcomingRoadObject]
     * @param distances a map of [String] roadObjectIds and [EHorizonObjectDistanceInfo]
     */
    fun getUpcomingRoadObjects(
        distances: Map<String, EHorizonObjectDistanceInfo>
    ): List<UpcomingRoadObject> {
        val upcomingObjects = mutableListOf<UpcomingRoadObject>()
        distances.forEach {
            val objectId = it.key
            val distanceInfo = it.value

            buildRoadObject(objectId, distanceInfo)?.let { roadObject ->
                upcomingObjects.add(
                    UpcomingRoadObject.Builder(roadObject, distanceInfo.distanceToEntry).build()
                )
            }
        }

        return upcomingObjects
    }

    private fun buildRoadObject(
        objectId: String,
        distanceInfo: EHorizonObjectDistanceInfo
    ): RoadObject? {
        val shape = getRoadObjectShape(objectId)
        val metadata = navigator.roadObjectsStore?.getRoadObjectMetadata(objectId)

        return ifNonNull(shape, metadata) { nonNullShape, nonNullMetadata ->
            val geometry = RoadObjectGeometry.Builder(
                distanceInfo.length,
                nonNullShape,
                null,
                null
            ).build()

            getRoadObject(nonNullMetadata, geometry)
        }
    }

    private fun getRoadObjectShape(objectId: String): Geometry? {
        with(navigator) {
            val location = roadObjectsStore
                ?.getRoadObjectLocation(objectId)
                ?.mapToEHorizonObjectLocation()

            var shape: Geometry? = null
            location?.path?.let {
                graphAccessor?.getPathShape(it.mapToGraphPath())?.let { points ->
                    shape = LineString.fromLngLats(points)
                }
            }
            location?.position?.let {
                graphAccessor?.getPositionCoordinate(it.mapToGraphPosition())?.let { point ->
                    shape = Point.fromLngLat(point.longitude(), point.latitude())
                }
            }

            return shape
        }
    }

    private fun addObjectToStore(
        roadObjectId: String,
        locations: List<Expected<OpenLRLocation, String>>
    ) {
        if (locations.isEmpty()) {
            logger.d(TAG, Message("No locations decoded for roadObjectId = $roadObjectId."))
            return
        }

        val either = locations.first()
        if (either.isValue) {
            navigator.roadObjectsStore?.addCustomRoadObject(roadObjectId, either.value!!)
        } else {
            logger.d(
                TAG,
                Message("Decoding failed. Error = ${either.error}, roadObjectId = $roadObjectId.")
            )
        }
    }
}
