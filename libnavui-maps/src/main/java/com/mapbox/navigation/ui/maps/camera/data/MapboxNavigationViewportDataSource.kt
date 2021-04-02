package com.mapbox.navigation.ui.maps.camera.data

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.utils.isSameUuid
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getAnchorPointFromPitchPercentage
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getBearingForMap
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getEdgeInsetsFromPoint
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.normalizeBearing
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteForPostManeuverFramingGeometry
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteInfo
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteIntersections
import com.mapbox.navigation.ui.maps.camera.utils.metersToKilometers
import com.mapbox.navigation.ui.maps.camera.utils.toPoint
import com.mapbox.navigation.ui.maps.internal.camera.data.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMisc
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.max
import kotlin.math.min

private val NULL_ISLAND_POINT = Point.fromLngLat(0.0, 0.0)
private val EMPTY_EDGE_INSETS = EdgeInsets(0.0, 0.0, 0.0, 0.0)
private val CENTER_SCREEN_COORDINATE = ScreenCoordinate(0.0, 0.0)

/**
 * Default implementation of [ViewportDataSource] to use with the [NavigationCamera].
 *
 * Use:
 * - [onRouteChanged] to produce overview geometries that need to be framed
 * - [onRouteProgressChanged] (requires also [onRouteChanged]) to produce following geometries of the current step
 * and overview geometries of the remaining points on the route that need to be framed.
 * This will make the frame in following mode change zoom level depending on the proximity to
 * the upcoming maneuver and resize the frame in overview mode to fit only the remaining portion of the route.
 * - [onLocationChanged] to pass a point to be framed and used as a source of bearing for the
 * following camera frame
 * - [additionalPointsToFrameForFollowing] - points that also need to be visible in
 * the following camera frame
 * - [additionalPointsToFrameForOverview] - points that also need to be visible in
 * the overview camera frame
 *
 * Whenever a set of these arguments is provided or refreshed, you need to call [evaluate]
 * to process the data and compute an opinionated [ViewportData] update that [NavigationCamera]
 * observes and applies.
 *
 * Based on the provided data, the class will make decisions on how the camera should be framed.
 * However, that might not always match with your expectations or needs.
 * Let’s imagine that you would like to temporarily zoom in (when the user double-tapped the map)
 * or change the camera’s bearing to focus on a POI. To serve those use-cases,
 * all of the camera property values that this data source produces can be overridden.
 * The source will keep producing the default, opinionated values, but as long as the override
 * is present, they won’t be used. Passing `null` as an override resets it to the default value.
 *
 * The class also offers various mutable options that can be modified at any point in time
 * to influence the style of frames that are produced.
 * Make sure to get familiar with all of the public fields and their documentation.
 *
 * Whenever any changes are made to the data source (new values provided, overrides added/removed,
 * or settings changed), remember to call [evaluate] recompute frames and notify observers.
 *
 * ## Padding and framing behavior
 * This data source initializes at the `null island` (0.0, 0.0). Make sure to first provide at least
 * [onLocationChanged] for following mode framing and [onRouteChanged] for overview mode framing
 * (or the [additionalPointsToFrameForOverview] and [additionalPointsToFrameForFollowing]).
 *
 * ### Overview
 * [overviewPadding] is used to generate the correct zoom level while positioning the contents on screen
 * and is also applied to the resulting default [ViewportData.cameraForOverview].
 * The default bearing for overview framing is north (`0.0`).
 *
 * ### Following
 * [followingPadding] is used to generate the correct zoom level while positioning the contents on screen
 * but the padding value is not applied the [ViewportData.cameraForFollowing].
 * Instead, the frame contains a specific [CameraOptions.padding] value that manipulates the vanishing point of the camera
 * to provide a better experience for end users when the camera is pitched.
 *
 * **This vanishing point change cannot be recovered from automatically without impacting the camera position.
 * That's why, if you use the [MapboxNavigationViewportDataSource],
 * you should explicitly define [CameraOptions.padding] in all other transition that your app is running.**
 *
 * **When following frame is used, the first point of the framed geometry list will be placed at the bottom edge of this padding, centered horizontally.**
 * This typically refers to the user's location provided via [onLocationChanged], if available.
 * This can be influenced with [maximizeViewableAreaWhenPitchZero].
 *
 * **The geometries that are below the bottom edge of the following padding on screen (based on camera's bearing) are ignored and not being framed.**
 * It's impossible to find a zoom level that would fit geometries that are below the vanishing point of the camera,
 * since the vanishing point is placed at the bottom edge of the provided padding.
 *
 * The default pitch for following frames is [defaultFollowingPitch] and zoom is determined based on upcoming geometries or [maxZoom].
 *
 * ## Debugging
 * **This feature is currently experimental an subject to change.**
 *
 * You can use [debugger] to provide a [MapboxNavigationViewportDataSourceDebugger] instance
 * which will draw various info on the screen when the [NavigationCamera] operates to together with
 * the [MapboxNavigationViewportDataSource].
 *
 * Make sure to also provide the same instance to [NavigationCamera.debugger].
 *
 * ## Examples
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
 *             navigationCamera.clearRouteData()
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
 * viewportDataSource.onLocationChanged(enhancedLocation)
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
 *     // request camera to idle first or use `NavigationBasicGesturesHandler` or `NavigationScaleGestureHandler`
 *     mapView.getCameraAnimationsPlugin().flyTo(
 *         CameraOptions.Builder()
 *             .padding(edgeInsets)
 *             .center(point)
 *             .bearing(0.0)
 *             .zoom(14.0)
 *             .pitch(0.0)
 *             .build(),
 *         MapAnimationOptions.mapAnimationOptions {
 *             duration(1000L)
 *         }
 *     )
 * }
 * ```
 */
