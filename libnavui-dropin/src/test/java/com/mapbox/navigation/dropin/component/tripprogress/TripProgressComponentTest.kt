package com.mapbox.navigation.dropin.component.tripprogress

import android.os.Build
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
@ExperimentalPreviewMapboxNavigationAPI
@ExperimentalCoroutinesApi
class TripProgressComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var store: TestStore

    @Before
    fun setUp() {
        store = spyk(TestStore())
    }

    @Test
    fun `onAttached renders trip progress with route preview`() = coroutineRule.runBlockingTest {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        val mockRouteLeg = mockk<RouteLeg>(relaxed = true) {
            every { duration() } returns 100.0
            every { distance() } returns 200.0
        }

        val mockDirectionsRoute = mockk<DirectionsRoute>(relaxed = true) {
            every { duration() } returns 100.0
            every { distance() } returns 200.0
            every { legs() } returns listOf(mockRouteLeg)
        }

        val navigationRoute = mockk<NavigationRoute>(relaxed = true) {
            every { directionsRoute } returns mockDirectionsRoute
        }

        val tripProgressView = mockk<MapboxTripProgressView>(relaxed = true)

        store.setState(
            State(
                previewRoutes = RoutePreviewState.Ready(
                    listOf(navigationRoute)
                )
            )
        )

        TripProgressComponent(
            store,
            R.style.DropInStyleTripProgressView,
            DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
            tripProgressView
        ).onAttached(mapboxNavigation)

        verify { tripProgressView.renderTripOverview(any()) }
    }

    @Test
    fun `onAttached renders trip progress with route progress`() = coroutineRule.runBlockingTest {
        mockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationEx")
        val routeProgress = mockk<RouteProgress> {
            every { durationRemaining } returns 600.0
            every { distanceRemaining } returns 100f
            every { currentLegProgress } returns mockk {
                every { durationRemaining } returns 2.0
            }
            every { distanceTraveled } returns 50f
            every { route } returns mockk {
                every { currentState } returns RouteProgressState.TRACKING
            }
        }
        val tripProgressView = mockk<MapboxTripProgressView>(relaxed = true)
        val mapboxNavigation = mockk<MapboxNavigation> {
            every { flowRouteProgress() } returns flowOf(routeProgress)
        }

        store.setState(State(previewRoutes = RoutePreviewState.Ready(mockk(relaxed = true))))

        TripProgressComponent(
            store,
            R.style.DropInStyleTripProgressView,
            DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
            tripProgressView
        ).onAttached(mapboxNavigation)

        verify { tripProgressView.render(any()) }
        unmockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationEx")
    }
}
