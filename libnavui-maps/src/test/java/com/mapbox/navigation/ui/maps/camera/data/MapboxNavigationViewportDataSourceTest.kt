package com.mapbox.navigation.ui.maps.camera.data

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.common.Logger
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.BEARING_NORTH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.EMPTY_EDGE_INSETS
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.NULL_ISLAND_POINT
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.ZERO_PITCH
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
import com.mapbox.navigation.utils.internal.toPoint
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class MapboxNavigationViewportDataSourceTest {
    private val mapboxMap: MapboxMap = mockk(relaxUnitFun = true)
    private val emptyCameraState = CameraState(
        NULL_ISLAND_POINT,
        EMPTY_EDGE_INSETS,
        0.0,
        BEARING_NORTH,
        ZERO_PITCH
    )
    private val mapSize = Size(1000f, 1000f)
    private val singlePixelEdgeInsets = EdgeInsets(1.0, 2.0, 3.0, 4.0)
    private val followingScreenBox = ScreenBox(
        ScreenCoordinate(1.0, 2.0),
        ScreenCoordinate(3.0, 4.0)
    )
    private val smoothedBearing = 333.0
    private val pitchFromProgress = 45.0
    private val route: DirectionsRoute = mockk(relaxed = true) {
        every { routeIndex() } returns "0"
    }
    private val routeProgress: RouteProgress = mockk()
    private val completeRoutePoints: List<List<List<Point>>> =
        // legs
        listOf(
            // steps
            listOf(
                // points
                listOf(Point.fromLngLat(1.0, 2.0)),
                listOf(Point.fromLngLat(3.0, 4.0)),
                listOf(Point.fromLngLat(5.0, 6.0))
            ),
            listOf(
                // points
                listOf(Point.fromLngLat(7.0, 8.0)),
                listOf(Point.fromLngLat(9.0, 10.0))
            ),
        )
    private val completeRoutePointsFlat: List<Point> = completeRoutePoints.flatten().flatten()
    private val averageIntersectionDistancesOnRoute: List<List<Double>> =
        // legs
        listOf(
            // steps
            listOf(1.0, 2.0, 3.0),
            listOf(4.0, 5.0)
        )

    private val postManeuverFramingPoints: List<List<List<Point>>> =
        // legs
        listOf(
            // steps
            listOf(
                // points
                listOf(Point.fromLngLat(11.0, 12.0)),
                listOf(Point.fromLngLat(13.0, 14.0)),
                listOf(Point.fromLngLat(15.0, 16.0))
            ),
            listOf(
                // points
                listOf(Point.fromLngLat(17.0, 18.0)),
                listOf(Point.fromLngLat(19.0, 20.0))
            ),
        )
    private val pointsToFrameOnCurrentStep: List<Point> = listOf(
        Point.fromLngLat(30.0, 31.0),
        Point.fromLngLat(32.0, 33.0),
    )
    private val pointsToFrameAfterCurrentStep: List<Point> = listOf(
        Point.fromLngLat(40.0, 41.0),
        Point.fromLngLat(42.0, 43.0),
    )
    private val remainingPointsOnRoute: List<Point> = listOf(
        Point.fromLngLat(50.0, 51.0),
        Point.fromLngLat(52.0, 53.0),
    )

    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    @Before
    fun setup() {
        mockkStatic("com.mapbox.navigation.base.route.NavigationRouteEx")
        every { route.toNavigationRoute() } returns mockk {
            every { routeOptions } returns route.routeOptions()!!
            every { directionsRoute } returns route
        }
        every { mapboxMap.cameraState } returns emptyCameraState
        every { mapboxMap.getSize() } returns mapSize

        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)

        mockkObject(ViewportDataSourceProcessor)
        every { getMapAnchoredPaddingFromUserPadding(mapSize, any()) } returns singlePixelEdgeInsets
        every { getScreenBoxForFraming(mapSize, any()) } returns followingScreenBox
        every {
            getSmootherBearingForMap(
                viewportDataSource.options.followingFrameOptions.bearingSmoothing.enabled,
                viewportDataSource.options.followingFrameOptions.bearingSmoothing
                    .maxBearingAngleDiff,
                any(),
                any(),
                any()
            )
        } returns smoothedBearing
        every { processRoutePoints(route) } returns completeRoutePoints
        every {
            processRouteIntersections(
                viewportDataSource.options.followingFrameOptions.intersectionDensityCalculation
                    .enabled,
                viewportDataSource.options.followingFrameOptions.intersectionDensityCalculation
                    .minimumDistanceBetweenIntersections,
                route,
                completeRoutePoints
            )
        } returns averageIntersectionDistancesOnRoute
        every {
            processRouteForPostManeuverFramingGeometry(
                viewportDataSource.options.followingFrameOptions.frameGeometryAfterManeuver.enabled,
                viewportDataSource.options.followingFrameOptions.frameGeometryAfterManeuver
                    .distanceToCoalesceCompoundManeuvers,
                viewportDataSource.options.followingFrameOptions.frameGeometryAfterManeuver
                    .distanceToFrameAfterManeuver,
                route,
                completeRoutePoints
            )
        } returns postManeuverFramingPoints
        every {
            getPitchFallbackFromRouteProgress(
                any(),
                any()
            )
        } returns pitchFromProgress
        every {
            getPointsToFrameOnCurrentStep(
                viewportDataSource.options.followingFrameOptions.intersectionDensityCalculation
                    .enabled,
                viewportDataSource.options.followingFrameOptions.intersectionDensityCalculation
                    .averageDistanceMultiplier,
                averageIntersectionDistancesOnRoute,
                any(),
                any()
            )
        } returns pointsToFrameOnCurrentStep
        every {
            getPointsToFrameAfterCurrentManeuver(
                viewportDataSource.options.followingFrameOptions.frameGeometryAfterManeuver.enabled,
                postManeuverFramingPoints,
                any(),
                any()
            )
        } returns pointsToFrameAfterCurrentStep
        every {
            getRemainingPointsOnRoute(
                completeRoutePoints,
                pointsToFrameOnCurrentStep,
                any(),
                any()
            )
        } returns remainingPointsOnRoute

        every { route.routeOptions() } returns mockk()
    }

    @Test
    fun sanity() {
        assertNotNull(viewportDataSource)
    }

    @Test
    fun `empty source initializes at null island`() {
        val data = viewportDataSource.getViewportData()

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
                zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
                padding(EMPTY_EDGE_INSETS)
            },
            data.cameraForFollowing
        )

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(ZERO_PITCH)
                zoom(viewportDataSource.options.overviewFrameOptions.maxZoom)
                padding(EMPTY_EDGE_INSETS)
            },
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame - just location`() {
        val location = createLocation()
        val expectedPoints = listOf(location.toPoint())

        // following mocks
        val followingCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(viewportDataSource.options.followingFrameOptions.minZoom)
            padding(singlePixelEdgeInsets)
        }

        // overview mocks
        val overviewCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(0.0)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame - just additional points`() {
        val additionalPoint = Point.fromLngLat(-30.0, -40.0)
        val expectedPoints = listOf(additionalPoint)

        // following mocks
        val followingCameraOptions = createCameraOptions {
            center(additionalPoint)
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(viewportDataSource.options.followingFrameOptions.minZoom)
            padding(singlePixelEdgeInsets)
        }

        // overview mocks
        val overviewZoom = viewportDataSource.options.overviewFrameOptions.maxZoom
        val overviewCameraOptions = createCameraOptions {
            center(additionalPoint)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.additionalPointsToFrameForOverview(listOf(additionalPoint))
        viewportDataSource.additionalPointsToFrameForFollowing(listOf(additionalPoint))
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame - just routeProgress`() {
        mockkStatic(Logger::class)
        every { Logger.w(any(), any()) } just Runs
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
                zoom(viewportDataSource.options.followingFrameOptions.minZoom)
                padding(EMPTY_EDGE_INSETS)
                anchor(null)
            },
            data.cameraForFollowing
        )

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(ZERO_PITCH)
                zoom(0.0)
                padding(EMPTY_EDGE_INSETS)
                anchor(null)
            },
            data.cameraForOverview
        )
        unmockkStatic(Logger::class)
    }

    @Test
    fun `verify frame - just route`() {
        // overview mocks
        val overviewCenter = Point.fromLngLat(5.0, 6.0)
        val overviewZoom = 10.0
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        every {
            mapboxMap.cameraForCoordinates(
                completeRoutePointsFlat,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        viewportDataSource.onRouteChanged(route)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
                zoom(viewportDataSource.options.followingFrameOptions.minZoom)
                padding(EMPTY_EDGE_INSETS)
                anchor(null)
            },
            data.cameraForFollowing
        )

        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame - location + additional points`() {
        val location = createLocation()
        val additionalPoint = Point.fromLngLat(-30.0, -40.0)
        val expectedPoints = listOf(location.toPoint(), additionalPoint)

        // following mocks
        val fallbackOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(mapboxMap.cameraState.zoom)
            padding(singlePixelEdgeInsets)
        }
        val followingZoom = 16.0
        val followingCameraOptions = fallbackOptions.toBuilder()
            .zoom(followingZoom)
            .build()
        every {
            mapboxMap.cameraForCoordinates(
                expectedPoints,
                fallbackOptions,
                followingScreenBox
            )
        } returns followingCameraOptions

        // overview mocks
        val overviewCenter = Point.fromLngLat(5.0, 6.0)
        val overviewZoom = 10.0
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.additionalPointsToFrameForOverview(listOf(additionalPoint))
        viewportDataSource.additionalPointsToFrameForFollowing(listOf(additionalPoint))
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame - location + route + progress`() {
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // following mocks
        val expectedFollowingPoints = mutableListOf(location.toPoint()).apply {
            addAll(pointsToFrameOnCurrentStep)
        }
        val fallbackOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(mapboxMap.cameraState.zoom)
            padding(singlePixelEdgeInsets)
        }
        val followingZoom = 16.0
        val followingCameraOptions = fallbackOptions.toBuilder()
            .zoom(followingZoom)
            .build()
        every {
            mapboxMap.cameraForCoordinates(
                expectedFollowingPoints,
                fallbackOptions,
                followingScreenBox
            )
        } returns followingCameraOptions

        // overview mocks
        val expectedOverviewPoints = mutableListOf(location.toPoint()).apply {
            addAll(remainingPointsOnRoute)
        }
        val overviewCenter = Point.fromLngLat(5.0, 6.0)
        val overviewZoom = 10.0
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedOverviewPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify state is not cleared if the same route is provided twice`() {
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // following mocks
        val expectedFollowingPoints = mutableListOf(location.toPoint()).apply {
            addAll(pointsToFrameOnCurrentStep)
        }
        val fallbackOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(mapboxMap.cameraState.zoom)
            padding(singlePixelEdgeInsets)
        }
        val followingZoom = 16.0
        val followingCameraOptions = fallbackOptions.toBuilder()
            .zoom(followingZoom)
            .build()
        every {
            mapboxMap.cameraForCoordinates(
                expectedFollowingPoints,
                fallbackOptions,
                followingScreenBox
            )
        } returns followingCameraOptions

        // overview mocks
        val expectedOverviewPoints = mutableListOf(location.toPoint()).apply {
            addAll(remainingPointsOnRoute)
        }
        val overviewCenter = Point.fromLngLat(5.0, 6.0)
        val overviewZoom = 10.0
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedOverviewPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame - location + route + progress + pitch 0`() {
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // following mocks
        val expectedFollowingPoints = mutableListOf(location.toPoint()).apply {
            addAll(pointsToFrameOnCurrentStep)
            addAll(pointsToFrameAfterCurrentStep)
        }
        val followingZoom = 16.0
        val followingCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(ZERO_PITCH)
            zoom(followingZoom)
            padding(EMPTY_EDGE_INSETS)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedFollowingPoints,
                EMPTY_EDGE_INSETS,
                smoothedBearing,
                ZERO_PITCH
            )
        } returns followingCameraOptions

        // overview mocks
        val expectedOverviewPoints = mutableListOf(location.toPoint()).apply {
            addAll(remainingPointsOnRoute)
        }
        val overviewCenter = Point.fromLngLat(5.0, 6.0)
        val overviewZoom = 10.0
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedOverviewPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.followingPitchPropertyOverride(ZERO_PITCH)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame - location + route + progress + pitch 0 + no view maximization`() {
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // following mocks
        val expectedFollowingPoints = mutableListOf(location.toPoint()).apply {
            addAll(pointsToFrameOnCurrentStep)
            addAll(pointsToFrameAfterCurrentStep)
        }
        val fallbackOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(ZERO_PITCH)
            zoom(mapboxMap.cameraState.zoom)
            padding(singlePixelEdgeInsets)
        }
        val followingZoom = 16.0
        val followingCameraOptions = fallbackOptions.toBuilder()
            .zoom(followingZoom)
            .build()
        every {
            mapboxMap.cameraForCoordinates(
                expectedFollowingPoints,
                fallbackOptions,
                followingScreenBox
            )
        } returns followingCameraOptions

        // overview mocks
        val expectedOverviewPoints = mutableListOf(location.toPoint()).apply {
            addAll(remainingPointsOnRoute)
        }
        val overviewCenter = Point.fromLngLat(5.0, 6.0)
        val overviewZoom = 10.0
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedOverviewPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.options.followingFrameOptions
            .maximizeViewableGeometryWhenPitchZero = false
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.followingPitchPropertyOverride(ZERO_PITCH)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame after route data cleanup`() {
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // following mocks
        val followingCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(viewportDataSource.options.followingFrameOptions.minZoom)
            padding(singlePixelEdgeInsets)
        }
        val pointsForInitialFollowingFrame = mutableListOf<Point>().apply {
            add(location.toPoint())
            addAll(pointsToFrameOnCurrentStep)
        }
        every {
            mapboxMap.cameraForCoordinates(
                pointsForInitialFollowingFrame,
                any<CameraOptions>(),
                any()
            )
        } returns mockk(relaxed = true)

        // overview mocks
        val pointsForInitialOverviewFrame = mutableListOf<Point>().apply {
            add(location.toPoint())
            addAll(remainingPointsOnRoute)
        }
        every {
            mapboxMap.cameraForCoordinates(
                pointsForInitialOverviewFrame,
                any(),
                any(),
                any()
            )
        } returns mockk(relaxed = true)
        val expectedPoints = listOf(location.toPoint())
        val overviewCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(0.0)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        viewportDataSource.clearRouteData()
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame after route data cleanup when in pitch 0`() {
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // following mocks
        val pointsForInitialFollowingFrame = mutableListOf<Point>().apply {
            add(location.toPoint())
            addAll(pointsToFrameOnCurrentStep)
            addAll(pointsToFrameAfterCurrentStep)
        }
        every {
            mapboxMap.cameraForCoordinates(
                pointsForInitialFollowingFrame,
                any(),
                any(),
                any()
            )
        } returns mockk(relaxed = true)
        val followingCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(ZERO_PITCH)
            zoom(viewportDataSource.options.followingFrameOptions.minZoom)
            padding(singlePixelEdgeInsets)
        }

        // overview mocks
        val pointsForInitialOverviewFrame = mutableListOf<Point>().apply {
            add(location.toPoint())
            addAll(remainingPointsOnRoute)
        }
        every {
            mapboxMap.cameraForCoordinates(
                pointsForInitialOverviewFrame,
                any(),
                any(),
                any()
            )
        } returns mockk(relaxed = true)
        val expectedPoints = listOf(location.toPoint())
        val overviewCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(0.0)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.followingPitchPropertyOverride(ZERO_PITCH)
        viewportDataSource.evaluate()
        viewportDataSource.clearRouteData()
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame - location + route + progress + padding`() {
        val followingPadding = EdgeInsets(11.0, 12.0, 13.0, 14.0)
        val overviewPadding = EdgeInsets(15.0, 16.0, 17.0, 18.0)
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // following mocks
        val expectedFollowingPoints = mutableListOf(location.toPoint()).apply {
            addAll(pointsToFrameOnCurrentStep)
        }
        val fallbackOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(mapboxMap.cameraState.zoom)
            padding(singlePixelEdgeInsets)
        }
        val followingZoom = 16.0
        val followingCameraOptions = fallbackOptions.toBuilder()
            .zoom(followingZoom)
            .build()
        every {
            mapboxMap.cameraForCoordinates(
                expectedFollowingPoints,
                fallbackOptions,
                followingScreenBox
            )
        } returns followingCameraOptions

        // overview mocks
        val expectedOverviewPoints = mutableListOf(location.toPoint()).apply {
            addAll(remainingPointsOnRoute)
        }
        val overviewCenter = Point.fromLngLat(5.0, 6.0)
        val overviewZoom = 10.0
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
            padding(overviewPadding)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedOverviewPoints,
                overviewPadding,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.followingPadding = followingPadding
        viewportDataSource.overviewPadding = overviewPadding
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
        verify { getScreenBoxForFraming(mapSize, followingPadding) }
    }

    @Test
    fun `verify frame - location + route + progress + reset route`() {
        every {
            getPitchFallbackFromRouteProgress(any(), any())
        } returns ZERO_PITCH

        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // following mocks
        val followingCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(viewportDataSource.options.followingFrameOptions.minZoom)
            padding(singlePixelEdgeInsets)
        }

        // overview mocks
        val overviewCenter = Point.fromLngLat(5.0, 6.0)
        val overviewZoom = 10.0
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        val expectedOverviewFramingPoints = mutableListOf(location.toPoint()).apply {
            addAll(completeRoutePointsFlat)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedOverviewFramingPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        mockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        every { route.isSameRoute(any()) } returns false
        viewportDataSource.onRouteChanged(route)
        unmockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @Test
    fun `verify frame - location + route + progress + reset route and forget to update`() {
        mockkStatic(Logger::class)
        every { Logger.e(any(), any()) } just Runs
        every {
            getPitchFallbackFromRouteProgress(any(), any())
        } returns ZERO_PITCH

        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // following mocks
        val followingCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(viewportDataSource.options.followingFrameOptions.minZoom)
            padding(singlePixelEdgeInsets)
        }

        // overview mocks
        val overviewCenter = Point.fromLngLat(5.0, 6.0)
        val overviewZoom = 10.0
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(BEARING_NORTH)
            pitch(ZERO_PITCH)
            zoom(overviewZoom)
        }
        val expectedOverviewFramingPoints = mutableListOf(location.toPoint()).apply {
            addAll(completeRoutePointsFlat)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedOverviewFramingPoints,
                EMPTY_EDGE_INSETS,
                BEARING_NORTH,
                ZERO_PITCH
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(route)
        viewportDataSource.onRouteProgressChanged(routeProgress)

        mockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        every { route.isSameRoute(any()) } returns false
        viewportDataSource.onRouteProgressChanged(routeProgress)
        unmockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")

        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
        unmockkStatic(Logger::class)
    }

    @Test
    fun `verify frame - overrides`() {
        val location = createLocation()
        val expectedPoints = listOf(location.toPoint())
        val followingCenter = Point.fromLngLat(1.0, 2.0)
        val followingZoom = 2.0
        val followingBearing = 3.0
        val followingPitch = 4.0
        val overviewCenter = Point.fromLngLat(3.0, 4.0)
        val overviewZoom = 5.0
        val overviewBearing = 6.0
        val overviewPitch = 7.0

        // following mocks
        val followingCameraOptions = createCameraOptions {
            center(followingCenter)
            bearing(followingBearing)
            pitch(followingPitch)
            zoom(followingZoom)
            padding(singlePixelEdgeInsets)
        }

        // overview mocks
        val overviewCameraOptions = createCameraOptions {
            center(overviewCenter)
            bearing(overviewBearing)
            pitch(overviewPitch)
            zoom(overviewZoom)
        }
        every {
            mapboxMap.cameraForCoordinates(
                expectedPoints,
                EMPTY_EDGE_INSETS,
                overviewBearing,
                overviewPitch
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.followingCenterPropertyOverride(followingCenter)
        viewportDataSource.followingZoomPropertyOverride(followingZoom)
        viewportDataSource.followingBearingPropertyOverride(followingBearing)
        viewportDataSource.followingPitchPropertyOverride(followingPitch)
        viewportDataSource.overviewCenterPropertyOverride(overviewCenter)
        viewportDataSource.overviewZoomPropertyOverride(overviewZoom)
        viewportDataSource.overviewBearingPropertyOverride(overviewBearing)
        viewportDataSource.overviewPitchPropertyOverride(overviewPitch)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview
        )
    }

    @After
    fun tearDown() {
        unmockkObject(ViewportDataSourceProcessor)
        unmockkStatic("com.mapbox.navigation.base.route.NavigationRouteEx")
    }

    private fun createLocation(
        lat: Double = 10.0,
        lng: Double = 20.0,
        bear: Double = 30.0
    ) = mockk<Location> {
        every { latitude } returns lat
        every { longitude } returns lng
        every { bearing } returns bear.toFloat()
    }

    private fun createCameraOptions(block: CameraOptions.Builder.() -> Unit): CameraOptions {
        return CameraOptions.Builder()
            .zoom(emptyCameraState.zoom)
            .bearing(emptyCameraState.bearing)
            .padding(emptyCameraState.padding)
            .center(emptyCameraState.center)
            .pitch(emptyCameraState.pitch)
            .apply(block)
            .build()
    }
}
