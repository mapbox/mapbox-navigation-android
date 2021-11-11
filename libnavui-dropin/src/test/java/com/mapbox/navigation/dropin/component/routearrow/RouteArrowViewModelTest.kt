package com.mapbox.navigation.dropin.component.routearrow

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError
import com.mapbox.navigation.ui.maps.route.arrow.model.UpdateManeuverArrowValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteArrowViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun routeProgressUpdated() = coroutineRule.runBlockingTest {
        val style = mockk<Style>()
        val routeProgress = mockk<RouteProgress>()
        val mockValue = mockk<UpdateManeuverArrowValue>()
        val expectedResponse: Expected<InvalidPointError, UpdateManeuverArrowValue> =
            ExpectedFactory.createValue(mockValue)
        val routeArrowApi = mockk<MapboxRouteArrowApi> {
            every { addUpcomingManeuverArrow(routeProgress) } returns expectedResponse
        }
        val routeArrowView = mockk<MapboxRouteArrowView>(relaxed = true)

        RouteArrowViewModel(routeArrowApi, routeArrowView)
            .routeProgressUpdated(routeProgress, style)

        verify { routeArrowApi.addUpcomingManeuverArrow(routeProgress) }
        verify { routeArrowView.renderManeuverUpdate(style, expectedResponse) }
    }

    @Test
    fun routeProgressUpdated_error() = coroutineRule.runBlockingTest {
        val style = mockk<Style>()
        val routeProgress = mockk<RouteProgress>()
        val mockValue = mockk<InvalidPointError>()
        val expectedResponse: Expected<InvalidPointError, UpdateManeuverArrowValue> =
            ExpectedFactory.createError(mockValue)
        val routeArrowApi = mockk<MapboxRouteArrowApi> {
            every { addUpcomingManeuverArrow(routeProgress) } returns expectedResponse
        }
        val routeArrowView = mockk<MapboxRouteArrowView>(relaxed = true)
        val viewModel = RouteArrowViewModel(routeArrowApi, routeArrowView)
        val def = async {
            viewModel.routeArrowErrors.first()
        }

        viewModel.routeProgressUpdated(routeProgress, style)
        val viewModelResult = def.await()

        verify { routeArrowApi.addUpcomingManeuverArrow(routeProgress) }
        verify { routeArrowView.renderManeuverUpdate(style, expectedResponse) }
        Assert.assertEquals(expectedResponse.error, viewModelResult)
    }
}
