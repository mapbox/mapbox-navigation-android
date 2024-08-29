package com.mapbox.navigation.core

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
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
import org.junit.Assert.assertTrue
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
    private val results = mutableListOf<Expected<RoutesSetError, RoutesSetSuccess>>()
    private val initialLegIndex = 2
    private val errorMessage = "some error"
    private val invalidAlternativeError = "invalid alternative"

    @Test
    fun `calls callback with successfully set routes with alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(initialLegIndex),
                )
            } returns NativeSetRouteValue(
                routes,
                listOf(
                    routeAlternativeWithId(alternativeId1),
                    routeAlternativeWithId(alternativeId2),
                ),
            )

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertTrue(this.isValue)
                assertEquals(emptyMap<String, RoutesSetError>(), this.value!!.ignoredAlternatives)
            }
        }

    @Test
    fun `calls callback with successfully set routes without alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute)
            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(initialLegIndex),
                )
            } returns NativeSetRouteValue(routes, emptyList())

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertTrue(this.isValue)
                assertEquals(emptyMap<String, RoutesSetError>(), this.value!!.ignoredAlternatives)
            }
        }

    @Test
    fun `calls callback with error for invalid primary route with alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(initialLegIndex),
                )
            } returns NativeSetRouteError(errorMessage)

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertTrue(this.isError)
                assertEquals(errorMessage, this.error!!.message)
            }
        }

    @Test
    fun `calls callback with success for empty list`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = emptyList<NavigationRoute>()
            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.CleanUp,

                )
            } returns NativeSetRouteValue(routes, emptyList())

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertTrue(this.isValue)
                assertEquals(emptyMap<String, RoutesSetError>(), this.value!!.ignoredAlternatives)
            }
        }

    @Test
    fun `calls callback for all ignored alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(initialLegIndex),
                )
            } returns NativeSetRouteValue(routes, emptyList())

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertTrue(this.isValue)
                assertEquals(
                    mapOf(
                        alternativeId1 to RoutesSetError(invalidAlternativeError),
                        alternativeId2 to RoutesSetError(invalidAlternativeError),
                    ),
                    this.value!!.ignoredAlternatives,
                )
            }
        }

    @Test
    fun `calls callback for all ignored alternatives with other valid ones`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(initialLegIndex),
                )
            } returns NativeSetRouteValue(
                routes,
                listOf(routeAlternativeWithId("bad id 1"), routeAlternativeWithId("bad id 2")),
            )

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertTrue(this.isValue)
                assertEquals(
                    mapOf(
                        alternativeId1 to RoutesSetError(invalidAlternativeError),
                        alternativeId2 to RoutesSetError(invalidAlternativeError),
                    ),
                    this.value!!.ignoredAlternatives,
                )
            }
        }

    @Test
    fun `calls callback for partially ignored alternatives`() =
        coroutineRule.runBlockingTest {
            createMapboxNavigation()
            val routes = listOf(passedPrimaryRoute, alternativeRoute1, alternativeRoute2)
            coEvery {
                tripSession.setRoutes(
                    routes,
                    SetRoutes.NewRoutes(initialLegIndex),
                )
            } returns NativeSetRouteValue(
                routes,
                listOf(routeAlternativeWithId(alternativeId2)),
            )

            mapboxNavigation.setNavigationRoutes(routes, initialLegIndex, callback)

            verify(exactly = 1) { callback.onRoutesSet(capture(results)) }
            results[0].run {
                assertTrue(this.isValue)
                assertEquals(
                    mapOf(alternativeId1 to RoutesSetError(invalidAlternativeError)),
                    this.value!!.ignoredAlternatives,
                )
            }
        }

    private fun routeAlternativeWithId(id: String): RouteAlternative = mockk(relaxed = true) {
        every { route } returns mockk(relaxed = true) {
            every { routeId } returns id
        }
    }
}
