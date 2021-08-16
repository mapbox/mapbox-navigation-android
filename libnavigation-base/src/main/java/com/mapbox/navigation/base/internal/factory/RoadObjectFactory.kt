package com.mapbox.navigation.base.internal.factory

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectMatcherError
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.mapToRoadObject

/**
 * Internal factory to build road objects
 */
object RoadObjectFactory {

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
