package com.mapbox.navigation.dropin.component.infopanel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.destination.Destination
import com.mapbox.navigation.dropin.component.destination.DestinationState
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesState
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderLayoutBinding
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class InfoPanelHeaderComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var binding: MapboxInfoPanelHeaderLayoutBinding

    private val mockNavigationStateViewModel: NavigationStateViewModel = mockk(relaxed = true) {
        every { state } returns MutableStateFlow(mockk(relaxed = true))
    }
    private val mockDestinationViewModel: DestinationViewModel = mockk(relaxed = true) {
        every { state } returns MutableStateFlow(mockk(relaxed = true))
    }
    private val mockLocationViewModel: LocationViewModel = mockk(relaxed = true) {
        every { state } returns MutableStateFlow(mockk(relaxed = true))
    }
    private val mockRoutesViewModel: RoutesViewModel = mockk(relaxed = true) {
        every { state } returns MutableStateFlow(mockk(relaxed = true))
    }

    private lateinit var sut: InfoPanelHeaderComponent

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        binding = MapboxInfoPanelHeaderLayoutBinding.inflate(
            context.getSystemService(LayoutInflater::class.java),
            FrameLayout(context)
        )

        sut = InfoPanelHeaderComponent(
            binding,
            mockNavigationStateViewModel,
            mockDestinationViewModel,
            mockLocationViewModel,
            mockRoutesViewModel,
            R.style.DropInStylePreviewButton,
            R.style.DropInStyleExitButton,
            R.style.DropInStyleStartButton
        )
    }

    @Test
    fun `should update views visibility for FreeDrive state`() = runBlockingTest {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.FreeDrive
        )

        sut.onAttached(mockk())

        assertGone("poiName", binding.poiName)
        assertGone("routePreview", binding.routePreview)
        assertGone("startNavigation", binding.startNavigation)
        assertGone("endNavigation", binding.endNavigation)
        assertGone("tripProgressLayout", binding.tripProgressLayout)
        assertGone("arrivedText", binding.arrivedText)
    }

    @Test
    fun `should update views visibility for DestinationPreview state`() = runBlockingTest {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.DestinationPreview
        )

        sut.onAttached(mockk())

        assertVisible("poiName", binding.poiName)
        assertVisible("routePreview", binding.routePreview)
        assertVisible("startNavigation", binding.startNavigation)
        assertGone("endNavigation", binding.endNavigation)
        assertGone("tripProgressLayout", binding.tripProgressLayout)
        assertGone("arrivedText", binding.arrivedText)
    }

    @Test
    fun `should update views visibility for RoutePreview state`() = runBlockingTest {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.RoutePreview
        )

        sut.onAttached(mockk())

        assertGone("poiName", binding.poiName)
        assertGone("routePreview", binding.routePreview)
        assertVisible("startNavigation", binding.startNavigation)
        assertGone("endNavigation", binding.endNavigation)
        assertVisible("tripProgressLayout", binding.tripProgressLayout)
        assertGone("arrivedText", binding.arrivedText)
    }

    @Test
    fun `should update views visibility for ActiveNavigation state`() = runBlockingTest {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.ActiveNavigation
        )

        sut.onAttached(mockk())

        assertGone("poiName", binding.poiName)
        assertGone("routePreview", binding.routePreview)
        assertGone("startNavigation", binding.startNavigation)
        assertVisible("endNavigation", binding.endNavigation)
        assertVisible("tripProgressLayout", binding.tripProgressLayout)
        assertGone("arrivedText", binding.arrivedText)
    }

    @Test
    fun `should update views visibility for Arrival state`() = runBlockingTest {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.Arrival
        )

        sut.onAttached(mockk())

        assertGone("poiName", binding.poiName)
        assertGone("routePreview", binding.routePreview)
        assertGone("startNavigation", binding.startNavigation)
        assertVisible("endNavigation", binding.endNavigation)
        assertGone("tripProgressLayout", binding.tripProgressLayout)
        assertVisible("arrivedText", binding.arrivedText)
    }

    @Test
    fun `should update poiName text`() {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(mockk(relaxed = true))

        val featurePlaceName = "POI NAME"
        val newDestination = Destination(
            Point.fromLngLat(1.0, 2.0),
            listOf(
                mockk {
                    every { placeName() } returns featurePlaceName
                }
            )
        )
        val destinationState = MutableStateFlow(DestinationState())
        every { mockDestinationViewModel.state } returns destinationState

        sut.onAttached(mockk())
        destinationState.tryEmit(DestinationState(newDestination))

        assertEquals(binding.poiName.text, featurePlaceName)
    }

    @Test
    fun `onClick routePreview should FetchPoints`() = runBlockingTest {
        val lastPoint = Point.fromLngLat(1.0, 2.0)
        val destination = Destination(Point.fromLngLat(2.0, 3.0))
        every { mockLocationViewModel.lastPoint } returns lastPoint
        every { mockNavigationStateViewModel.state } returns
            MutableStateFlow(NavigationState.DestinationPreview)
        every { mockDestinationViewModel.state } returns
            MutableStateFlow(DestinationState(destination))
        every { mockRoutesViewModel.state } returns
            MutableStateFlow(RoutesState.Empty)

        sut.onAttached(mockk())
        binding.routePreview.performClick()

        verify(exactly = 1) {
            val action = RoutesAction.FetchPoints(listOf(lastPoint, destination.point))
            mockRoutesViewModel.invoke(action)
        }
    }

    @Test
    fun `onClick routePreview should NOT FetchPoints when already in Ready state`() =
        runBlockingTest {
            val lastPoint = Point.fromLngLat(1.0, 2.0)
            val destination = Destination(Point.fromLngLat(2.0, 3.0))
            every { mockLocationViewModel.lastPoint } returns lastPoint
            every { mockNavigationStateViewModel.state } returns
                MutableStateFlow(NavigationState.DestinationPreview)
            every { mockDestinationViewModel.state } returns
                MutableStateFlow(DestinationState(destination))
            every { mockRoutesViewModel.state } returns
                MutableStateFlow(RoutesState.Ready(mockk()))

            sut.onAttached(mockk())
            binding.routePreview.performClick()

            verify(exactly = 0) {
                val action = RoutesAction.FetchPoints(listOf(lastPoint, destination.point))
                mockRoutesViewModel.invoke(action)
            }
        }

    @Test
    fun `onClick startNavigation start ActiveNavigation`() = runBlockingTest {
        every { mockNavigationStateViewModel.state } returns MutableStateFlow(
            NavigationState.DestinationPreview
        )
        every { mockLocationViewModel.lastPoint } returns mockk()
        every { mockRoutesViewModel.state } returns MutableStateFlow(RoutesState.Ready(mockk()))
        every { mockRoutesViewModel.invoke(any()) } returns RoutesState.Ready(mockk())

        sut.onAttached(mockk())
        binding.startNavigation.performClick()

        verify {
            mockNavigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.ActiveNavigation)
            )
        }
    }

    @Test
    fun `onClick startNavigation should NOT FetchPoints when already in Ready state`() =
        runBlockingTest {
            val lastPoint = Point.fromLngLat(1.0, 2.0)
            val destination = Destination(Point.fromLngLat(2.0, 3.0))
            every { mockLocationViewModel.lastPoint } returns lastPoint
            every { mockNavigationStateViewModel.state } returns
                MutableStateFlow(NavigationState.DestinationPreview)
            every { mockDestinationViewModel.state } returns
                MutableStateFlow(DestinationState(destination))
            every { mockRoutesViewModel.state } returns
                MutableStateFlow(RoutesState.Ready(mockk()))

            sut.onAttached(mockk())
            binding.startNavigation.performClick()

            verify(exactly = 0) {
                val action = RoutesAction.FetchPoints(listOf(lastPoint, destination.point))
                mockRoutesViewModel.invoke(action)
            }
        }
}

private fun assertVisible(name: String, view: View) =
    assertTrue("$name should be VISIBLE", view.isVisible)

private fun assertGone(name: String, view: View) =
    assertFalse("$name should be GONE", view.isVisible)
