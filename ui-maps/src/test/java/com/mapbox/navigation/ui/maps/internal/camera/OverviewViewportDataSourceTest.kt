package com.mapbox.navigation.ui.maps.internal.camera

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.toCameraOptions
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.utils.DecodeUtils
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.BEARING_NORTH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.EMPTY_EDGE_INSETS
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.NULL_ISLAND_POINT
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.ZERO_PITCH
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getRemainingPointsOnRoute
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.simplifyCompleteRoutePoints
import com.mapbox.navigation.utils.internal.toPoint
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OverviewViewportDataSourceTest {

    private val mapboxMap: MapboxMap = mockk(relaxed = true)
    private val routeProgress: RouteProgress = mockk(relaxed = true)
    private val indicesConverter = mockk<RoutesIndicesConverter>(relaxed = true)

    private val emptyCameraState = CameraState(
        NULL_ISLAND_POINT,
        EMPTY_EDGE_INSETS,
        0.0,
        BEARING_NORTH,
        ZERO_PITCH,
    )

    private val currentPrimaryStep = mockk<LegStep>(relaxed = true)
    private val remainingPointsOnStepPrimary: List<Point> = listOf(
        Point.fromLngLat(30.0, 31.0),
        Point.fromLngLat(32.0, 33.0),
    )

    private val currentAlternativeStep = mockk<LegStep>(relaxed = true)
    private val remainingPointsOnStepAlternative: List<Point> = listOf(
        Point.fromLngLat(40.0, 41.0),
        Point.fromLngLat(42.0, 43.0),
    )

    private val route: DirectionsRoute = mockk(relaxed = true) {
        every { routeIndex() } returns "0"
        every { routeOptions() } returns mockk()
    }
    private val route2: DirectionsRoute = mockk(relaxed = true) {
        every { routeIndex() } returns "0"
        every { routeOptions() } returns mockk()
    }

    private val navigationRouteId = "routeId"
    private val alternativeRouteId = "routeId2"
    private val navigationRoute: NavigationRoute = mockk(relaxed = true) {
        every { id } returns navigationRouteId
        every { routeOptions } returns route.routeOptions()!!
        every { directionsRoute } returns route
    }
    private val navigationRoute2: NavigationRoute = mockk(relaxed = true) {
        every { routeOptions } returns route2.routeOptions()!!
        every { directionsRoute } returns route2
        every { id } returns alternativeRouteId
    }

    private val completeRoutePoints: List<List<List<Point>>> =
        // legs
        listOf(
            // steps
            listOf(
                // points
                listOf(Point.fromLngLat(1.0, 2.0)),
                listOf(Point.fromLngLat(3.0, 4.0)),
                listOf(Point.fromLngLat(5.0, 6.0)),
            ),
            listOf(
                // points
                listOf(Point.fromLngLat(7.0, 8.0)),
                listOf(Point.fromLngLat(9.0, 10.0)),
            ),
        )

    private val completeRoutePoints2: List<List<List<Point>>> =
        // legs
        listOf(
            // steps
            listOf(
                // points
                listOf(Point.fromLngLat(10.0, 2.0)),
                listOf(Point.fromLngLat(30.0, 4.0)),
                listOf(Point.fromLngLat(50.0, 6.0)),
            ),
            listOf(
                // points
                listOf(Point.fromLngLat(70.0, 8.0)),
                listOf(Point.fromLngLat(90.0, 10.0)),
            ),
        )

    private val completeRoutePointsFlat: List<Point> = completeRoutePoints.flatten().flatten()

    private val remainingPointsOnRoute: List<Point> = listOf(
        Point.fromLngLat(50.0, 51.0),
        Point.fromLngLat(52.0, 53.0),
    )

    private val remainingPointsOnRoute2: List<Point> = listOf(
        Point.fromLngLat(55.0, 51.0),
        Point.fromLngLat(57.0, 53.0),
    )

    private val viewportDataSource = OverviewViewportDataSource(
        mapboxMap,
        InternalViewportDataSourceOptions(
            ignoreMinZoomWhenFramingManeuver = false,
            overviewMode = OverviewMode.ACTIVE_LEG,
            overviewAlternatives = false,
        ),
        indicesConverter,
    )

    private val viewportDataSourceWithAlternatives = OverviewViewportDataSource(
        mapboxMap,
        InternalViewportDataSourceOptions(
            ignoreMinZoomWhenFramingManeuver = false,
            overviewMode = OverviewMode.ACTIVE_LEG,
            overviewAlternatives = true,
        ),
        indicesConverter,
    )

    private val primaryLegIndex = 1
    private val primaryStepIndex = 14
    private val primaryLegGeometryIndex = 46
    private val primaryStepGeometryIndex = 4
    private val alternativeLegIndex = 2
    private val alternativeStepIndex = 18
    private val alternativeLegGeometryIndex = 52
    private val alternativeStepGeometryIndex = 3

    @Before
    fun setUp() {
        mockkObject(ViewportDataSourceProcessor)
        mockkStatic(DecodeUtils::class)
        every {
            route.legs()
        } returns List(primaryLegIndex) { mockk<RouteLeg>(relaxed = true) } + listOf(
            mockk(relaxed = true) {
                every {
                    steps()
                } returns List(primaryStepIndex) { mockk<LegStep>(relaxed = true) } + listOf(
                    currentPrimaryStep,
                )
            },
        )

        every {
            route2.legs()
        } returns List(alternativeLegIndex) { mockk<RouteLeg>(relaxed = true) } +
            listOf(
                mockk(relaxed = true) {
                    every {
                        steps()
                    } returns List(alternativeStepIndex) { mockk<LegStep>(relaxed = true) } +
                        listOf(currentAlternativeStep)
                },
            )

        every {
            route.stepGeometryToPoints(currentPrimaryStep)
        } returns List(primaryStepGeometryIndex) { mockk<Point>() } + remainingPointsOnStepPrimary
        every {
            route2.stepGeometryToPoints(currentAlternativeStep)
        } returns List(alternativeStepGeometryIndex) { mockk<Point>() } +
            remainingPointsOnStepAlternative
        every {
            routeProgress.internalAlternativeRouteIndices()
        } returns mapOf(
            navigationRoute2.id to mockk {
                every { legIndex } returns alternativeLegIndex
                every { stepIndex } returns alternativeStepIndex
                every { legGeometryIndex } returns alternativeLegGeometryIndex
            },
        )
        every { routeProgress.currentLegProgress } returns mockk(relaxed = true) {
            every { legIndex } returns primaryLegIndex
            every { currentStepProgress } returns mockk(relaxed = true) {
                every { stepIndex } returns primaryStepIndex
            }
            every { geometryIndex } returns primaryLegGeometryIndex
        }
        every {
            indicesConverter.convert(
                navigationRouteId,
                primaryLegIndex,
                primaryStepIndex,
                primaryLegGeometryIndex,
            )
        } returns primaryStepGeometryIndex
        every {
            indicesConverter.convert(
                alternativeRouteId,
                alternativeLegIndex,
                alternativeStepIndex,
                alternativeLegGeometryIndex,
            )
        } returns alternativeStepGeometryIndex

        every { processRoutePoints(route) } returns completeRoutePoints
        every { processRoutePoints(route2) } returns completeRoutePoints2
        every {
            getRemainingPointsOnRoute(
                completeRoutePoints,
                any(),
                any(),
                any(),
                any(),
            )
        } returns remainingPointsOnRoute
        every {
            getRemainingPointsOnRoute(
                completeRoutePoints2,
                any(),
                any(),
                any(),
                any(),
            )
        } returns remainingPointsOnRoute2
        every { mapboxMap.cameraState } returns emptyCameraState
    }

    @After
    fun tearDown() {
        unmockkObject(ViewportDataSourceProcessor)
        unmockkStatic(DecodeUtils::class)
    }

    @Test
    fun `onRoutesChanged passes routes to indices converter, alternatives disabled`() {
        viewportDataSource.onRoutesChanged(listOf(navigationRoute, navigationRoute2))

        verify { indicesConverter.onRoutesChanged(listOf(navigationRoute)) }

        clearMocks(indicesConverter, answers = false)

        viewportDataSource.clearRouteData()

        verify { indicesConverter.onRoutesChanged(emptyList()) }
    }

    @Test
    fun `onRoutesChanged passes routes to indices converter, alternatives enabled`() {
        viewportDataSourceWithAlternatives.onRoutesChanged(
            listOf(
                navigationRoute,
                navigationRoute2,
            ),
        )

        verify { indicesConverter.onRoutesChanged(listOf(navigationRoute, navigationRoute2)) }

        clearMocks(indicesConverter, answers = false)

        viewportDataSourceWithAlternatives.clearRouteData()

        verify { indicesConverter.onRoutesChanged(emptyList()) }
    }

    @Test
    fun `empty source initializes at null island`() {
        val data = viewportDataSource.viewportData

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(ZERO_PITCH)
                zoom(viewportDataSource.options.overviewFrameOptions.maxZoom)
                padding(EMPTY_EDGE_INSETS)
            },
            data,
        )
    }

    @Test
    fun `verify frame - just location`() {
        val location = createLocation()
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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame - just additional points`() {
        val additionalPoint = Point.fromLngLat(-30.0, -40.0)
        val expectedPoints = listOf(additionalPoint)

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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.additionalPointsToFrame(listOf(additionalPoint))
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame - just routeProgress`() {
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(ZERO_PITCH)
                zoom(0.0)
                padding(EMPTY_EDGE_INSETS)
                anchor(null)
            },
            data,
        )
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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame - location + additional points`() {
        val location = createLocation()
        val additionalPoint = Point.fromLngLat(-30.0, -40.0)
        val expectedPoints = listOf(location.toPoint(), additionalPoint)

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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.additionalPointsToFrame(listOf(additionalPoint))
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        // verify
        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame - location + route + progress`() {
        val location = createLocation()

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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        // verify
        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame - overview current leg only`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        // overview mocks

        val remainingPointsOnRouteForCurrentLeg = listOf(
            Point.fromLngLat(50.5, 51.5),
            Point.fromLngLat(52.5, 53.5),
        )

        every {
            getRemainingPointsOnRoute(
                any(),
                any(),
                OverviewMode.ACTIVE_LEG,
                any(),
                any(),
            )
        } returns remainingPointsOnRouteForCurrentLeg

        val expectedOverviewPoints = mutableListOf(location.toPoint()).apply {
            addAll(remainingPointsOnRouteForCurrentLeg)
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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        // verify
        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame after route data cleanup`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

        val pointsForInitialOverviewFrame = mutableListOf<Point>().apply {
            add(location.toPoint())
            addAll(remainingPointsOnRoute)
        }
        every {
            mapboxMap.cameraForCoordinates(
                pointsForInitialOverviewFrame,
                any(),
                null,
                null,
                null,
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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        viewportDataSource.clearRouteData()
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        // verify
        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame - location + route + progress + padding`() {
        val overviewPadding = EdgeInsets(15.0, 16.0, 17.0, 18.0)
        val location = createLocation()

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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == overviewPadding
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.padding = overviewPadding
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        // verify
        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame - location + route + progress + reset route`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        mockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        every { route.isSameRoute(any()) } returns false
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        unmockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        // verify
        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame - location + route + progress + reset route and forget to update`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        val location = createLocation()

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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == BEARING_NORTH &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)

        mockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        every { route.isSameRoute(any()) } returns false
        viewportDataSource.onRouteProgressChanged(routeProgress)
        unmockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")

        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        // verify
        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun `verify frame - overrides`() {
        val location = createLocation()
        val expectedPoints = listOf(location.toPoint())
        val overviewCenter = Point.fromLngLat(3.0, 4.0)
        val overviewZoom = 5.0
        val overviewBearing = 6.0
        val overviewPitch = 7.0

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
                match<CameraOptions> {
                    it.pitch == overviewPitch &&
                        it.bearing == overviewBearing &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns overviewCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.centerPropertyOverride(overviewCenter)
        viewportDataSource.zoomPropertyOverride(overviewZoom)
        viewportDataSource.bearingPropertyOverride(overviewBearing)
        viewportDataSource.pitchPropertyOverride(overviewPitch)
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        // verify
        assertEquals(
            overviewCameraOptions,
            data,
        )
    }

    @Test
    fun activeByDefault() {
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        verify {
            processRoutePoints(route)
        }
        verify {
            getRemainingPointsOnRoute(any(), remainingPointsOnStepPrimary, any(), any(), any())
        }
        verify {
            mapboxMap.cameraForCoordinates(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun inactiveThenActive() {
        every { mapboxMap.cameraState } returns mockk(relaxed = true) {
            every { bearing } returns 12.0
        }
        val location = createLocation(20.0, 10.0)
        val point = Point.fromLngLat(10.0, 20.0)
        val expected = createCameraOptions {
            center(Point.fromLngLat(1.0, 2.0))
            bearing(0.0)
            pitch(0.0)
            zoom(14.2)
        }
        every {
            mapboxMap.cameraForCoordinates(
                any(),
                any(),
                null,
                null,
                null,
            )
        } returns createCameraOptions {
            center(Point.fromLngLat(1.0, 2.0))
            zoom(14.2)
        }
        viewportDataSource.setActive(false)

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        verify(exactly = 0) {
            processRoutePoints(any())
        }
        verify(exactly = 0) {
            getRemainingPointsOnRoute(any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            mapboxMap.cameraForCoordinates(any(), any(), any(), any(), any())
        }

        viewportDataSource.setActive(true)

        verify {
            processRoutePoints(route)
        }
        verify {
            indicesConverter.convert(
                navigationRouteId,
                primaryLegIndex,
                primaryStepIndex,
                primaryLegGeometryIndex,
            )
        }
        verify {
            getRemainingPointsOnRoute(any(), remainingPointsOnStepPrimary, any(), any(), any())
        }
        verify {
            mapboxMap.cameraForCoordinates(
                listOf(point) + remainingPointsOnRoute,
                any(),
                any(),
                any(),
                any(),
            )
        }

        assertEquals(expected, viewportDataSource.viewportData)
    }

    @Test
    fun inactiveThenActiveNoData() {
        val location = createLocation(20.0, 10.0)
        val point = Point.fromLngLat(10.0, 20.0)
        viewportDataSource.setActive(false)

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        viewportDataSource.clearRouteData()
        viewportDataSource.clearProgressData()

        viewportDataSource.setActive(true)

        verify(exactly = 0) {
            processRoutePoints(route)
        }
        verify(exactly = 0) {
            getRemainingPointsOnRoute(any(), remainingPointsOnStepPrimary, any(), any(), any())
        }
        verify {
            mapboxMap.cameraForCoordinates(
                listOf(point),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun active() {
        viewportDataSource.setActive(false)

        viewportDataSource.setActive(true)

        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        verify {
            processRoutePoints(route)
        }
        verify {
            getRemainingPointsOnRoute(any(), remainingPointsOnStepPrimary, any(), any(), any())
        }
        verify {
            mapboxMap.cameraForCoordinates(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun changeOverviewModeReevaluates() {
        val location = createLocation(20.0, 10.0)

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        clearMocks(ViewportDataSourceProcessor, mapboxMap, answers = false)

        viewportDataSource.internalOptions = viewportDataSource.internalOptions.copy(
            overviewMode = OverviewMode.ENTIRE_ROUTE,
        )

        verify {
            processRoutePoints(route)
        }
        verify {
            getRemainingPointsOnRoute(any(), any(), OverviewMode.ENTIRE_ROUTE, any(), any())
        }
        verify {
            mapboxMap.cameraForCoordinates(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun changeOverviewModeToSameValueDoesNotReevaluate() {
        val location = createLocation(20.0, 10.0)

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        clearMocks(ViewportDataSourceProcessor, mapboxMap, answers = false)

        viewportDataSource.internalOptions = viewportDataSource.internalOptions.copy(
            overviewMode = OverviewMode.ACTIVE_LEG,
        )

        verify(exactly = 0) {
            processRoutePoints(route)
        }
        verify(exactly = 0) {
            getRemainingPointsOnRoute(any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            mapboxMap.cameraForCoordinates(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun clearRoutesDataClearsData() {
        val currentCameraState = CameraState(
            Point.fromLngLat(32.4, 56.7),
            EdgeInsets(1.0, 1.0, 2.0, 2.0),
            15.0,
            33.0,
            55.0,
        )

        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        every { mapboxMap.cameraState } returns currentCameraState

        viewportDataSource.clearRouteData()
        viewportDataSource.evaluate()

        val options = viewportDataSource.viewportData
        assertEquals(currentCameraState.center, options.center)
    }

    @Test
    fun onRoutesChangedEmptyClearsViewport() {
        val currentCameraState = CameraState(
            Point.fromLngLat(32.4, 56.7),
            EdgeInsets(1.0, 1.0, 2.0, 2.0),
            15.0,
            33.0,
            55.0,
        )

        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        every { mapboxMap.cameraState } returns currentCameraState

        viewportDataSource.onRoutesChanged(emptyList())
        viewportDataSource.evaluate()

        val options = viewportDataSource.viewportData
        assertEquals(currentCameraState.toCameraOptions().center, options.center)
    }

    @Test
    fun onRoutesChangedWithOverviewAlternativesEnabled() {
        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(1.0, 2.0))
            .zoom(16.0)
            .build()

        every {
            mapboxMap.cameraForCoordinates(
                remainingPointsOnRoute + remainingPointsOnRoute2,
                any(),
                any(),
                any(),
                any(),
            )
        } returns cameraOptions

        viewportDataSourceWithAlternatives.onRoutesChanged(
            listOf(navigationRoute, navigationRoute2),
        )

        verify { processRoutePoints(route) }
        verify { processRoutePoints(route2) }

        viewportDataSourceWithAlternatives.onRouteProgressChanged(
            routeProgress,
        )

        verify {
            getRemainingPointsOnRoute(
                completeRoutePoints,
                any(),
                any(),
                primaryLegIndex,
                primaryStepIndex,
            )
        }
        verify {
            getRemainingPointsOnRoute(
                completeRoutePoints2,
                any(),
                any(),
                alternativeLegIndex,
                alternativeStepIndex,
            )
        }

        viewportDataSourceWithAlternatives.evaluate()

        verify {
            mapboxMap.cameraForCoordinates(
                remainingPointsOnRoute + remainingPointsOnRoute2,
                any(),
                any(),
                any(),
                any(),
            )
        }

        val options = viewportDataSourceWithAlternatives.viewportData
        assertEquals(cameraOptions.center, options.center)
    }

    @Test
    fun onRoutesChangedWithOverviewAlternativesEnabledNoAlternativeId() {
        every {
            routeProgress.internalAlternativeRouteIndices()
        } returns mapOf("routeId3" to mockk(relaxed = true))

        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(1.0, 2.0))
            .zoom(16.0)
            .build()

        every {
            mapboxMap.cameraForCoordinates(
                remainingPointsOnRoute,
                any(),
                any(),
                any(),
                any(),
            )
        } returns cameraOptions

        viewportDataSourceWithAlternatives.onRoutesChanged(
            listOf(navigationRoute, navigationRoute2),
        )

        verify { processRoutePoints(route) }
        verify { processRoutePoints(route2) }

        viewportDataSourceWithAlternatives.onRouteProgressChanged(
            routeProgress,
        )

        verify {
            getRemainingPointsOnRoute(
                completeRoutePoints,
                any(),
                any(),
                primaryLegIndex,
                primaryStepIndex,
            )
        }
        verify(exactly = 1) { getRemainingPointsOnRoute(any(), any(), any(), any(), any()) }

        viewportDataSourceWithAlternatives.evaluate()

        verify {
            mapboxMap.cameraForCoordinates(
                remainingPointsOnRoute,
                any(),
                any(),
                any(),
                any(),
            )
        }

        val options = viewportDataSourceWithAlternatives.viewportData
        assertEquals(cameraOptions.center, options.center)
    }

    @Test
    fun onRoutesChangedWithOverviewAlternativesDisabled() {
        every {
            routeProgress.internalAlternativeRouteIndices()
        } returns mapOf("routeId2" to mockk(relaxed = true))

        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(1.0, 2.0))
            .zoom(16.0)
            .build()

        every {
            mapboxMap.cameraForCoordinates(
                remainingPointsOnRoute,
                any(),
                any(),
                any(),
                any(),
            )
        } returns cameraOptions

        viewportDataSource.onRoutesChanged(listOf(navigationRoute, navigationRoute2))

        verify { processRoutePoints(route) }
        verify(exactly = 1) { processRoutePoints(any()) }

        viewportDataSource.onRouteProgressChanged(routeProgress)

        verify { getRemainingPointsOnRoute(completeRoutePoints, any(), any(), any(), any()) }
        verify(exactly = 1) { getRemainingPointsOnRoute(any(), any(), any(), any(), any()) }

        viewportDataSource.evaluate()

        verify {
            mapboxMap.cameraForCoordinates(
                remainingPointsOnRoute,
                any(),
                any(),
                any(),
                any(),
            )
        }

        val options = viewportDataSource.viewportData
        assertEquals(cameraOptions.center, options.center)
    }

    @Test
    fun overviewAlternativesEnabledInfluencesViewportImmediately() {
        val oldCameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(1.0, 2.0))
            .zoom(16.0)
            .build()
        val newCameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(3.0, 4.0))
            .zoom(16.0)
            .build()
        every {
            mapboxMap.cameraForCoordinates(
                remainingPointsOnRoute,
                any(),
                any(),
                any(),
                any(),
            )
        } returns oldCameraOptions
        every {
            mapboxMap.cameraForCoordinates(
                remainingPointsOnRoute + remainingPointsOnRoute2,
                any(),
                any(),
                any(),
                any(),
            )
        } returns newCameraOptions
        viewportDataSource.onRoutesChanged(listOf(navigationRoute, navigationRoute2))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val options = viewportDataSource.viewportData
        assertEquals(oldCameraOptions.center, options.center)

        viewportDataSource.internalOptions = viewportDataSource.internalOptions
            .copy(overviewAlternatives = true)

        val newOptions = viewportDataSource.viewportData
        assertEquals(newCameraOptions.center, newOptions.center)
    }

    @Test
    fun setActiveToTrueWithAlternatives() {
        viewportDataSourceWithAlternatives.setActive(false)

        viewportDataSourceWithAlternatives.onRoutesChanged(
            listOf(navigationRoute, navigationRoute2),
        )
        viewportDataSourceWithAlternatives.onRouteProgressChanged(
            routeProgress,
        )
        viewportDataSourceWithAlternatives.evaluate()

        clearAllMocks(answers = false)

        viewportDataSourceWithAlternatives.setActive(true)

        verify { processRoutePoints(route) }
        verify { processRoutePoints(route2) }
        verify { simplifyCompleteRoutePoints(any(), any(), completeRoutePoints) }
        verify { simplifyCompleteRoutePoints(any(), any(), completeRoutePoints2) }
        verify {
            getRemainingPointsOnRoute(
                completeRoutePoints,
                remainingPointsOnStepPrimary,
                any(),
                any(),
                any(),
            )
        }
        verify {
            getRemainingPointsOnRoute(
                completeRoutePoints2,
                remainingPointsOnStepAlternative,
                any(),
                any(),
                any(),
            )
        }
        verify {
            mapboxMap.cameraForCoordinates(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `onRouteProgress with same indices doesn't convert twice`() {
        every { mapboxMap.cameraState } returns mockk(relaxed = true) {
            every { bearing } returns 12.0
        }
        val location = createLocation(20.0, 10.0)
        val expected = createCameraOptions {
            center(Point.fromLngLat(1.0, 2.0))
            bearing(0.0)
            pitch(0.0)
            zoom(14.2)
        }
        every {
            mapboxMap.cameraForCoordinates(
                any(),
                any(),
                null,
                null,
                null,
            )
        } returns createCameraOptions {
            center(Point.fromLngLat(1.0, 2.0))
            zoom(14.2)
        }

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        clearMocks(indicesConverter, answers = false)

        val routeProgress2 = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk(relaxed = true) {
                every { legIndex } returns primaryLegIndex
                every { currentStepProgress } returns mockk(relaxed = true) {
                    every { stepIndex } returns primaryStepIndex
                }
                every { geometryIndex } returns primaryLegGeometryIndex
            }
            every { route } returns this@OverviewViewportDataSourceTest.route
        }

        viewportDataSource.onRouteProgressChanged(routeProgress2)
        viewportDataSource.evaluate()

        verify(exactly = 0) {
            indicesConverter.convert(
                navigationRouteId,
                primaryLegIndex,
                primaryStepIndex,
                primaryLegGeometryIndex,
            )
        }

        assertEquals(expected, viewportDataSource.viewportData)
    }

    @Test
    fun `onRouteProgress with null stepGeometryIndex`() {
        every { mapboxMap.cameraState } returns mockk(relaxed = true) {
            every { bearing } returns 12.0
        }
        val location = createLocation(20.0, 10.0)
        val expected = createCameraOptions {
            center(Point.fromLngLat(1.0, 2.0))
            bearing(0.0)
            pitch(0.0)
            zoom(14.2)
        }
        every {
            mapboxMap.cameraForCoordinates(
                any(),
                any(),
                null,
                null,
                null,
            )
        } returns createCameraOptions {
            center(Point.fromLngLat(1.0, 2.0))
            zoom(14.2)
        }
        every {
            indicesConverter.convert(
                navigationRouteId,
                primaryLegIndex,
                primaryStepIndex,
                primaryLegGeometryIndex,
            )
        } returns null
        every {
            getRemainingPointsOnRoute(any(), emptyList(), any(), primaryLegIndex, primaryStepIndex)
        } returns remainingPointsOnRoute

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRoutesChanged(listOf(navigationRoute))
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        verify {
            getRemainingPointsOnRoute(
                any(),
                emptyList(),
                any(),
                primaryLegIndex,
                primaryStepIndex,
            )
        }

        assertEquals(expected, viewportDataSource.viewportData)
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

    private fun createLocation(
        lat: Double = 10.0,
        lng: Double = 20.0,
        bear: Double = 30.0,
    ) = mockk<Location> {
        every { latitude } returns lat
        every { longitude } returns lng
        every { bearing } returns bear
    }
}
