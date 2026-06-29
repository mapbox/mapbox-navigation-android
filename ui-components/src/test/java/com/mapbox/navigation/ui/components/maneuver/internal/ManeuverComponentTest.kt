package com.mapbox.navigation.ui.components.maneuver.internal

import android.os.Build
import android.view.View
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldCallback
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import com.mapbox.navigation.ui.components.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.components.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.components.maneuver.view.MapboxManeuverViewState
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
@ExperimentalCoroutinesApi
class ManeuverComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val maneuverViewOptions = ManeuverViewOptions.Builder().build()

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationExtensions")
        every { mockNavigation.flowRouteProgress() } returns flowOf(routeProgress)
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationExtensions")
    }

    private val callbackSlot = slot<RouteShieldCallback>()
    private val maneuvers = listOf<Maneuver>()
    private val expectedManeuvers =
        ExpectedFactory.createValue<ManeuverError, List<Maneuver>>(maneuvers)
    private val mockManeuverApi = mockk<MapboxManeuverApi>(relaxed = true) {
        every { getManeuvers(any<RouteProgress>()) } returns expectedManeuvers
        every {
            getRoadShields(
                DirectionsCriteria.PROFILE_DEFAULT_USER,
                "navigation-day-v1",
                maneuvers,
                capture(callbackSlot),
            )
        } returns Unit
    }
    private val maneuverView: MapboxManeuverView = mockk(relaxed = true)
    private val mockNavigation = mockk<MapboxNavigation> {
        every { navigationOptions } returns mockk()
    }
    private val routeProgress = mockk<RouteProgress>()

    @Test
    fun `maneuvers are rendered when trip session has started and route is available`() =
        coroutineRule.runBlockingTest {
            val mockNavigationRoute: List<NavigationRoute> = listOf(
                mockk {
                    every { directionsRoute } returns mockk()
                },
                mockk(),
            )
            val mockRoutesUpdatedResult: RoutesUpdatedResult = mockk {
                every { navigationRoutes } returns mockNavigationRoute
            }
            every {
                mockNavigation.registerRoutesObserver(any())
            } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult)
            }
            every {
                mockNavigation.registerTripSessionStateObserver(any())
            } answers {
                firstArg<TripSessionStateObserver>().onSessionStateChanged(TripSessionState.STARTED)
            }
            val maneuverComponent =
                ManeuverComponent(
                    maneuverView = maneuverView,
                    userId = DirectionsCriteria.PROFILE_DEFAULT_USER,
                    styleId = "navigation-day-v1",
                    options = maneuverViewOptions,
                    maneuverApi = mockManeuverApi,
                    formatterOptions =
                    DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
                )

            maneuverComponent.onAttached(mockNavigation)

            verify { maneuverView.renderManeuvers(expectedManeuvers) }
        }

    @Test
    fun `road shields are rendered when trip session has started and route is available`() =
        coroutineRule.runBlockingTest {
            val mockNavigationRoute: List<NavigationRoute> = listOf(
                mockk {
                    every { directionsRoute } returns mockk()
                },
                mockk(),
            )
            val mockRoutesUpdatedResult: RoutesUpdatedResult = mockk {
                every { navigationRoutes } returns mockNavigationRoute
            }
            every {
                mockNavigation.registerRoutesObserver(any())
            } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult)
            }
            every {
                mockNavigation.registerTripSessionStateObserver(any())
            } answers {
                firstArg<TripSessionStateObserver>().onSessionStateChanged(TripSessionState.STARTED)
            }
            val expectedList: List<Expected<RouteShieldError, RouteShieldResult>> = listOf()
            val maneuverComponent =
                ManeuverComponent(
                    maneuverView = maneuverView,
                    userId = DirectionsCriteria.PROFILE_DEFAULT_USER,
                    styleId = "navigation-day-v1",
                    options = maneuverViewOptions,
                    maneuverApi = mockManeuverApi,
                    formatterOptions =
                    DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
                )

            maneuverComponent.onAttached(mockNavigation).also {
                callbackSlot.captured.onRoadShields(expectedList)
            }

            verify { maneuverView.renderManeuverWith(expectedList) }
        }

    @Test
    fun `maneuvers and shields are not rendered when trip session has stopped`() =
        coroutineRule.runBlockingTest {
            val mockNavigationRoute: List<NavigationRoute> = listOf(
                mockk {
                    every { directionsRoute } returns mockk()
                },
                mockk(),
            )
            val mockRoutesUpdatedResult: RoutesUpdatedResult = mockk {
                every { navigationRoutes } returns mockNavigationRoute
            }
            every {
                mockNavigation.registerRoutesObserver(any())
            } answers {
                firstArg<RoutesObserver>().onRoutesChanged(mockRoutesUpdatedResult)
            }
            every {
                mockNavigation.registerTripSessionStateObserver(any())
            } answers {
                firstArg<TripSessionStateObserver>().onSessionStateChanged(TripSessionState.STOPPED)
            }

            val maneuverComponent =
                ManeuverComponent(
                    maneuverView = maneuverView,
                    userId = DirectionsCriteria.PROFILE_DEFAULT_USER,
                    styleId = "navigation-day-v1",
                    options = maneuverViewOptions,
                    maneuverApi = mockManeuverApi,
                    formatterOptions =
                    DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
                )

            maneuverComponent.onAttached(mockNavigation)

            verify(exactly = 0) {
                maneuverView.renderManeuvers(any())
                maneuverView.renderManeuverWith(any())
            }
        }

    @Test
    fun `maneuvers and shields are not rendered when there is no route`() =
        coroutineRule.runBlockingTest {
            every {
                mockNavigation.registerRoutesObserver(any())
            } answers {
                firstArg<RoutesObserver>().onRoutesChanged(
                    mockk {
                        every { navigationRoutes } returns listOf()
                    },
                )
            }
            every {
                mockNavigation.registerTripSessionStateObserver(any())
            } answers {
                firstArg<TripSessionStateObserver>().onSessionStateChanged(TripSessionState.STARTED)
            }

            val maneuverComponent =
                ManeuverComponent(
                    maneuverView = maneuverView,
                    userId = DirectionsCriteria.PROFILE_DEFAULT_USER,
                    styleId = "navigation-day-v1",
                    options = maneuverViewOptions,
                    maneuverApi = mockManeuverApi,
                    formatterOptions =
                    DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
                )

            maneuverComponent.onAttached(mockNavigation)

            verify(exactly = 0) {
                maneuverView.renderManeuvers(any())
                maneuverView.renderManeuverWith(any())
            }
        }

    @Test
    fun `expand maneuver view state is collected`() =
        coroutineRule.runBlockingTest {
            val contract = mockk<ManeuverComponentContract>(relaxed = true) {
                every { onManeuverViewStateChanged(any()) } just Runs
            }
            every {
                maneuverView.maneuverViewState
            } returns MutableStateFlow(MapboxManeuverViewState.EXPANDED)
            val maneuverComponent =
                ManeuverComponent(
                    maneuverView = maneuverView,
                    userId = DirectionsCriteria.PROFILE_DEFAULT_USER,
                    styleId = "navigation-day-v1",
                    options = maneuverViewOptions,
                    maneuverApi = mockManeuverApi,
                    formatterOptions =
                    DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
                    contract = { contract },
                )
            maneuverComponent.onAttached(mockNavigation)

            maneuverView.updateUpcomingManeuversVisibility(View.VISIBLE)

            verify {
                contract.onManeuverViewStateChanged(MapboxManeuverViewState.EXPANDED)
            }
        }

    @Test
    fun `collapse maneuver view state is collected`() =
        coroutineRule.runBlockingTest {
            val contract = mockk<ManeuverComponentContract>(relaxed = true) {
                every { onManeuverViewStateChanged(any()) } just Runs
            }
            every {
                maneuverView.maneuverViewState
            } returns MutableStateFlow(MapboxManeuverViewState.COLLAPSED)
            val maneuverComponent =
                ManeuverComponent(
                    maneuverView = maneuverView,
                    userId = DirectionsCriteria.PROFILE_DEFAULT_USER,
                    styleId = "navigation-day-v1",
                    options = maneuverViewOptions,
                    maneuverApi = mockManeuverApi,
                    formatterOptions =
                    DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
                    contract = { contract },
                )
            maneuverComponent.onAttached(mockNavigation)

            maneuverView.updateUpcomingManeuversVisibility(View.GONE)

            verify {
                contract.onManeuverViewStateChanged(MapboxManeuverViewState.COLLAPSED)
            }
        }

    @Test
    fun `maneuver view visibility is set to true on onAttached`() =
        coroutineRule.runBlockingTest {
            val contract = mockk<ManeuverComponentContract>(relaxed = true) {
                every { onManeuverViewStateChanged(any()) } just Runs
            }
            val maneuverComponent =
                ManeuverComponent(
                    maneuverView = maneuverView,
                    userId = DirectionsCriteria.PROFILE_DEFAULT_USER,
                    styleId = "navigation-day-v1",
                    options = maneuverViewOptions,
                    maneuverApi = mockManeuverApi,
                    formatterOptions =
                    DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
                    contract = { contract },
                )
            maneuverComponent.onAttached(mockNavigation)

            verify {
                contract.onManeuverViewVisibilityChanged(true)
            }
        }

    @Test
    fun `maneuver view visibility is set to false on onDetached`() =
        coroutineRule.runBlockingTest {
            val contract = mockk<ManeuverComponentContract>(relaxed = true) {
                every { onManeuverViewStateChanged(any()) } just Runs
            }
            val maneuverComponent =
                ManeuverComponent(
                    maneuverView = maneuverView,
                    userId = DirectionsCriteria.PROFILE_DEFAULT_USER,
                    styleId = "navigation-day-v1",
                    options = maneuverViewOptions,
                    maneuverApi = mockManeuverApi,
                    formatterOptions =
                    DistanceFormatterOptions.Builder(mockk(relaxed = true)).build(),
                    contract = { contract },
                )
            maneuverComponent.onAttached(mockNavigation)
            clearMocks(contract, answers = false)
            maneuverComponent.onDetached(mockNavigation)

            verify {
                contract.onManeuverViewVisibilityChanged(false)
            }
        }
}
