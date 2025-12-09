package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.annotation.MapboxDelicateApi
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.camera.data.MapboxFollowingCameraFramingStrategy.getPointsToFrameAfterCurrentManeuver
import com.mapbox.navigation.ui.maps.camera.data.MapboxFollowingCameraFramingStrategy.getPointsToFrameOnCurrentStep
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.BEARING_NORTH
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.EMPTY_EDGE_INSETS
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.NULL_ISLAND_POINT
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource.Companion.ZERO_PITCH
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getMapAnchoredPaddingFromUserPadding
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getRemainingPointsOnRoute
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getScreenBoxForFraming
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getSmootherBearingForMap
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.isFramingManeuver
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteForPostManeuverFramingGeometry
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteIntersections
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.internal.camera.FollowingFramingModeHolder
import com.mapbox.navigation.ui.maps.internal.camera.InternalViewportDataSourceOptions
import com.mapbox.navigation.ui.maps.internal.camera.OverviewMode
import com.mapbox.navigation.ui.maps.internal.camera.OverviewViewportDataSource
import com.mapbox.navigation.utils.internal.toPoint
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapboxNavigationViewportDataSourceTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mapboxMap: MapboxMap = mockk(relaxed = true)
    private val followingFrameModeHolder = mockk<FollowingFramingModeHolder>(relaxed = true)
    private val emptyCameraState = CameraState(
        NULL_ISLAND_POINT,
        EMPTY_EDGE_INSETS,
        0.0,
        BEARING_NORTH,
        ZERO_PITCH,
    )
    private val mapSize = Size(1000f, 1000f)
    private val singlePixelEdgeInsets = EdgeInsets(1.0, 2.0, 3.0, 4.0)
    private val followingScreenBox = ScreenBox(
        ScreenCoordinate(1.0, 2.0),
        ScreenCoordinate(3.0, 4.0),
    )
    private val smoothedBearing = 333.0

    private val route: DirectionsRoute = mockk(relaxed = true) {
        every { routeIndex() } returns "0"
        every { routeOptions() } returns mockk()
    }

    private val navigationRoute: NavigationRoute = mockk(relaxed = true) {
        every { routeOptions } returns route.routeOptions()!!
        every { directionsRoute } returns route
    }
    private val navigationRoute2: NavigationRoute = mockk(relaxed = true)

    private val routeProgress: RouteProgress = mockk()
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
    private val averageIntersectionDistancesOnRoute: List<List<Double>> =
        // legs
        listOf(
            // steps
            listOf(1.0, 2.0, 3.0),
            listOf(4.0, 5.0),
        )

    private val postManeuverFramingPoints: List<List<List<Point>>> =
        // legs
        listOf(
            // steps
            listOf(
                // points
                listOf(Point.fromLngLat(11.0, 12.0)),
                listOf(Point.fromLngLat(13.0, 14.0)),
                listOf(Point.fromLngLat(15.0, 16.0)),
            ),
            listOf(
                // points
                listOf(Point.fromLngLat(17.0, 18.0)),
                listOf(Point.fromLngLat(19.0, 20.0)),
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
        every { mapboxMap.cameraState } returns emptyCameraState
        every { mapboxMap.getSize() } returns mapSize
        val actionSlot = slot<() -> Unit>()
        every {
            mapboxMap.whenSizeReady(
                capture(actionSlot),
            )
        } answers {
            actionSlot.captured()
        }

        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)

        mockkObject(ViewportDataSourceProcessor)
        mockkObject(MapboxFollowingCameraFramingStrategy)
        every {
            getMapAnchoredPaddingFromUserPadding(mapSize, any(), any())
        } returns singlePixelEdgeInsets
        every { getScreenBoxForFraming(mapSize, any()) } returns followingScreenBox
        every {
            getSmootherBearingForMap(
                viewportDataSource.options.followingFrameOptions.bearingSmoothing.enabled,
                viewportDataSource.options.followingFrameOptions.bearingSmoothing
                    .maxBearingAngleDiff,
                any(),
                any(),
                any(),
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
                completeRoutePoints,
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
                completeRoutePoints,
            )
        } returns postManeuverFramingPoints
        every {
            isFramingManeuver(
                any(),
                any(),
            )
        } returns false
        every {
            getPointsToFrameOnCurrentStep(
                any(),
                viewportDataSource.options.followingFrameOptions,
                averageIntersectionDistancesOnRoute,
            )
        } returns pointsToFrameOnCurrentStep
        every {
            getPointsToFrameAfterCurrentManeuver(
                any(),
                viewportDataSource.options.followingFrameOptions,
                postManeuverFramingPoints,
            )
        } returns pointsToFrameAfterCurrentStep
        every {
            getRemainingPointsOnRoute(
                completeRoutePoints,
                any(),
                any(),
                any(),
                any(),
            )
        } returns remainingPointsOnRoute
    }

    @Test
    fun publicConstructorUsesSameOptionsForDataSourceAndOverview() {
        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)

        assertTrue(
            viewportDataSource.options === viewportDataSource.overviewViewportDataSource.options,
        )
        assertTrue(
            viewportDataSource.internalOptions ===
                viewportDataSource.overviewViewportDataSource.internalOptions,
        )
    }

    @Test
    fun internalConstructorUsesSameOptionsForDataSourceAndOverview() {
        val options = mockk<MapboxNavigationViewportDataSourceOptions>(relaxed = true)
        val newOptions = mockk<MapboxNavigationViewportDataSourceOptions>(relaxed = true)
        val internalOptions = mockk<InternalViewportDataSourceOptions>(relaxed = true)
        val newInternalOptions = mockk<InternalViewportDataSourceOptions>(relaxed = true)
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true) {
            every { this@mockk.options } returns options
            every { this@mockk.internalOptions } returns internalOptions
        }
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        assertTrue(viewportDataSource.options === options)
        assertTrue(viewportDataSource.internalOptions === internalOptions)

        every { overviewViewportDataSource.options } returns newOptions
        every { overviewViewportDataSource.internalOptions } returns newInternalOptions

        assertTrue(viewportDataSource.internalOptions === newInternalOptions)
    }

    @Test
    fun internalOptionsSetterIsPassedToOverview() {
        val internalOptions = mockk<InternalViewportDataSourceOptions>(relaxed = true)
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        viewportDataSource.internalOptions = internalOptions

        verify {
            overviewViewportDataSource.internalOptions = internalOptions
        }
    }

    @Test
    fun sanity() {
        assertNotNull(viewportDataSource)
    }

    // region mocked OverviewViewportDataSource
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun settingDebuggerSetsItToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val debugger = mockk<MapboxNavigationViewportDataSourceDebugger>(relaxed = true)
        viewportDataSource.debugger = debugger

        verify { overviewViewportDataSource.debugger = debugger }
    }

    @Test
    fun settingOverviewPaddingSetsItToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val padding = EdgeInsets(1.0, 2.0, 3.0, 4.0)
        viewportDataSource.overviewPadding = padding

        verify { overviewViewportDataSource.padding = padding }
    }

    @Test
    fun initialOverviewViewportDataIsTakenFromOverviewDataSource() {
        val overviewOptions = mockk<CameraOptions>()
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true) {
            every { viewportData } returns overviewOptions
        }
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        assertEquals(overviewOptions, viewportDataSource.getViewportData().cameraForOverview)
    }

    @Test
    fun evaluateUsesOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        clearMocks(overviewViewportDataSource)

        val overviewOptions = mockk<CameraOptions>()
        every { overviewViewportDataSource.viewportData } returns overviewOptions

        viewportDataSource.evaluate()

        verifyOrder {
            overviewViewportDataSource.evaluate()
            overviewViewportDataSource.viewportData
        }

        assertEquals(overviewOptions, viewportDataSource.getViewportData().cameraForOverview)
    }

    @Test
    fun onRouteProgressChangedIsNotPassedToOverviewDataSourceIfNoRouteIsSet() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns mockk()
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        viewportDataSource.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) {
            overviewViewportDataSource.onRouteProgressChanged(any())
        }
    }

    @Test
    fun onRouteProgressChangedIsNotPassedToOverviewDataSourceIfDifferentRouteIsSet() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns mockk()
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns mockk(relaxed = true)

        viewportDataSource.onRouteChanged(navigationRoute)
        clearMocks(overviewViewportDataSource)

        mockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        every { route.isSameRoute(any()) } returns false
        viewportDataSource.onRouteProgressChanged(routeProgress)
        unmockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")

        verify(exactly = 0) {
            overviewViewportDataSource.onRouteProgressChanged(any())
        }
    }

    @Test
    fun onRouteProgressChangedIsNotPassedToOverviewDataSourceIfStepProgressIsNull() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns null
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress)

        verify(exactly = 0) {
            overviewViewportDataSource.onRouteProgressChanged(any())
        }
    }

    @Test
    fun onRouteProgressChangedIsPassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true) {
            every { options } returns mockk(relaxed = true) {
                every { followingFrameOptions } returns mockk(relaxed = true) {
                    every { framingStrategy } returns mockk(relaxed = true) {
                        every {
                            getPointsToFrameOnCurrentStep(routeProgress, any(), any())
                        } returns pointsToFrameOnCurrentStep
                    }
                }
            }
        }
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val legProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns mockk()
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress)

        verify {
            overviewViewportDataSource.onRouteProgressChanged(
                routeProgress,
            )
        }
    }

    @Test
    fun onLocationChangedIsPassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val location = createLocation(1.0, 2.0)
        viewportDataSource.onLocationChanged(location)

        verify { overviewViewportDataSource.onLocationChanged(location) }
    }

    @Test
    fun clearRouteDataClearsOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        viewportDataSource.clearRouteData()

        verify { overviewViewportDataSource.clearRouteData() }
    }

    @Test
    fun onRouteChangedIsPassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        viewportDataSource.onRouteChanged(navigationRoute)

        verify { overviewViewportDataSource.onRoutesChanged(listOf(navigationRoute)) }
    }

    @Test
    fun onRoutesChangedArePassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val routes = listOf(navigationRoute, navigationRoute2)
        viewportDataSource.onRoutesChanged(routes)

        verify { overviewViewportDataSource.onRoutesChanged(routes) }
    }

    @Test
    fun onRouteChangedIsNotPassedToOverviewDataSourceIfSame() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        viewportDataSource.onRouteChanged(navigationRoute)

        clearMocks(overviewViewportDataSource)

        viewportDataSource.onRouteChanged(navigationRoute)

        verify(exactly = 0) { overviewViewportDataSource.onRoutesChanged(any()) }
    }

    @Test
    fun onRoutesChangedAreNotPassedToOverviewDataSourceIfSame() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val routes = listOf(navigationRoute, navigationRoute2)
        viewportDataSource.onRoutesChanged(routes)

        clearMocks(overviewViewportDataSource)

        viewportDataSource.onRoutesChanged(routes)

        verify(exactly = 0) { overviewViewportDataSource.onRoutesChanged(any()) }
    }

    @Test
    fun routeIsPassedToOverviewDataSourceAfterReevaluate() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        viewportDataSource.onRouteChanged(navigationRoute)

        clearMocks(overviewViewportDataSource)

        viewportDataSource.reevaluateRoute()

        verify { overviewViewportDataSource.onRoutesChanged(listOf(navigationRoute)) }
    }

    @Test
    fun routesArePassedToOverviewDataSourceAfterReevaluate() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val routes = listOf(navigationRoute, navigationRoute2)
        viewportDataSource.onRoutesChanged(routes)

        clearMocks(overviewViewportDataSource)

        viewportDataSource.reevaluateRoute()

        verify { overviewViewportDataSource.onRoutesChanged(routes) }
    }

    @Test
    fun clearProgressDataIsPassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        viewportDataSource.onRouteChanged(navigationRoute)
        clearMocks(overviewViewportDataSource)
        every { routeProgress.route } returns mockk(relaxed = true)
        every { routeProgress.currentLegProgress } returns mockk(relaxed = true)

        mockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        every { route.isSameRoute(any()) } returns false
        viewportDataSource.onRouteProgressChanged(routeProgress)
        unmockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")

        verify { overviewViewportDataSource.clearProgressData() }
    }

    @Test
    fun additionalPointsToFrameForOverviewArePassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val points = listOf(
            Point.fromLngLat(1.0, 2.0),
            Point.fromLngLat(3.0, 4.0),
        )

        viewportDataSource.additionalPointsToFrameForOverview(points)

        verify { overviewViewportDataSource.additionalPointsToFrame(points) }
    }

    @Test
    fun overviewCenterPropertyOverrideIsPassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val point = Point.fromLngLat(1.0, 2.0)

        viewportDataSource.overviewCenterPropertyOverride(point)

        verify { overviewViewportDataSource.centerPropertyOverride(point) }
    }

    @Test
    fun overviewZoomPropertyOverrideIsPassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val zoom = 13.5

        viewportDataSource.overviewZoomPropertyOverride(zoom)

        verify { overviewViewportDataSource.zoomPropertyOverride(zoom) }
    }

    @Test
    fun overviewBearingPropertyOverrideIsPassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val bearing = 13.5

        viewportDataSource.overviewBearingPropertyOverride(bearing)

        verify { overviewViewportDataSource.bearingPropertyOverride(bearing) }
    }

    @Test
    fun overviewPitchPropertyOverrideIsPassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        val pitch = 13.5

        viewportDataSource.overviewPitchPropertyOverride(pitch)

        verify { overviewViewportDataSource.pitchPropertyOverride(pitch) }
    }

    @Test
    fun clearOverviewOverridesIsPassedToOverviewDataSource() {
        val overviewViewportDataSource = mockk<OverviewViewportDataSource>(relaxed = true)
        viewportDataSource = MapboxNavigationViewportDataSource(
            mapboxMap,
            followingFrameModeHolder,
            overviewViewportDataSource,
        )

        viewportDataSource.clearOverviewOverrides()

        verify { overviewViewportDataSource.clearOverrides() }
    }

    // endregion mocked OverviewViewportDataSource

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
            data.cameraForFollowing,
        )

        assertEquals(
            createCameraOptions {
                center(NULL_ISLAND_POINT)
                bearing(BEARING_NORTH)
                pitch(ZERO_PITCH)
                zoom(viewportDataSource.options.overviewFrameOptions.maxZoom)
                padding(EMPTY_EDGE_INSETS)
            },
            data.cameraForOverview,
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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
        val data = viewportDataSource.getViewportData()

        // verify
        verify(exactly = 1) {
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
        }
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
        viewportDataSource.additionalPointsToFrameForOverview(listOf(additionalPoint))
        viewportDataSource.additionalPointsToFrameForFollowing(listOf(additionalPoint))
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @Test
    fun `verify frame - just routeProgress`() {
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
            data.cameraForFollowing,
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
            data.cameraForOverview,
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
            data.cameraForFollowing,
        )

        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
                followingScreenBox,
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
        viewportDataSource.additionalPointsToFrameForOverview(listOf(additionalPoint))
        viewportDataSource.additionalPointsToFrameForFollowing(listOf(additionalPoint))
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @Test
    fun `verify frame - location + route + progress`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
                followingScreenBox,
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
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @OptIn(MapboxDelicateApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun `verify frame - location + route + progress + not framing a maneuver, but isFramingManeuver is overridden to true`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        every {
            isFramingManeuver(any(), any())
        } returns true

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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == smoothedBearing &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
            )
        } returns followingCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.isFramingManeuverPropertyOverride(true)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
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
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @Test
    fun `verify state is not cleared if the same route is provided twice`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
                followingScreenBox,
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
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @Test
    fun `verify frame - location + route + progress + framing a maneuver`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        every {
            isFramingManeuver(any(), any())
        } returns true

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
                match<CameraOptions> {
                    it.pitch == ZERO_PITCH &&
                        it.bearing == smoothedBearing &&
                        it.padding == EMPTY_EDGE_INSETS
                },
                null,
                null,
                null,
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
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @OptIn(MapboxDelicateApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
    @Test
    fun `verify frame - location + route + progress + framing a maneuver, isFramingManeuver is overridden to false`() {
        every {
            isFramingManeuver(any(), any())
        } returns true

        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
                followingScreenBox,
            )
        } returns followingCameraOptions

        // run
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.isFramingManeuverPropertyOverride(false)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        verify(exactly = 1) {
            mapboxMap.cameraForCoordinates(
                expectedFollowingPoints,
                fallbackOptions,
                followingScreenBox,
            )
        }
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
    }

    @Test
    fun `verify frame - location + route + progress + framing maneuver + no view maximization`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        every {
            isFramingManeuver(any(), any())
        } returns true

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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
                followingScreenBox,
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
        viewportDataSource.options.followingFrameOptions
            .maximizeViewableGeometryWhenPitchZero = false
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @Test
    fun `verify frame - location + route + progress + overridden pitch 0`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
        }
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.route } returns route

        every {
            isFramingManeuver(any(), any())
        } returns false

        val location = createLocation()

        // following mocks
        val expectedFollowingPoints = mutableListOf(location.toPoint()).apply {
            addAll(pointsToFrameOnCurrentStep)
        }
        val fallbackOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(ZERO_PITCH)
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
            padding(singlePixelEdgeInsets)
        }
        val followingZoom = 5.0 // less than min zoom - should use min zoom
        val followingCameraOptions = fallbackOptions.toBuilder()
            .zoom(followingZoom)
            .build()
        every {
            mapboxMap.cameraForCoordinates(
                expectedFollowingPoints,
                fallbackOptions,
                followingScreenBox,
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
        viewportDataSource.options.followingFrameOptions
            .maximizeViewableGeometryWhenPitchZero = false
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.followingPitchPropertyOverride(0.0)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        verify(exactly = 1) {
            mapboxMap.cameraForCoordinates(
                expectedFollowingPoints,
                fallbackOptions,
                followingScreenBox,
            )
        }
        assertEquals(
            followingCameraOptions.toBuilder()
                .zoom(10.5)
                .build(),
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
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

        // following mocks
        val followingCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(viewportDataSource.options.followingFrameOptions.defaultPitch)
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
                any(),
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
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        viewportDataSource.clearRouteData()
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @Test
    fun `verify frame after route data cleanup when in pitch 0`() {
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
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
                null,
                null,
                null,
            )
        } returns mockk(relaxed = true)
        val followingCameraOptions = createCameraOptions {
            center(location.toPoint())
            bearing(smoothedBearing)
            pitch(ZERO_PITCH)
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.followingPitchPropertyOverride(ZERO_PITCH)
        viewportDataSource.evaluate()
        viewportDataSource.clearRouteData()
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @Test
    fun `verify frame - location + route + progress + padding`() {
        val followingPadding = EdgeInsets(11.0, 12.0, 13.0, 14.0)
        val overviewPadding = EdgeInsets(15.0, 16.0, 17.0, 18.0)
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
                followingScreenBox,
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
        viewportDataSource.followingPadding = followingPadding
        viewportDataSource.overviewPadding = overviewPadding
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
        verify { getScreenBoxForFraming(mapSize, followingPadding) }
    }

    @Test
    fun `verify frame - location + route + progress + reset route`() {
        every {
            isFramingManeuver(any(), any())
        } returns true

        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
        viewportDataSource.onRouteProgressChanged(routeProgress)
        mockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        every { route.isSameRoute(any()) } returns false
        viewportDataSource.onRouteChanged(navigationRoute)
        unmockkStatic("com.mapbox.navigation.base.internal.utils.DirectionsRouteEx")
        viewportDataSource.evaluate()
        val data = viewportDataSource.getViewportData()

        // verify
        assertEquals(
            followingCameraOptions,
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @Test
    fun `verify frame - location + route + progress + reset route and forget to update`() {
        every {
            isFramingManeuver(any(), any())
        } returns true

        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { distanceRemaining } returns 123f
        }
        val legProgress = mockk<RouteLegProgress>(relaxed = true) {
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
            zoom(viewportDataSource.options.followingFrameOptions.maxZoom)
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
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
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
            data.cameraForFollowing,
        )
        assertEquals(
            overviewCameraOptions,
            data.cameraForOverview,
        )
    }

    @Test
    fun `viewport data changed only when map size is ready`() {
        val actionSlot = slot<() -> Unit>()
        every {
            mapboxMap.whenSizeReady(
                capture(actionSlot),
            )
        } returns Unit

        val initialViewportData = viewportDataSource.getViewportData()

        viewportDataSource.additionalPointsToFrameForOverview(listOf(Point.fromLngLat(10.0, 20.0)))
        viewportDataSource.evaluate()

        assertEquals(initialViewportData, viewportDataSource.getViewportData())

        actionSlot.captured.invoke()

        assertNotEquals(initialViewportData, viewportDataSource.getViewportData())
    }

    @Test
    fun `viewport data does not change while map size is not ready`() {
        every {
            mapboxMap.whenSizeReady(any())
        } answers {
            // do nothing, callback is not called
        }

        val initialViewportData = viewportDataSource.getViewportData()

        viewportDataSource.additionalPointsToFrameForOverview(listOf(Point.fromLngLat(10.0, 20.0)))
        viewportDataSource.evaluate()

        assertEquals(initialViewportData, viewportDataSource.getViewportData())
    }

    @Test
    fun `viewport data does not change if map size is ready after clearRouteData() called`() {
        val actionSlot = slot<() -> Unit>()
        every {
            mapboxMap.whenSizeReady(
                capture(actionSlot),
            )
        } returns Unit

        val initialViewportData = viewportDataSource.getViewportData()

        viewportDataSource.additionalPointsToFrameForOverview(listOf(Point.fromLngLat(10.0, 20.0)))
        viewportDataSource.evaluate()
        viewportDataSource.clearRouteData()

        actionSlot.captured.invoke()

        assertEquals(initialViewportData, viewportDataSource.getViewportData())
    }

    @Test
    fun `reevaluate route - no route`() {
        every { routeProgress.route } returns route
        every { routeProgress.currentLegProgress } returns mockk(relaxed = true) {
            every { currentStepProgress } returns mockk(relaxed = true)
        }
        every {
            isFramingManeuver(
                any(),
                any(),
            )
        } returns true

        val location = createLocation()

        viewportDataSource.onLocationChanged(location)

        viewportDataSource.evaluate()
        val oldData = viewportDataSource.getViewportData()

        val newDistance = 150.0
        val newPostManeuverFramingPoints =
            // legs
            listOf(
                // steps
                listOf(
                    // points
                    listOf(Point.fromLngLat(10.0, 11.0)),
                    listOf(Point.fromLngLat(12.0, 13.0)),
                    listOf(Point.fromLngLat(14.0, 15.0)),
                ),
                listOf(
                    // points
                    listOf(Point.fromLngLat(16.0, 17.0)),
                    listOf(Point.fromLngLat(18.0, 19.0)),
                ),
            )
        val newPointsToFrameAfterCurrentStep: List<Point> = listOf(
            Point.fromLngLat(39.0, 40.0),
            Point.fromLngLat(41.0, 42.0),
        )

        every {
            processRouteForPostManeuverFramingGeometry(any(), any(), newDistance, any(), any())
        } returns newPostManeuverFramingPoints

        every {
            getPointsToFrameAfterCurrentManeuver(
                any(),
                any(),
                newPostManeuverFramingPoints,
            )
        } returns newPointsToFrameAfterCurrentStep

        every {
            mapboxMap.cameraForCoordinates(
                match<List<Point>> { points: List<Point> ->
                    points.containsAll(newPointsToFrameAfterCurrentStep)
                },
                any(),
                any(),
                any(),
                any(),
            )
        } returns CameraOptions.Builder()
            .center(Point.fromLngLat(12.3, 13.4))
            .zoom(14.5)
            .build()

        viewportDataSource.options.followingFrameOptions.frameGeometryAfterManeuver
            .distanceToFrameAfterManeuver = newDistance
        viewportDataSource.reevaluateRoute()

        verify(exactly = 0) {
            processRouteForPostManeuverFramingGeometry(any(), any(), newDistance, any(), any())
        }
        verify(exactly = 0) {
            mapboxMap.cameraForCoordinates(
                match<List<Point>> { points: List<Point> ->
                    points.containsAll(newPointsToFrameAfterCurrentStep)
                },
                any(),
                any(),
                any(),
                any(),
            )
        }

        val newData = viewportDataSource.getViewportData()
        assertEquals(oldData, newData)
    }

    @Test
    fun `reevaluate route - has route, no route progress`() {
        every { routeProgress.route } returns route
        every { routeProgress.currentLegProgress } returns mockk(relaxed = true) {
            every { currentStepProgress } returns mockk(relaxed = true)
        }
        every {
            isFramingManeuver(
                any(),
                any(),
            )
        } returns true

        val location = createLocation()

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(navigationRoute)

        viewportDataSource.evaluate()
        val oldData = viewportDataSource.getViewportData()

        val newDistance = 150.0
        val newPostManeuverFramingPoints =
            // legs
            listOf(
                // steps
                listOf(
                    // points
                    listOf(Point.fromLngLat(10.0, 11.0)),
                    listOf(Point.fromLngLat(12.0, 13.0)),
                    listOf(Point.fromLngLat(14.0, 15.0)),
                ),
                listOf(
                    // points
                    listOf(Point.fromLngLat(16.0, 17.0)),
                    listOf(Point.fromLngLat(18.0, 19.0)),
                ),
            )
        val newPointsToFrameAfterCurrentStep: List<Point> = listOf(
            Point.fromLngLat(39.0, 40.0),
            Point.fromLngLat(41.0, 42.0),
        )

        every {
            processRouteForPostManeuverFramingGeometry(any(), any(), newDistance, any(), any())
        } returns newPostManeuverFramingPoints

        every {
            getPointsToFrameAfterCurrentManeuver(
                any(),
                any(),
                newPostManeuverFramingPoints,
            )
        } returns newPointsToFrameAfterCurrentStep

        every {
            mapboxMap.cameraForCoordinates(
                match<List<Point>> { points: List<Point> ->
                    points.containsAll(newPointsToFrameAfterCurrentStep)
                },
                any(),
                any(),
                any(),
                any(),
            )
        } returns CameraOptions.Builder()
            .center(Point.fromLngLat(12.3, 13.4))
            .zoom(14.5)
            .build()

        viewportDataSource.options.followingFrameOptions.frameGeometryAfterManeuver
            .distanceToFrameAfterManeuver = newDistance
        viewportDataSource.reevaluateRoute()

        verify {
            processRouteForPostManeuverFramingGeometry(any(), any(), newDistance, any(), any())
        }
        verify(exactly = 0) {
            mapboxMap.cameraForCoordinates(
                match<List<Point>> { points: List<Point> ->
                    points.containsAll(newPointsToFrameAfterCurrentStep)
                },
                any(),
                any(),
                any(),
                any(),
            )
        }

        val newData = viewportDataSource.getViewportData()
        assertEquals(oldData, newData)
    }

    @Test
    fun `reevaluate route - has route and route progress`() {
        val expectedOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(12.3, 13.4))
            .zoom(14.5)
            .build()

        every { routeProgress.route } returns route
        every { routeProgress.currentLegProgress } returns mockk(relaxed = true) {
            every { currentStepProgress } returns mockk(relaxed = true)
        }
        every {
            isFramingManeuver(
                any(),
                any(),
            )
        } returns true

        val location = createLocation()

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.onRouteChanged(navigationRoute)
        viewportDataSource.onRouteProgressChanged(routeProgress)

        viewportDataSource.evaluate()

        val newDistance = 150.0
        val newPostManeuverFramingPoints =
            // legs
            listOf(
                // steps
                listOf(
                    // points
                    listOf(Point.fromLngLat(10.0, 11.0)),
                    listOf(Point.fromLngLat(12.0, 13.0)),
                    listOf(Point.fromLngLat(14.0, 15.0)),
                ),
                listOf(
                    // points
                    listOf(Point.fromLngLat(16.0, 17.0)),
                    listOf(Point.fromLngLat(18.0, 19.0)),
                ),
            )
        val newPointsToFrameAfterCurrentStep: List<Point> = listOf(
            Point.fromLngLat(39.0, 40.0),
            Point.fromLngLat(41.0, 42.0),
        )

        every {
            processRouteForPostManeuverFramingGeometry(any(), any(), newDistance, any(), any())
        } returns newPostManeuverFramingPoints

        every {
            getPointsToFrameAfterCurrentManeuver(
                any(),
                any(),
                newPostManeuverFramingPoints,
            )
        } returns newPointsToFrameAfterCurrentStep

        every {
            mapboxMap.cameraForCoordinates(
                match<List<Point>> { points: List<Point> ->
                    points.containsAll(newPointsToFrameAfterCurrentStep)
                },
                any(),
                any(),
                any(),
                any(),
            )
        } returns expectedOptions

        viewportDataSource.options.followingFrameOptions.frameGeometryAfterManeuver
            .distanceToFrameAfterManeuver = newDistance
        viewportDataSource.reevaluateRoute()

        verify {
            processRouteForPostManeuverFramingGeometry(any(), any(), newDistance, any(), any())
        }
        verify {
            mapboxMap.cameraForCoordinates(
                match<List<Point>> { points: List<Point> ->
                    points.containsAll(newPointsToFrameAfterCurrentStep)
                },
                any(),
                any(),
                any(),
                any(),
            )
        }

        val newData = viewportDataSource.getViewportData()
        assertEquals(expectedOptions.center, newData.cameraForFollowing.center)
    }

    @Test
    fun `defaultPitch update is applied in Free Drive`() {
        val location = createLocation()

        val firstPitch = 30.0
        val secondPitch = 60.0
        viewportDataSource.options.followingFrameOptions.defaultPitch = firstPitch

        viewportDataSource.onLocationChanged(location)
        viewportDataSource.evaluate()

        assertEquals(
            firstPitch,
            viewportDataSource.getViewportData().cameraForFollowing.pitch,
        )

        viewportDataSource.options.followingFrameOptions.defaultPitch = secondPitch
        viewportDataSource.onLocationChanged(location)
        viewportDataSource.evaluate()

        assertEquals(
            secondPitch,
            viewportDataSource.getViewportData().cameraForFollowing.pitch,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(ViewportDataSourceProcessor)
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
