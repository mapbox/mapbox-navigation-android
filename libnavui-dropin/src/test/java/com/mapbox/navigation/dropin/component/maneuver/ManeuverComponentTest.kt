package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.shield.model.RouteShieldCallback
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
@ExperimentalPreviewMapboxNavigationAPI
class ManeuverComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    lateinit var style: Style

    private val maneuverViewOptions = ManeuverViewOptions.Builder().build()

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationEx")
        every { mockNavigation.flowRouteProgress() } returns flowOf(routeProgress)
        style = mockk {
            every { styleURI } returns NavigationStyles.NAVIGATION_DAY_STYLE
        }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationEx")
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
                NavigationStyles.NAVIGATION_DAY_STYLE_ID,
                "token",
                maneuvers,
                capture(callbackSlot)
            )
        } returns Unit
    }
    private val maneuverView: MapboxManeuverView = mockk(relaxed = true)
    private val mockNavigation = mockk<MapboxNavigation>() {
        every { navigationOptions } returns mockk {
            every { accessToken } returns "token"
        }
    }
    private val routeProgress = mockk<RouteProgress>()

    @Test
    fun `maneuvers are rendered when trip session has started and route is available`() =
        coroutineRule.runBlockingTest {
            val mockNavigationRoute: List<NavigationRoute> = listOf(
                mockk {
                    every { directionsRoute } returns mockk()
                },
                mockk()
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
                ManeuverComponent(maneuverView, style, maneuverViewOptions, mockManeuverApi)

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
                mockk()
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
                ManeuverComponent(maneuverView, style, maneuverViewOptions, mockManeuverApi)

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
                mockk()
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
                ManeuverComponent(maneuverView, style, maneuverViewOptions, mockManeuverApi)

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
                    }
                )
            }
            every {
                mockNavigation.registerTripSessionStateObserver(any())
            } answers {
                firstArg<TripSessionStateObserver>().onSessionStateChanged(TripSessionState.STARTED)
            }

            val maneuverComponent =
                ManeuverComponent(maneuverView, style, maneuverViewOptions, mockManeuverApi)

            maneuverComponent.onAttached(mockNavigation)

            verify(exactly = 0) {
                maneuverView.renderManeuvers(any())
                maneuverView.renderManeuverWith(any())
            }
        }
}
