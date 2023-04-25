package com.mapbox.navigation.core.trip.session.eh

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.factory.EHorizonFactory
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
import com.mapbox.navigation.base.trip.model.eh.MatchableGeometry
import com.mapbox.navigation.base.trip.model.eh.MatchableOpenLr
import com.mapbox.navigation.base.trip.model.eh.MatchablePoint
import com.mapbox.navigation.base.trip.model.eh.OpenLRStandard
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.MatchingOptions
import com.mapbox.navigator.PartialPolylineDistanceCalculationStrategy
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

    private val roadObjectMatcherListener = object : RoadObjectMatcherListener {
        override fun onRoadObjectMatched(
            roadObject: Expected<RoadObjectMatcherError, RoadObject>
        ) {
            val result: Expected<SDKRoadObjectMatcherError, SDKRoadObject>? =
                if (roadObject.isValue) {
                    RoadObjectFactory.buildRoadObject(roadObject.value!!)?.let {
                        ExpectedFactory.createValue(it)
                    }
                } else {
                    ExpectedFactory.createError(
                        RoadObjectFactory.buildRoadObjectMatchingError(roadObject.error!!)
                    )
                }

            result?.let(::notifyMatchingObservers)
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
    @Deprecated(
        message = "Use matchOpenLRObjects() instead",
        ReplaceWith(
            expression = "matchOpenLRObjects(" +
                "listOf(" +
                "MatchableOpenLr(roadObjectId, openLRLocation, openLRStandard)))",
            imports = arrayOf("com.mapbox.navigation.base.trip.model.eh.MatchableOpenLr")
        )
    )
    fun matchOpenLRObject(
        roadObjectId: String,
        openLRLocation: String,
        @OpenLRStandard.Type openLRStandard: String
    ) {
        navigator.roadObjectMatcher?.matchOpenLRs(
            listOf(
                EHorizonFactory.buildNativeMatchableOpenLr(
                    MatchableOpenLr(roadObjectId, openLRLocation, openLRStandard)
                )
            ),
            MatchingOptions(
                false,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
        )
    }

    /**
     * Matches given OpenLR objects to the graph.
     * @param matchableOpenLrs matchable objects
     * @param useOnlyPreloadedTiles If there is no data for the specified geometry yet cached on the device,
     * setting this to `false` will wait (potentially indefinitely) for the data covering this geometry to be loaded
     * via other means (location updates, predictive ambient caching, offline regions etc.) before attempting to match.
     * If set to `true` and there is no data on device covering specified geometry, this call will return an error immediately.
     */
    @JvmOverloads
    fun matchOpenLRObjects(
        matchableOpenLrs: List<MatchableOpenLr>,
        useOnlyPreloadedTiles: Boolean = false
    ) {
        navigator.roadObjectMatcher?.matchOpenLRs(
            matchableOpenLrs.map {
                EHorizonFactory.buildNativeMatchableOpenLr(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
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
    @Deprecated(
        message = "Use matchPolylineObjects() instead.",
        ReplaceWith(
            expression = "matchPolylineObject(listOf(MatchableGeometry(roadObjectId, polyline)))",
            imports = arrayOf("com.mapbox.navigation.base.trip.model.eh.MatchableGeometry")
        )
    )
    fun matchPolylineObject(roadObjectId: String, polyline: List<Point>) {
        navigator.roadObjectMatcher?.matchPolylines(
            listOf(
                EHorizonFactory.buildNativeMatchableGeometry(
                    MatchableGeometry(roadObjectId, polyline)
                )
            ),
            MatchingOptions(
                false,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
        )
    }

    /**
     * Matches given polylines to graph.
     * Polyline should define valid path on graph,
     * i.e. it should be possible to drive this path according to traffic rules.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param matchableGeometries matchable geometries
     * @param useOnlyPreloadedTiles If there is no data for the specified geometry yet cached on the device,
     * setting this to `false` will wait (potentially indefinitely) for the data covering this geometry to be loaded
     * via other means (location updates, predictive ambient caching, offline regions etc.) before attempting to match.
     * If set to `true` and there is no data on device covering specified geometry, this call will return an error immediately.
     */
    @JvmOverloads
    fun matchPolylineObjects(
        matchableGeometries: List<MatchableGeometry>,
        useOnlyPreloadedTiles: Boolean = false
    ) {
        navigator.roadObjectMatcher?.matchPolylines(
            matchableGeometries.map {
                EHorizonFactory.buildNativeMatchableGeometry(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
        )
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
    @Deprecated(
        message = "Use matchPolygonObjects() instead.",
        ReplaceWith(
            expression = "matchPolygonObject(listOf(MatchableGeometry(roadObjectId, polygon)))",
            imports = arrayOf("com.mapbox.navigation.base.trip.model.eh.MatchableGeometry")
        )
    )
    fun matchPolygonObject(roadObjectId: String, polygon: List<Point>) {
        navigator.roadObjectMatcher?.matchPolygons(
            listOf(
                EHorizonFactory.buildNativeMatchableGeometry(
                    MatchableGeometry(roadObjectId, polygon)
                )
            ),
            MatchingOptions(
                false,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
        )
    }

    /**
     * Matches given polygons to graph.
     * "Matching" here means we try to find all intersections of polygon with the road graph
     * and track distances to those intersections as distance to polygon.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param matchableGeometries matchable geometries
     * @param useOnlyPreloadedTiles If there is no data for the specified geometry yet cached on the device,
     * setting this to `false` will wait (potentially indefinitely) for the data covering this geometry to be loaded
     * via other means (location updates, predictive ambient caching, offline regions etc.) before attempting to match.
     * If set to `true` and there is no data on device covering specified geometry, this call will return an error immediately.
     */
    @JvmOverloads
    fun matchPolygonObjects(
        matchableGeometries: List<MatchableGeometry>,
        useOnlyPreloadedTiles: Boolean = false
    ) {
        navigator.roadObjectMatcher?.matchPolygons(
            matchableGeometries.map {
                EHorizonFactory.buildNativeMatchableGeometry(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
        )
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
    @Deprecated(
        message = "Use matchGantryObjects() instead.",
        ReplaceWith(
            expression = "matchGantryObject(listOf(MatchableGeometry(roadObjectId, gantry)))",
            imports = arrayOf("com.mapbox.navigation.base.trip.model.eh.MatchableGeometry")
        )
    )
    fun matchGantryObject(roadObjectId: String, gantry: List<Point>) {
        navigator.roadObjectMatcher?.matchGantries(
            listOf(
                EHorizonFactory.buildNativeMatchableGeometry(
                    MatchableGeometry(roadObjectId, gantry)
                )
            ),
            MatchingOptions(
                false,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
        )
    }

    /**
     * Matches given gantries (i.e. polyline orthogonal to the road) to the graph.
     * "Matching" here means we try to find all intersections of gantry with road graph
     * and track distances to those intersections as distance to gantry.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param matchableGeometries matchable geometries
     * @param useOnlyPreloadedTiles If there is no data for the specified geometry yet cached on the device,
     * setting this to `false` will wait (potentially indefinitely) for the data covering this geometry to be loaded
     * via other means (location updates, predictive ambient caching, offline regions etc.) before attempting to match.
     * If set to `true` and there is no data on device covering specified geometry, this call will return an error immediately.
     */
    @JvmOverloads
    fun matchGantryObjects(
        matchableGeometries: List<MatchableGeometry>,
        useOnlyPreloadedTiles: Boolean = false
    ) {
        navigator.roadObjectMatcher?.matchGantries(
            matchableGeometries.map {
                EHorizonFactory.buildNativeMatchableGeometry(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
        )
    }

    /**
     * Matches given point to road graph.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param roadObjectId unique id of the object
     * @param point point representing the object
     */
    @Deprecated(
        message = "Use matchPointObjects() instead.",
        ReplaceWith(
            expression = "matchPointObject(listOf(MatchablePoint(roadObjectId, point)))",
            imports = arrayOf("com.mapbox.navigation.base.trip.model.eh.MatchablePoint")
        )
    )
    fun matchPointObject(roadObjectId: String, point: Point) {
        navigator.roadObjectMatcher?.matchPoints(
            listOf(
                EHorizonFactory.buildNativeMatchablePoint(
                    MatchablePoint(roadObjectId, point)
                )
            ),
            MatchingOptions(
                false,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
        )
    }

    /**
     * Matches given points to road graph.
     * In case of error (if there are no tiles in cache, decoding failed, etc.)
     * object won't be matched.
     *
     * @param matchablePoints matchable points
     * @param useOnlyPreloadedTiles If there is no data for the specified geometry yet cached on the device,
     * setting this to `false` will wait (potentially indefinitely) for the data covering this geometry to be loaded
     * via other means (location updates, predictive ambient caching, offline regions etc.) before attempting to match.
     * If set to `true` and there is no data on device covering specified geometry, this call will return an error immediately.
     */
    @JvmOverloads
    fun matchPointObjects(
        matchablePoints: List<MatchablePoint>,
        useOnlyPreloadedTiles: Boolean = false
    ) {
        navigator.roadObjectMatcher?.matchPoints(
            matchablePoints.map {
                EHorizonFactory.buildNativeMatchablePoint(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED
            ),
        )
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
