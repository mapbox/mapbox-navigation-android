package com.mapbox.navigation.core

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.DirectionsSessionRoutes
import com.mapbox.navigation.core.routealternatives.SuggestionType
import com.mapbox.navigation.core.routealternatives.UpdateRouteSuggestion
import com.mapbox.navigation.core.routealternatives.UpdateRoutesSuggestionObserver
import com.mapbox.navigation.core.trip.session.NativeSetRouteValue
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNativeWaypoint
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigator.WaypointType
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A [SetRoutes.SwitchToOnlineAlternative] suggestion computed against a primary route that
 * changed by the time it's applied must be rejected, not silently overwrite the newer route.
 */
@ExperimentalPreviewMapboxNavigationAPI
@Config(shadows = [ShadowReachabilityFactory::class])
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class MapboxNavigationSwitchToOnlineAlternativeTest : MapboxNavigationBaseTest() {

    private val origin = Point.fromLngLat(1.0, 1.0)
    private val stopA = Point.fromLngLat(2.0, 2.0)
    private val destination = Point.fromLngLat(3.0, 3.0)
    private val routeA1 = mockk<NavigationRoute>(relaxed = true) { every { id } returns "A1" }
    private val routeA2 = mockk<NavigationRoute>(relaxed = true) { every { id } returns "A2" }
    private val routeB1 = mockk<NavigationRoute>(relaxed = true) { every { id } returns "B1" }
    private var currentRoutes: List<NavigationRoute> = emptyList()

    private fun capturedUpdateRoutesListener(): CapturingSlot<UpdateRoutesSuggestionObserver> {
        val slot = slot<UpdateRoutesSuggestionObserver>()
        every {
            routeAlternativesController.setRouteUpdateSuggestionListener(capture(slot))
        } returns Unit
        createMapboxNavigation()
        return slot
    }

    private fun routeWithWaypoints(
        requestUuid: String,
        vararg waypoints: Point,
        routerOrigin: String = RouterOrigin.ONLINE,
    ): NavigationRoute =
        createNavigationRoute(
            directionsRoute = createDirectionsRoute(requestUuid = requestUuid),
            nativeWaypoints = waypoints.map {
                createNativeWaypoint(location = it, type = WaypointType.REGULAR)
            },
            routerOrigin = routerOrigin,
        )

    /**
     * The guard re-derives the current primary's *upcoming* waypoints from
     * [RouteProgress.remainingWaypoints], so the progress must point back at [route].
     */
    private fun stubRouteProgress(route: NavigationRoute, remainingWaypoints: Int) {
        every { tripSession.getRouteProgress() } returns mockk<RouteProgress>(relaxed = true) {
            every { navigationRoute } returns route
            every { this@mockk.remainingWaypoints } returns remainingWaypoints
        }
    }

    @Test
    fun `stale online-alternative switch is rejected when the primary route has since changed`() =
        coroutineRule.runBlockingTest {
            val listener = capturedUpdateRoutesListener()

            // The app already applied a newer 2-leg route.
            val currentPrimaryRoute = routeWithWaypoints(
                "current-primary",
                origin,
                stopA,
                destination,
                routerOrigin = RouterOrigin.OFFLINE,
            )
            every { directionsSession.routes } returns listOf(currentPrimaryRoute)
            stubRouteProgress(currentPrimaryRoute, remainingWaypoints = 2)

            // A stale online route for the old 1-leg trip arrives late.
            val staleOnlineCandidate = routeWithWaypoints("stale-online", origin, destination)

            listener.captured(
                UpdateRouteSuggestion(
                    listOf(staleOnlineCandidate),
                    SuggestionType.SwitchToOnlineAlternative,
                ),
            )

            // The stale route must never reach the trip session / native navigator.
            coVerify(exactly = 0) { tripSession.setRoutes(any(), any()) }
        }

    @Test
    fun `switch to online alternative is applied when it still matches the current primary route`() =
        coroutineRule.runBlockingTest {
            val listener = capturedUpdateRoutesListener()

            // The app is still on the same 1-leg route - no race.
            val currentPrimaryRoute = routeWithWaypoints(
                "current-primary",
                origin,
                destination,
                routerOrigin = RouterOrigin.OFFLINE,
            )
            every { directionsSession.routes } returns listOf(currentPrimaryRoute)
            stubRouteProgress(currentPrimaryRoute, remainingWaypoints = 1)

            val onlineCandidate = routeWithWaypoints("online-candidate", origin, destination)

            listener.captured(
                UpdateRouteSuggestion(
                    listOf(onlineCandidate),
                    SuggestionType.SwitchToOnlineAlternative,
                ),
            )

            coVerify(exactly = 1) {
                tripSession.setRoutes(
                    listOf(onlineCandidate),
                    SetRoutes.SwitchToOnlineAlternative(0),
                )
            }
        }

    @Test
    fun `AlternativesUpdated suggestions bypass the online-alternative guard`() =
        coroutineRule.runBlockingTest {
            val listener = capturedUpdateRoutesListener()

            val currentPrimaryRoute = routeWithWaypoints("current-primary", origin, destination)
            every { directionsSession.routes } returns listOf(currentPrimaryRoute)

            val updatedAlternatives = listOf(currentPrimaryRoute)

            listener.captured(
                UpdateRouteSuggestion(updatedAlternatives, SuggestionType.AlternativesUpdated),
            )

            // Goes through the normal setNavigationRoutes classification (SetRoutes.Alternatives
            // here, since the route equals the current primary), not the new guard.
            coVerify(exactly = 1) {
                tripSession.setRoutes(updatedAlternatives, SetRoutes.Alternatives(0))
            }
        }

    @Test
    fun `stale reorder from switchToAlternativeRoute is dismissed if a new route is already active`() =
        coroutineRule.runBlockingTest {
            // GIVEN
            createMapboxNavigation()

            // Guidance is already active on route A: primary A1, alternative A2.
            currentRoutes = listOf(routeA1, routeA2)
            every { directionsSession.routes } answers { currentRoutes }
            every {
                directionsSession.setNavigationRoutesFinished(any())
            } answers {
                currentRoutes = firstArg<DirectionsSessionRoutes>().acceptedRoutes
            }
            every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
                every {
                    internalAlternativeRouteIndices()
                } returns mapOf("A2" to mockk { every { legIndex } returns 0 })
            }

            // setNavigationRoutes(B1) starts and suspends returning result
            val bRouteApplied = CompletableDeferred<Unit>()
            coEvery {
                tripSession.setRoutes(listOf(routeB1), SetRoutes.NewRoutes(0))
            } coAnswers {
                bRouteApplied.await()
                NativeSetRouteValue(routes = listOf(routeB1), nativeAlternatives = emptyList())
            }
            coEvery {
                tripSession.setRoutes(listOf(routeA2, routeA1), SetRoutes.Reorder(0))
            } returns NativeSetRouteValue(
                routes = listOf(routeA2, routeA1),
                nativeAlternatives = emptyList(),
            )

            mapboxNavigation.setNavigationRoutes(listOf(routeB1))

            // WHEN
            // While B1 is still in flight, a switch to the A2 alternative is requested.
            mapboxNavigation.switchToAlternativeRoute(routeA2)

            // B1 finishes and is committed correctly
            bRouteApplied.complete(Unit)
            coroutineRule.testDispatcher.scheduler.advanceUntilIdle()

            // THEN
            // Expect to see B1 as current route
            assertEquals(
                "setNavigationRoutes(B1) should remain the latest applied routes; a stale " +
                    "switchToAlternativeRoute(A2) scheduled before it must not silently " +
                    "overwrite it",
                listOf(routeB1),
                currentRoutes,
            )
        }
}
