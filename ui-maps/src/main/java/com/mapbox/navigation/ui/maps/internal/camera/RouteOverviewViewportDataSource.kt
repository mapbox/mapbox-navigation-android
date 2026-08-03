package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.internal.utils.areSameRoutes
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getRemainingPointsOnRoute
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.simplifyCompleteRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.toPoint

private data class RouteIndices(
    val legIndex: Int,
    val stepIndex: Int,
    val legGeometryIndex: Int,
)

private data class CachedRemainingPoints(
    val indices: RouteIndices,
    val remainingPointsOnCurrentStep: List<Point>,
)

/**
 * Produces the overview camera frame for the **route overview** feature: it frames the remaining
 * portion of the route geometry (based on [onRouteProgressChanged]) together with the puck and any
 * [additionalPointsToFrame].
 *
 * Points overview (framing an arbitrary set of points) is handled by the separate
 * [PointsOverviewViewportDataSource]. Both share the framing logic in [BaseOverviewViewportDataSource].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteOverviewViewportDataSource @VisibleForTesting internal constructor(
    mapboxMap: MapboxMap,
    internalOptions: InternalRouteOverviewOptions,
    private val indicesConverter: RoutesIndicesConverter,
) : BaseOverviewViewportDataSource(mapboxMap) {

    constructor(
        mapboxMap: MapboxMap,
        internalOptions: InternalRouteOverviewOptions,
    ) : this(mapboxMap, internalOptions, RoutesIndicesConverter())

    var internalOptions = internalOptions
        set(value) {
            if (field != value) {
                field = value
                routeDataUpToDate = false
                reevaluate()
            }
        }

    private var navigationRoutes: List<NavigationRoute> = emptyList()
    private var routeProgress: RouteProgress? = null
    private var simplifiedCompleteRoutesPoints: List<List<List<List<Point>>>> = emptyList()
    private var simplifiedRemainingPointsOnRoutes: List<Point> = emptyList()
    private var targetLocation: Location? = null
    private var cachedRemainingPoints: MutableMap<String, CachedRemainingPoints> = hashMapOf()

    // Tracks whether simplifiedCompleteRoutesPoints is current. Avoids re-decoding all step
    // geometries on every setActive(true) when routes and simplification settings have not changed.
    private var routeDataUpToDate = false

    // Initial values are never read - hasSimplificationChanged() is only called
    // when routeDataUpToDate = true, which is set only after calculateRouteData()
    // has already updated these fields.
    private var lastSimplificationEnabled = true
    private var lastSimplificationFactor = -1

    override fun reevaluate() {
        if (!routeDataUpToDate || hasSimplificationChanged()) {
            logD(TAG) { "Route data outdated - re-calculating" }
            calculateRouteData(navigationRoutes)
        } else {
            logD(TAG) { "Route data up-to-date - skipping calculation" }
        }
        routeProgress?.let { onRouteProgressChanged(it) }
        super.reevaluate()
    }

    fun onRoutesChanged(routes: List<NavigationRoute>) {
        if (!areSameRoutes(navigationRoutes, routes)) {
            navigationRoutes = routes
            routeDataUpToDate = false
            calculateRouteData(routes)
        }
    }

    private fun calculateRouteData(routes: List<NavigationRoute>) {
        runIfActive {
            if (routes.isEmpty()) {
                clearRouteData()
            } else {
                PerformanceTracker.trackPerformanceSync(
                    "RouteOverviewViewportDataSource#calculateRouteData",
                ) {
                    val completeRoutesPoints = routes
                        .mapIndexedNotNull { index, route ->
                            if (index == 0 || internalOptions.overviewAlternatives) {
                                processRoutePoints(route.directionsRoute)
                            } else {
                                null
                            }
                        }
                    indicesConverter.onRoutesChanged(
                        if (internalOptions.overviewAlternatives) {
                            routes
                        } else {
                            routes.take(1)
                        },
                    )
                    simplifiedCompleteRoutesPoints = completeRoutesPoints.map {
                        simplifyCompleteRoutePoints(
                            options.geometrySimplification.enabled,
                            options.geometrySimplification.simplificationFactor,
                            it,
                        )
                    }
                    simplifiedRemainingPointsOnRoutes =
                        simplifiedCompleteRoutesPoints.flatten().flatten().flatten()
                    routeDataUpToDate = true
                    lastSimplificationEnabled = options.geometrySimplification.enabled
                    lastSimplificationFactor = options.geometrySimplification.simplificationFactor
                }
            }
        }
    }

    private fun clearRoutePointsData() {
        routeDataUpToDate = false
        indicesConverter.onRoutesChanged(emptyList())
        runIfActive {
            simplifiedCompleteRoutesPoints = emptyList()
            simplifiedRemainingPointsOnRoutes = emptyList()
        }
    }

    fun clearRouteData() {
        routeDataUpToDate = false
        this.navigationRoutes = emptyList()
        clearRoutePointsData()
    }

    fun clearProgressData() {
        this.routeProgress = null
        cachedRemainingPoints = hashMapOf()
        runIfActive {
            simplifiedRemainingPointsOnRoutes = simplifiedCompleteRoutesPoints
                .flatten().flatten().flatten()
        }
    }

    fun onRouteProgressChanged(
        routeProgress: RouteProgress,
    ) {
        this.routeProgress = routeProgress
        val currentRoute = this.navigationRoutes.firstOrNull()
        if (currentRoute == null) {
            return
        }
        if (!currentRoute.directionsRoute.isSameRoute(routeProgress.route)) {
            clearProgressData()
            return
        }
        runIfActive {
            ifNonNull(
                routeProgress.currentLegProgress,
                routeProgress.currentLegProgress?.currentStepProgress,
            ) { currentLegProgress, currentStepProgress ->
                PerformanceTracker.trackPerformanceSync(
                    "RouteOverviewViewportDataSource#onRouteProgressChanged",
                ) {
                    simplifiedRemainingPointsOnRoutes =
                        navigationRoutes.mapIndexedNotNull { index, route ->
                            if (index > 0 && !internalOptions.overviewAlternatives) {
                                null
                            } else {
                                val indices = if (index == 0) {
                                    RouteIndices(
                                        currentLegProgress.legIndex,
                                        currentStepProgress.stepIndex,
                                        currentLegProgress.geometryIndex,
                                    )
                                } else {
                                    routeProgress.internalAlternativeRouteIndices()[route.id]?.let {
                                        RouteIndices(it.legIndex, it.stepIndex, it.legGeometryIndex)
                                    }
                                }
                                if (indices == null) {
                                    null
                                } else {
                                    if (indices != cachedRemainingPoints[route.id]?.indices) {
                                        val stepGeometryIndex = indicesConverter.convert(
                                            route.id,
                                            indices.legIndex,
                                            indices.stepIndex,
                                            indices.legGeometryIndex,
                                        )
                                        if (stepGeometryIndex != null) {
                                            cachedRemainingPoints[route.id] =
                                                getCachedRemainingPoints(
                                                    route,
                                                    indices,
                                                    stepGeometryIndex,
                                                )
                                        }
                                    }
                                    getRemainingPointsOnRoute(
                                        simplifiedCompleteRoutesPoints[index],
                                        cachedRemainingPoints[route.id]
                                            ?.remainingPointsOnCurrentStep.orEmpty(),
                                        internalOptions.overviewMode,
                                        indices.legIndex,
                                        indices.stepIndex,
                                    )
                                }
                            }
                        }.flatten()
                }
            }
        }
    }

    private fun getCachedRemainingPoints(
        route: NavigationRoute,
        indices: RouteIndices,
        stepGeometryIndex: Int,
    ): CachedRemainingPoints {
        val remainingPointsOnCurrentStep = route.directionsRoute.legs()
            ?.getOrNull(indices.legIndex)
            ?.steps()
            ?.getOrNull(indices.stepIndex)?.let {
                route.directionsRoute.stepGeometryToPoints(it)
            }
            ?.drop(stepGeometryIndex)
            .orEmpty()
        return CachedRemainingPoints(indices, remainingPointsOnCurrentStep)
    }

    fun onLocationChanged(location: Location) {
        this.targetLocation = location
    }

    override fun updateDebuggerPoints(
        debugger: MapboxNavigationViewportDataSourceDebugger,
        pointsForOverview: List<Point>,
    ) {
        debugger.routePointsForOverview = pointsForOverview
    }

    override fun getPointsToFrame(): List<Point> {
        val points = simplifiedRemainingPointsOnRoutes.toMutableList()
        // Track the puck as part of the route overview frame.
        targetLocation?.let { points.add(0, it.toPoint()) }
        return points
    }

    private fun hasSimplificationChanged(): Boolean {
        val simplification = options.geometrySimplification
        return lastSimplificationEnabled != simplification.enabled ||
            lastSimplificationFactor != simplification.simplificationFactor
    }

    private companion object {
        private const val TAG = "RouteOverviewViewportDataSource"
    }
}
