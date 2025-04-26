package com.mapbox.navigation.ui.maps.internal.camera

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.BEARING_NORTH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.EMPTY_EDGE_INSETS
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.NULL_ISLAND_POINT
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.ZERO_PITCH
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getRemainingPointsOnRoute
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRoutePoints
import com.mapbox.navigation.utils.internal.toPoint
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

    private val emptyCameraState = CameraState(
        NULL_ISLAND_POINT,
        EMPTY_EDGE_INSETS,
        0.0,
        BEARING_NORTH,
        ZERO_PITCH,
    )

    private val pointsToFrameOnCurrentStep: List<Point> = listOf(
        Point.fromLngLat(30.0, 31.0),
        Point.fromLngLat(32.0, 33.0),
    )

    private val route: DirectionsRoute = mockk(relaxed = true) {
        every { routeIndex() } returns "0"
        every { routeOptions() } returns mockk()
    }

    private val navigationRoute: NavigationRoute = mockk(relaxed = true) {
        every { routeOptions } returns route.routeOptions()!!
        every { directionsRoute } returns route
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
    private val completeRoutePointsFlat: List<Point> = completeRoutePoints.flatten().flatten()

    private val remainingPointsOnRoute: List<Point> = listOf(
        Point.fromLngLat(50.0, 51.0),
        Point.fromLngLat(52.0, 53.0),
    )

    private val viewportDataSource = OverviewViewportDataSource(
        mapboxMap,
        InternalViewportDataSourceOptions(
            ignoreMinZoomWhenFramingManeuver = false,
            overviewMode = OverviewMode.ACTIVE_LEG,
        ),
    )

    @Before
    fun setUp() {
        mockkObject(ViewportDataSourceProcessor)
        every { processRoutePoints(route) } returns completeRoutePoints
        every {
            getRemainingPointsOnRoute(
                completeRoutePoints,
                pointsToFrameOnCurrentStep,
                any(),
                any(),
                any(),
            )
        } returns remainingPointsOnRoute
    }

    @After
    fun tearDown() {
        unmockkObject(ViewportDataSourceProcessor)
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
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
        viewportDataSource.evaluate()
        val data = viewportDataSource.viewportData

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(ZERO_PITCH)
                zoom(16.35) // overviewOptions.maxZoom
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

        viewportDataSource.onRouteChanged(navigationRoute)
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
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
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
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
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
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
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
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
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
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
        mockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        every { route.isSameRoute(any()) } returns false
        viewportDataSource.onRouteChanged(navigationRoute)
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
        val stepProgress = mockk<RouteStepProgress> {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress> {
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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)

        mockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        every { route.isSameRoute(any()) } returns false
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
        viewportDataSource.evaluate()

        verify {
            processRoutePoints(route)
        }
        verify {
            getRemainingPointsOnRoute(any(), pointsToFrameOnCurrentStep, any(), any(), any())
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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
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
            getRemainingPointsOnRoute(any(), pointsToFrameOnCurrentStep, any(), any(), any())
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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
        viewportDataSource.evaluate()

        viewportDataSource.clearRouteData()
        viewportDataSource.clearProgressData()

        viewportDataSource.setActive(true)

        verify(exactly = 0) {
            processRoutePoints(route)
        }
        verify(exactly = 0) {
            getRemainingPointsOnRoute(any(), pointsToFrameOnCurrentStep, any(), any(), any())
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

        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
        viewportDataSource.evaluate()

        verify {
            processRoutePoints(route)
        }
        verify {
            getRemainingPointsOnRoute(any(), pointsToFrameOnCurrentStep, any(), any(), any())
        }
        verify {
            mapboxMap.cameraForCoordinates(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun changeOverviewModeReevaluates() {
        val location = createLocation(20.0, 10.0)

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress, pointsToFrameOnCurrentStep)
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
