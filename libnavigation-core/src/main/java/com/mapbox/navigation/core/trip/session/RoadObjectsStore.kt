package com.mapbox.navigation.core.trip.session

import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.MapboxNavigation
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

            navigator.navigatorMapper.getRoadObject(nonNullMetadata, geometry)
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
}
