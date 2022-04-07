package com.mapbox.navigation.dropin.component.camera

import com.mapbox.android.gestures.Utils
import com.mapbox.common.Logger
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.toCameraOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.location.LocationAction
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CameraComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val mockCamera: CameraAnimationsPlugin = mockk(relaxUnitFun = true) {
        every { addCameraAnimationsLifecycleListener(any()) } just Runs
    }
    private val mockNavigationCamera: NavigationCamera = mockk(relaxed = true)
    private val mockViewPortDataSource: MapboxNavigationViewportDataSource = mockk(relaxed = true)
    private val mockMapboxMap: MapboxMap = mockk(relaxUnitFun = true) {
        every { cameraState } returns mockk()
    }
    private val mockMapView: MapView = mockk(relaxed = true) {
        every { camera } returns mockCamera
        every { getMapboxMap() } returns mockMapboxMap
        every { context } returns mockk(relaxed = true)
        every { resources } returns mockk(relaxed = true)
    }
    private val locMatcherResult: LocationMatcherResult =
        makeLocationMatcherResult(-121.4567, 37.9876, 45f)
    private val cameraViewModel = spyk(CameraViewModel())
    private val locationViewModel = LocationViewModel()
    private val navigationStateViewModel = NavigationStateViewModel(NavigationState.FreeDrive)

    private lateinit var cameraComponent: CameraComponent

    @Before
    fun setUp() {
        mockkStatic(Utils::class, Logger::class)
        mockkObject(MapboxNavigationApp)
        every { Utils.dpToPx(any()) } returns 50f
        every { Logger.d(any(), any()) } returns Unit
        cameraComponent = CameraComponent(
            mockMapView,
            cameraViewModel,
            locationViewModel,
            navigationStateViewModel,
            mockViewPortDataSource,
            mockNavigationCamera,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onAttached should restore camera state from view model`() {
        val cameraState = com.mapbox.maps.CameraState(
            Point.fromLngLat(11.0, 22.0),
            EdgeInsets(1.0, 2.0, 3.0, 4.0),
            30.0,
            40.0,
            50.0
        )
        every { cameraViewModel.state } returns MutableStateFlow(
            CameraState(
                mapCameraState = cameraState
            )
        )

        cameraComponent.onAttached(mockMapboxNavigation())

        verify { mockMapboxMap.setCamera(cameraState.toCameraOptions()) }
    }

    @Test
    fun `onDetached should save camera state in view model`() {
        val cameraState = com.mapbox.maps.CameraState(
            Point.fromLngLat(11.0, 22.0),
            EdgeInsets(1.0, 2.0, 3.0, 4.0),
            30.0,
            40.0,
            50.0
        )
        every { mockMapboxMap.cameraState } returns cameraState

        val mockMapboxNavigation = mockMapboxNavigation()
        cameraComponent.onAttached(mockMapboxNavigation)
        cameraComponent.onDetached(mockMapboxNavigation)

        verify { cameraViewModel.invoke(CameraAction.SaveMapState(cameraState)) }
    }

    @Test
    fun `when location update is received first time in free drive camera frame is updated`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            locationViewModel.onAttached(mockMapboxNavigation)
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraComponent.onAttached(mockMapboxNavigation)
            locationViewModel.invoke(LocationAction.Update(locMatcherResult))

            verify(exactly = 1) {
                mockNavigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0) // instant transition
                        .build()
                )
            }
        }

    @Test
    fun `when location update is received first time in active guidance camera frame is updated`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            navigationStateViewModel.onAttached(mockMapboxNavigation)
            locationViewModel.onAttached(mockMapboxNavigation)
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraComponent.onAttached(mockMapboxNavigation)
            navigationStateViewModel.invoke(
                NavigationStateAction.Update(
                    NavigationState.ActiveNavigation
                )
            )
            locationViewModel.invoke(LocationAction.Update(locMatcherResult))

            verify(exactly = 1) {
                mockNavigationCamera.requestNavigationCameraToFollowing(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0) // instant transition
                        .build()
                )
            }
        }

    @Test
    fun `camera frame is not updated on subsequent location updates`() =
        coroutineRule.runBlockingTest {
            val nextLocMatcherResult =
                makeLocationMatcherResult(-121.4567, 37.9876, 45f)
            val mockMapboxNavigation = mockMapboxNavigation()
            locationViewModel.onAttached(mockMapboxNavigation)
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraComponent.onAttached(mockMapboxNavigation)
            locationViewModel.invoke(LocationAction.Update(locMatcherResult))
            locationViewModel.invoke(LocationAction.Update(nextLocMatcherResult))

            verify(exactly = 1) {
                mockNavigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0) // instant transition
                        .build()
                )
            }
        }

    @Test
    fun `camera frame is not updated if camera component instantiation is fresh`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraComponent.onAttached(mockMapboxNavigation)

            verify(exactly = 0) {
                mockNavigationCamera.requestNavigationCameraToOverview()
            }
        }

    @Test
    fun `camera frame updates to idle when requested`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            locationViewModel.onAttached(mockMapboxNavigation)
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraComponent.onAttached(mockMapboxNavigation)
            locationViewModel.invoke(LocationAction.Update(locMatcherResult))
            cameraViewModel.invoke(CameraAction.ToIdle)

            verify(exactly = 1) {
                mockNavigationCamera.requestNavigationCameraToIdle()
            }
        }

    @Test
    fun `camera frame updates to following when requested`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            locationViewModel.onAttached(mockMapboxNavigation)
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraComponent.onAttached(mockMapboxNavigation)
            locationViewModel.invoke(LocationAction.Update(locMatcherResult))
            cameraViewModel.invoke(CameraAction.ToFollowing)

            verify(atLeast = 1) {
                mockNavigationCamera.requestNavigationCameraToFollowing()
            }
        }

    @Test
    fun `when in free drive zoom and pitch property is overridden`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            cameraComponent.onAttached(mockMapboxNavigation)

            verify {
                mockViewPortDataSource.overviewZoomPropertyOverride(16.5)
                mockViewPortDataSource.overviewPitchPropertyOverride(0.0)
                mockViewPortDataSource.followingZoomPropertyOverride(16.5)
                mockViewPortDataSource.evaluate()
            }
        }

    @Test
    fun `when in mode other than free drive zoom property override is cleared`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            navigationStateViewModel.onAttached(mockMapboxNavigation)
            navigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.RoutePreview)
            )
            cameraComponent.onAttached(mockMapboxNavigation)

            verify {
                mockViewPortDataSource.clearOverviewOverrides()
                mockViewPortDataSource.clearFollowingOverrides()
                mockViewPortDataSource.evaluate()
            }
        }

    @Test
    fun `when route progress updates camera viewport updates`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            every {
                mockMapboxNavigation.registerRouteProgressObserver(any())
            } answers {
                firstArg<RouteProgressObserver>().onRouteProgressChanged(mockk())
            }
            cameraComponent.onAttached(mockMapboxNavigation)
            mockMapboxNavigation.startTripSession()

            verify {
                mockViewPortDataSource.onRouteProgressChanged(any())
                mockViewPortDataSource.evaluate()
            }
        }

    @Test
    fun `when route obtained in route overview and is not empty camera viewport updates`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            val mockNavigationRoute: List<NavigationRoute> = listOf(
                mockk {
                    every { directionsRoute } returns mockk()
                },
                mockk()
            )
            val mockRoutesUpdatedResult: RoutesUpdatedResult = mockk {
                every { navigationRoutes } returns mockNavigationRoute
            }
            every {
                mockMapboxNavigation.registerRoutesObserver(any())
            } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult)
            }
            navigationStateViewModel.onAttached(mockMapboxNavigation)
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraComponent.onAttached(mockMapboxNavigation)
            navigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.RoutePreview)
            )
            mockMapboxNavigation.setNavigationRoutes(mockNavigationRoute)

            verify {
                mockViewPortDataSource.onRouteChanged(mockNavigationRoute.first())
                mockViewPortDataSource.evaluate()
            }
            assertEquals(TargetCameraMode.Overview, cameraViewModel.state.value.cameraMode)
        }

    @Test
    fun `when route updates in arrival and is not empty camera viewport updates`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            navigationStateViewModel.onAttached(mockMapboxNavigation)
            navigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.Arrival)
            )
            val mockNavigationRoute: List<NavigationRoute> = listOf(
                mockk {
                    every { directionsRoute } returns mockk()
                },
                mockk()
            )
            val mockRoutesUpdatedResult: RoutesUpdatedResult = mockk {
                every { navigationRoutes } returns mockNavigationRoute
            }
            every {
                mockMapboxNavigation.registerRoutesObserver(any())
            } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult)
            }
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraComponent.onAttached(mockMapboxNavigation)
            mockMapboxNavigation.setNavigationRoutes(mockNavigationRoute)

            verify {
                mockViewPortDataSource.onRouteChanged(mockNavigationRoute.first())
                mockViewPortDataSource.evaluate()
            }
            assertEquals(TargetCameraMode.Following, cameraViewModel.state.value.cameraMode)
        }

    @Test
    fun `when route updates and is empty camera viewport clear`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            val mockNavigationRoute: List<NavigationRoute> = listOf()
            val mockRoutesUpdatedResult: RoutesUpdatedResult = mockk {
                every { navigationRoutes } returns mockNavigationRoute
            }
            every {
                mockMapboxNavigation.registerRoutesObserver(any())
            } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult)
            }
            cameraComponent.onAttached(mockMapboxNavigation)
            mockMapboxNavigation.setNavigationRoutes(mockNavigationRoute)

            verify {
                mockViewPortDataSource.clearRouteData()
                mockViewPortDataSource.evaluate()
            }
        }

    @Test
    fun `when route is ready and navigation state is active guidance camera starts in following`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            val mockNavigationRoute: List<NavigationRoute> = listOf(
                mockk {
                    every { directionsRoute } returns mockk()
                },
                mockk()
            )
            val mockRoutesUpdatedResult: RoutesUpdatedResult = mockk {
                every { navigationRoutes } returns mockNavigationRoute
            }
            every {
                mockMapboxNavigation.registerRoutesObserver(any())
            } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult)
            }
            cameraViewModel.onAttached(mockMapboxNavigation)
            cameraComponent.onAttached(mockMapboxNavigation)
            locationViewModel.onAttached(mockMapboxNavigation)
            navigationStateViewModel.onAttached(mockMapboxNavigation)
            locationViewModel.invoke(LocationAction.Update(locMatcherResult))
            navigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.ActiveNavigation)
            )
            cameraComponent.onDetached(mockMapboxNavigation)
            cameraViewModel.onAttached(mockMapboxNavigation)

            mockMapboxNavigation.setNavigationRoutes(mockNavigationRoute)

            assertEquals(TargetCameraMode.Following, cameraViewModel.state.value.cameraMode)
        }

    @Test
    fun `when called detach route updates should not happen`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            val mockNavigationRoute: List<NavigationRoute> = listOf(
                mockk {
                    every { directionsRoute } returns mockk()
                },
                mockk()
            )
            every {
                mockMapboxNavigation.registerRoutesObserver(any())
            } just Runs
            cameraComponent.onAttached(mockMapboxNavigation)
            cameraComponent.onDetached(mockMapboxNavigation)

            mockMapboxNavigation.setNavigationRoutes(mockNavigationRoute)

            verify(exactly = 0) {
                mockViewPortDataSource.onRouteChanged(mockNavigationRoute.first())
            }
        }

    @Test
    fun `when called detach route progress updates should not happen`() =
        coroutineRule.runBlockingTest {
            val mockMapboxNavigation = mockMapboxNavigation()
            every {
                mockMapboxNavigation.registerRouteProgressObserver(any())
            } just Runs
            cameraComponent.onAttached(mockMapboxNavigation)
            cameraComponent.onDetached(mockMapboxNavigation)

            mockMapboxNavigation.startTripSession()

            verify(exactly = 0) {
                mockViewPortDataSource.onRouteProgressChanged(any())
            }
        }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }
}

private fun makeLocationMatcherResult(lon: Double, lat: Double, bearing: Float) =
    mockk<LocationMatcherResult> {
        every { enhancedLocation } returns mockk {
            every { longitude } returns lon
            every { latitude } returns lat
            every { this@mockk.bearing } returns bearing
        }
    }
