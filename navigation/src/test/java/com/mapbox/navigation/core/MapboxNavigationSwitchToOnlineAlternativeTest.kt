package com.mapbox.navigation.core

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routealternatives.SuggestionType
import com.mapbox.navigation.core.routealternatives.UpdateRouteSuggestion
import com.mapbox.navigation.core.routealternatives.UpdateRoutesSuggestionObserver
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNativeWaypoint
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigator.WaypointType
import io.mockk.CapturingSlot
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
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

    private fun capturedUpdateRoutesListener(): CapturingSlot<UpdateRoutesSuggestionObserver> {
        val slot = slot<UpdateRoutesSuggestionObserver>()
        every {
            routeAlternativesController.setRouteUpdateSuggestionListener(capture(slot))
        } returns Unit
        createMapboxNavigation()
        // Not stubbed by the base test class; needed whenever setNavigationRoutesFinished
        // is invoked (both the accepted and the rejected-as-outdated paths).
        every { directionsSession.ignoredRoutes } returns emptyList()
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
}
