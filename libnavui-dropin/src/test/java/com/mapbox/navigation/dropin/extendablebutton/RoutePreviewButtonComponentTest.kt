package com.mapbox.navigation.dropin.extendablebutton

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
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
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
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

    private lateinit var store: TestStore
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var routeOptionsProvider: RouteOptionsProvider
    private lateinit var button: MapboxExtendableButton
    private lateinit var sut: RoutePreviewButtonComponent

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = spyk(TestStore())
        mapboxNavigation = mockk(relaxed = true)
        routeOptionsProvider = mockk()
        button = spyk(MapboxExtendableButton(context))

        sut = RoutePreviewButtonComponent(store, routeOptionsProvider, button)
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
