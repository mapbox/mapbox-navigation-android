package com.mapbox.navigation.dropin.component.marker

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.util.HapticFeedback
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
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
internal class FreeDriveLongPressMapComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val enableOnMapLongClick = MutableStateFlow<Boolean>(true)
    private val mockGesturesPlugin: GesturesPlugin = mockk(relaxed = true)
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
    }

    lateinit var sut: FreeDriveLongPressMapComponent
    private lateinit var testStore: TestStore

    @Before
    fun setUp() {
        mockkObject(HapticFeedback)
        every { HapticFeedback.create(any()) } returns mockk(relaxed = true)
        testStore = spyk(TestStore())

        sut = FreeDriveLongPressMapComponent(testStore, mockMapView, navigationViewContext)
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
    fun `onMapLongClick should update view model state when long click intercept is enabled`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        sut.onAttached(mockMapboxNavigation)

        val point = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(point)

        verifyOrder {
            testStore.dispatch(DestinationAction.SetDestination(Destination(point)))
            testStore.dispatch(
                NavigationStateAction.Update(NavigationState.DestinationPreview)
            )
        }
    }

    @Test
    fun `onMapLongClick should not update state when long click intercept is disabled`() {
        enableOnMapLongClick.value = false
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        sut.onAttached(mockMapboxNavigation)

        val point = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(point)

        verify(exactly = 0) {
            testStore.dispatch(DestinationAction.SetDestination(Destination(point)))
            testStore.dispatch(
                NavigationStateAction.Update(NavigationState.DestinationPreview)
            )
        }
    }
}
