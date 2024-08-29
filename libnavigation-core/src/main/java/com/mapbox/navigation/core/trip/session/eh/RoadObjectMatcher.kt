package com.mapbox.navigation.core.trip.session.eh

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.factory.EHorizonFactory
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
import com.mapbox.navigation.base.trip.model.eh.MatchableGeometry
import com.mapbox.navigation.base.trip.model.eh.MatchableOpenLr
import com.mapbox.navigation.base.trip.model.eh.MatchablePoint
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
    private val navigator: MapboxNativeNavigator,
) {

    private val roadObjectMatcherObservers = CopyOnWriteArraySet<RoadObjectMatcherObserver>()

    init {
        navigator.setNativeNavigatorRecreationObserver {
            if (roadObjectMatcherObservers.isNotEmpty()) {
                navigator.roadObjectMatcher.setListener(roadObjectMatcherListener)
            }
        }
    }

    /**
     * Register road object matcher observer. It needs to be registered before any of the other
     * methods are called. Otherwise, the results are lost.
     */
    fun registerRoadObjectMatcherObserver(roadObjectMatcherObserver: RoadObjectMatcherObserver) {
        if (roadObjectMatcherObservers.isEmpty()) {
            navigator.roadObjectMatcher.setListener(roadObjectMatcherListener)
        }
        roadObjectMatcherObservers.add(roadObjectMatcherObserver)
    }

    private val roadObjectMatcherListener = object : RoadObjectMatcherListener {
        override fun onRoadObjectMatched(
            roadObject: Expected<RoadObjectMatcherError, RoadObject>,
        ) {
            val result: Expected<SDKRoadObjectMatcherError, SDKRoadObject> =
                if (roadObject.isValue) {
                    ExpectedFactory.createValue(
                        RoadObjectFactory.buildRoadObject(roadObject.value!!),
                    )
                } else {
                    ExpectedFactory.createError(
                        RoadObjectFactory.buildRoadObjectMatchingError(roadObject.error!!),
                    )
                }

            notifyMatchingObservers(result)
        }

        override fun onMatchingCancelled(id: String) {
            val result: Expected<SDKRoadObjectMatcherError, SDKRoadObject> =
                ExpectedFactory.createError(
                    RoadObjectFactory.buildRoadObjectMatchingError(
                        RoadObjectMatcherError("Matching cancelled", id),
                    ),
                )

            notifyMatchingObservers(result)
        }
    }

    private fun notifyMatchingObservers(
        result: Expected<SDKRoadObjectMatcherError, SDKRoadObject>,
    ) {
        roadObjectMatcherObservers.forEach {
            it.onRoadObjectMatched(result)
        }
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
        useOnlyPreloadedTiles: Boolean = false,
    ) {
        navigator.roadObjectMatcher.matchOpenLRs(
            matchableOpenLrs.map {
                EHorizonFactory.buildNativeMatchableOpenLr(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED,
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
        useOnlyPreloadedTiles: Boolean = false,
    ) {
        navigator.roadObjectMatcher.matchPolylines(
            matchableGeometries.map {
                EHorizonFactory.buildNativeMatchableGeometry(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED,
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
        useOnlyPreloadedTiles: Boolean = false,
    ) {
        navigator.roadObjectMatcher.matchPolygons(
            matchableGeometries.map {
                EHorizonFactory.buildNativeMatchableGeometry(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED,
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
        useOnlyPreloadedTiles: Boolean = false,
    ) {
        navigator.roadObjectMatcher.matchGantries(
            matchableGeometries.map {
                EHorizonFactory.buildNativeMatchableGeometry(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED,
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
        useOnlyPreloadedTiles: Boolean = false,
    ) {
        navigator.roadObjectMatcher.matchPoints(
            matchablePoints.map {
                EHorizonFactory.buildNativeMatchablePoint(it)
            },
            MatchingOptions(
                useOnlyPreloadedTiles,
                false,
                PartialPolylineDistanceCalculationStrategy.ONLY_MATCHED,
            ),
        )
    }

    /**
     * Cancel road objects matching
     *
     * @param roadObjectIds list of object ids to cancel matching
     */
    fun cancel(roadObjectIds: List<String>) {
        navigator.roadObjectMatcher.cancel(roadObjectIds)
    }

    /**
     * Cancel all road objects matching
     */
    fun cancelAll() {
        navigator.roadObjectMatcher.cancelAll()
    }
}
