package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.toCameraOptions
import com.mapbox.maps.util.isEmpty
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.internal.utils.areSameRoutes
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.BEARING_NORTH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.EMPTY_EDGE_INSETS
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.NULL_ISLAND_POINT
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.ZERO_PITCH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSourceOptions
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getRemainingPointsOnRoute
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.simplifyCompleteRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.ViewportProperty
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigation.utils.internal.toPoint
import kotlin.math.min

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class OverviewViewportDataSource(
    private val mapboxMap: MapboxMap,
    internalOptions: InternalViewportDataSourceOptions,
) {

    internal var internalOptions = internalOptions
        set(value) {
            if (field != value) {
                field = value
                reevaluate()
            }
        }

    val options = MapboxNavigationViewportDataSourceOptions()

    private var navigationRoutes: List<NavigationRoute> = emptyList()
    private var routeProgress: RouteProgress? = null
    private var pointsToFrameOnCurrentStep: List<Point> = emptyList()
    private var simplifiedCompleteRoutesPoints: List<List<List<List<Point>>>> = emptyList()
    private var simplifiedRemainingPointsOnRoutes: List<Point> = emptyList()
    private var targetLocation: Location? = null

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    var debugger: MapboxNavigationViewportDataSourceDebugger? = null

    private val centerProperty = ViewportProperty.CenterProperty(null, NULL_ISLAND_POINT)
    private val zoomProperty =
        ViewportProperty.ZoomProperty(null, options.overviewFrameOptions.maxZoom)
    private val bearingProperty = ViewportProperty.BearingProperty(null, BEARING_NORTH)
    private val pitchProperty = ViewportProperty.PitchProperty(null, ZERO_PITCH)

    var padding: EdgeInsets = EMPTY_EDGE_INSETS
    private var additionalPointsToFrame: List<Point> = emptyList()

    private var active = true

    var viewportData: CameraOptions =
        CameraOptions.Builder()
            .center(centerProperty.get())
            .zoom(zoomProperty.get())
            .bearing(bearingProperty.get())
            .pitch(pitchProperty.get())
            .padding(padding)
            .build()
        private set

    fun setActive(active: Boolean) {
        this.active = active
        if (active) {
            reevaluate()
        }
    }

    private fun reevaluate() {
        calculateRouteData(navigationRoutes)
        routeProgress?.let { onRouteProgressChanged(it, pointsToFrameOnCurrentStep) }
        evaluate()
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
                val completeRoutePoints = routes
                    .mapIndexedNotNull { index, route ->
                        if (index == 0 || internalOptions.overviewAlternatives) {
                            processRoutePoints(route.directionsRoute)
                        } else {
                            null
                        }
                    }
                simplifiedCompleteRoutesPoints = completeRoutePoints.map {
                    simplifyCompleteRoutePoints(
                        options.overviewFrameOptions.geometrySimplification.enabled,
                        options.overviewFrameOptions.geometrySimplification.simplificationFactor,
                        it,
                    )
                }
                simplifiedRemainingPointsOnRoutes =
                    simplifiedCompleteRoutesPoints.flatten().flatten().flatten()
            }
        }
    }

    fun clearRouteData() {
        this.navigationRoutes = emptyList()
        runIfActive {
            simplifiedCompleteRoutesPoints = emptyList()
            simplifiedRemainingPointsOnRoutes = emptyList()
        }
    }

    fun clearProgressData() {
        this.routeProgress = null
        pointsToFrameOnCurrentStep = emptyList()
        runIfActive {
            simplifiedRemainingPointsOnRoutes = simplifiedCompleteRoutesPoints
                .flatten().flatten().flatten()
        }
    }

    fun onRouteProgressChanged(
        routeProgress: RouteProgress,
        pointsToFrameOnCurrentStep: List<Point>,
    ) {
        this.routeProgress = routeProgress
        this.pointsToFrameOnCurrentStep = pointsToFrameOnCurrentStep
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
                val primaryPoints = getRemainingPointsOnRoute(
                    simplifiedCompleteRoutesPoints.first(),
                    pointsToFrameOnCurrentStep,
                    internalOptions.overviewMode,
                    currentLegProgress.legIndex,
                    currentStepProgress.stepIndex,
                )
                val alternativePoints = if (internalOptions.overviewAlternatives) {
                    navigationRoutes.drop(1).mapIndexedNotNull { index, route ->
                        val alternativeIndices = routeProgress
                            .internalAlternativeRouteIndices()[route.id]
                        if (alternativeIndices == null) {
                            null
                        } else {
                            getRemainingPointsOnRoute(
                                simplifiedCompleteRoutesPoints[index + 1],
                                emptyList(), // already overviewed by primary route
                                internalOptions.overviewMode,
                                alternativeIndices.legIndex,
                                alternativeIndices.stepIndex,
                            )
                        }
                    }.flatten()
                } else {
                    emptyList()
                }
                simplifiedRemainingPointsOnRoutes = primaryPoints + alternativePoints
            }
        }
    }

    fun onLocationChanged(location: Location) {
        this.targetLocation = location
    }

    fun additionalPointsToFrame(points: List<Point>) {
        additionalPointsToFrame = ArrayList(points)
    }

    fun centerPropertyOverride(value: Point?) {
        centerProperty.override = value
    }

    fun zoomPropertyOverride(value: Double?) {
        zoomProperty.override = value
    }

    fun bearingPropertyOverride(value: Double?) {
        bearingProperty.override = value
    }

    fun pitchPropertyOverride(value: Double?) {
        pitchProperty.override = value
    }

    fun clearOverrides() {
        centerProperty.override = null
        zoomProperty.override = null
        bearingProperty.override = null
        pitchProperty.override = null
    }

    fun evaluate() {
        val cameraState = mapboxMap.cameraState
        runIfActive {
            val pointsForOverview = simplifiedRemainingPointsOnRoutes.toMutableList()

            val localTargetLocation = targetLocation
            if (localTargetLocation != null) {
                pointsForOverview.add(0, localTargetLocation.toPoint())
            }

            pointsForOverview.addAll(additionalPointsToFrame)

            if (pointsForOverview.isEmpty()) {
                // nothing to frame
                options.overviewFrameOptions.run {
                    bearingProperty.fallback = cameraState.bearing
                    pitchProperty.fallback = cameraState.pitch
                    centerProperty.fallback = cameraState.center
                    zoomProperty.fallback = min(cameraState.zoom, maxZoom)
                }
            } else {
                bearingProperty.fallback = normalizeBearing(
                    cameraState.bearing,
                    BEARING_NORTH,
                )

                val cameraFrame = if (pointsForOverview.isNotEmpty()) {
                    mapboxMap.cameraForCoordinates(
                        pointsForOverview,
                        CameraOptions.Builder()
                            .padding(padding)
                            .bearing(bearingProperty.get())
                            .pitch(pitchProperty.get())
                            .build(),
                        null,
                        null,
                        null,
                    )
                } else {
                    cameraState.toCameraOptions()
                }

                if (cameraFrame.isEmpty) {
                    logW { "CameraOptions is empty" }
                } else {
                    // TODO should be non-null (reproducible with Camera test)
                    centerProperty.fallback = cameraFrame.center!!
                    zoomProperty.fallback = min(
                        cameraFrame.zoom!!,
                        options.overviewFrameOptions.maxZoom,
                    )
                }
            }

            updateDebugger(pointsForOverview)

            options.overviewFrameOptions.run {
                viewportData =
                    CameraOptions.Builder().apply {
                        if (centerUpdatesAllowed) {
                            center(centerProperty.get())
                        }
                        if (zoomUpdatesAllowed) {
                            zoom(zoomProperty.get())
                        }
                        if (bearingUpdatesAllowed) {
                            bearing(bearingProperty.get())
                        }
                        if (pitchUpdatesAllowed) {
                            pitch(pitchProperty.get())
                        }
                        if (paddingUpdatesAllowed) {
                            padding(padding)
                        }
                    }.build()
            }
        }
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun updateDebugger(
        pointsForOverview: List<Point>,
    ) {
        runIfActive {
            debugger?.overviewPoints = pointsForOverview
            debugger?.overviewUserPadding = padding
        }
    }

    private fun runIfActive(action: () -> Unit) {
        if (active) {
            action()
        }
    }
}
