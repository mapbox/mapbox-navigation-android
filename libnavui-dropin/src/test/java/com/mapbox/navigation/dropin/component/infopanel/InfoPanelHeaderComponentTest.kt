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
import com.mapbox.navigation.dropin.databinding.MapboxInfoPanelHeaderLayoutBinding
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil.makeLocationMatcherResult
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private lateinit var store: TestStore
    private lateinit var sut: InfoPanelHeaderComponent

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        binding = MapboxInfoPanelHeaderLayoutBinding.inflate(
            context.getSystemService(LayoutInflater::class.java),
            FrameLayout(context)
        )
        store = spyk(TestStore())
        sut = InfoPanelHeaderComponent(
            store,
            binding,
            R.style.DropInStylePreviewButton,
            R.style.DropInStyleExitButton,
            R.style.DropInStyleStartButton
        )
    }

    @Test
    fun `should update views visibility for FreeDrive state`() = runBlockingTest {
        store.setState(
            State(
                navigation = NavigationState.FreeDrive
            )
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
        store.setState(
            State(
                navigation = NavigationState.DestinationPreview
            )
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
        store.setState(
            State(
                navigation = NavigationState.RoutePreview
            )
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
        store.setState(
            State(
                navigation = NavigationState.ActiveNavigation
            )
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
        store.setState(
            State(
                navigation = NavigationState.Arrival
            )
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
        store.setState(
            State(
                destination = newDestination
            )
        )

        assertEquals(binding.poiName.text, featurePlaceName)
    }

    @Test
    fun `onClick routePreview should FetchPoints`() = runBlockingTest {
        val origPoint = Point.fromLngLat(1.0, 2.0)
        val destPoint = Point.fromLngLat(2.0, 3.0)
        store.setState(
            State(
                location = makeLocationMatcherResult(
                    origPoint.longitude(),
                    origPoint.latitude(),
                    0f
                ),
                destination = Destination(destPoint),
                navigation = NavigationState.DestinationPreview,
                previewRoutes = RoutePreviewState.Empty
            )
        )

        sut.onAttached(mockk())
        binding.routePreview.performClick()

        verify(exactly = 1) {
            val action = RoutePreviewAction.FetchPoints(listOf(origPoint, destPoint))
            store.dispatch(action)
        }
    }

    @Test
    fun `onClick routePreview should NOT FetchPoints when already in Ready state`() =
        runBlockingTest {
            val origPoint = Point.fromLngLat(1.0, 2.0)
            val destPoint = Point.fromLngLat(2.0, 3.0)
            store.setState(
                State(
                    location = makeLocationMatcherResult(
                        origPoint.longitude(),
                        origPoint.latitude(),
                        0f
                    ),
                    destination = Destination(destPoint),
                    navigation = NavigationState.DestinationPreview,
                    previewRoutes = mockk<RoutePreviewState.Ready> {
                        every { routes } returns mockk()
                    }
                )
            )

            sut.onAttached(mockk())
            binding.routePreview.performClick()

            verify(exactly = 0) {
                val action = RoutePreviewAction.FetchPoints(listOf(origPoint, destPoint))
                store.dispatch(action)
            }
        }

    @Test
    fun `onClick startNavigation start ActiveNavigation`() = runBlockingTest {
        val origPoint = Point.fromLngLat(1.0, 2.0)
        val destPoint = Point.fromLngLat(2.0, 3.0)
        store.setState(
            State(
                location = makeLocationMatcherResult(
                    origPoint.longitude(),
                    origPoint.latitude(),
                    0f
                ),
                destination = Destination(destPoint),
                navigation = NavigationState.DestinationPreview,
                previewRoutes = mockk<RoutePreviewState.Ready> {
                    every { routes } returns mockk()
                }
            )
        )

        sut.onAttached(mockk())
        binding.startNavigation.performClick()

        verify {
            store.dispatch(NavigationStateAction.Update(NavigationState.ActiveNavigation))
        }
    }

    @Test
    fun `onClick startNavigation should NOT FetchPoints when already in Ready state`() =
        runBlockingTest {
            val origPoint = Point.fromLngLat(1.0, 2.0)
            val destPoint = Point.fromLngLat(2.0, 3.0)
            store.setState(
                State(
                    location = makeLocationMatcherResult(
                        origPoint.longitude(),
                        origPoint.latitude(),
                        0f
                    ),
                    destination = Destination(destPoint),
                    navigation = NavigationState.DestinationPreview,
                    previewRoutes = mockk<RoutePreviewState.Ready> {
                        every { routes } returns mockk()
                    }
                )
            )

            sut.onAttached(mockk())
            binding.startNavigation.performClick()

            verify(exactly = 0) {
                val action = RoutePreviewAction.FetchPoints(listOf(origPoint, destPoint))
                store.dispatch(action)
            }
        }
}

private fun assertVisible(name: String, view: View) =
    assertTrue("$name should be VISIBLE", view.isVisible)

private fun assertGone(name: String, view: View) =
    assertFalse("$name should be GONE", view.isVisible)
