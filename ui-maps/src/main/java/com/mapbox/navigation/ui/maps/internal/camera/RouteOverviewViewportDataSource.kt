package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
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
                reevaluate()
            }
        }

    private var navigationRoutes: List<NavigationRoute> = emptyList()
    private var routeProgress: RouteProgress? = null
    private var simplifiedCompleteRoutesPoints: List<List<List<List<Point>>>> = emptyList()
    private var simplifiedRemainingPointsOnRoutes: List<Point> = emptyList()
    private var targetLocation: Location? = null
    private var cachedRemainingPoints: MutableMap<String, CachedRemainingPoints> = hashMapOf()

    override fun reevaluate() {
        calculateRouteData(navigationRoutes)
        routeProgress?.let { onRouteProgressChanged(it) }
        super.reevaluate()
    }

    fun onRoutesChanged(routes: List<NavigationRoute>) {
        if (!areSameRoutes(navigationRoutes, routes)) {
            navigationRoutes = routes
            calculateRouteData(routes)
        }
    }

    private fun calculateRouteData(routes: List<NavigationRoute>) {
        runIfActive {
            if (routes.isEmpty()) {
                clearRouteData()
            } else {
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
            }
        }
    }

    private fun clearRoutePointsData() {
        indicesConverter.onRoutesChanged(emptyList())
        runIfActive {
            simplifiedCompleteRoutesPoints = emptyList()
            simplifiedRemainingPointsOnRoutes = emptyList()
        }
    }

    fun clearRouteData() {
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
                                    cachedRemainingPoints[route.id]?.remainingPointsOnCurrentStep
                                        .orEmpty(),
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
}
