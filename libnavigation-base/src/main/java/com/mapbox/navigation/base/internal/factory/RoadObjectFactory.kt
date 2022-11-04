package com.mapbox.navigation.base.internal.factory

import android.os.Trace
import android.view.ViewDebug.trace
import androidx.core.os.trace
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.GeometryCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiLineString
import com.mapbox.geojson.MultiPoint
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.Companion.SUPPORTED_ROAD_OBJECTS
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectMatcherError
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.mapToRoadObject
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigator.RoadObjectType
import kotlin.system.measureNanoTime

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

        var sharedInstance: RoadObjectFactory = MapboxRoadObjectFactory.create()

        @Suppress("MaxLineLength")
        fun List<com.mapbox.navigator.UpcomingRouteAlert>.toUpcomingRoadObjects(): List<UpcomingRoadObject> =
            with(sharedInstance) {
                buildUpcomingRoadObjectsList(this@toUpcomingRoadObjects)
            }

        /**
         * Build road object from native object
         */
        fun buildRoadObject(nativeRoadObject: com.mapbox.navigator.RoadObject): RoadObject =
            with(sharedInstance) {
                buildRoadObject(nativeRoadObject)
            }

        /**
         * Build matching error from native error
         */
        fun buildRoadObjectMatchingError(
            nativeError: com.mapbox.navigator.RoadObjectMatcherError
        ): RoadObjectMatcherError =
            with(sharedInstance) {
                buildRoadObjectMatchingError(nativeError)
            }

        /**
         * Build native road object from SDK road object
         */
        fun buildNativeRoadObject(roadObject: RoadObject): com.mapbox.navigator.RoadObject =
            with(sharedInstance) {
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
            with(sharedInstance) {
                buildUpcomingRoadObject(roadObject, distanceToStart, distanceInfo)
            }
    }
}

/**
 * Default implementation of RoadObjectFactory
 */
class MapboxRoadObjectFactory internal constructor() : RoadObjectFactory {
    private val cache = mutableMapOf<String, RoadObject>()
    private var cacheHits = 0

    var cachingStrategy: RoadObjectCachingStrategy = RoadObjectCachingStrategy.ONLY_STATIC_OBJECTS

    override fun buildRoadObject(nativeRoadObject: com.mapbox.navigator.RoadObject): RoadObject =
        traceSection("MapboxRoadObjectFactory.buildRoadObject") {
            var cacheHit = false

            val res: RoadObject
            val cacheKey: String?
//        val time = measureNanoTime {
            cacheKey = cachingStrategy.getCacheKey(nativeRoadObject)
            res = if (cacheKey != null) {
                cacheHit = true
                cache.getOrPut(cacheKey) {
                    cacheHit = false
                    nativeRoadObject.mapToRoadObject()
                }
            } else {
                nativeRoadObject.mapToRoadObject()
            }
//        }

            if (cacheHit) cacheHits++
//        val processingTime = "${time}ns"
//        val type = nativeRoadObject.type.name
//        logD("MapboxRoadObjectFactory") { "buildRoadObject processingTime=$processingTime; [cached=$cacheHit; type=$type; cacheKey=$cacheKey]" }
            return@traceSection res
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
            // --------- NON CACHE processing measurements
            logD("MapboxRoadObjectFactory") { "buildUpcomingRoadObjectsList -------------------------------" }
            cachingStrategy = RoadObjectCachingStrategy.NONE
            val nonCacheTime = measureNanoTime {
                traceSection("MapboxRoadObjectFactory.buildUpcomingRoadObjectsList.0") {
                    alertsList
                        .filter { SUPPORTED_ROAD_OBJECTS.contains(it.roadObject.type) }
                        .map(this::buildUpcomingRoadObject)
                }
            }
            logD("MapboxRoadObjectFactory") { "buildUpcomingRoadObjectsList count = ${alertsList.size}; cacheStrategy = NONE                ; processingTime = ${nonCacheTime}ns " }


        // ----------- ONLY_STATIC_OBJECTS
        cachingStrategy = RoadObjectCachingStrategy.ONLY_STATIC_OBJECTS

        cacheHits = 0
        val withCacheTime1 = measureNanoTime {
            traceSection("MapboxRoadObjectFactory.buildUpcomingRoadObjectsList.1") {
                alertsList
                    .filter { SUPPORTED_ROAD_OBJECTS.contains(it.roadObject.type) }
                    .map(this::buildUpcomingRoadObject)
            }
        }
        var cacheStats = "hits = $cacheHits, miss = ${alertsList.size - cacheHits}"
        logD("MapboxRoadObjectFactory") { "buildUpcomingRoadObjectsList count = ${alertsList.size}; cacheStrategy = ONLY_STATIC_OBJECTS ; processingTime = ${withCacheTime1}ns [diff ${nonCacheTime - withCacheTime1}ns]; cache($cacheStats)" }


        // ----------- ONLY_STATIC_OBJECTS22
        cachingStrategy = RoadObjectCachingStrategy.ONLY_STATIC_OBJECTS2

        var res: List<UpcomingRoadObject>
        cacheHits = 0
        val withCacheTime2 = measureNanoTime {
            res = traceSection("MapboxRoadObjectFactory.buildUpcomingRoadObjectsList.2") {
                alertsList
                    .filter { SUPPORTED_ROAD_OBJECTS.contains(it.roadObject.type) }
                    .map(this::buildUpcomingRoadObject)
            }
        }
        cacheStats = "hits = $cacheHits, miss = ${alertsList.size - cacheHits}"
        logD("MapboxRoadObjectFactory") { "buildUpcomingRoadObjectsList count = ${alertsList.size}; cacheStrategy = ONLY_STATIC_OBJECTS2; processingTime = ${withCacheTime2}ns [diff ${nonCacheTime - withCacheTime2}ns]; cache($cacheStats)" }

        return res
    }

