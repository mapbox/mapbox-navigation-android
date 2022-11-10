package com.mapbox.navigation.base.internal.factory

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectMatcherError
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.mapToRoadObject
import com.mapbox.navigator.RoadObjectType

/**
 * Internal factory to build road objects
 */
object RoadObjectFactory {

    private val SUPPORTED_ROAD_OBJECTS = arrayOf(
        RoadObjectType.INCIDENT,
        RoadObjectType.TOLL_COLLECTION_POINT,
        RoadObjectType.BORDER_CROSSING,
        RoadObjectType.TUNNEL,
        RoadObjectType.RESTRICTED_AREA,
        RoadObjectType.SERVICE_AREA,
        RoadObjectType.BRIDGE,
        RoadObjectType.CUSTOM,
        RoadObjectType.RAILWAY_CROSSING,
    )

    fun List<com.mapbox.navigator.UpcomingRouteAlert>.toUpcomingRoadObjects():
        List<UpcomingRoadObject> {
        return this
            .filter { SUPPORTED_ROAD_OBJECTS.contains(it.roadObject.type) }
            .map {
                buildUpcomingRoadObject(
                    buildRoadObject(it.roadObject),
                    it.distanceToStart,
                    null
                )
            }
    }

    /**
     * Build road object from native object
     */
    fun buildRoadObject(nativeRoadObject: com.mapbox.navigator.RoadObject) =
        nativeRoadObject.mapToRoadObject()

    /**
     * Build matching error from native error
     */
    fun buildRoadObjectMatchingError(
        nativeError: com.mapbox.navigator.RoadObjectMatcherError
    ): RoadObjectMatcherError {
        with(nativeError) {
            return RoadObjectMatcherError(roadObjectId, description)
        }
    }

    /**
     * Build native road object from SDK road object
     */
    fun buildNativeRoadObject(roadObject: RoadObject): com.mapbox.navigator.RoadObject {
        // we can't build native road objects on SDK side because of some native classes
        // constructors limitations (e.g. RoadObjectMetadata can't be built for any object)
        // we use internal link to the native object
        return roadObject.nativeRoadObject
    }

    /**
     * Build upcoming road object
     */
    fun buildUpcomingRoadObject(
        roadObject: RoadObject,
        distanceToStart: Double?,
        distanceInfo: RoadObjectDistanceInfo?
    ) =
        UpcomingRoadObject(roadObject, distanceToStart, distanceInfo)
}
