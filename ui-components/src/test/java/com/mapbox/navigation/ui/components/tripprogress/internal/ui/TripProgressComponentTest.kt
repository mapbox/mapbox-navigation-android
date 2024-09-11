package com.mapbox.navigation.ui.components.tripprogress.internal.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TripProgressComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var ctx: Context
    private lateinit var tripProgressView: MapboxTripProgressView
    private lateinit var tripProgressApi: MapboxTripProgressApi
    private lateinit var previewRoutesFlow: MutableStateFlow<List<NavigationRoute>>
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: TripProgressComponent

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        tripProgressView = spyk(MapboxTripProgressView(ctx))
        tripProgressApi = mockk(relaxed = true)
        previewRoutesFlow = MutableStateFlow(emptyList())
        mapboxNavigation = mockk(relaxed = true)
        val contract = object : TripProgressComponentContract {
            override val previewRoutes: Flow<List<NavigationRoute>> = previewRoutesFlow
        }

        sut = TripProgressComponent(
            tripProgressView,
            { contract },
            TripProgressUpdateFormatter.Builder(ctx).build(),
            tripProgressApi,
        )
    }

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
            ),
        ),
        totalTime = 10.0,
        totalDistance = 20.0,
        totalEstimatedTimeToArrival = 10,
        formatter = TripProgressUpdateFormatter.Builder(ctx).build(),
    )

    private fun tripProgressUpdateValue() = createTripProgressUpdateValue(
        5,
        20.0,
        20.0,
        5.0,
        50.0,
        -1,
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
