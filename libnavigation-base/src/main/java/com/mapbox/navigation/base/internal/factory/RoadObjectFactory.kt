package com.mapbox.navigation.base.internal.factory

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectMatcherError
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.mapToRoadObject
import com.mapbox.navigator.RoadObjectType

/**
 * Factory for building road objects
 */
interface RoadObjectFactory {

    /**
     * Build road object from native object
     */
    fun buildRoadObject(nativeRoadObject: com.mapbox.navigator.RoadObject): RoadObject

    /**
     * Build native road object from SDK road object
     */
    fun buildNativeRoadObject(roadObject: RoadObject): com.mapbox.navigator.RoadObject

    /**
     * Build matching error from native error
     */
    fun buildRoadObjectMatchingError(
        nativeError: com.mapbox.navigator.RoadObjectMatcherError
    ): RoadObjectMatcherError

    /**
     * Build upcoming road object
     */
    fun buildUpcomingRoadObject(
        roadObject: RoadObject,
        distanceToStart: Double?,
        distanceInfo: RoadObjectDistanceInfo?
    ): UpcomingRoadObject

    /**
     * Build upcoming road object from native UpcomingRouteAlert
     */
    fun buildUpcomingRoadObject(
        nativeAlert: com.mapbox.navigator.UpcomingRouteAlert
    ): UpcomingRoadObject

    /**
     * Build a list of upcoming road objects from the native UpcomingRouteAlert list
     */
    fun buildUpcomingRoadObjectsList(
        alertsList: List<com.mapbox.navigator.UpcomingRouteAlert>
    ): List<UpcomingRoadObject>

    companion object {

        private var sharedInstance: RoadObjectFactory = MapboxRoadObjectFactory.create()

        fun getInstance(): RoadObjectFactory = sharedInstance

        fun setInstance(factory: RoadObjectFactory) {
            sharedInstance = factory
        }

        @Suppress("MaxLineLength")
        fun List<com.mapbox.navigator.UpcomingRouteAlert>.toUpcomingRoadObjects(): List<UpcomingRoadObject> =
            with(getInstance()) {
                buildUpcomingRoadObjectsList(this@toUpcomingRoadObjects)
            }

        /**
         * Build road object from native object
         */
        fun buildRoadObject(nativeRoadObject: com.mapbox.navigator.RoadObject): RoadObject =
            with(getInstance()) {
                buildRoadObject(nativeRoadObject)
            }

        /**
         * Build matching error from native error
         */
        fun buildRoadObjectMatchingError(
            nativeError: com.mapbox.navigator.RoadObjectMatcherError
        ): RoadObjectMatcherError =
            with(getInstance()) {
                buildRoadObjectMatchingError(nativeError)
            }

        /**
         * Build native road object from SDK road object
         */
        fun buildNativeRoadObject(roadObject: RoadObject): com.mapbox.navigator.RoadObject =
            with(getInstance()) {
                buildNativeRoadObject(roadObject)
            }

        /**
         * Build upcoming road object
         */
        fun buildUpcomingRoadObject(
            roadObject: RoadObject,
            distanceToStart: Double?,
            distanceInfo: RoadObjectDistanceInfo?
        ): UpcomingRoadObject =
            with(getInstance()) {
                buildUpcomingRoadObject(roadObject, distanceToStart, distanceInfo)
            }
    }
}

/**
 * Default implementation of RoadObjectFactory
 */
class MapboxRoadObjectFactory internal constructor() : RoadObjectFactory {

    override fun buildRoadObject(nativeRoadObject: com.mapbox.navigator.RoadObject): RoadObject {
        return nativeRoadObject.mapToRoadObject()
    }

    override fun buildNativeRoadObject(roadObject: RoadObject): com.mapbox.navigator.RoadObject {
        // we can't build native road objects on SDK side because of some native classes
        // constructors limitations (e.g. RoadObjectMetadata can't be built for any object)
        // we use internal link to the native object
        return roadObject.nativeRoadObject
    }

    override fun buildRoadObjectMatchingError(
        nativeError: com.mapbox.navigator.RoadObjectMatcherError
    ): RoadObjectMatcherError {
        with(nativeError) {
            return RoadObjectMatcherError(roadObjectId, description)
        }
    }

    override fun buildUpcomingRoadObject(
        roadObject: RoadObject,
        distanceToStart: Double?,
        distanceInfo: RoadObjectDistanceInfo?
    ): UpcomingRoadObject {
        return UpcomingRoadObject(roadObject, distanceToStart, distanceInfo)
    }

    override fun buildUpcomingRoadObject(
        nativeAlert: com.mapbox.navigator.UpcomingRouteAlert
    ): UpcomingRoadObject {
        return buildUpcomingRoadObject(
            RoadObjectFactory.buildRoadObject(nativeAlert.roadObject),
            nativeAlert.distanceToStart,
            null
        )
    }

    override fun buildUpcomingRoadObjectsList(
        alertsList: List<com.mapbox.navigator.UpcomingRouteAlert>
    ): List<UpcomingRoadObject> {
        return alertsList
            .filter { SUPPORTED_ROAD_OBJECTS.contains(it.roadObject.type) }
            .map(this::buildUpcomingRoadObject)
    }

    companion object {
        val SUPPORTED_ROAD_OBJECTS = arrayOf(
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

        fun create(): MapboxRoadObjectFactory = MapboxRoadObjectFactory()
    }
}