    companion object {

        fun create(): MapboxRoadObjectFactory = MapboxRoadObjectFactory()
    }
}

@Suppress("ClassName")
abstract class RoadObjectCachingStrategy {

    abstract fun getCacheKey(nativeRoadObject: com.mapbox.navigator.RoadObject): String?

    object NONE : RoadObjectCachingStrategy() {
        override fun getCacheKey(
            nativeRoadObject: com.mapbox.navigator.RoadObject
        ): String? = null
    }

    // Static road object caching strategy that uses Geometry.toString() as the cache key.
    object ONLY_STATIC_OBJECTS : RoadObjectCachingStrategy() {
        override fun getCacheKey(
            nativeRoadObject: com.mapbox.navigator.RoadObject
        ): String? = traceSection("RoadObjectCachingStrategy.ONLY_STATIC_OBJECTS.getCacheKey") {
            when (nativeRoadObject.type) {
                // we are only caching static road objects
                RoadObjectType.TUNNEL,
                RoadObjectType.BORDER_CROSSING,
                RoadObjectType.BRIDGE,
                RoadObjectType.RAILWAY_CROSSING,
                RoadObjectType.TOLL_COLLECTION_POINT -> {
                    nativeRoadObject.id.ifEmpty {
                        val geometry = nativeRoadObject.location.getGeometry()
                        "${nativeRoadObject.type}.$geometry"
                    }
                }

                // the following road objects are considered dynamic and are always re-created
                // RoadObjectType.RESTRICTED_AREA
                // RoadObjectType.INCIDENT,
                // RoadObjectType.SERVICE_AREA,
                // RoadObjectType.CUSTOM
                else -> null
            }
        }
    }

    // Static road object caching strategy that uses first Point.toString() from the Geometry as the cache key.
    object ONLY_STATIC_OBJECTS2 : RoadObjectCachingStrategy() {

        override fun getCacheKey(
            nativeRoadObject: com.mapbox.navigator.RoadObject
        ): String? =
            traceSection("RoadObjectCachingStrategy.ONLY_STATIC_OBJECTS2.getCacheKey") {
                when (nativeRoadObject.type) {
                    // we are only caching static road objects
                    RoadObjectType.TUNNEL,
                    RoadObjectType.BORDER_CROSSING,
                    RoadObjectType.BRIDGE,
                    RoadObjectType.RAILWAY_CROSSING,
                    RoadObjectType.TOLL_COLLECTION_POINT -> {
                        nativeRoadObject.id.ifEmpty {
                            val point = nativeRoadObject.location.getGeometry().firstPoint()
                                ?: return@traceSection null
                            "${nativeRoadObject.type}.${point.latitude()}.${point.longitude()}"
                        }
                    }

                    // the following road objects are considered dynamic and are always re-created
                    // RoadObjectType.RESTRICTED_AREA
                    // RoadObjectType.INCIDENT,
                    // RoadObjectType.SERVICE_AREA,
                    // RoadObjectType.CUSTOM
                    else -> null
                }
            }

        private fun Geometry.firstPoint(): Point? {
            return when (this) {
                is Point -> this
                is MultiPoint -> coordinates().firstOrNull()
                is LineString -> coordinates().firstOrNull()
                is MultiLineString -> coordinates().firstOrNull()?.firstOrNull()
                is Polygon -> coordinates()?.firstOrNull()?.firstOrNull()
                is MultiPolygon -> coordinates()?.firstOrNull()?.firstOrNull()?.firstOrNull()
                is GeometryCollection -> geometries()?.firstOrNull()?.firstPoint()
                else -> null
            }
        }

    }

    internal fun com.mapbox.navigator.MatchedRoadObjectLocation.getGeometry(): Geometry =
        traceSection("RoadObjectCachingStrategy.getGeometry") {
            when {
                isMatchedGantryLocation -> matchedGantryLocation.shape
                isMatchedPointLocation -> matchedPointLocation.position.coordinate
                isMatchedPolygonLocation -> matchedPolygonLocation.shape
                isMatchedPolylineLocation -> matchedPolylineLocation.shape
                isOpenLRLineLocation -> openLRLineLocation.shape
                isOpenLRPointAlongLineLocation -> openLRPointAlongLineLocation.coordinate
                isRouteAlertLocation -> routeAlertLocation.shape
                isMatchedSubgraphLocation -> matchedSubgraphLocation.shape
                else -> throw IllegalArgumentException("Unsupported object location type.")
            }
        }
}

private inline fun <R> traceSection(name: String, crossinline block: () -> R): R {
    Trace.beginSection(name)
    return block().also { Trace.endSection() }
}
