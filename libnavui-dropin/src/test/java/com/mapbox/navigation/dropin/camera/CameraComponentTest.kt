package com.mapbox.navigation.dropin.camera

import com.mapbox.android.gestures.Utils
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
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.makeLocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.CameraAction.SetCameraMode
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
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
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CameraComponentTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockCamera: CameraAnimationsPlugin = mockk(relaxUnitFun = true) {
        every { addCameraAnimationsLifecycleListener(any()) } just Runs
    }
    private val cameraDebuggerSlot = mutableListOf<MapboxNavigationViewportDataSourceDebugger?>()
    private val mockNavigationCamera: NavigationCamera = mockk(relaxed = true) {
        every { debugger = captureNullable(cameraDebuggerSlot) } just Runs
    }
    private val viewPortDebuggerSlot = mutableListOf<MapboxNavigationViewportDataSourceDebugger?>()
    private val mockViewPortDataSource: MapboxNavigationViewportDataSource = mockk(relaxed = true) {
        every { debugger = captureNullable(viewPortDebuggerSlot) } just Runs
    }
    private val debuggerSlot = slot<Boolean>()
    private val mockDebugger: MapboxNavigationViewportDataSourceDebugger = mockk(relaxed = true) {
        every { enabled = capture(debuggerSlot) } just Runs
    }
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
    private lateinit var viewContext: NavigationViewContext
    private lateinit var testStore: TestStore

    @Before
    fun setUp() {
        mockkStatic(Utils::class)
        mockkObject(MapboxNavigationApp)
        every { Utils.dpToPx(any()) } returns 50f
        mockMapboxNavigation = mockk(relaxed = true)
        every { MapboxNavigationApp.current() } returns mockMapboxNavigation
        testStore = spyk(TestStore())

        viewContext = mockk(relaxed = true) {
            every { store } returns testStore
        }

        cameraComponent = CameraComponent(
            viewContext,
            mockMapView,
            mockViewPortDataSource,
            mockNavigationCamera,
            mockDebugger,
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
                camera = mockk(relaxed = true) {
                    every { mapCameraState } returns cameraState
                }
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
    fun `when location update is received first time overview camera frame is updated`() =
        coroutineRule.runBlockingTest {
            val initialCamera = testStore.state.value.camera.copy(TargetCameraMode.Overview)
            val initialState = State(camera = initialCamera, location = locMatcherResult)
            testStore.setState(initialState)
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
    fun `when location update is received first time following camera frame is updated`() =
        coroutineRule.runBlockingTest {
            val initialCamera = testStore.state.value.camera.copy(TargetCameraMode.Following)
            val initialState = State(camera = initialCamera, location = locMatcherResult)
            testStore.setState(initialState)
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
    fun `when location update is received first time idle camera frame is updated`() =
        coroutineRule.runBlockingTest {
            val initialCamera = testStore.state.value.camera.copy(TargetCameraMode.Idle)
            val initialState = State(camera = initialCamera, location = locMatcherResult)
            testStore.setState(initialState)
            cameraComponent.onAttached(mockMapboxNavigation)

            verify(exactly = 1) {
                mockNavigationCamera.requestNavigationCameraToIdle()
            }
        }

    @Test
    fun `camera frame is not updated on subsequent location updates`() =
        coroutineRule.runBlockingTest {
            val nextLocMatcherResult =
                makeLocationMatcherResult(-121.4567, 37.9876, 45f)
            val initialCamera = testStore.state.value.camera.copy(TargetCameraMode.Overview)
            val initialState = State(camera = initialCamera, location = locMatcherResult)
            testStore.setState(initialState)
            cameraComponent.onAttached(mockMapboxNavigation)
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
    fun `should update CameraState#cameraMode when NavigationCamera#state changes`() =
        coroutineRule.runBlockingTest {
            val initialState = State(
                camera = testStore.state.value.camera.copy(cameraMode = TargetCameraMode.Idle),
            )
            testStore.setState(initialState)
            val observer = slot<NavigationCameraStateChangedObserver>().also {
                every {
                    mockNavigationCamera.registerNavigationCameraStateChangeObserver(capture(it))
                } returns Unit
            }
            cameraComponent.onAttached(mockMapboxNavigation)
            observer.captured.onNavigationCameraStateChanged(NavigationCameraState.IDLE)

            mapOf(
                NavigationCameraState.TRANSITION_TO_FOLLOWING to TargetCameraMode.Following,
                NavigationCameraState.FOLLOWING to TargetCameraMode.Following,
                NavigationCameraState.TRANSITION_TO_OVERVIEW to TargetCameraMode.Overview,
                NavigationCameraState.OVERVIEW to TargetCameraMode.Overview,
                NavigationCameraState.IDLE to TargetCameraMode.Idle,
            ).forEach { (cameraState, mode) ->
                observer.captured.onNavigationCameraStateChanged(cameraState)

                verify { testStore.dispatch(SetCameraMode(mode)) }
            }
        }

    @Test
    fun `should update NavigationCamera#state when CameraState#cameraMode changes`() =
        coroutineRule.runBlockingTest {
            val cameraState = testStore.state.value.camera.copy(cameraMode = TargetCameraMode.Idle)
            testStore.setState(
                State(
                    camera = cameraState,
                    location = locMatcherResult,
                    navigation = NavigationState.FreeDrive
                )
            )
            cameraComponent.onAttached(mockMapboxNavigation)

            every { mockNavigationCamera.state } returns NavigationCameraState.IDLE
            testStore.setState(State(camera = cameraState.copy(TargetCameraMode.Following)))
            verify { mockNavigationCamera.requestNavigationCameraToFollowing() }

            every { mockNavigationCamera.state } returns NavigationCameraState.FOLLOWING
            testStore.setState(State(camera = cameraState.copy(TargetCameraMode.Overview)))
            verify { mockNavigationCamera.requestNavigationCameraToOverview() }

            every { mockNavigationCamera.state } returns NavigationCameraState.OVERVIEW
            testStore.setState(State(camera = cameraState.copy(TargetCameraMode.Idle)))
            verify { mockNavigationCamera.requestNavigationCameraToIdle() }
        }

    @Test
    fun `camera frame updates to following when requested`() =
        coroutineRule.runBlockingTest {
            testStore.setState(
                State(
                    camera = mockk(relaxed = true) {
                        every { cameraMode } returns TargetCameraMode.Idle
                    },
                    location = locMatcherResult,
                    navigation = NavigationState.FreeDrive
                )
            )
            cameraComponent.onAttached(mockMapboxNavigation)

            testStore.setState(
                testStore.state.value.copy(
                    camera = mockk(relaxed = true) {
                        every { cameraMode } returns TargetCameraMode.Following
                    },
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
    fun `when in mode other than free drive only zoom property override is cleared`() =
        coroutineRule.runBlockingTest {
            testStore.setState(
                testStore.state.value.copy(navigation = NavigationState.RoutePreview)
            )

            cameraComponent.onAttached(mockMapboxNavigation)

            verify {
                mockViewPortDataSource.overviewZoomPropertyOverride(value = null)
                mockViewPortDataSource.overviewPitchPropertyOverride(value = 0.0)
                mockViewPortDataSource.followingZoomPropertyOverride(value = null)
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
    fun `when route is set camera viewport should update route data`() =
        coroutineRule.runBlockingTest {
            val mockNavigationRoute = mockk<NavigationRoute>(relaxed = true)
            val slot = slot<RoutesObserver>()
            every { mockMapboxNavigation.registerRoutesObserver(capture(slot)) } just Runs
            cameraComponent.onAttached(mockMapboxNavigation)
            slot.captured.onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns listOf(mockNavigationRoute)
                },
            )
            verifyOrder {
                mockViewPortDataSource.onRouteChanged(mockNavigationRoute)
                mockViewPortDataSource.evaluate()
            }
        }

    @Test
    fun `when preview route is available camera viewport should update route data`() =
        coroutineRule.runBlockingTest {
            val mockNavigationRoute = mockk<NavigationRoute>(relaxed = true)
            cameraComponent.onAttached(mockMapboxNavigation)
            testStore.setState(
                testStore.state.value.copy(
                    previewRoutes = RoutePreviewState.Ready(listOf(mockNavigationRoute)),
                ),
            )
            verifyOrder {
                mockViewPortDataSource.onRouteChanged(mockNavigationRoute)
                mockViewPortDataSource.evaluate()
            }
        }

    @Test
    fun `when preview route is available camera is not changed in free drive mode`() =
        coroutineRule.runBlockingTest {
            val mockNavigationRoute = mockk<NavigationRoute>(relaxed = true)
            testStore.setState(
                testStore.state.value.copy(
                    navigation = NavigationState.FreeDrive,
                    previewRoutes = RoutePreviewState.Ready(
                        listOf(mockNavigationRoute)
                    )
                )
            )
            cameraComponent.onAttached(mockMapboxNavigation)
            verify(exactly = 0) {
                testStore.dispatch(ofType<SetCameraMode>())
            }
        }

    @Test
    fun `when preview route is not available camera viewport is clear of route data`() =
        coroutineRule.runBlockingTest {
            cameraComponent.onAttached(mockMapboxNavigation)
            testStore.setState(
                testStore.state.value.copy(
                    previewRoutes = RoutePreviewState.Empty,
                )
            )

            verify {
                mockViewPortDataSource.clearRouteData()
                mockViewPortDataSource.evaluate()
            }
        }

    @Test
    fun `when route is set camera is not changed in free drive mode`() =
        coroutineRule.runBlockingTest {
            val mockNavigationRoute = mockk<NavigationRoute>(relaxed = true)
            every { mockMapboxNavigation.registerRoutesObserver(any()) } returns Unit
            every { mockMapboxNavigation.getNavigationRoutes() } returns listOf(mockNavigationRoute)
            cameraComponent.onAttached(mockMapboxNavigation)
            testStore.setState(
                testStore.state.value.copy(
                    navigation = NavigationState.FreeDrive
                )
            )
            verify(exactly = 0) {
                testStore.dispatch(ofType<SetCameraMode>())
            }
        }

    @Test
    fun `when both preview and active routes are set, active route is preferred`() =
        coroutineRule.runBlockingTest {
            val previewNavigationRoute = mockk<NavigationRoute>(relaxed = true)
            val activeNavigationRoute = mockk<NavigationRoute>(relaxed = true)
            val slot = slot<RoutesObserver>()
            every { mockMapboxNavigation.registerRoutesObserver(capture(slot)) } returns Unit
            cameraComponent.onAttached(mockMapboxNavigation)
            testStore.setState(
                testStore.state.value.copy(
                    navigation = NavigationState.ActiveNavigation,
                    previewRoutes = RoutePreviewState.Ready(listOf(previewNavigationRoute)),
                ),
            )
            slot.captured.onRoutesChanged(
                mockk {
                    every { navigationRoutes } returns listOf(activeNavigationRoute)
                }
            )
            verifyOrder {
                mockViewPortDataSource.onRouteChanged(previewNavigationRoute)
                mockViewPortDataSource.onRouteChanged(activeNavigationRoute)
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

    @Test
    fun `when show camera debug info enabled debugger is set`() =
        coroutineRule.runBlockingTest {
            every { viewContext.options.showCameraDebugInfo } returns MutableStateFlow(true)

            cameraComponent.onAttached(mockMapboxNavigation)

            assertTrue(debuggerSlot.captured)
            assertEquals(cameraDebuggerSlot.first(), mockDebugger)
            assertEquals(viewPortDebuggerSlot.first(), mockDebugger)
        }

    @Test
    fun `when show camera debug info disabled debugger is reset`() =
        coroutineRule.runBlockingTest {
            every { viewContext.options.showCameraDebugInfo } returns MutableStateFlow(false)

            cameraComponent.onAttached(mockMapboxNavigation)

            assertFalse(debuggerSlot.captured)
            assertNull(cameraDebuggerSlot.first())
            assertNull(viewPortDebuggerSlot.first())
        }
}
