package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteRefreshValidatorTest {

    private val offlineRouteMessage = "Offline routes can't be refreshed"

    private val notEvOfflineRouteMessage =
        "This offline route can't be refreshed. Only an offline EV route that " +
            "requests state_of_charge is refreshed on the device."

    private val noUuidMessage = "DirectionsRoute#requestUuid is blank. " +
        "A route is refreshed by the uuid of the request that built it."

    private val route = mockk<NavigationRoute>(relaxed = true)

    @Test
    fun `validateRoute valid`() {
        val route = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                requestUuid = "uuid",
                routeOptions = createRouteOptions(enableRefresh = true),
            ),
        )

        assertEquals(
            RouteRefreshValidator.RouteValidationResult.Valid,
            RouteRefreshValidator.validateRoute(route, notAsked),
        )
    }

    @Test
    fun `validateRoute enableRefresh is null`() {
        every { route.directionsRoute } returns createDirectionsRoute(requestUuid = "uuid")

        assertEquals(
            RouteRefreshValidator.RouteValidationResult
                .Invalid("RouteOptions#enableRefresh is false"),
            RouteRefreshValidator.validateRoute(route, notAsked),
        )
    }

    @Test
    fun `validateRoute enableRefresh is false`() {
        every { route.directionsRoute } returns createDirectionsRoute(requestUuid = "uuid")

        assertEquals(
            RouteRefreshValidator.RouteValidationResult
                .Invalid("RouteOptions#enableRefresh is false"),
            RouteRefreshValidator.validateRoute(route, notAsked),
        )
    }

    @Test
    fun `offline route is invalid when the device can't refresh`() {
        for (route in listOf(offlineRoute(stateOfCharge = false), offlineRoute())) {
            assertEquals(
                RouteRefreshValidator.RouteValidationResult.Invalid(offlineRouteMessage),
                RouteRefreshValidator.validateRoute(route) { false },
            )
        }
    }

    @Test
    fun `offline route without state_of_charge is invalid when the device can refresh`() {
        assertEquals(
            RouteRefreshValidator.RouteValidationResult.Invalid(notEvOfflineRouteMessage),
            RouteRefreshValidator.validateRoute(offlineRoute(stateOfCharge = false)) { true },
        )
    }

    @Test
    fun `offline EV route is valid when the device can refresh`() {
        assertEquals(
            RouteRefreshValidator.RouteValidationResult.Valid,
            RouteRefreshValidator.validateRoute(offlineRoute()) { true },
        )
    }

    @Test
    fun `online route never asks whether the device can refresh`() {
        val route = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                requestUuid = "uuid",
                routeOptions = createRouteOptions(enableRefresh = true).toBuilder()
                    .annotations("distance,state_of_charge")
                    .build(),
            ),
            routerOrigin = RouterOrigin.ONLINE,
        )

        assertEquals(
            RouteRefreshValidator.RouteValidationResult.Valid,
            RouteRefreshValidator.validateRoute(route, notAsked),
        )
    }

    @Test
    fun `validateRoute requestUuid is null`() {
        val route = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                requestUuid = null,
                routeOptions = createRouteOptions(enableRefresh = true),
            ),
        )

        assertEquals(
            RouteRefreshValidator.RouteValidationResult.Invalid(noUuidMessage),
            RouteRefreshValidator.validateRoute(route, notAsked),
        )
    }

    @Test
    fun `validateRoute requestUuid is empty`() {
        val route = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                requestUuid = "",
                routeOptions = createRouteOptions(enableRefresh = true),
            ),
        )

        assertEquals(
            RouteRefreshValidator.RouteValidationResult.Invalid(noUuidMessage),
            RouteRefreshValidator.validateRoute(route, notAsked),
        )
    }

    @Test
    fun `validateRoute requestUuid is blank`() {
        val route = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                requestUuid = "   ",
                routeOptions = createRouteOptions(enableRefresh = true),
            ),
        )
        assertEquals(
            RouteRefreshValidator.RouteValidationResult.Invalid(noUuidMessage),
            RouteRefreshValidator.validateRoute(route, notAsked),
        )
    }

    @Test
    fun `joinValidationErrorMessages empty list`() {
        assertEquals("", RouteRefreshValidator.joinValidationErrorMessages(emptyList()))
    }

    @Test
    fun `joinValidationErrorMessages all valid`() {
        val list = listOf<Pair<RouteRefreshValidator.RouteValidationResult, NavigationRoute>>(
            RouteRefreshValidator.RouteValidationResult.Valid to mockk(relaxed = true),
            RouteRefreshValidator.RouteValidationResult.Valid to mockk(relaxed = true),
            RouteRefreshValidator.RouteValidationResult.Valid to mockk(relaxed = true),
        )
        assertEquals("", RouteRefreshValidator.joinValidationErrorMessages(list))
    }

    @Test
    fun `joinValidationErrorMessages one invalid`() {
        val list = listOf<Pair<RouteRefreshValidator.RouteValidationResult, NavigationRoute>>(
            RouteRefreshValidator.RouteValidationResult.Valid to mockk(relaxed = true),
            RouteRefreshValidator.RouteValidationResult.Valid to mockk(relaxed = true),
            RouteRefreshValidator.RouteValidationResult.Invalid("some reason") to
                mockk(relaxed = true) { every { id } returns "id#0" },
        )
        assertEquals("id#0 some reason", RouteRefreshValidator.joinValidationErrorMessages(list))
    }

    @Test
    fun `joinValidationErrorMessages all invalid`() {
        val list = listOf<Pair<RouteRefreshValidator.RouteValidationResult, NavigationRoute>>(
            RouteRefreshValidator.RouteValidationResult.Invalid("reason 1") to
                mockk(relaxed = true) { every { id } returns "id#0" },
            RouteRefreshValidator.RouteValidationResult.Invalid("reason 2") to
                mockk(relaxed = true) { every { id } returns "id#1" },
            RouteRefreshValidator.RouteValidationResult.Invalid("reason 3") to
                mockk(relaxed = true) { every { id } returns "id#2" },
        )
        assertEquals(
            "id#0 reason 1. id#1 reason 2. id#2 reason 3",
            RouteRefreshValidator.joinValidationErrorMessages(list),
        )
    }

    private val notAsked: () -> Boolean = { error("only an offline route asks") }

    private fun offlineRoute(stateOfCharge: Boolean = true) = createNavigationRoute(
        directionsRoute = createDirectionsRoute(
            requestUuid = "local@uuid",
            routeOptions = createRouteOptions(enableRefresh = true).toBuilder()
                .annotations(if (stateOfCharge) "distance,state_of_charge" else "distance")
                .build(),
        ),
        routerOrigin = RouterOrigin.OFFLINE,
    )
}
