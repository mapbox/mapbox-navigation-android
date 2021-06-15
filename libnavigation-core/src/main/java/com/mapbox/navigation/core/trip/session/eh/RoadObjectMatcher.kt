package com.mapbox.navigation.core.trip.session.eh

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.factory.EHorizonFactory
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
import com.mapbox.navigation.base.trip.model.eh.OpenLRStandard
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.RoadObject
import com.mapbox.navigator.RoadObjectMatcherError
import com.mapbox.navigator.RoadObjectMatcherListener
import java.util.concurrent.CopyOnWriteArraySet

private typealias SDKRoadObject =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObject

private typealias SDKRoadObjectMatcherError =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectMatcherError

/**
 * [MapboxNavigation.roadObjectMatcher] provides methods to match custom road objects.
 */
class RoadObjectMatcher internal constructor(
    private val navigator: MapboxNativeNavigator
) {

    private val roadObjectMatcherObservers = CopyOnWriteArraySet<RoadObjectMatcherObserver>()

    init {
        navigator.setNativeNavigatorRecreationObserver {
            if (roadObjectMatcherObservers.isNotEmpty()) {
                navigator.roadObjectMatcher?.setListener(roadObjectMatcherListener)
            }
        }
    }

    /**
     * Register road object matcher observer. It needs to be registered before any of the other
     * methods are called. Otherwise, the results are lost.
     */
    fun registerRoadObjectMatcherObserver(roadObjectMatcherObserver: RoadObjectMatcherObserver) {
        if (roadObjectMatcherObservers.isEmpty()) {
            navigator.roadObjectMatcher?.setListener(roadObjectMatcherListener)
        }
        roadObjectMatcherObservers.add(roadObjectMatcherObserver)
    }

    private val roadObjectMatcherListener = object : RoadObjectMatcherListener() {
        override fun onRoadObjectMatched(
            roadObject: Expected<RoadObjectMatcherError, RoadObject>
        ) {
            val result: Expected<SDKRoadObjectMatcherError, SDKRoadObject> =
                if (roadObject.isValue) {
                    ExpectedFactory.createValue(
                        RoadObjectFactory.buildRoadObject(roadObject.value!!)
                    )
                } else {
                    ExpectedFactory.createError(
                        RoadObjectFactory.buildRoadObjectMatchingError(roadObject.error!!)
                    )
                }

            notifyMatchingObservers(result)
        }

        override fun onMatchingCancelled(id: String) {
            val result: Expected<SDKRoadObjectMatcherError, SDKRoadObject> =
                ExpectedFactory.createError(
                    RoadObjectFactory.buildRoadObjectMatchingError(
                        RoadObjectMatcherError("Matching cancelled", id)
                    )
                )

            notifyMatchingObservers(result)
        }
    }

    private fun notifyMatchingObservers(
        result: Expected<SDKRoadObjectMatcherError, SDKRoadObject>
    ) {
        roadObjectMatcherObservers.forEach {
            it.onRoadObjectMatched(result)
        }
    }

    /**
     * Matches given OpenLR object to the graph.
     * @param roadObjectId unique id of the object
     * @param openLRLocation road object location
     * @param openLRStandard standard used to encode openLRLocation
     */
    fun matchOpenLRObject(
        roadObjectId: String,
        openLRLocation: String,
        @OpenLRStandard.Type openLRStandard: String
    ) {
        navigator.roadObjectMatcher?.matchOpenLR(
            openLRLocation,
            EHorizonFactory.buildOpenLRStandard(openLRStandard),
            roadObjectId
        )
    }

    /**
     * Matches given polyline to graph.
     * Polyline should define valid path on graph,
     * i.e. it should be possible to drive this path according to traffic rules.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param roadObjectId unique id of the object
     * @param polyline polyline representing the object
     */
    fun matchPolylineObject(roadObjectId: String, polyline: List<Point>) {
        navigator.roadObjectMatcher?.matchPolyline(polyline, roadObjectId)
    }

    /**
     * Matches given polygon to graph.
     * "Matching" here means we try to find all intersections of polygon with the road graph
     * and track distances to those intersections as distance to polygon.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param roadObjectId unique id of the object
     * @param polygon polygon representing the object
     */
    fun matchPolygonObject(roadObjectId: String, polygon: List<Point>) {
        navigator.roadObjectMatcher?.matchPolygon(polygon, roadObjectId)
    }

    /**
     * Matches given gantry (i.e. polyline orthogonal to the road) to the graph.
     * "Matching" here means we try to find all intersections of gantry with road graph
     * and track distances to those intersections as distance to gantry.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param roadObjectId unique id of the object
     * @param gantry gantry representing the object
     */
    fun matchGantryObject(roadObjectId: String, gantry: List<Point>) {
        navigator.roadObjectMatcher?.matchGantry(gantry, roadObjectId)
    }

    /**
     * Matches given point to road graph.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param roadObjectId unique id of the object
     * @param point point representing the object
     */
    fun matchPointObject(roadObjectId: String, point: Point) {
        navigator.roadObjectMatcher?.matchPoint(point, roadObjectId)
    }

    /**
     * Cancel road objects matching
     *
     * @param roadObjectIds list of object ids to cancel matching
     */
    fun cancel(roadObjectIds: List<String>) {
        navigator.roadObjectMatcher?.cancel(roadObjectIds)
    }

    /**
     * Cancel all road objects matching
     */
    fun cancelAll() {
        navigator.roadObjectMatcher?.cancelAll()
    }
}
