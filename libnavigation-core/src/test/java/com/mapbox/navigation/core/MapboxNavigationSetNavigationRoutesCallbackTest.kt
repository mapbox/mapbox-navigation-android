package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.trip.session.NativeSetRouteError
import com.mapbox.navigation.core.trip.session.NativeSetRouteValue
import com.mapbox.navigator.RouteAlternative
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalPreviewMapboxNavigationAPI
@Config(shadows = [ShadowReachabilityFactory::class])
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class MapboxNavigationSetNavigationRoutesCallbackTest : MapboxNavigationBaseTest() {

    private val passedPrimaryRoute = mockk<NavigationRoute>(relaxed = true)
    private val alternativeId1 = "id1"
    private val alternativeId2 = "id2"
    private val alternativeRoute1 = mockk<NavigationRoute>(relaxed = true) {
        every { id } returns alternativeId1
    }
    private val alternativeRoute2 = mockk<NavigationRoute>(relaxed = true) {
        every { id } returns alternativeId2
    }
    private val callback = mockk<RoutesSetCallback>(relaxed = true)
    private val results = mutableListOf<RoutesSetResult>()
    private val initialLegIndex = 2
    private val error = "some error"
    private val invalidAlternativeError = "invalid alternative"

    @Test
    fun `calls callback with successfully set routes with alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(routes, initialLegIndex, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
            } returns NativeSetRouteValue(
                listOf(
                    routeAlternativeWithId(alternativeId1),
                    routeAlternativeWithId(alternativeId2),
                )
            )

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertEquals(RoutesSetResult.SUCCESS, this.status)
                assertEquals(routes, this.passedRoutes)
                assertEquals(RouteStatus(passedPrimaryRoute, true, null), this.primaryRoute)
                assertEquals(
                    listOf(
                        RouteStatus(alternativeRoute1, true, null),
                        RouteStatus(alternativeRoute2, true, null),
                    ),
                    this.acceptedAlternatives
                )
                assertEquals(emptyList<RoutesSetResult>(), this.ignoredAlternatives)
            }
        }

    @Test
    fun `calls callback with successfully set routes without alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute)
            coEvery {
                tripSession.setRoutes(routes, initialLegIndex, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
            } returns NativeSetRouteValue(emptyList())

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertEquals(RoutesSetResult.SUCCESS, this.status)
                assertEquals(routes, this.passedRoutes)
                assertEquals(RouteStatus(passedPrimaryRoute, true, null), this.primaryRoute)
                assertEquals(emptyList<RoutesSetResult>(), this.acceptedAlternatives)
                assertEquals(emptyList<RoutesSetResult>(), this.ignoredAlternatives)
            }
        }

    @Test
    fun `calls callback with error for invalid primary route with alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(routes, initialLegIndex, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
            } returns NativeSetRouteError(error)

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertEquals(RoutesSetResult.PRIMARY_ROUTE_IGNORED, this.status)
                assertEquals(routes, this.passedRoutes)
                assertEquals(RouteStatus(passedPrimaryRoute, false, error), this.primaryRoute)
                assertEquals(emptyList<RoutesSetResult>(), this.acceptedAlternatives)
                assertEquals(
                    listOf(
                        RouteStatus(alternativeRoute1, false, error),
                        RouteStatus(alternativeRoute2, false, error),
                    ),
                    this.ignoredAlternatives
                )
            }
        }

    @Test
    fun `calls callback with error for invalid primary route without alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute)
            coEvery {
                tripSession.setRoutes(routes, initialLegIndex, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
            } returns NativeSetRouteError(error)

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertEquals(RoutesSetResult.PRIMARY_ROUTE_IGNORED, this.status)
                assertEquals(routes, this.passedRoutes)
                assertEquals(RouteStatus(passedPrimaryRoute, false, error), this.primaryRoute)
                assertEquals(emptyList<RoutesSetResult>(), this.acceptedAlternatives)
                assertEquals(emptyList<RoutesSetResult>(), this.ignoredAlternatives)
            }
        }

    @Test
    fun `calls callback with success for empty list`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = emptyList<NavigationRoute>()
            coEvery {
                tripSession.setRoutes(routes, initialLegIndex, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
            } returns NativeSetRouteValue(emptyList())

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertEquals(RoutesSetResult.SUCCESS, this.status)
                assertEquals(routes, this.passedRoutes)
                assertNull(this.primaryRoute)
                assertEquals(emptyList<RoutesSetResult>(), this.acceptedAlternatives)
                assertEquals(emptyList<RoutesSetResult>(), this.ignoredAlternatives)
            }
        }

    @Test
    fun `calls callback for all ignored alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(routes, initialLegIndex, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
            } returns NativeSetRouteValue(emptyList())

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertEquals(RoutesSetResult.ALTERNATIVES_ARE_FILTERED, this.status)
                assertEquals(routes, this.passedRoutes)
                assertNotNull(this.primaryRoute)
                assertEquals(emptyList<RoutesSetResult>(), this.acceptedAlternatives)
                assertEquals(
                    listOf(
                        RouteStatus(alternativeRoute1, false, invalidAlternativeError),
                        RouteStatus(alternativeRoute2, false, invalidAlternativeError),
                    ),
                    this.ignoredAlternatives
                )
            }
        }

    @Test
    fun `calls callback for all ignored alternatives with other valid ones`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(routes, initialLegIndex, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
            } returns NativeSetRouteValue(
                listOf(routeAlternativeWithId("bad id 1"), routeAlternativeWithId("bad id 2"))
            )

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertEquals(RoutesSetResult.ALTERNATIVES_ARE_FILTERED, this.status)
                assertEquals(routes, this.passedRoutes)
                assertNotNull(this.primaryRoute)
                assertEquals(emptyList<RoutesSetResult>(), this.acceptedAlternatives)
                assertEquals(
                    listOf(
                        RouteStatus(alternativeRoute1, false, invalidAlternativeError),
                        RouteStatus(alternativeRoute2, false, invalidAlternativeError),
                    ),
                    this.ignoredAlternatives
                )
            }
        }

    @Test
    fun `calls callback for partially ignored alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(routes, initialLegIndex, RoutesExtra.ROUTES_UPDATE_REASON_NEW)
            } returns NativeSetRouteValue(listOf(routeAlternativeWithId(alternativeId2)))

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertEquals(RoutesSetResult.ALTERNATIVES_ARE_FILTERED, this.status)
                assertEquals(routes, this.passedRoutes)
                assertNotNull(this.primaryRoute)
                assertEquals(
                    listOf(RouteStatus(alternativeRoute2, true, null)),
                    this.acceptedAlternatives
                )
                assertEquals(
                    listOf(RouteStatus(alternativeRoute1, false, invalidAlternativeError)),
                    this.ignoredAlternatives
                )
            }
        }

    private fun routeAlternativeWithId(id: String): RouteAlternative = mockk(relaxed = true) {
        every { route } returns mockk(relaxed = true) {
            every { routeId } returns id
        }
    }
}
