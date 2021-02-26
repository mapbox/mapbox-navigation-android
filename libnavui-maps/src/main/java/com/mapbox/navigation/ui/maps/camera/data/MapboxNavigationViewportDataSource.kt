package com.mapbox.navigation.ui.maps.camera.data

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.*
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.utils.metersToKilometers
import com.mapbox.navigation.ui.maps.camera.utils.shortestRotation
import com.mapbox.navigation.ui.maps.camera.utils.toPoint
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.max
import kotlin.math.min

private val NULL_ISLAND_POINT = Point.fromLngLat(0.0, 0.0)
private val EMPTY_EDGE_INSETS = EdgeInsets(0.0, 0.0, 0.0, 0.0)
private val CENTER_SCREEN_COORDINATE = ScreenCoordinate(0.0, 0.0)
private const val MINIMUM_ZOOM_LEVEL_FOR_GEO = 2.0

/**
 * Default implementation of [ViewportDataSource] to use with the [NavigationCamera].
 *
 * Use:
 * - [onRouteChanged] to produce overview geometries that need to be framed
 * - [onRouteProgressChanged] to produce following geometries of the current step
 * and overview geometries of the remaining points on the route that need to be framed.
 * This will make the following frame change zoom level and pitch depending on the proximity to
 * the upcoming maneuver and resize overview to fit only remaining portion of the route.
 * - [onLocationChanged] to pass a point to be framed and used as a source of bearing for the
 * following camera frame
 * - [additionalPointsToFrameForFollowing] - points that also need to be visible in
 * the following camera frame
 * - [additionalPointsToFrameForOverview] - points that also need to be visible in
 * the overview camera frame
 *
 * Whenever a set of these arguments is provided or refreshed, you need to call [evaluate]
 * to process the data and compute an opinionated [ViewportData] updates that [NavigationCamera]
 * observes and executes.
 *
 * Based on the provided data, the class will make decisions on how the camera should be framed.
 * However, that might not always match with your expectations or needs.
 * Let’s imagine that you would like to temporarily zoom in (when the user double-tapped the map)
 * or change the camera’s bearing to focus on a POI. To serve those use-cases,
 * all of the camera property values that this data source produces can be overridden.
 * The source will keep producing the default, opinionated values, but as long as the override
 * is present, they won’t be used. Passing `null` as an override resets it to the default value.
 *
 * Whenever any changes are made to the data source (new values provided
 * or overrides added/removed), remember to call [evaluate] to notify the observers.
 *
 * ### Examples
 * #### Show route overview with padding
 * ```kotlin
 * private val routesObserver = object : RoutesObserver {
 *     override fun onRoutesChanged(routes: List<DirectionsRoute>) {
 *         if (routes.isNotEmpty()) {
 *             viewportDataSource.onRouteChanged(routes.first())
 *             viewportDataSource.overviewPaddingPropertyOverride(overviewEdgeInsets)
 *             viewportDataSource.evaluate()
 *             navigationCamera.requestNavigationCameraToOverview()
 *         } else {
 *             navigationCamera.requestNavigationCameraToIdle()
 *         }
 *     }
 * }
 * ```
 *
 * #### Update current location
 * ```kotlin
 * private val locationObserver = object : LocationObserver {
 *     override fun onRawLocationChanged(rawLocation: Location) {
 *         // no impl
 *     }
 *     override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
 *         viewportDataSource.onLocationChanged(enhancedLocation)
 *         viewportDataSource.evaluate()
 *     }
 * }
 * ```
 *
 * #### Update route progress
 * ```kotlin
 * private val routeProgressObserver = object : RouteProgressObserver {
 *     override fun onRouteProgressChanged(routeProgress: RouteProgress) {
 *         viewportDataSource.onRouteProgressChanged(routeProgress)
 *         viewportDataSource.evaluate()
 *     }
 * }
 * ```
 *
 * #### Request state to following with padding
 * ```kotlin
 * viewportDataSource.followingPaddingPropertyOverride(followingEdgeInsets)
 * viewportDataSource.evaluate()
 * navigationCamera.requestNavigationCameraToFollowing()
 * ```
 *
 * #### Update current location and reset frame
 * ```kotlin
 * private val mapMatcherResultObserver = object : MapMatcherResultObserver {
 *     override fun onNewMapMatcherResult(mapMatcherResult: MapMatcherResult) {
 *         viewportDataSource.onLocationChanged(mapMatcherResult.enhancedLocation)
 *         viewportDataSource.evaluate()
 *         if (mapMatcherResult.isTeleport) {
 *             navigationCamera.resetFrame()
 *         }
 *     }
 * }
 * ```
 *
 * #### Run your own animation to a POI
 * ```kotlin
 * private fun animateToPOI() {
 *     navigationCamera.requestNavigationCameraToIdle()
 *     mapView.getCameraAnimationsPlugin().flyTo(
 *         CameraOptions.Builder()
 *             .center(point)
 *             .bearing(0.0)
 *             .zoom(14.0)
 *             .pitch(0.0)
 *             .build(),
 *         1500
 *     )
 * }
 * ```
 */
