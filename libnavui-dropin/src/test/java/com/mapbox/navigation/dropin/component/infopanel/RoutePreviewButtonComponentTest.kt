package com.mapbox.navigation.dropin.component.infopanel

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxExtendableButtonParams
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.internal.extensions.recreateButton
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RouteOptionsProvider
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutePreviewButtonComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val layoutParams = mockk<LinearLayout.LayoutParams>(relaxed = true)
    private val initialParams =
        MapboxExtendableButtonParams(R.style.DropInStyleExitButton, layoutParams)
    private val updatedParams =
        MapboxExtendableButtonParams(R.style.DropInStyleRecenterButton, layoutParams)

    private val store = spyk(TestStore())
    private val button = spyk(MapboxExtendableButton(ApplicationProvider.getApplicationContext()))
    private val buttonContainer = mockk<ViewGroup>(relaxed = true)
    private val buttonParams = MutableStateFlow(initialParams)
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val routeOptionsProvider = mockk<RouteOptionsProvider>()
    private val sut =
        RoutePreviewButtonComponent(store, buttonContainer, buttonParams, routeOptionsProvider)

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.dropin.internal.extensions.ViewGroupExKt")
        every { buttonContainer.recreateButton(any()) } returns button
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `should observe params and recreate button`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)
        buttonParams.value = updatedParams

        verifyOrder {
            buttonContainer.recreateButton(initialParams)
            buttonContainer.recreateButton(updatedParams)
        }
    }

    @Test
    fun `onClick routePreview should FetchOptions`() = runBlockingTest {
        val origPoint = Point.fromLngLat(1.0, 2.0)
        val destPoint = Point.fromLngLat(2.0, 3.0)
        val options = mockk<RouteOptions>()
        every {
            routeOptionsProvider.getOptions(mapboxNavigation, origPoint, destPoint)
        } returns options
        store.setState(
            State(
                location = TestingUtil.makeLocationMatcherResult(
                    origPoint.longitude(),
                    origPoint.latitude(),
                    0f
                ),
                destination = Destination(destPoint),
                navigation = NavigationState.DestinationPreview,
                previewRoutes = RoutePreviewState.Empty
            )
        )

        sut.onAttached(mapboxNavigation)
        button.performClick()

        verify(exactly = 1) {
            store.dispatch(RoutePreviewAction.FetchOptions(options))
        }
    }

    @Test
    fun `onClick routePreview should NOT FetchOptions when already in Ready state`() =
        runBlockingTest {
            val origPoint = Point.fromLngLat(1.0, 2.0)
            val destPoint = Point.fromLngLat(2.0, 3.0)
            store.setState(
                State(
                    location = TestingUtil.makeLocationMatcherResult(
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
            button.performClick()

            verify(exactly = 0) {
                store.dispatch(ofType<RoutePreviewAction.FetchOptions>())
            }
        }
}