class MapboxNavigationViewportDataSource(
    private val mapboxMap: MapboxMap
) : ViewportDataSource {

    /**
     * Set a [MapboxNavigationViewportDataSourceDebugger].
     */
    var debugger: MapboxNavigationViewportDataSourceDebugger? = null

    private var route: DirectionsRoute? = null
    private var completeRoutePoints: List<List<List<Point>>> = emptyList()
    private var postManeuverFramingPoints: List<List<List<Point>>> = emptyList()
    private var remainingPointsOnCurrentStep: List<Point> = emptyList()
    private var pointsToFrameAfterCurrentStep: List<Point> = emptyList()
    private var remainingPointsOnRoute: List<Point> = emptyList()
    private var targetLocation: Location? = null
    private var averageIntersectionDistancesOnRoute: List<List<Double>> = emptyList()
    private var distanceRemainingOnCurrentStep: Float? = null

    /* -------- CONSTRAINTS -------- */
    /**
     * The default pitch that will be generated for following camera frames.
     *
     * Defaults to `45.0` degrees.
     */
    var defaultFollowingPitch = 45.0

    /**
     * The min zoom that will be generated for camera following frames.
     *
     * Defaults to `10.5`.
     */
    var minFollowingZoom = 10.5

    /**
     * The max zoom that will be generated for all camera frames.
     *
     * Defaults to `17.0`.
     */
    var maxZoom = 17.0

    /* -------- GENERATED OPTIONS -------- */
    private var followingCameraOptions = CameraOptions.Builder().build()
    private var overviewCameraOptions = CameraOptions.Builder().build()

    /* -------- OVERRIDES -------- */
    private val followingCenterProperty = ViewportProperty.CenterProperty(null, NULL_ISLAND_POINT)
    private val followingZoomProperty = ViewportProperty.ZoomProperty(null, 0.0)
    private val followingBearingProperty = ViewportProperty.BearingProperty(null, 0.0)
    private val followingPitchProperty = ViewportProperty.PitchProperty(
        null,
        defaultFollowingPitch
    )
    private val followingAnchorProperty = ViewportProperty.AnchorProperty(
        null,
        CENTER_SCREEN_COORDINATE
    )
    private val overviewCenterProperty = ViewportProperty.CenterProperty(null, NULL_ISLAND_POINT)
    private val overviewZoomProperty = ViewportProperty.ZoomProperty(null, 0.0)
    private val overviewBearingProperty = ViewportProperty.BearingProperty(null, 0.0)
    private val overviewPitchProperty = ViewportProperty.PitchProperty(null, 0.0)
    private val overviewAnchorProperty = ViewportProperty.AnchorProperty(
        null,
        CENTER_SCREEN_COORDINATE
    )

    /* -------- FOLLOWING FRAME SETTINGS -------- */
    /**
     * When enabled and a route is provided via [onRouteChanged] and updates via [onRouteProgressChanged],
     * the geometry that's going to be **framed for following** will not match the whole remainder of the current step
     * but a smaller subset of that geometry to make the zoom level higher.
     *
     * This has an effect of zooming closer in urban locations when intersections are dense and zooming out on highways where opportunities to turn are farther apart.
     *
     * Defaults to `true`.
     */
    var useIntersectionDensityToCalculateGeometryForFraming = true

    /**
     * When [useIntersectionDensityToCalculateGeometryForFraming] is enabled,
     * this multiplier can be used to adjust the size of the portion of the remaining step that's going to be selected for framing.
     *
     * Defaults to `5.0`.
     */
    var averageIntersectionDistanceMultiplier = 5.0

    /**
     * When [useIntersectionDensityToCalculateGeometryForFraming] is enabled,
     * describes the minimum distance between intersections to count them as 2 instances.
     *
     * This has an effect of filtering out intersections based on parking lot entrances, driveways and alleys from the average intersection distance.
     *
     * Defaults to `20.0` meters.
     */
    var minimumMetersForIntersectionDensity = 20.0

    /**
     * When enabled and a route is provided via [onRouteChanged] and updates via [onRouteProgressChanged],
     * the generated **following camera frame** will have pitch `0`.
     *
     * Depends on [distanceFromManeuverToUsePitchZero].
     *
     * Defaults to `true`.
     */
    var usePitchZeroNearManeuvers = true

    /**
     * When [usePitchZeroNearManeuvers] is enabled,
     * this variable describes the threshold distance to the next maneuver makes the frame with pitch `0`,
     * based on the [onRouteProgressChanged].
     *
     * Defaults to `180.0` meters.
     */
    var distanceFromManeuverToUsePitchZero = 180.0

    /**
     * When a produced **following frame** has pitch `0`,
     * the puck will not be tied to the bottom edge of the [followingPadding] and instead move
     * around the centroid of the maneuver's geometry to maximize the view of the maneuver's geometry within the [followingPadding].
     *
     * Defaults to `true`.
     */
    var maximizeViewableAreaWhenPitchZero = true

    /**
     * When a produced **following frame** has pitch `0`,
     * this controls whether additional points _after_ the upcoming maneuver should be framed to provide more context.
     *
     * Defaults to `true`.
     */
    var framePointsAfterManeuverWhenPitchZero = true

    /**
     * When [framePointsAfterManeuverWhenPitchZero] is enabled,
     * this controls the distance between maneuvers closely following the current one to treat them for inclusion in the frame.
     *
     * Defaults to `150.0` meters.
     */
    var distanceToCoalesceCompoundManeuvers = 150.0

    /**
     * When [framePointsAfterManeuverWhenPitchZero] is enabled,
     * this controls the distance on route after the current maneuver to include in the frame.
     *
     * This is added on top of potentially included compound maneuvers that closely follow the upcoming one,
     * controlled by [distanceToCoalesceCompoundManeuvers].
     *
     * Defaults to `100.0` meters.
     */
    var distanceToFrameAfterManeuver = 100.0

    /**
     * If enabled, the **following frame**'s bearing won't exactly reflect the bearing returned by the [Location],
     * but will also be affected by the direction to the upcoming framed geometry, to maximize the viewable area.
     *
     * Defaults to `true`.
     *
     * @see [maxBearingAngleDiffWhenSmoothing]
     */
    var useBearingSmoothing = true

    /**
     * When [useBearingSmoothing] is enabled, this controls how much the **following frame**'s bearing
     * can deviate from the [Location] bearing, in degrees.
     *
     * Defaults to `20.0` degrees.
     */
    var maxBearingAngleDiffWhenSmoothing = 20.0

    /* -------- PADDING UPDATES -------- */

    /**
     * Holds a padding (in pixels, in reference to the [MapView]'s size) used for generating a following frame.
     *
     * **When following frame is used, the first point of the framed geometry list will be placed at the bottom edge of this padding, centered horizontally.**
     * This typically refers to the user's location provided via [onLocationChanged], if available.
     * This can be influenced with [maximizeViewableAreaWhenPitchZero].
     *
     * The frame will contain the remaining portion of the current [LegStep] of the route provided via [onRouteChanged]
     * based on [onRouteProgressChanged].
     *
     * If the [RouteProgress] is not available,
     * the frame will focus on the location sample from [onLocationChanged] with [maxZoom] zoom and [defaultFollowingPitch].
     *
     * You can use [followingZoomPropertyOverride] and [followingPitchPropertyOverride]
     * to control the camera in scenarios like free drive where the maneuver points are not available.
     */
    var followingPadding: EdgeInsets = EMPTY_EDGE_INSETS
    private var appliedFollowingPadding = followingPadding

    /**
     * Holds a padding (in pixels, in reference to the [MapView]'s size) used for generating an overview frame.
     *
     * The frame will contain the entirety of the route provided via [onRouteChanged]
     * or its remainder if [onRouteProgressChanged] is also available, and the [additionalPointsToFrameForOverview].
     */
    var overviewPadding: EdgeInsets = EMPTY_EDGE_INSETS

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
            .padding(appliedFollowingPadding)
            .anchor(followingAnchorProperty.get())
            .build(),
        cameraForOverview = CameraOptions.Builder()
            .center(overviewCenterProperty.get())
            .zoom(overviewZoomProperty.get())
            .bearing(overviewBearingProperty.get())
            .pitch(overviewPitchProperty.get())
            .padding(overviewPadding)
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
        updateFollowingData()
        updateOverviewData()

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
                    padding(appliedFollowingPadding)
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
                    padding(overviewPadding)
                }
                if (overviewAnchorUpdatesAllowed) {
                    anchor(overviewAnchorProperty.get())
                }
            }.build()

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
        this.route = route
        completeRoutePoints = processRouteInfo(route)
        remainingPointsOnRoute = completeRoutePoints.flatten().flatten()
        remainingPointsOnCurrentStep = emptyList()
        averageIntersectionDistancesOnRoute = processRouteIntersections(
            minimumMetersForIntersectionDensity,
            route
        )
        postManeuverFramingPoints = processRouteForPostManeuverFramingGeometry(
            distanceToCoalesceCompoundManeuvers,
            distanceToFrameAfterManeuver,
            route
        )
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
        // todo abort if there's no route cached or differs form the one in rp
        if (this.route == null) {
            return
        }

        if (this.route?.isSameUuid(routeProgress.route) != true) {
            Log.w(
                "ViewportDataSource",
                "Provided route and navigated route do not have the same UUID."
            )
        }

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

                distanceRemainingOnCurrentStep = currentStepProgress.distanceRemaining

                var lookaheadDistanceForZoom = fullDistanceOfCurrentStepKM
                if (useIntersectionDensityToCalculateGeometryForFraming &&
                    averageIntersectionDistancesOnRoute.isNotEmpty()
                ) {
                    val lookaheadInKM =
                        averageIntersectionDistancesOnRoute
                            .get(currentLegProgress.legIndex)
                            .get(currentStepProgress.stepIndex)
                            .div(1000.0)
                    lookaheadDistanceForZoom = distanceTraveledOnStepKM +
                        (lookaheadInKM * averageIntersectionDistanceMultiplier)
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

                pointsToFrameAfterCurrentStep = if (postManeuverFramingPoints.isNotEmpty()) {
                    postManeuverFramingPoints
                        .get(currentLegProgress.legIndex)
                        .get(currentStepProgress.stepIndex)
                } else {
                    emptyList()
                }

                val currentLegPoints = if (completeRoutePoints.isNotEmpty()) {
                    completeRoutePoints[currentLegProgress.legIndex]
                } else {
                    emptyList()
                }
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
                return
            }
        }
        remainingPointsOnCurrentStep = emptyList()
        remainingPointsOnRoute = emptyList()
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
        route = null
        completeRoutePoints = emptyList()
        postManeuverFramingPoints = emptyList()
        remainingPointsOnCurrentStep = emptyList()
        pointsToFrameAfterCurrentStep = emptyList()
        remainingPointsOnRoute = emptyList()
        averageIntersectionDistancesOnRoute = emptyList()
        distanceRemainingOnCurrentStep = null
    }

    /**
     * Provide additional points that should be fitted into the following frame update.
     */
    fun additionalPointsToFrameForFollowing(points: List<Point>) {
        additionalPointsToFrameForFollowing = ArrayList(points)
    }

    /**
     * Provide additional points that should be fitted into the overview frame update.
     */
    fun additionalPointsToFrameForOverview(points: List<Point>) {
        additionalPointsToFrameForOverview = ArrayList(points)
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
    }

    /**
     * Helper method that clears all user-set overrides for camera properties when in following.
     */
    fun clearFollowingOverrides() {
        followingCenterProperty.override = null
        followingZoomProperty.override = null
        followingBearingProperty.override = null
        followingPitchProperty.override = null
        followingAnchorProperty.override = null
    }

    /**
     * Helper method that clears all user-set overrides for camera properties when in overview.
     */
    fun clearOverviewOverrides() {
        overviewCenterProperty.override = null
        overviewZoomProperty.override = null
        overviewBearingProperty.override = null
        overviewPitchProperty.override = null
        overviewAnchorProperty.override = null
    }

    private fun updateFollowingData() {
        val pointsForFollowing: MutableList<Point> = remainingPointsOnCurrentStep.toMutableList()
        val localTargetLocation = targetLocation

        if (localTargetLocation != null) {
            pointsForFollowing.add(0, localTargetLocation.toPoint())
        }

        // needs to be added here to be taken into account for bearing smoothing
        pointsForFollowing.addAll(additionalPointsToFrameForFollowing)

        followingBearingProperty.fallback = getBearingForMap(
            if (useBearingSmoothing) maxBearingAngleDiffWhenSmoothing else 0.0,
            mapboxMap.getCameraOptions().bearing ?: 0.0,
            localTargetLocation?.bearing?.toDouble() ?: 0.0,
            pointsForFollowing
        )

        val distanceRemainingOnCurrentStep = this.distanceRemainingOnCurrentStep
        followingPitchProperty.fallback =
            if (usePitchZeroNearManeuvers && distanceRemainingOnCurrentStep != null) {
                if (distanceRemainingOnCurrentStep <= distanceFromManeuverToUsePitchZero) {
                    0.0
                } else {
                    defaultFollowingPitch
                }
            } else {
                defaultFollowingPitch
            }

        if (framePointsAfterManeuverWhenPitchZero && followingPitchProperty.get() == 0.0) {
            pointsForFollowing.addAll(pointsToFrameAfterCurrentStep)
        }

        val cameraFrame =
            if (maximizeViewableAreaWhenPitchZero && followingPitchProperty.get() == 0.0) {
                followingAnchorProperty.fallback = getAnchorPointFromPitchPercentage(
                    0.0,
                    mapboxMap.getSize(),
                    followingPadding
                )

                mapboxMap.cameraForCoordinates(
                    pointsForFollowing,
                    followingPadding,
                    followingBearingProperty.get(),
                    followingPitchProperty.get()
                )
            } else {
                followingAnchorProperty.fallback = getAnchorPointFromPitchPercentage(
                    1.0,
                    mapboxMap.getSize(),
                    followingPadding
                )

                val mapSize = mapboxMap.getSize()

                val topLeft = ScreenCoordinate(
                    followingPadding.left,
                    followingPadding.top
                )
                val bottomRight = ScreenCoordinate(
                    mapSize.width - followingPadding.right,
                    mapSize.height - followingPadding.bottom
                )
                val screenBox = ScreenBox(
                    topLeft,
                    bottomRight
                )

                val cameraOptions = mapboxMap.getCameraOptions()
                    .toBuilder()
                    .center(
                        pointsForFollowing.firstOrNull()
                            ?: mapboxMap.getCameraOptions().center!!
                    )
                    .padding(getEdgeInsetsFromPoint(mapSize, followingAnchorProperty.get()))
                    .bearing(followingBearingProperty.get())
                    .pitch(followingPitchProperty.get())
                    .build()
                if (pointsForFollowing.size > 1) {
                    mapboxMap.cameraForCoordinates(
                        pointsForFollowing,
                        cameraOptions,
                        screenBox
                    )
                } else {
                    cameraOptions
                }
            }

        followingCenterProperty.fallback = cameraFrame.center!!
        followingZoomProperty.fallback =
            max(min(cameraFrame.zoom!!, maxZoom), minFollowingZoom)
        appliedFollowingPadding = cameraFrame.padding!!

        debugger?.followingPoints = pointsForFollowing
        debugger?.followingUserPadding = followingPadding
    }

    private fun updateOverviewData() {
        val pointsForOverview: MutableList<Point> = remainingPointsOnRoute.toMutableList()

        val localTargetLocation = targetLocation
        if (localTargetLocation != null) {
            pointsForOverview.add(0, localTargetLocation.toPoint())
        }

        pointsForOverview.addAll(additionalPointsToFrameForOverview)

        overviewBearingProperty.fallback = normalizeBearing(
            mapboxMap.getCameraOptions(null).bearing ?: 0.0,
            0.0
        )

        val cameraFrame = if (pointsForOverview.isNotEmpty()) {
            mapboxMap.cameraForCoordinates(
                pointsForOverview,
                overviewPadding,
                overviewBearingProperty.get(),
                overviewPitchProperty.get()
            )
        } else {
            mapboxMap.getCameraOptions()
        }

        overviewCenterProperty.fallback = cameraFrame.center!!
        overviewZoomProperty.fallback = min(cameraFrame.zoom!!, maxZoom)

        debugger?.overviewPoints = pointsForOverview
        debugger?.overviewUserPadding = overviewPadding
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

    class AnchorProperty(override: ScreenCoordinate?, fallback: ScreenCoordinate) :
        ViewportProperty<ScreenCoordinate>(
            override,
            fallback
        )
}
