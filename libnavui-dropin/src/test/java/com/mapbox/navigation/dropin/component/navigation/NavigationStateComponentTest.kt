package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.dropin.component.destination.DestinationState
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction.Update
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState.ActiveNavigation
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState.Arrival
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState.FreeDrive
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState.RoutePreview
import com.mapbox.navigation.dropin.component.routefetch.RoutesState
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.model.Destination
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class NavigationStateComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    lateinit var sut: NavigationStateComponent
    lateinit var navigationStateFlow: MutableStateFlow<NavigationState>
    lateinit var destinationStateFlow: MutableStateFlow<DestinationState>
    lateinit var routesStateFlow: MutableStateFlow<RoutesState>
    lateinit var flowRoutesUpdated: MutableSharedFlow<RoutesUpdatedResult>
    lateinit var flowOnFinalDestinationArrival: MutableSharedFlow<RouteProgress>

    @MockK
    lateinit var mockNavigationStateViewModel: NavigationStateViewModel

    @MockK
    lateinit var mockRoutesViewModel: RoutesViewModel

    @MockK
    lateinit var mockDestinationViewModel: DestinationViewModel

    @MockK
    lateinit var mockMapboxNavigation: MapboxNavigation

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
        MockKAnnotations.init(this, relaxUnitFun = true)
        navigationStateFlow = MutableStateFlow(FreeDrive)
        routesStateFlow = MutableStateFlow(RoutesState())
        destinationStateFlow = MutableStateFlow(DestinationState())
        flowRoutesUpdated = MutableSharedFlow()
        flowOnFinalDestinationArrival = MutableSharedFlow()

        every { mockNavigationStateViewModel.state } returns navigationStateFlow
        every { mockDestinationViewModel.state } returns destinationStateFlow
        every { mockRoutesViewModel.state } returns routesStateFlow
        every { mockMapboxNavigation.flowRoutesUpdated() } returns flowRoutesUpdated
        every {
            mockMapboxNavigation.flowOnFinalDestinationArrival()
        } returns flowOnFinalDestinationArrival

        sut = NavigationStateComponent(
            mockNavigationStateViewModel,
            mockDestinationViewModel,
            mockRoutesViewModel,
        )
    }

    @After
    fun cleanUp() {
        unmockkStatic("com.mapbox.navigation.dropin.extensions.MapboxNavigationEx")
    }

    @Test
    fun `should transition to RoutePreview state when destination and routes are set`() =
        coroutineRule.runBlockingTest {
            given(
                destination = Destination(Point.fromLngLat(10.0, 11.0)),
                navigationStarted = false,
                routesList = listOf(mockk())
            )

            sut.onAttached(mockMapboxNavigation)

            verify { mockNavigationStateViewModel.invoke(Update(RoutePreview)) }
        }

    @Test
    fun `should transition to FreeDrive when destination is set to null`() =
        coroutineRule.runBlockingTest {
            given(
                destination = null,
                navigationStarted = false,
                routesList = listOf(mockk())
            )

            sut.onAttached(mockMapboxNavigation)

            verify { mockNavigationStateViewModel.invoke(Update(FreeDrive)) }
        }

    @Test
    fun `should transition to FreeDrive when routes are empty`() =
        coroutineRule.runBlockingTest {
            given(
                destination = Destination(Point.fromLngLat(10.0, 11.0)),
                navigationStarted = false,
                routesList = listOf(mockk())
            )

            sut.onAttached(mockMapboxNavigation)
            givenRoutesUpdateCallback(routesList = emptyList())

            verify { mockNavigationStateViewModel.invoke(Update(FreeDrive)) }
        }

    @Test
    fun `should transition to ActiveNavigation when navigationStarted is true`() =
        coroutineRule.runBlockingTest {
            given(
                destination = Destination(Point.fromLngLat(10.0, 11.0)),
                navigationStarted = true,
                routesList = listOf(mockk())
            )

            sut.onAttached(mockMapboxNavigation)

            verify { mockNavigationStateViewModel.invoke(Update(ActiveNavigation)) }
        }

    @Test
    fun `should transition Arrival on onFinalDestinationArrival callback`() =
        coroutineRule.runBlockingTest {
            given(
                destination = Destination(Point.fromLngLat(10.0, 11.0)),
                navigationStarted = true,
                routesList = listOf(mockk())
            )

            sut.onAttached(mockMapboxNavigation)
            navigationStateFlow.value = ActiveNavigation
            givenFinalDestinationArrivalCallback()

            verify { mockNavigationStateViewModel.invoke(Update(Arrival)) }
        }

    private fun given(
        destination: Destination?,
        navigationStarted: Boolean,
        routesList: List<DirectionsRoute>
    ) {
        destinationStateFlow.value = DestinationState(destination)
        routesStateFlow.value = RoutesState(navigationStarted)
        every { mockMapboxNavigation.getRoutes() } returns routesList
    }

    private suspend fun givenRoutesUpdateCallback(routesList: List<DirectionsRoute>) {
        val routesUpdatedResult = mockk<RoutesUpdatedResult> {
            every { routes } returns routesList
        }
        flowRoutesUpdated.emit(routesUpdatedResult)
    }

    private suspend fun givenFinalDestinationArrivalCallback() {
        val routeProgress = mockk<RouteProgress> {
            every { currentState } returns RouteProgressState.COMPLETE
        }
        flowOnFinalDestinationArrival.emit(routeProgress)
    }
}
