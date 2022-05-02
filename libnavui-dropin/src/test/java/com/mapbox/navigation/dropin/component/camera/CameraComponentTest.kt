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
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.makeLocationMatcherResult
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
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
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
    private lateinit var mockMapboxNavigation: MapboxNavigation
    private lateinit var cameraComponent: CameraComponent
    private lateinit var testStore: TestStore

    @Before
    fun setUp() {
        mockkStatic(Utils::class, Logger::class)
        mockkObject(MapboxNavigationApp)
        every { Utils.dpToPx(any()) } returns 50f
        every { Logger.d(any(), any()) } returns Unit
        mockMapboxNavigation = mockk(relaxed = true)
        every { MapboxNavigationApp.current() } returns mockMapboxNavigation
        testStore = spyk(TestStore())

        cameraComponent = CameraComponent(
            testStore,
            mockMapView,
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
        testStore.setState(
            testStore.state.value.copy(
                camera = CameraState(
                    mapCameraState = cameraState
                )
            )
        )

        cameraComponent.onAttached(mockMapboxNavigation)

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

        cameraComponent.onAttached(mockMapboxNavigation)
        cameraComponent.onDetached(mockMapboxNavigation)

        verify { testStore.dispatch(CameraAction.SaveMapState(cameraState)) }
    }

    @Test
    fun `when location update is received first time in free drive camera frame is updated`() =
        coroutineRule.runBlockingTest {
            testStore.setState(
                State(
                    location = locMatcherResult,
                    navigation = NavigationState.FreeDrive
                )
            )
            cameraComponent.onAttached(mockMapboxNavigation)

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
            testStore.setState(
                testStore.state.value.copy(
                    navigation = NavigationState.ActiveNavigation,
                    location = locMatcherResult
                )
            )
            cameraComponent.onAttached(mockMapboxNavigation)

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
            cameraComponent.onAttached(mockMapboxNavigation)
            testStore.setState(testStore.state.value.copy(location = locMatcherResult))
            testStore.setState(testStore.state.value.copy(location = nextLocMatcherResult))

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
            cameraComponent.onAttached(mockMapboxNavigation)

            verify(exactly = 0) {
                mockNavigationCamera.requestNavigationCameraToOverview()
            }
        }

    @Test
    fun `camera frame updates to idle when requested`() =
        coroutineRule.runBlockingTest {
            testStore.setState(
                State(
                    camera = CameraState(cameraMode = TargetCameraMode.Following),
                    location = locMatcherResult,
                    navigation = NavigationState.FreeDrive
                )
            )
            cameraComponent.onAttached(mockMapboxNavigation)

            testStore.setState(
                testStore.state.value.copy(
                    camera = CameraState(cameraMode = TargetCameraMode.Idle)
                )
            )

            verify(exactly = 1) {
                mockNavigationCamera.requestNavigationCameraToIdle()
            }
        }

    @Test
    fun `camera frame updates to following when requested`() =
        coroutineRule.runBlockingTest {
            testStore.setState(
                State(
                    camera = CameraState(cameraMode = TargetCameraMode.Idle),
                    location = locMatcherResult,
                    navigation = NavigationState.FreeDrive
                )
            )
            cameraComponent.onAttached(mockMapboxNavigation)

            testStore.setState(
                testStore.state.value.copy(
                    camera = CameraState(cameraMode = TargetCameraMode.Following)
                )
            )

            verify(atLeast = 1) {
                mockNavigationCamera.requestNavigationCameraToFollowing()
            }
        }

    @Test
    fun `when in free drive zoom and pitch property is overridden`() =
        coroutineRule.runBlockingTest {
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
            testStore.setState(
                testStore.state.value.copy(navigation = NavigationState.RoutePreview)
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
    fun `when route progress updates with empty list camera viewport should clear route data`() =
        coroutineRule.runBlockingTest {
            val slot = slot<RoutesObserver>()
            every { mockMapboxNavigation.registerRoutesObserver(capture(slot)) } returns Unit
            cameraComponent.onAttached(mockMapboxNavigation)
            testStore.setState(
                testStore.state.value.copy(
                    navigation = NavigationState.RoutePreview
                )
            )

            slot.captured.onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns emptyList()
                }
            )

            verify {
                mockViewPortDataSource.clearRouteData()
                mockViewPortDataSource.evaluate()
            }
        }

    @Test
    fun `when route progress updates in arrival camera mode should update`() =
        coroutineRule.runBlockingTest {
            val slot = slot<RoutesObserver>()
            every { mockMapboxNavigation.registerRoutesObserver(capture(slot)) } returns Unit
            cameraComponent.onAttached(mockMapboxNavigation)
            testStore.setState(
                testStore.state.value.copy(
                    navigation = NavigationState.Arrival
                )
            )

            slot.captured.onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns listOf(mockk())
                }
            )

            verify {
                testStore.dispatch(CameraAction.ToFollowing)
            }
        }

    @Test
    fun `when called detach route updates should not happen`() =
        coroutineRule.runBlockingTest {
            val slot = slot<RoutesObserver>()
            every { mockMapboxNavigation.registerRoutesObserver(capture(slot)) } returns Unit
            cameraComponent.onAttached(mockMapboxNavigation)
            testStore.setState(
                testStore.state.value.copy(
                    navigation = NavigationState.Arrival
                )
            )
            cameraComponent.onAttached(mockMapboxNavigation)
            cameraComponent.onDetached(mockMapboxNavigation)

            val change = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns listOf(mockk())
            }
            slot.captured.onRoutesChanged(change)

            verify(exactly = 0) {
                mockViewPortDataSource.onRouteChanged(any<NavigationRoute>())
            }
        }

    @Test
    fun `when called detach route progress updates should not happen`() =
        coroutineRule.runBlockingTest {
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
}
