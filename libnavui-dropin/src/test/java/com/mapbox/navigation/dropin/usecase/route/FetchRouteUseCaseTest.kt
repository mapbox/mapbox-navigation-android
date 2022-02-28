package com.mapbox.navigation.dropin.usecase.route

import android.location.Location
import android.location.LocationManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.usecase.location.GetCurrentLocationUseCase
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.toPoint
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class FetchRouteUseCaseTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    lateinit var mockNavigation: MapboxNavigation

    @MockK
    lateinit var mockGetCurrentLocationUseCase: GetCurrentLocationUseCase

    lateinit var stubRouteOptionsBuilder: RouteOptions.Builder

    lateinit var sut: FetchRouteUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        stubRouteOptionsBuilder = RouteOptions.builder().applyDefaultNavigationOptions()

        sut = FetchRouteUseCase(
            mockNavigation,
            { stubRouteOptionsBuilder },
            mockGetCurrentLocationUseCase,
            Dispatchers.Main
        )
    }

    @Test
    fun `should request routes from MapboxNavigation between current and destination location`() =
        coroutineRule.runBlockingTest {
            val optionsCapture = slot<RouteOptions>()
            val currentLoc = location(10.0, 11.0)
            val destination = Point.fromLngLat(20.0, 21.0)
            givenCurrentLocationResponse(currentLoc)
            givenRequestRoutesResponse(
                requestOptions = optionsCapture,
                responseRoutes = emptyList(),
                responseRouterOrigin = RouterOrigin.Onboard
            )

            val result = sut.invoke(destination)

            assertTrue(result.isSuccess)
            assertEquals(
                listOf(currentLoc.toPoint(), destination),
                optionsCapture.captured.coordinatesList()
            )
        }

    @Test
    fun `should return empty list when device location is not available`() =
        coroutineRule.runBlockingTest {
            coEvery { mockGetCurrentLocationUseCase.invoke(Unit) } coAnswers {
                Result.failure(Exception("cannot fetch device location"))
            }

            val result = sut.invoke(Point.fromLngLat(20.0, 21.0))

            assertTrue(result.isSuccess)
            assertEquals(emptyList<DirectionsRoute>(), result.getOrNull())
        }

    private suspend fun givenCurrentLocationResponse(currentLoc: Location) {
        coEvery { mockGetCurrentLocationUseCase.invoke(Unit) } coAnswers {
            Result.success(currentLoc)
        }
    }

    private fun givenRequestRoutesResponse(
        requestOptions: CapturingSlot<RouteOptions>,
        responseRoutes: List<DirectionsRoute>,
        responseRouterOrigin: RouterOrigin,
        requestId: Long = 0
    ) {
        val callback = slot<RouterCallback>()
        every {
            mockNavigation.requestRoutes(
                routeOptions = capture(requestOptions),
                routesRequestCallback = capture(callback)
            )
        } answers {
            callback.captured.onRoutesReady(responseRoutes, responseRouterOrigin)
            requestId
        }
    }

    private fun location(lat: Double, lon: Double) =
        Location(LocationManager.PASSIVE_PROVIDER).apply {
            latitude = lat
            longitude = lon
        }
}
