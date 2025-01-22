package com.mapbox.navigation.ui.components.tripprogress.internal.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.internal.TripProgressUpdateValueFactory.createRouteLegTripOverview
import com.mapbox.navigation.tripdata.progress.internal.TripProgressUpdateValueFactory.createTripOverviewValue
import com.mapbox.navigation.tripdata.progress.internal.TripProgressUpdateValueFactory.createTripProgressUpdateValue
import com.mapbox.navigation.tripdata.progress.model.TripOverviewError
import com.mapbox.navigation.tripdata.progress.model.TripOverviewValue
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.components.tripprogress.view.MapboxTripProgressView
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TripProgressComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val ctx = ApplicationProvider.getApplicationContext<Context>()
    private val tripProgressView = spyk(MapboxTripProgressView(ctx))
    private val tripProgressApi = mockk<MapboxTripProgressApi>(relaxed = true)
    private val previewRoutesFlow = MutableStateFlow<List<NavigationRoute>>(emptyList())
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val sut = TripProgressComponent(
        tripProgressView,
        {
            object : TripProgressComponentContract {
                override val previewRoutes = previewRoutesFlow
            }
        },
        TripProgressUpdateFormatter.Builder(ctx).build(),
        tripProgressApi,
    )

    @Test
    fun `onAttached should render trip overview when previewRoutes are not empty`() =
        coroutineRule.runBlockingTest {
            val navigationRoute = mockk<NavigationRoute>()
            previewRoutesFlow.value = listOf(navigationRoute)
            val tripDetails = createValue<TripOverviewError, TripOverviewValue>(tripOverviewValue())
            every { tripProgressApi.getTripDetails(navigationRoute) } returns tripDetails

            sut.onAttached(mapboxNavigation)

            verify(exactly = 1) { tripProgressView.renderTripOverview(tripDetails) }
        }

    @Test
    fun `onAttached should NOT render trip overview when previewRoutes are empty`() =
        coroutineRule.runBlockingTest {
            previewRoutesFlow.value = listOf()

            sut.onAttached(mapboxNavigation)

            verify(exactly = 0) { tripProgressView.renderTripOverview(any()) }
        }

    @Test
    fun `onAttached should render trip progress on RouteProgress change`() =
        coroutineRule.runBlockingTest {
            val routeProgress = mockk<RouteProgress>()
            val tripProgressUpdateValue = tripProgressUpdateValue()
            given(routeProgress = routeProgress)
            every { tripProgressApi.getTripProgress(routeProgress) } returns tripProgressUpdateValue

            sut.onAttached(mapboxNavigation)

            verify { tripProgressView.render(tripProgressUpdateValue) }
        }

    private fun tripOverviewValue() = createTripOverviewValue(
        routeLegTripDetail = listOf(
            createRouteLegTripOverview(
                legIndex = 0,
                legTime = 10.0,
                legDistance = 20.0,
                estimatedTimeToArrival = 10,
                arrivalTimeZone = null,
            ),
        ),
        totalTime = 10.0,
        totalDistance = 20.0,
        totalEstimatedTimeToArrival = 10,
        arrivalTimeZone = null,
        formatter = TripProgressUpdateFormatter.Builder(ctx).build(),
    )

    private fun tripProgressUpdateValue() = createTripProgressUpdateValue(
        estimatedTimeToArrival = 5,
        arrivalTimeZone = null,
        distanceRemaining = 20.0,
        currentLegTimeRemaining = 20.0,
        totalTimeRemaining = 5.0,
        percentRouteTraveled = 50.0,
        trafficCongestionColor = -1,
        TripProgressUpdateFormatter.Builder(ctx).build(),
    )

    private fun given(
        routeProgress: RouteProgress? = null,
    ) {
        if (routeProgress != null) {
            val progressObserver = slot<RouteProgressObserver>()
            every {
                mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
            } answers {
                progressObserver.captured.onRouteProgressChanged(routeProgress)
            }
        }
    }
}
