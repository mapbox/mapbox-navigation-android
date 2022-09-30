package com.mapbox.navigation.dropin.map.longpress

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.util.HapticFeedback
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.makeLocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.routefetch.RouteOptionsProvider
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.utils.internal.toPoint
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class RoutePreviewLongPressMapComponentTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val enableOnMapLongClick = MutableStateFlow(value = true)
    private val mockGesturesPlugin: GesturesPlugin = mockk(relaxed = true)
    private val mockRouteOptionsProvider = mockk<RouteOptionsProvider>()
    private val mockMapView: MapView = mockk {
        every { gestures } returns mockGesturesPlugin
    }
    private val mockMapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { navigationOptions } returns mockk {
            every { applicationContext } returns mockk(relaxed = true)
        }
    }
    private val navigationViewContext: NavigationViewContext = mockk(relaxed = true) {
        every { options } returns mockk(relaxed = true) {
            every { enableMapLongClickIntercept } returns enableOnMapLongClick.asStateFlow()
        }
        every { routeOptionsProvider } returns mockRouteOptionsProvider
    }

    private val testStore = spyk(TestStore())
    private val sut =
        RoutePreviewLongPressMapComponent(testStore, mockMapView, navigationViewContext)

    @Before
    fun setUp() {
        mockkObject(HapticFeedback)
        every { HapticFeedback.create(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkObject(HapticFeedback)
    }

    @Test
    fun `should register OnMapLongClickListener in onAttached`() {
        sut.onAttached(mockMapboxNavigation)

        verify { mockGesturesPlugin.addOnMapLongClickListener(any()) }
    }

    @Test
    fun `should unregister OnMapLongClickListener in onDetached`() {
        sut.onAttached(mockMapboxNavigation)

        sut.onDetached(mockMapboxNavigation)

        verify { mockGesturesPlugin.removeOnMapLongClickListener(any()) }
    }

    @Test
    fun `onMapLongClick should do nothing if location is unknown`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        sut.onAttached(mockMapboxNavigation)

        val point = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(point)

        verify(exactly = 0) {
            testStore.dispatch(any())
        }
    }

    @Test
    fun `onMapLongClick should update view model state when long click intercept is enabled`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        val locationMatcherResult = makeLocationMatcherResult(21.0, 22.0, 0f)
        testStore.setState(State(location = locationMatcherResult))
        sut.onAttached(mockMapboxNavigation)

        val origin = locationMatcherResult.enhancedLocation.toPoint()
        val clickPoint = Point.fromLngLat(11.0, 12.0)
        val options = mockk<RouteOptions>()
        every {
            mockRouteOptionsProvider.getOptions(mockMapboxNavigation, origin, clickPoint)
        } returns options
        slot.captured.onMapLongClick(clickPoint)

        verifyOrder {
            testStore.dispatch(DestinationAction.SetDestination(Destination(clickPoint)))
            testStore.dispatch(RoutePreviewAction.FetchOptions(options))
        }
    }

    @Test
    fun `onMapLongClick should not update state when long click intercept is disabled`() {
        enableOnMapLongClick.value = false
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        val locationMatcherResult = makeLocationMatcherResult(21.0, 22.0, 0f)
        testStore.setState(State(location = locationMatcherResult))
        sut.onAttached(mockMapboxNavigation)

        val clickPoint = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(clickPoint)

        verify(exactly = 0) {
            testStore.dispatch(DestinationAction.SetDestination(Destination(clickPoint)))
            testStore.dispatch(ofType<RoutePreviewAction.FetchOptions>())
        }
    }
}