class MapboxNavigationViewportDataSource(
    private val options: MapboxNavigationViewportDataSourceOptions,
    private val mapboxMap: MapboxMap
) : ViewportDataSource {

    private var completeRoutePoints: List<List<List<Point>>> = emptyList()
    private var remainingPointsOnCurrentStep: List<Point> = emptyList()
    private var remainingPointsOnRoute: List<Point> = emptyList()
    private var targetLocation: Location? = null
    private var averageIntersectionDistancesOnRoute: List<List<Double>> = emptyList()

    /* -------- GENERATED OPTIONS -------- */
    private var followingCameraOptions = CameraOptions.Builder().build()
    private var overviewCameraOptions = CameraOptions.Builder().build()

    /* -------- OVERRIDES -------- */
    private val followingCenterProperty = ViewportProperty.CenterProperty(null, NULL_ISLAND_POINT)
    private val followingZoomProperty = ViewportProperty.ZoomProperty(null, 0.0)
    private val followingBearingProperty = ViewportProperty.BearingProperty(null, 0.0)
    private val followingPitchProperty = ViewportProperty.PitchProperty(
        null,
        options.maxFollowingPitch
    )
    private val followingPaddingProperty = ViewportProperty.PaddingProperty(null, EMPTY_EDGE_INSETS)
    private val followingAnchorProperty = ViewportProperty.AnchorProperty(
        null,
        CENTER_SCREEN_COORDINATE
    )
    private val overviewCenterProperty = ViewportProperty.CenterProperty(null, NULL_ISLAND_POINT)
    private val overviewZoomProperty = ViewportProperty.ZoomProperty(null, 0.0)
    private val overviewBearingProperty = ViewportProperty.BearingProperty(null, 0.0)
    private val overviewPitchProperty = ViewportProperty.PitchProperty(null, 0.0)
    private val overviewPaddingProperty = ViewportProperty.PaddingProperty(null, EMPTY_EDGE_INSETS)
    private val overviewAnchorProperty = ViewportProperty.AnchorProperty(
        null,
        CENTER_SCREEN_COORDINATE
    )

    /* -------- ALLOWED UPDATES -------- */
    /**
     * If `true`, the source will manipulate Camera Center Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Center Property.
     *
     * Default to `true`.
     */
    var followingCenterUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Zoom Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Zoom Center Property.
     *
     * Default to `true`.
     */
    var followingZoomUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Bearing Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Bearing Property.
     *
     * Default to `true`.
     */
    var followingBearingUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Pitch Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Pitch Property.
     *
     * Default to `true`.
     */
    var followingPitchUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Padding Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Padding Property.
     *
     * Default to `true`.
     */
    var followingPaddingUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Anchor Property when producing following frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Anchor Property.
     *
     * Default to `true`.
     */
    var followingAnchorUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Center Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Center Property.
     *
     * Default to `true`.
     */
    var overviewCenterUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Zoom Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Zoom Center Property.
     *
     * Default to `true`.
     */
    var overviewZoomUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Bearing Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Bearing Property.
     *
     * Default to `true`.
     */
    var overviewBearingUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Pitch Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Pitch Property.
     *
     * Default to `true`.
     */
    var overviewPitchUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Padding Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Padding Property.
     *
     * Default to `true`.
     */
    var overviewPaddingUpdatesAllowed = true

    /**
     * If `true`, the source will manipulate Camera Anchor Property when producing overview frame
     * updates as necessary.
     *
     * If `false`, the source will not change the current Camera Anchor Property.
     *
     * Default to `true`.
     */
    var overviewAnchorUpdatesAllowed = true

    private var additionalPointsToFrameForFollowing: List<Point> = emptyList()
    private var additionalPointsToFrameForOverview: List<Point> = emptyList()

    private var viewportData: ViewportData = ViewportData(
        cameraForFollowing = CameraOptions.Builder()
            .center(followingCenterProperty.get())
            .zoom(followingZoomProperty.get())
            .bearing(followingBearingProperty.get())
            .pitch(followingPitchProperty.get())
            .padding(followingPaddingProperty.get())
            .anchor(followingAnchorProperty.get())
            .build(),
        cameraForOverview = CameraOptions.Builder()
            .center(overviewCenterProperty.get())
            .zoom(overviewZoomProperty.get())
            .bearing(overviewBearingProperty.get())
            .pitch(overviewPitchProperty.get())
            .padding(overviewPaddingProperty.get())
            .anchor(overviewAnchorProperty.get())
            .build()
    )
        set(value) {
            if (value != field) {
                field = value
                viewportDataSourceUpdateObservers.forEach {
                    it.viewportDataSourceUpdated(value)
                }
            }
        }
    private val viewportDataSourceUpdateObservers =
        CopyOnWriteArraySet<ViewportDataSourceUpdateObserver>()

    override fun getViewportData(): ViewportData = viewportData

    override fun registerUpdateObserver(
        viewportDataSourceUpdateObserver: ViewportDataSourceUpdateObserver
    ) {
        viewportDataSourceUpdateObservers.add(viewportDataSourceUpdateObserver)
        viewportDataSourceUpdateObserver.viewportDataSourceUpdated(viewportData)
    }

    override fun unregisterUpdateObserver(
        viewportDataSourceUpdateObserver: ViewportDataSourceUpdateObserver
    ) {
        viewportDataSourceUpdateObservers.remove(viewportDataSourceUpdateObserver)
    }

    /**
     * Computes [ViewportData] based on the available data, saves the value,
     * and notifies the [ViewportDataSourceUpdateObserver]s.
     *
     * @see [registerUpdateObserver]
     * @see [getViewportData]
     */
    fun evaluate() {
        viewportData = ViewportData(
            cameraForFollowing = followingCameraOptions,
            cameraForOverview = overviewCameraOptions
        )
    }

    /**
     * Call whenever the primary route changes.
     * This produces and stores geometries that need to be framed for overview.
     *
     * @see [MapboxNavigation.registerRoutesObserver]
     * @see [clearRouteData]
     * @see [evaluate]
     */
    fun onRouteChanged(route: DirectionsRoute) {
        completeRoutePoints = processRouteInfo(route)
        remainingPointsOnRoute = completeRoutePoints.flatten().flatten()
        remainingPointsOnCurrentStep = emptyList()
        averageIntersectionDistancesOnRoute = processRouteIntersections(route)
        updateData()
    }

    /**
     * Call whenever [RouteProgress] changes to produce following geometries of the current step
     * and overview geometries of the remaining points on the route that need to be framed.
     * This will make the following frame change zoom level and pitch depending on the proximity to
     * the upcoming maneuver and resize overview to fit only remaining portion of the route.
     *
     * [onRouteChanged] has to be called before providing progress to compute any updates based
     * on the current primary route's geometry.
     *
     * @see [MapboxNavigation.registerRouteProgressObserver]
     * @see [clearRouteData]
     * @see [evaluate]
     */
    fun onRouteProgressChanged(routeProgress: RouteProgress) {
        routeProgress.currentLegProgress?.let { currentLegProgress ->
            currentLegProgress.currentStepProgress?.let { currentStepProgress ->
                val currentStepFullPoints = currentStepProgress.stepPoints ?: emptyList()
                var distanceTraveledOnStepKM =
                    currentStepProgress.distanceTraveled.metersToKilometers().coerceAtLeast(0.0)
                val fullDistanceOfCurrentStepKM =
                    (currentStepProgress.distanceRemaining + currentStepProgress.distanceTraveled)
                        .metersToKilometers().coerceAtLeast(0.0)
                if (distanceTraveledOnStepKM > fullDistanceOfCurrentStepKM) {
                    distanceTraveledOnStepKM = 0.0
                }

                var lookaheadDistanceForZoom = fullDistanceOfCurrentStepKM
                if (usingIntersectionDensityToCalculateZoom) {
                    val lookaheadInKM = averageIntersectionDistancesOnRoute[currentLegProgress.legIndex][currentStepProgress.stepIndex] / 1000.0
                    lookaheadDistanceForZoom = distanceTraveledOnStepKM + (lookaheadInKM * averageIntersectionDistanceMultiplier)
                }

                try {
                    remainingPointsOnCurrentStep = TurfMisc.lineSliceAlong(
                        LineString.fromLngLats(currentStepFullPoints),
                        distanceTraveledOnStepKM,
                        lookaheadDistanceForZoom,
                        TurfConstants.UNIT_KILOMETERS
                    ).coordinates()
                } catch (e: TurfException) {
                    return
                }

                val currentLegPoints = completeRoutePoints[currentLegProgress.legIndex]
                val remainingStepsAfterCurrentStep =
                    if (currentStepProgress.stepIndex < currentLegPoints.size) {
                        currentLegPoints.slice(
                            currentStepProgress.stepIndex + 1 until currentLegPoints.size - 1
                        )
                    } else {
                        emptyList()
                    }
                val remainingPointsAfterCurrentStep = remainingStepsAfterCurrentStep.flatten()
                remainingPointsOnRoute = listOf(
                    remainingPointsOnCurrentStep,
                    remainingPointsAfterCurrentStep
                ).flatten()

                updateData()
                return
            }
        }
        remainingPointsOnCurrentStep = emptyList()
        remainingPointsOnRoute = emptyList()
        updateData()
    }

    /**
     * Call whenever new user location is available.
     *
     * @see [MapboxNavigation.registerLocationObserver]
     * @see [MapboxNavigation.registerMapMatcherResultObserver]
     * @see [evaluate]
     */
    fun onLocationChanged(location: Location) {
        targetLocation = location
        updateData()
    }

    /**
     * Clears all data associated with [onRouteChanged] and [onRouteProgressChanged] calls.
     *
     * Use this to clear the data source's cache after navigation session finishes and reposition
     * the camera on top of the puck in free drive.
     *
     * @see [evaluate]
     */
    fun clearRouteData() {
        completeRoutePoints = emptyList()
        remainingPointsOnCurrentStep = emptyList()
        remainingPointsOnRoute = emptyList()
        updateData()
    }

    /**
     * Provide additional points that should be fitted into the following frame update.
     */
    fun additionalPointsToFrameForFollowing(points: List<Point>) {
        additionalPointsToFrameForFollowing = ArrayList(points)
        updateData()
    }

    /**
     * Provide additional points that should be fitted into the overview frame update.
     */
    fun additionalPointsToFrameForOverview(points: List<Point>) {
        additionalPointsToFrameForOverview = ArrayList(points)
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Center Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] following updates instead of the opinionated value.
     * @see [evaluate]
     */
    fun followingCenterPropertyOverride(value: Point?) {
        followingCenterProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Center Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] following updates instead of the opinionated value.
     * @see [evaluate]
     */
    fun followingZoomPropertyOverride(value: Double?) {
        followingZoomProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Center Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] following updates instead of the opinionated value.
     * @see [evaluate]
     */
    fun followingBearingPropertyOverride(value: Double?) {
        followingBearingProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Center Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] following updates instead of the opinionated value.
     * @see [evaluate]
     */
    fun followingPitchPropertyOverride(value: Double?) {
        followingPitchProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Center Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] following updates instead of the opinionated value.
     * @see [evaluate]
     */
    fun followingPaddingPropertyOverride(value: EdgeInsets?) {
        followingPaddingProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Center Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] following updates instead of the opinionated value.
     * @see [evaluate]
     */
    fun followingAnchorPropertyOverride(value: ScreenCoordinate?) {
        followingAnchorProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Center Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] overview updates instead of the opinionated value.
     *
     * @see [evaluate]
     */
    fun overviewCenterPropertyOverride(value: Point?) {
        overviewCenterProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Zoom Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] overview updates instead of the opinionated value.
     *
     * @see [evaluate]
     */
    fun overviewZoomPropertyOverride(value: Double?) {
        overviewZoomProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Bearing Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] overview updates instead of the opinionated value.
     *
     * @see [evaluate]
     */
    fun overviewBearingPropertyOverride(value: Double?) {
        overviewBearingProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Pitch Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] overview updates instead of the opinionated value.
     *
     * @see [evaluate]
     */
    fun overviewPitchPropertyOverride(value: Double?) {
        overviewPitchProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Padding Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] overview updates instead of the opinionated value.
     *
     * @see [evaluate]
     */
    fun overviewPaddingPropertyOverride(value: EdgeInsets?) {
        overviewPaddingProperty.override = value
        updateData()
    }

    /**
     * Whenever [evaluate] is called, the source produces [ViewportData] updates
     * with opinionated values for all camera properties.
     *
     * Use this method to override the Anchor Camera Property. As long as the override is present,
     * it will be used for all [ViewportData] overview updates instead of the opinionated value.
     *
     * @see [evaluate]
     */
    fun overviewAnchorPropertyOverride(value: ScreenCoordinate?) {
        overviewAnchorProperty.override = value
        updateData()
    }

    /**
     * Helper method that clears all user-set overrides for camera properties when in following.
     */
    fun clearFollowingOverrides() {
        followingCenterProperty.override = null
        followingZoomProperty.override = null
        followingBearingProperty.override = null
        followingPitchProperty.override = null
        followingPaddingProperty.override = null
        followingAnchorProperty.override = null
        updateData()
    }

    /**
     * Helper method that clears all user-set overrides for camera properties when in overview.
     */
    fun clearOverviewOverrides() {
        overviewCenterProperty.override = null
        overviewZoomProperty.override = null
        overviewBearingProperty.override = null
        overviewPitchProperty.override = null
        overviewPaddingProperty.override = null
        overviewAnchorProperty.override = null
        updateData()
    }

    private fun processRouteInfo(route: DirectionsRoute): List<List<List<Point>>> {
        return route.legs()?.map { routeLeg ->
            routeLeg.steps()?.map { legStep ->
                legStep.geometry()?.let { geometry ->
                    PolylineUtils.decode(geometry, Constants.PRECISION_6).toList()
                } ?: emptyList()
            } ?: emptyList()
        } ?: emptyList()
    }

//    TODO: Make these ViewportDataSourceOptions
    private var usingIntersectionDensityToCalculateZoom = true
    private var averageIntersectionDistanceMultiplier = 5
    private var minimumMetersForIntersectionDensity = 20.0

    private fun processRouteIntersections(route: DirectionsRoute): List<List<Double>> {
        val output = route.legs()?.map {routeLeg ->
            routeLeg.steps()?.map { legStep ->
                legStep.geometry()?.let { geometry ->
                    val stepPoints = PolylineUtils.decode(geometry, Constants.PRECISION_6).toList()
                    val intersectionLocations: List<Point> = legStep.intersections()?.map { it.location() }
                        ?: emptyList()
                    val list: MutableList<Point> = ArrayList()
                    list.addAll(listOf(stepPoints.first()))
                    list.addAll(intersectionLocations)
                    list.addAll(listOf(stepPoints.last()))
                    val comparisonList = list.toMutableList()
                    list.removeFirst()
                    val intersectionDistances = list.mapIndexed { index, point ->
                        TurfMeasurement.distance(point, comparisonList[index]) * 1000.0
                    }
                    val filteredIntersectionDistances = intersectionDistances.filter { it > minimumMetersForIntersectionDensity }
                    if (filteredIntersectionDistances.size > 0) {
                        filteredIntersectionDistances.reduce {acc, next -> acc + next} / filteredIntersectionDistances.size
                    } else {
                        minimumMetersForIntersectionDensity
                    }
                } ?: 0.0
            } ?: emptyList()
        } ?: emptyList()
        return output
    }

    private fun updateData() {
        val pointsForFollowing: MutableList<Point> = remainingPointsOnCurrentStep.toMutableList()
        val pointsForOverview: MutableList<Point> = remainingPointsOnRoute.toMutableList()

        val localTargetLocation = targetLocation
        if (localTargetLocation != null) {
            pointsForFollowing.add(0, localTargetLocation.toPoint())
            pointsForOverview.add(0, localTargetLocation.toPoint())
        }

        pointsForFollowing.addAll(additionalPointsToFrameForFollowing)
        pointsForOverview.addAll(additionalPointsToFrameForOverview)

        updateFollowingData(pointsForFollowing)
        updateOverviewData(pointsForOverview)

        val anchorPointFromPitch = getAnchorPointFromPitch(followingPitchProperty.get(), options.maxFollowingPitch, mapboxMap.getSize(), followingPaddingProperty.get())
        val paddingFromAnchorPoint = getEdgeInsetsFromPoint(mapboxMap.getSize(), anchorPointFromPitch)

        followingCameraOptions =
            CameraOptions.Builder().apply {
                if (followingCenterUpdatesAllowed) {
                    center(followingCenterProperty.get())
                }
                if (followingZoomUpdatesAllowed) {
                    zoom(followingZoomProperty.get())
                }
                if (followingBearingUpdatesAllowed) {
                    bearing(followingBearingProperty.get())
                }
                if (followingPitchUpdatesAllowed) {
                    pitch(followingPitchProperty.get())
                }
                if (followingPaddingUpdatesAllowed) {
                    padding(paddingFromAnchorPoint)
                }
                if (followingAnchorUpdatesAllowed) {
                    anchor(followingAnchorProperty.get())
                }
            }.build()

        overviewCameraOptions =
            CameraOptions.Builder().apply {
                if (overviewCenterUpdatesAllowed) {
                    center(overviewCenterProperty.get())
                }
                if (overviewZoomUpdatesAllowed) {
                    zoom(overviewZoomProperty.get())
                }
                if (overviewBearingUpdatesAllowed) {
                    bearing(overviewBearingProperty.get())
                }
                if (overviewPitchUpdatesAllowed) {
                    pitch(overviewPitchProperty.get())
                }
                if (overviewPaddingUpdatesAllowed) {
                    padding(overviewPaddingProperty.get())
                }
                if (overviewAnchorUpdatesAllowed) {
                    anchor(overviewAnchorProperty.get())
                }
            }.build()
    }

    private fun getAnchorPointFromPitch(currentPitch: Double = 0.0, maxPitch: Double = 60.0, mapSize: Size, padding: EdgeInsets): ScreenCoordinate {
        val centerInsidePaddingX = ((mapSize.width - padding.left - padding.right) / 2.0) + padding.left
        val centerInsidePaddingY = ((mapSize.height - padding.top - padding.bottom) / 2.0) + padding.top
        val pitchPercentage = min(max(currentPitch / maxPitch, 0.0), 1.0)
        val anchorPointX = centerInsidePaddingX
        val anchorPointY = ((mapSize.height - padding.bottom - centerInsidePaddingY) * pitchPercentage) + centerInsidePaddingY
        return ScreenCoordinate(anchorPointX,anchorPointY)
    }

    private fun getEdgeInsetsFromPoint(mapSize: Size, screenPoint: ScreenCoordinate? = null): EdgeInsets {
        var point = screenPoint ?: ScreenCoordinate(mapSize.width.toDouble() / 2.0, mapSize.height.toDouble() / 2.0)
        return EdgeInsets(point.y.toDouble(), point.x.toDouble(), (mapSize.height - point.y).toDouble(), (mapSize.width - point.x).toDouble())
    }

    private fun updateFollowingData(pointsForFollowing: List<Point>) {
        followingBearingProperty.fallback = normalizeBearing(
            mapboxMap.getCameraOptions(null).bearing ?: 0.0,
            targetLocation?.bearing?.toDouble() ?: 0.0
        )

        val zoomAndCenter = getZoomLevelAndCenterCoordinate(
            pointsForFollowing,
            followingBearingProperty.get(),
            followingPitchProperty.get(),
            followingPaddingProperty.get()
        )

        followingCenterProperty.fallback = pointsForFollowing.firstOrNull()
            ?: Point.fromLngLat(0.0, 0.0) // todo how about "zoomAndCenter.second"?

        followingZoomProperty.fallback =
            max(min(zoomAndCenter.first, options.maxZoom), options.minFollowingZoom)
    }

    private fun updateOverviewData(pointsForOverview: List<Point>) {
        overviewBearingProperty.fallback = normalizeBearing(
            mapboxMap.getCameraOptions(null).bearing ?: 0.0,
            0.0
        )

        val zoomAndCenter = getZoomLevelAndCenterCoordinate(
            pointsForOverview,
            overviewBearingProperty.get(),
            overviewPitchProperty.get(),
            overviewPaddingProperty.get()
        )

        overviewCenterProperty.fallback = zoomAndCenter.second

        overviewZoomProperty.fallback = min(zoomAndCenter.first, options.maxZoom)
    }

    private fun normalizeBearing(currentBearing: Double, targetBearing: Double) =
        currentBearing + shortestRotation(currentBearing, targetBearing)

    private fun getZoomLevelAndCenterCoordinate(
        points: List<Point>,
        bearing: Double,
        pitch: Double,
        padding: EdgeInsets
    ): Pair<Double, Point> {
        val cam = if (points.isNotEmpty()) {
            mapboxMap.cameraForCoordinates(points, padding, bearing, pitch)
        } else null

        return Pair(cam?.zoom ?: MINIMUM_ZOOM_LEVEL_FOR_GEO, cam?.center ?: NULL_ISLAND_POINT)
    }
}

private sealed class ViewportProperty<T>(var override: T?, var fallback: T) {

    fun get() = override ?: fallback

    class CenterProperty(override: Point?, fallback: Point) : ViewportProperty<Point>(
        override,
        fallback
    )

    class ZoomProperty(override: Double?, fallback: Double) : ViewportProperty<Double>(
        override,
        fallback
    )

    class BearingProperty(override: Double?, fallback: Double) : ViewportProperty<Double>(
        override,
        fallback
    )

    class PitchProperty(override: Double?, fallback: Double) : ViewportProperty<Double>(
        override,
        fallback
    )

    class PaddingProperty(override: EdgeInsets?, fallback: EdgeInsets) :
        ViewportProperty<EdgeInsets>(
            override,
            fallback
        )

    class AnchorProperty(override: ScreenCoordinate?, fallback: ScreenCoordinate) :
        ViewportProperty<ScreenCoordinate>(
            override,
            fallback
        )
}
