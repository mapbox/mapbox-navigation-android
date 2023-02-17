package com.mapbox.navigation.ui.maps.camera.data

import android.location.Location
import androidx.annotation.UiThread
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.toCameraOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getMapAnchoredPaddingFromUserPadding
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getPitchFallbackFromRouteProgress
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getPointsToFrameAfterCurrentManeuver
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getPointsToFrameOnCurrentStep
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getRemainingPointsOnRoute
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getScreenBoxForFraming
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getSmootherBearingForMap
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteForPostManeuverFramingGeometry
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteIntersections
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.simplifyCompleteRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.camera.utils.normalizeBearing
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigation.utils.internal.toPoint
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.max
import kotlin.math.min

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
 * Make sure to get familiar with all of the public nested fields and their documentation in
 * [MapboxNavigationViewportDataSourceOptions]. The options can be mutated by accessing
 * [options] field.
 *
 * Whenever any changes are made to the data source (new values provided, overrides added/removed,
 * or options changed), remember to call [evaluate] to recompute frames and notify observers.
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
 * The side-effect of not recovering from the vanishing point change can be the center of the camera that's offset from the center of the [MapView].
 *
 * **When following frame is used, the first point of the framed geometry list will be placed at the bottom edge of this padding, centered horizontally.**
 * This typically refers to the user's location provided via [onLocationChanged], if available.
 * This can be influenced by [FollowingFrameOptions.maximizeViewableGeometryWhenPitchZero] when there are at least 2 points available for framing.
 *
 * **The geometries that are below the bottom edge of the following padding on screen (based on camera's bearing) are ignored and not being framed.**
 * It's impossible to find a zoom level that would fit geometries that are below the vanishing point of the camera,
 * since the vanishing point is placed at the bottom edge of the provided padding.
 *
 * The default pitch for following frames is [FollowingFrameOptions.defaultPitch] and zoom is determined based on upcoming geometries or [FollowingFrameOptions.maxZoom].
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
 *     override fun onRoutesChanged(result: RoutesUpdatedResult) {
 *         if (result.navigationRoutes.isNotEmpty()) {
 *             viewportDataSource.onRouteChanged(routes.first())
 *             viewportDataSource.overviewPadding = overviewEdgeInsets
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
 *     override fun onNewRawLocation(rawLocation: Location) {
 *         // no impl
 *     }
 *     override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
 *         viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
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
 * viewportDataSource.followingPadding = followingEdgeInsets
 * viewportDataSource.evaluate()
 * navigationCamera.requestNavigationCameraToFollowing()
 * ```
 *
 * #### Update current location and reset frame
 * ```kotlin
 * private val locationObserver = object : LocationObserver {
 *     override fun onNewRawLocation(rawLocation: Location) {}
 *     override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
 *         viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
 *         viewportDataSource.evaluate()
 *         if (locationMatcherResult.isTeleport) {
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
 *     mapView.camera.flyTo(
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
@UiThread
class MapboxNavigationViewportDataSource(
    private val mapboxMap: MapboxMap
) : ViewportDataSource {

    internal companion object {
        private const val LOG_CATEGORY = "MapboxNavigationViewportDataSource"
        internal val NULL_ISLAND_POINT = Point.fromLngLat(0.0, 0.0)
        internal val EMPTY_EDGE_INSETS = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        internal const val ZERO_PITCH = 0.0
        internal const val BEARING_NORTH = 0.0
    }

    /**
     * Set a [MapboxNavigationViewportDataSourceDebugger].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    var debugger: MapboxNavigationViewportDataSourceDebugger? = null

    /**
     * Holds options that impact generation of camera frames.
     *
     * You can freely mutate these options and results will be applied when the next frame is evaluated.
     */
    val options = MapboxNavigationViewportDataSourceOptions()

    private var navigationRoute: NavigationRoute? = null
    private var completeRoutePoints: List<List<List<Point>>> = emptyList()
    private var simplifiedCompleteRoutePoints: List<List<List<Point>>> = emptyList()
    private var postManeuverFramingPoints: List<List<List<Point>>> = emptyList()
    private var pointsToFrameOnCurrentStep: List<Point> = emptyList()
    private var pointsToFrameAfterCurrentStep: List<Point> = emptyList()
    private var simplifiedRemainingPointsOnRoute: List<Point> = emptyList()
    private var targetLocation: Location? = null
    private var averageIntersectionDistancesOnRoute: List<List<Double>> = emptyList()

    /* -------- GENERATED FRAMES -------- */
    private var followingCameraOptions = CameraOptions.Builder().build()
    private var overviewCameraOptions = CameraOptions.Builder().build()

    /* -------- OVERRIDES -------- */
    private val followingCenterProperty = ViewportProperty.CenterProperty(null, NULL_ISLAND_POINT)
    private val followingZoomProperty =
        ViewportProperty.ZoomProperty(null, options.followingFrameOptions.maxZoom)
    private val followingBearingProperty = ViewportProperty.BearingProperty(null, BEARING_NORTH)
    private val followingPitchProperty = ViewportProperty.PitchProperty(
        null,
        options.followingFrameOptions.defaultPitch
    )
    private val overviewCenterProperty = ViewportProperty.CenterProperty(null, NULL_ISLAND_POINT)
    private val overviewZoomProperty =
        ViewportProperty.ZoomProperty(null, options.overviewFrameOptions.maxZoom)
    private val overviewBearingProperty = ViewportProperty.BearingProperty(null, BEARING_NORTH)
    private val overviewPitchProperty = ViewportProperty.PitchProperty(null, ZERO_PITCH)

    /* -------- PADDING UPDATES -------- */

    /**
     * Holds a padding (in pixels, in reference to the [MapView]'s size) used for generating a following frame.
     *
     * **When following frame is used, the first point of the framed geometry list will be placed at the bottom edge of this padding, centered horizontally.**
     * This typically refers to the user's location provided via [onLocationChanged], if available.
     * This can be influenced by [FollowingFrameOptions.maximizeViewableGeometryWhenPitchZero] when there are at least 2 points available for framing.
     *
     * The frame will contain the remaining portion of the current [LegStep] of the route provided via [onRouteChanged]
     * based on [onRouteProgressChanged].
     *
     * If the [RouteProgress] is not available,
     * the frame will focus on the location sample from [onLocationChanged] with [FollowingFrameOptions.maxZoom] zoom and [FollowingFrameOptions.defaultPitch].
     *
     * You can use [followingZoomPropertyOverride] and [followingPitchPropertyOverride]
     * to control the camera in scenarios like free drive where the maneuver points are not available.
     */
    var followingPadding: EdgeInsets = EMPTY_EDGE_INSETS
        set(value) {
            field = value
            logI("followingPadding: $value")
        }
    private var appliedFollowingPadding = followingPadding

    /**
     * Holds a padding (in pixels, in reference to the [MapView]'s size) used for generating an overview frame.
     *
     * The frame will contain the entirety of the route provided via [onRouteChanged]
     * or its remainder if [onRouteProgressChanged] is also available, and the [additionalPointsToFrameForOverview].
     */
    var overviewPadding: EdgeInsets = EMPTY_EDGE_INSETS
        set(value) {
            field = value
            logI("overviewPadding: $value")
        }

    private var additionalPointsToFrameForFollowing: List<Point> = emptyList()
    private var additionalPointsToFrameForOverview: List<Point> = emptyList()

    private var viewportData: ViewportData = ViewportData(
        cameraForFollowing = CameraOptions.Builder()
            .center(followingCenterProperty.get())
            .zoom(followingZoomProperty.get())
            .bearing(followingBearingProperty.get())
            .pitch(followingPitchProperty.get())
            .padding(appliedFollowingPadding)
            .build(),
        cameraForOverview = CameraOptions.Builder()
            .center(overviewCenterProperty.get())
            .zoom(overviewZoomProperty.get())
            .bearing(overviewBearingProperty.get())
            .pitch(overviewPitchProperty.get())
            .padding(overviewPadding)
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
        logI("evaluate; options: $options")
        logI("evaluate; followingPadding: $followingPadding")
        logI("evaluate; overviewPadding: $overviewPadding")

        updateFollowingData()
        updateOverviewData()

        options.followingFrameOptions.run {
            followingCameraOptions =
                CameraOptions.Builder().apply {
                    if (centerUpdatesAllowed) {
                        center(followingCenterProperty.get())
                    }
                    if (zoomUpdatesAllowed) {
                        zoom(followingZoomProperty.get())
                    }
                    if (bearingUpdatesAllowed) {
                        bearing(followingBearingProperty.get())
                    }
                    if (pitchUpdatesAllowed) {
                        pitch(followingPitchProperty.get())
                    }
                    if (paddingUpdatesAllowed) {
                        padding(appliedFollowingPadding)
                    }
                }.build()
        }

        options.overviewFrameOptions.run {
            overviewCameraOptions =
                CameraOptions.Builder().apply {
                    if (centerUpdatesAllowed) {
                        center(overviewCenterProperty.get())
                    }
                    if (zoomUpdatesAllowed) {
                        zoom(overviewZoomProperty.get())
                    }
                    if (bearingUpdatesAllowed) {
                        bearing(overviewBearingProperty.get())
                    }
                    if (pitchUpdatesAllowed) {
                        pitch(overviewPitchProperty.get())
                    }
                    if (paddingUpdatesAllowed) {
                        padding(overviewPadding)
                    }
                }.build()
        }

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
    @Deprecated(
        "use #onRouteChanged(NavigationRoute) instead",
        ReplaceWith(
            "onRouteChanged(route.toNavigationRoute())",
            "com.mapbox.navigation.base.route.toNavigationRoute"
        )
    )
    fun onRouteChanged(route: DirectionsRoute) {
        onRouteChanged(route.toNavigationRoute())
    }

    /**
     * Call whenever the primary route changes.
     * This produces and stores geometries that need to be framed for overview.
     *
     * @see [MapboxNavigation.registerRoutesObserver]
     * @see [clearRouteData]
     * @see [evaluate]
     */
    fun onRouteChanged(route: NavigationRoute) {
        logI("onRouteChanged; route: ${route.id}", LOG_CATEGORY)
        if (!route.directionsRoute.isSameRoute(navigationRoute?.directionsRoute)) {
            clearRouteData()
            this.navigationRoute = route
            completeRoutePoints = processRoutePoints(route.directionsRoute)
            simplifiedCompleteRoutePoints = simplifyCompleteRoutePoints(
                options.overviewFrameOptions.geometrySimplification.enabled,
                options.overviewFrameOptions.geometrySimplification.simplificationFactor,
                completeRoutePoints
            )
            simplifiedRemainingPointsOnRoute = simplifiedCompleteRoutePoints.flatten().flatten()

            options.followingFrameOptions.intersectionDensityCalculation.run {
                averageIntersectionDistancesOnRoute = processRouteIntersections(
                    enabled,
                    minimumDistanceBetweenIntersections,
                    route.directionsRoute,
                    completeRoutePoints
                )
            }

            options.followingFrameOptions.frameGeometryAfterManeuver.run {
                postManeuverFramingPoints = processRouteForPostManeuverFramingGeometry(
                    enabled,
                    distanceToCoalesceCompoundManeuvers,
                    distanceToFrameAfterManeuver,
                    route.directionsRoute,
                    completeRoutePoints
                )
            }
        }
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
        val currentRoute = this.navigationRoute
        if (currentRoute == null) {
            logW(
                "You're calling #onRouteProgressChanged but you didn't call #onRouteChanged.",
                LOG_CATEGORY
            )
            clearProgressData()
            return
        } else if (!currentRoute.directionsRoute.isSameRoute(routeProgress.route)) {
            logE(
                "Provided route (#onRouteChanged) and navigated route " +
                    "(#onRouteProgressChanged) are not the same. " +
                    "Aborting framed geometry updates based on route progress.",
                LOG_CATEGORY
            )
            clearProgressData()
            return
        }

        val stepProgress = routeProgress.currentLegProgress?.currentStepProgress
        logI("onRouteProgressChanged; route: ${routeProgress.navigationRoute.id}, stepIndex=${stepProgress?.stepIndex}, distanceRemaining=${stepProgress?.distanceRemaining}", LOG_CATEGORY)

        ifNonNull(
            routeProgress.currentLegProgress,
            routeProgress.currentLegProgress?.currentStepProgress
        ) { currentLegProgress, currentStepProgress ->
            followingPitchProperty.fallback = getPitchFallbackFromRouteProgress(
                routeProgress,
                options.followingFrameOptions
            )

            options.followingFrameOptions.intersectionDensityCalculation.run {
                pointsToFrameOnCurrentStep = getPointsToFrameOnCurrentStep(
                    intersectionDensityCalculationEnabled = enabled,
                    intersectionDensityAverageDistanceMultiplier = this.averageDistanceMultiplier,
                    averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
                    currentLegProgress = currentLegProgress,
                    currentStepProgress = currentStepProgress
                )
            }

            options.followingFrameOptions.frameGeometryAfterManeuver.run {
                pointsToFrameAfterCurrentStep = getPointsToFrameAfterCurrentManeuver(
                    frameGeometryAfterManeuverEnabled = enabled,
                    generatedPostManeuverFramingPoints = postManeuverFramingPoints,
                    currentLegProgress = currentLegProgress,
                    currentStepProgress = currentStepProgress
                )
            }

            simplifiedRemainingPointsOnRoute = getRemainingPointsOnRoute(
                simplifiedCompleteRoutePoints,
                pointsToFrameOnCurrentStep,
                currentLegProgress,
                currentStepProgress
            )
        } ?: run {
            logE(
                "You're calling #onRouteProgressChanged with empty leg or step progress.",
                LOG_CATEGORY
            )
            clearProgressData()
        }
    }

    /**
     * Call whenever new user location is available.
     *
     * @see [MapboxNavigation.registerLocationObserver]
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
        navigationRoute = null
        completeRoutePoints = emptyList()
        postManeuverFramingPoints = emptyList()
        simplifiedCompleteRoutePoints = emptyList()
        averageIntersectionDistancesOnRoute = emptyList()
        clearProgressData()
    }

    private fun clearProgressData() {
        followingPitchProperty.fallback = options.followingFrameOptions.defaultPitch
        pointsToFrameOnCurrentStep = emptyList()
        pointsToFrameAfterCurrentStep = emptyList()
        simplifiedRemainingPointsOnRoute = simplifiedCompleteRoutePoints.flatten().flatten()
    }

    /**
     * Provide additional points that should be fitted into the following frame update.
     */
    fun additionalPointsToFrameForFollowing(points: List<Point>) {
        logI("additionalPointsToFrameForFollowing: $points", LOG_CATEGORY)
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
        logI("followingCenterPropertyOverride: $value", LOG_CATEGORY)
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
        logI("followingZoomPropertyOverride: $value", LOG_CATEGORY)
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
        logI("followingBearingPropertyOverride: $value", LOG_CATEGORY)
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
        logI("followingPitchPropertyOverride: $value", LOG_CATEGORY)
        followingPitchProperty.override = value
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
     * Helper method that clears all user-set overrides for camera properties when in following.
     */
    fun clearFollowingOverrides() {
        followingCenterProperty.override = null
        followingZoomProperty.override = null
        followingBearingProperty.override = null
        followingPitchProperty.override = null
    }

    /**
     * Helper method that clears all user-set overrides for camera properties when in overview.
     */
    fun clearOverviewOverrides() {
        overviewCenterProperty.override = null
        overviewZoomProperty.override = null
        overviewBearingProperty.override = null
        overviewPitchProperty.override = null
    }

    private fun updateFollowingData() {
        val pointsForFollowing: MutableList<Point> = pointsToFrameOnCurrentStep.toMutableList()
        val localTargetLocation = targetLocation

        if (localTargetLocation != null) {
            pointsForFollowing.add(0, localTargetLocation.toPoint())
        }

        // needs to be added here to be taken into account for bearing smoothing
        pointsForFollowing.addAll(additionalPointsToFrameForFollowing)

        logI(
            "updateFollowingData; location: $localTargetLocation",
            LOG_CATEGORY
        )

        if (pointsForFollowing.isEmpty()) {
            options.followingFrameOptions.run {
                val cameraState = mapboxMap.cameraState
                followingBearingProperty.fallback = cameraState.bearing
                followingPitchProperty.fallback = defaultPitch
                followingCenterProperty.fallback = cameraState.center
                followingZoomProperty.fallback = max(min(cameraState.zoom, maxZoom), minZoom)
            }
            // nothing to frame
            return
        }

        options.followingFrameOptions.bearingSmoothing.run {
            val locationBearing = localTargetLocation?.bearing?.toDouble() ?: BEARING_NORTH
            followingBearingProperty.fallback =
                getSmootherBearingForMap(
                    enabled,
                    maxBearingAngleDiff,
                    mapboxMap.cameraState.bearing,
                    locationBearing,
                    pointsForFollowing
                )
        }

        options.followingFrameOptions.frameGeometryAfterManeuver.run {
            if (enabled && followingPitchProperty.get() == ZERO_PITCH) {
                pointsForFollowing.addAll(pointsToFrameAfterCurrentStep)
            }
        }

        val cameraFrame =
            if (pointsForFollowing.size > 1 &&
                options.followingFrameOptions.maximizeViewableGeometryWhenPitchZero &&
                followingPitchProperty.get() == ZERO_PITCH
            ) {
                logI(
                    "updateFollowingData; generating for maximal viewable area in pitch 0",
                    LOG_CATEGORY
                )
                mapboxMap.cameraForCoordinates(
                    pointsForFollowing,
                    followingPadding,
                    followingBearingProperty.get(),
                    followingPitchProperty.get()
                )
            } else {
                val mapSize = mapboxMap.getSize()
                val screenBox = getScreenBoxForFraming(mapSize, followingPadding)
                val cameraState = mapboxMap.cameraState
                val padding = getMapAnchoredPaddingFromUserPadding(
                    mapSize,
                    followingPadding,
                    options.followingFrameOptions.focalPoint
                )
                val fallbackCameraOptions = CameraOptions.Builder()
                    .center(
                        pointsForFollowing.firstOrNull() ?: cameraState.center
                    )
                    .padding(padding)
                    .bearing(followingBearingProperty.get())
                    .pitch(followingPitchProperty.get())
                    .zoom(cameraState.zoom)
                    .build()
                logI(
                    "updateFollowingData; fallbackCameraOptions: $fallbackCameraOptions",
                    LOG_CATEGORY
                )
                logI(
                    "updateFollowingData; map size: $mapSize, screenBox=$screenBox, resultingPadding=$padding",
                    LOG_CATEGORY
                )
                if (pointsForFollowing.size > 1) {
                    logI(
                        "updateFollowingData; generating for multiple points",
                        LOG_CATEGORY
                    )
                    mapboxMap.cameraForCoordinates(
                        pointsForFollowing,
                        fallbackCameraOptions,
                        screenBox
                    )
                } else {
                    logI(
                        "updateFollowingData; generating for one point",
                        LOG_CATEGORY
                    )
                    fallbackCameraOptions
                }
            }

        logI(
            "updateFollowingData; generated cameraFrame: $cameraFrame",
            LOG_CATEGORY
        )
        followingCenterProperty.fallback = cameraFrame.center!!
        options.followingFrameOptions.run {
            followingZoomProperty.fallback = max(min(cameraFrame.zoom!!, maxZoom), minZoom)
        }
        appliedFollowingPadding = cameraFrame.padding!!

        updateDebuggerForFollowing(pointsForFollowing)
    }

    private fun updateOverviewData() {
        val pointsForOverview = simplifiedRemainingPointsOnRoute.toMutableList()

        val localTargetLocation = targetLocation
        if (localTargetLocation != null) {
            pointsForOverview.add(0, localTargetLocation.toPoint())
        }

        pointsForOverview.addAll(additionalPointsToFrameForOverview)

        if (pointsForOverview.isEmpty()) {
            options.overviewFrameOptions.run {
                val cameraState = mapboxMap.cameraState
                overviewBearingProperty.fallback = cameraState.bearing
                overviewPitchProperty.fallback = cameraState.pitch
                overviewCenterProperty.fallback = cameraState.center
                overviewZoomProperty.fallback = min(cameraState.zoom, maxZoom)
            }
            // nothing to frame
            return
        }

        overviewBearingProperty.fallback = normalizeBearing(
            mapboxMap.cameraState.bearing,
            BEARING_NORTH
        )

        val cameraFrame = if (pointsForOverview.isNotEmpty()) {
            mapboxMap.cameraForCoordinates(
                pointsForOverview,
                overviewPadding,
                overviewBearingProperty.get(),
                overviewPitchProperty.get()
            )
        } else {
            mapboxMap.cameraState.toCameraOptions()
        }

        overviewCenterProperty.fallback = cameraFrame.center!!
        overviewZoomProperty.fallback = min(
            cameraFrame.zoom!!,
            options.overviewFrameOptions.maxZoom
        )

        updateDebuggerForOverview(pointsForOverview)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun updateDebuggerForFollowing(pointsForFollowing: List<Point>) {
        debugger?.followingPoints = pointsForFollowing
        debugger?.followingUserPadding = followingPadding
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun updateDebuggerForOverview(pointsForOverview: List<Point>) {
        debugger?.overviewPoints = pointsForOverview
        debugger?.overviewUserPadding = overviewPadding
    }
}
