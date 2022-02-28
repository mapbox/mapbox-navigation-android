package com.mapbox.navigation.dropin.usecase.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class FetchAndSetRouteUseCaseTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    lateinit var mockNavigation: MapboxNavigation

    @MockK
    lateinit var mockFetchRouteUseCase: FetchRouteUseCase

    lateinit var sut: FetchAndSetRouteUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        sut = FetchAndSetRouteUseCase(
            mockNavigation,
            mockFetchRouteUseCase,
            Dispatchers.Main
        )
    }

    @Test
    fun `should fetch and set routes`() = coroutineRule.runBlockingTest {
        val destination = Point.fromLngLat(1.0, 2.0)
        val routes = listOf(
            DirectionsRoute.builder().distance(123.0).duration(321.0).build()
        )
        coEvery { mockFetchRouteUseCase.invoke(destination) } coAnswers { Result.success(routes) }

        sut.invoke(destination)

        coVerify { mockNavigation.setRoutes(routes) }
    }
}
