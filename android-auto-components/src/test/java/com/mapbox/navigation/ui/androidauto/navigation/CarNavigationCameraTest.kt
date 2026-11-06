package com.mapbox.navigation.ui.androidauto.navigation

import android.graphics.Rect
import androidx.car.app.CarContext
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapSurface
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.internal.RendererUtils.dpToPx
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.internal.camera.MapboxNavigationViewportDataSourceFactory
import com.mapbox.navigation.ui.maps.internal.camera.PointsOverviewViewportDataSource
import com.mapbox.navigation.ui.maps.internal.camera.requestNavigationCameraToPointsOverview
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
class CarNavigationCameraTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val carRoutesProvider = mockk<CarRoutesProvider> {
        every { navigationRoutes } returns flowOf(emptyList())
    }
    private lateinit var pointsDataSource: PointsOverviewViewportDataSource

    private fun createCamera() = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.FOLLOWING,
        alternativeCarCameraMode = CarCameraMode.OVERVIEW,
        carRoutesProvider = carRoutesProvider,
    )

    @Test
    fun `zoom updates can be disabled before the map surface is attached`() {
        val camera = createCamera()

        camera.zoomUpdatesAllowed(false)

        assertFalse(camera.followingZoomUpdatesAllowed())
    }

    @Test
    fun `zoom updates are allowed by default`() {
        assertTrue(createCamera().followingZoomUpdatesAllowed())
    }

    @Test
    fun `zoom setting is applied to the viewport created on attach`() {
        val camera = createCamera()
        camera.zoomUpdatesAllowed(false)

        carAppTestRule.onAttached(mockk<MapboxNavigation>(relaxUnitFun = true))
        val carMapSurface = mockCarMapSurface()
        camera.onAttached(carMapSurface)

        val options = camera.viewportDataSource.options
        assertFalse(options.followingFrameOptions.zoomUpdatesAllowed)
        assertFalse(options.overviewFrameOptions.zoomUpdatesAllowed)
        assertFalse(camera.followingZoomUpdatesAllowed())
        camera.onDetached(carMapSurface)
    }

    @Test
    fun `automatic zoom is re-enabled after detach`() {
        // Attaching resets the camera zoom, so a manual zoom level must not stay locked in.
        val camera = createCamera()
        carAppTestRule.onAttached(mockk<MapboxNavigation>(relaxUnitFun = true))
        val carMapSurface = mockCarMapSurface()
        camera.onAttached(carMapSurface)
        camera.zoomUpdatesAllowed(false)

        camera.onDetached(carMapSurface)
        assertTrue(camera.followingZoomUpdatesAllowed())
        val newCarMapSurface = mockCarMapSurface()
        camera.onAttached(newCarMapSurface)

        assertTrue(camera.viewportDataSource.options.followingFrameOptions.zoomUpdatesAllowed)
        assertTrue(camera.viewportDataSource.options.overviewFrameOptions.zoomUpdatesAllowed)
        camera.onDetached(newCarMapSurface)
    }

    private fun mockCarMapSurface(): MapboxCarMapSurface {
        val mapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
            every { cameraForCoordinates(any(), any<CameraOptions>(), any(), any(), any()) } returns
                CameraOptions.Builder().center(Point.fromLngLat(17.0, 52.0)).zoom(5.0).build()
        }
        val aMapSurface = mockk<MapSurface> {
            every { getMapboxMap() } returns mapboxMap
            every { camera } returns mockk<CameraAnimationsPlugin>(relaxed = true)
        }
        val aCarContext = mockk<CarContext> {
            every { resources } returns RuntimeEnvironment.getApplication().resources
        }
        return mockk {
            every { mapSurface } returns aMapSurface
            every { carContext } returns aCarContext
        }
    }

    @Test
    fun `map surface is reported as attached only between attach and detach`() {
        val camera = createCamera()
        assertFalse(camera.isMapSurfaceAttached)
        carAppTestRule.onAttached(mockk<MapboxNavigation>(relaxUnitFun = true))
        val carMapSurface = mockCarMapSurface()

        camera.onAttached(carMapSurface)
        assertTrue(camera.isMapSurfaceAttached)
        camera.onDetached(carMapSurface)

        assertFalse(camera.isMapSurfaceAttached)
    }

    @Test
    fun `zoom flag changes are observable`() {
        val camera = createCamera()

        camera.zoomUpdatesAllowed(false)

        assertFalse(camera.zoomUpdatesAllowedFlow.value)
    }

    @Test
    fun `favorite points set before attachment are framed without waiting for a location`() {
        mockCameraTransitions()
        val camera = createCamera()
        camera.zoomUpdatesAllowed(false)
        val points = listOf(Point.fromLngLat(21.0, 52.0), Point.fromLngLat(13.0, 52.0))
        camera.updateOverviewPoints(points)
        val surface = mockCarMapSurface()

        camera.onAttached(surface)

        pointsDataSource.evaluate()
        assertTrue(pointsDataSource.options.zoomUpdatesAllowed)
        assertEquals(5.0, pointsDataSource.cameraOptions.zoom!!, 0.0)
        assertFalse(camera.followingZoomUpdatesAllowed())
        val mapboxMap = surface.mapSurface.getMapboxMap()
        verify {
            mapboxMap.cameraForCoordinates(
                points,
                any<CameraOptions>(),
                any(),
                any(),
                any(),
            )
        }
        verify {
            any<NavigationCamera>().requestNavigationCameraToPointsOverview(any(), any(), any())
        }
        assertEquals(CarCameraMode.FOLLOWING, camera.nextCameraMode.value)
        camera.onDetached(surface)
    }

    @Test
    fun `closing points overview restores the previous idle mode and zoom preference`() {
        mockCameraTransitions()
        val camera = createCamera()
        val surface = mockCarMapSurface()
        camera.onAttached(surface)
        camera.updateCameraMode(CarCameraMode.IDLE)
        camera.zoomUpdatesAllowed(false)
        camera.updateOverviewPoints(listOf(Point.fromLngLat(21.0, 52.0)))
        assertTrue(pointsDataSource.options.zoomUpdatesAllowed)

        camera.updateOverviewPoints(emptyList())

        verify(exactly = 2) {
            anyConstructed<NavigationCamera>().requestNavigationCameraToIdle()
        }
        pointsDataSource.evaluate()
        assertNull(pointsDataSource.cameraOptions.center)
        assertFalse(pointsDataSource.options.zoomUpdatesAllowed)
        assertFalse(camera.followingZoomUpdatesAllowed())
        assertFalse(camera.viewportDataSource.options.overviewFrameOptions.zoomUpdatesAllowed)
        camera.onDetached(surface)
    }

    @Test
    fun `manual zoom stays applied while favorite markers and viewport padding update`() {
        mockCameraTransitions()
        val camera = createCamera()
        val surface = mockCarMapSurface()
        camera.onAttached(surface)
        camera.updateOverviewPoints(listOf(Point.fromLngLat(21.0, 52.0)))
        pointsDataSource.evaluate()
        assertEquals(5.0, pointsDataSource.cameraOptions.zoom!!, 0.0)

        camera.zoomUpdatesAllowed(false)
        camera.zoomInAction()
        pointsDataSource.evaluate()

        assertFalse(pointsDataSource.options.zoomUpdatesAllowed)
        assertNull(pointsDataSource.cameraOptions.zoom)
        camera.updateOverviewPoints(listOf(Point.fromLngLat(13.0, 52.0)))
        camera.onVisibleAreaChanged(Rect(200, 30, 800, 450), EdgeInsets(30.0, 200.0, 50.0, 0.0))
        pointsDataSource.evaluate()
        assertFalse(pointsDataSource.options.zoomUpdatesAllowed)
        assertNull(pointsDataSource.cameraOptions.zoom)
        camera.zoomUpdatesAllowed(false)
        camera.zoomOutAction()
        pointsDataSource.evaluate()
        assertFalse(pointsDataSource.options.zoomUpdatesAllowed)
        assertNull(pointsDataSource.cameraOptions.zoom)
        camera.updateOverviewPoints(emptyList())
        assertFalse(camera.followingZoomUpdatesAllowed())
        assertFalse(camera.viewportDataSource.options.overviewFrameOptions.zoomUpdatesAllowed)
        camera.onDetached(surface)
    }

    @Test
    fun `reopening favorites frames their markers without changing the saved zoom preference`() {
        mockCameraTransitions()
        val camera = createCamera()
        val surface = mockCarMapSurface()
        camera.onAttached(surface)
        camera.zoomUpdatesAllowed(false)
        val points = listOf(Point.fromLngLat(21.0, 52.0))
        camera.updateOverviewPoints(points)
        assertTrue(pointsDataSource.options.zoomUpdatesAllowed)
        camera.zoomUpdatesAllowed(false)
        pointsDataSource.evaluate()
        assertNull(pointsDataSource.cameraOptions.zoom)

        camera.updateOverviewPoints(emptyList())
        camera.updateOverviewPoints(points)
        pointsDataSource.evaluate()

        assertEquals(5.0, pointsDataSource.cameraOptions.zoom!!, 0.0)
        assertTrue(pointsDataSource.options.zoomUpdatesAllowed)
        assertFalse(camera.followingZoomUpdatesAllowed())
        camera.updateOverviewPoints(emptyList())
        assertFalse(pointsDataSource.options.zoomUpdatesAllowed)
        assertFalse(camera.followingZoomUpdatesAllowed())
        camera.onDetached(surface)
    }

    @Test
    fun `automatic zoom can be reenabled while favorites stay open`() {
        mockCameraTransitions()
        val camera = createCamera()
        val surface = mockCarMapSurface()
        camera.onAttached(surface)
        camera.updateOverviewPoints(listOf(Point.fromLngLat(21.0, 52.0)))
        camera.zoomUpdatesAllowed(false)
        pointsDataSource.evaluate()
        assertNull(pointsDataSource.cameraOptions.zoom)

        camera.zoomUpdatesAllowed(true)
        pointsDataSource.evaluate()

        assertTrue(pointsDataSource.options.zoomUpdatesAllowed)
        assertEquals(5.0, pointsDataSource.cameraOptions.zoom!!, 0.0)
        camera.updateOverviewPoints(emptyList())
        assertTrue(camera.followingZoomUpdatesAllowed())
        camera.onDetached(surface)
    }

    @Test
    fun `points overview keeps receiving route and progress updates without clearing route data`() {
        mockCameraTransitions()
        val initialRoute = mockk<NavigationRoute> { every { id } returns "initial" }
        val routes = MutableStateFlow(listOf(initialRoute))
        every { carRoutesProvider.navigationRoutes } returns routes
        val progressObserver = slot<RouteProgressObserver>()
        val navigation = mockk<MapboxNavigation>(relaxUnitFun = true) {
            every { registerRouteProgressObserver(capture(progressObserver)) } just runs
        }
        carAppTestRule.onAttached(navigation)
        val camera = createCamera()
        val surface = mockCarMapSurface()
        camera.onAttached(surface)
        camera.updateOverviewPoints(listOf(Point.fromLngLat(21.0, 52.0)))
        val updatedRoute = mockk<NavigationRoute> { every { id } returns "updated" }
        val progress = mockk<RouteProgress> {
            every { navigationRoute } returns updatedRoute
        }

        routes.value = listOf(updatedRoute)
        progressObserver.captured.onRouteProgressChanged(progress)
        camera.updateOverviewPoints(emptyList())

        verify { anyConstructed<MapboxNavigationViewportDataSource>().onRouteChanged(updatedRoute) }
        verify { anyConstructed<MapboxNavigationViewportDataSource>().onRouteProgressChanged(progress) }
        verify(exactly = 0) {
            anyConstructed<MapboxNavigationViewportDataSource>().clearRouteData()
        }
        verify {
            anyConstructed<NavigationCamera>().requestNavigationCameraToFollowing(
                any<NavigationCameraTransitionOptions>(),
                any(),
                any(),
            )
        }
        camera.onDetached(surface)
    }

    @Test
    fun `preview camera ignores progress from ongoing guidance until active routes are restored`() {
        mockCameraTransitions()
        val activeRoute = mockk<NavigationRoute> { every { id } returns "active" }
        val previewRoute = mockk<NavigationRoute> { every { id } returns "favorite" }
        val routes = MutableStateFlow(listOf(previewRoute))
        every { carRoutesProvider.navigationRoutes } returns routes
        val progressObserver = slot<RouteProgressObserver>()
        val navigation = mockk<MapboxNavigation>(relaxUnitFun = true) {
            every { registerRouteProgressObserver(capture(progressObserver)) } just runs
        }
        carAppTestRule.onAttached(navigation)
        val camera = createCamera()
        val surface = mockCarMapSurface()
        camera.onAttached(surface)
        val progress = mockk<RouteProgress> {
            every { navigationRoute } returns activeRoute
        }

        progressObserver.captured.onRouteProgressChanged(progress)

        verify(exactly = 0) {
            anyConstructed<MapboxNavigationViewportDataSource>().onRouteProgressChanged(any())
        }
        routes.value = listOf(activeRoute)
        progressObserver.captured.onRouteProgressChanged(progress)
        verify(exactly = 1) {
            anyConstructed<MapboxNavigationViewportDataSource>().onRouteProgressChanged(progress)
        }
        camera.onDetached(surface)
    }

    @Test
    fun `favorite markers are framed within the visible map area with marker padding`() {
        mockCameraTransitions()
        val camera = createCamera()
        val surface = mockCarMapSurface()
        camera.onAttached(surface)
        val padding = surface.carContext.dpToPx(15)

        camera.onVisibleAreaChanged(Rect(200, 30, 800, 450), EdgeInsets(30.0, 200.0, 50.0, 0.0))

        assertEquals(
            EdgeInsets(
                30.0 + padding,
                200.0 + padding,
                50.0 + padding,
                0.0 + padding,
            ),
            pointsDataSource.padding,
        )
        camera.onDetached(surface)
    }

    private fun mockCameraTransitions() {
        mockkObject(MapboxNavigationViewportDataSourceFactory)
        every {
            MapboxNavigationViewportDataSourceFactory.create(any(), any(), any(), any(), any())
        } answers {
            pointsDataSource = arg(3)
            callOriginal()
        }
        mockkConstructor(MapboxNavigationViewportDataSource::class)
        every { anyConstructed<MapboxNavigationViewportDataSource>().evaluate() } just runs
        every {
            anyConstructed<MapboxNavigationViewportDataSource>().onRouteChanged(any())
        } just runs
        every {
            anyConstructed<MapboxNavigationViewportDataSource>().onRouteProgressChanged(any())
        } just runs
        mockkConstructor(NavigationCamera::class)
        every {
            anyConstructed<NavigationCamera>().requestNavigationCameraToFollowing(
                any<NavigationCameraTransitionOptions>(),
                any(),
                any(),
            )
        } just runs
        every { anyConstructed<NavigationCamera>().requestNavigationCameraToIdle() } just runs
        mockkStatic("com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraExKt")
        every {
            any<NavigationCamera>().requestNavigationCameraToPointsOverview(any(), any(), any())
        } just runs
    }
}
