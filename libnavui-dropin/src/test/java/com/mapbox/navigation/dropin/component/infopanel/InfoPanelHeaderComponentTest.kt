package com.mapbox.navigation.dropin.component.infopanel

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.destination.DestinationState
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderLayoutBinding
import com.mapbox.navigation.dropin.model.Destination
import com.mapbox.navigation.dropin.testutil.DispatchRegistry
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    private lateinit var ctx: Context
    private lateinit var sut: InfoPanelHeaderComponent
    private lateinit var binding: MapboxInfoPanelHeaderLayoutBinding
    private lateinit var dispatchRegistry: DispatchRegistry
    private lateinit var navigationState: MutableStateFlow<NavigationState>
    private lateinit var destinationState: MutableStateFlow<DestinationState>

    @MockK
    lateinit var mockNavContext: DropInNavigationViewContext

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        ctx = ApplicationProvider.getApplicationContext()
        binding = MapboxInfoPanelHeaderLayoutBinding.inflate(
            ctx.getSystemService(LayoutInflater::class.java),
            FrameLayout(ctx)
        )

        dispatchRegistry = DispatchRegistry()
        navigationState = MutableStateFlow(NavigationState.FreeDrive)
        destinationState = MutableStateFlow(DestinationState())
        every { mockNavContext.dispatch } returns { dispatchRegistry(it) }
        every { mockNavContext.navigationState } returns navigationState
        every { mockNavContext.destinationState } returns destinationState

        sut = InfoPanelHeaderComponent(binding, mockNavContext)
    }

    @Test
    fun `should update views visibility for FreeDrive state`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockk())

            assertVisible("poiName", binding.poiName)
            assertVisible("routePreview", binding.routePreview)
            assertVisible("startNavigation", binding.startNavigation)
            assertGone("endNavigation", binding.endNavigation)
            assertGone("tripProgressLayout", binding.tripProgressLayout)
            assertGone("arrivedText", binding.arrivedText)
        }

    @Test
    fun `should update views visibility for RoutePreview state`() =
        coroutineRule.runBlockingTest {
            navigationState.value = NavigationState.RoutePreview

            sut.onAttached(mockk())

            assertGone("poiName", binding.poiName)
            assertGone("routePreview", binding.routePreview)
            assertVisible("startNavigation", binding.startNavigation)
            assertGone("endNavigation", binding.endNavigation)
            assertVisible("tripProgressLayout", binding.tripProgressLayout)
            assertGone("arrivedText", binding.arrivedText)
        }

    @Test
    fun `should update views visibility for ActiveNavigation state`() =
        coroutineRule.runBlockingTest {
            navigationState.value = NavigationState.ActiveNavigation

            sut.onAttached(mockk())

            assertGone("poiName", binding.poiName)
            assertGone("routePreview", binding.routePreview)
            assertGone("startNavigation", binding.startNavigation)
            assertVisible("endNavigation", binding.endNavigation)
            assertVisible("tripProgressLayout", binding.tripProgressLayout)
            assertGone("arrivedText", binding.arrivedText)
        }

    @Test
    fun `should update views visibility for Arrival state`() =
        coroutineRule.runBlockingTest {
            navigationState.value = NavigationState.Arrival

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
        val featurePlaceName = "POI NAME"
        val newDestination = Destination(
            Point.fromLngLat(1.0, 2.0),
            listOf(
                mockk {
                    every { placeName() } returns featurePlaceName
                }
            )
        )
        sut.onAttached(mockk())

        destinationState.tryEmit(DestinationState(newDestination))

        assertEquals(binding.poiName.text, featurePlaceName)
    }

    @Test
    fun `onClick routePreview should dispatch FetchAndSetRoute action`() {
        sut.onAttached(mockk())

        binding.routePreview.performClick()

        dispatchRegistry.verifyDispatched(RoutesAction.FetchAndSetRoute)
    }

    @Test
    fun `onClick startNavigation should dispatch StartNavigation action`() {
        sut.onAttached(mockk())

        binding.startNavigation.performClick()

        dispatchRegistry.verifyDispatched(RoutesAction.StartNavigation)
    }

    @Test
    fun `onClick endNavigation should dispatch StopNavigation action`() {
        sut.onAttached(mockk())

        binding.endNavigation.performClick()

        dispatchRegistry.verifyDispatched(RoutesAction.StopNavigation)
    }
}

private fun assertVisible(name: String, view: View) =
    assertTrue("$name should be VISIBLE", view.isVisible)

private fun assertGone(name: String, view: View) =
    assertFalse("$name should be GONE", view.isVisible)
