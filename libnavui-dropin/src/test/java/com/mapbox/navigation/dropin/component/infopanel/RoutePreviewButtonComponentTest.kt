package com.mapbox.navigation.dropin.component.infopanel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.dropin.util.TestingUtil
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    var coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore
    private lateinit var button: MapboxExtendableButton
    private lateinit var buttonStyle: MutableStateFlow<Int>
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: RoutePreviewButtonComponent

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        mapboxNavigation = mockk(relaxed = true)
        store = spyk(TestStore())
        button = spyk(MapboxExtendableButton(context))
        buttonStyle = MutableStateFlow(R.style.DropInStylePreviewButton)

        sut = RoutePreviewButtonComponent(store, button, buttonStyle)
    }

    @Test
    fun `should observe and update button style`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)

        buttonStyle.value = R.style.DropInStyleRecenterButton

        verifyOrder {
            button.updateStyle(R.style.DropInStylePreviewButton)
            button.updateStyle(R.style.DropInStyleRecenterButton)
        }
    }

    @Test
    fun `onClick routePreview should FetchPoints`() = runBlockingTest {
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
                previewRoutes = RoutePreviewState.Empty
            )
        )

        sut.onAttached(mockk())
        button.performClick()

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
                val action = RoutePreviewAction.FetchPoints(listOf(origPoint, destPoint))
                store.dispatch(action)
            }
        }
}
