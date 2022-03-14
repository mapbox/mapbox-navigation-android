package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.DirectionsJsonObject
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.navigator.internal.mapToDirectionsResponse
import com.mapbox.navigation.testing.verifyNoOne
import com.mapbox.navigation.testing.verifyOnce
import com.mapbox.navigator.RerouteCallback
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteErrorType
import com.mapbox.navigator.RerouteInfo
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RerouteControllerAdapterTest {

    private companion object {
        fun provideRerouteControllerAdapter(
            accessToken: String = "pk.***",
            platformRerouteObserver: RerouteControllersManager.Observer = mockk(),
            navigationRerouteController: NavigationRerouteController = mockk(),
        ): Pair<RerouteControllerAdapter, Wrapper> = RerouteControllerAdapter(
            accessToken,
            platformRerouteObserver,
            navigationRerouteController,
        ) to Wrapper(
            navigationRerouteController,
            platformRerouteObserver,
        )

        private class Wrapper(
            val navigationRerouteController: NavigationRerouteController,
            val platformRerouteObserver: RerouteControllersManager.Observer = mockk(),
        )
    }

    @Test
    fun `reroute success`() {
        mockkStatic(
            "com.mapbox.navigation.navigator.internal.RouterMapper",
            "com.mapbox.navigation.base.internal.utils.RouterEx"
        ) {
            mockkStatic(DirectionsJsonObject::class) {
                val (rerouteControllerAdapter, wrapper) = provideRerouteControllerAdapter()
                val navRoute = mockk<NavigationRoute>()
                val navRoutesList = listOf(navRoute)
                val directionsResponseJson = "{directions routes}"
                val directionsUrl = "some.url"
                every {
                    navRoutesList.mapToDirectionsResponse().toJson()
                } returns directionsResponseJson
                every {
                    navRoute.routeOptions.toUrl(any()).toString()
                } returns directionsUrl
                every {
                    wrapper.navigationRerouteController
                        .reroute(any<NavigationRerouteController.RoutesCallback>())
                } answers {
                    firstArg<NavigationRerouteController.RoutesCallback>()
                        .onNewRoutes(navRoutesList, RouterOrigin.Onboard)
                }
                every {
                    wrapper.platformRerouteObserver.onNewRoutes(navRoutesList)
                } just runs
                val slotExpected = slot<Expected<RerouteError, RerouteInfo>>()
                val mockRerouteCallback = mockk<RerouteCallback>(relaxUnitFun = true) {
                    every { run(capture(slotExpected)) } just runs
                }

                rerouteControllerAdapter.reroute("url.com", mockRerouteCallback)

                assertTrue(slotExpected.isCaptured)
                assertTrue(slotExpected.captured.isValue)
                with(slotExpected.captured.value!!) {
                    assertEquals(
                        com.mapbox.navigator.RouterOrigin.ONBOARD,
                        origin
                    )
                    assertEquals(
                        directionsUrl,
                        routeRequest
                    )
                    assertEquals(
                        directionsResponseJson,
                        routeResponse
                    )
                }
                verifyOnce {
                    wrapper.platformRerouteObserver.onNewRoutes(navRoutesList)
                }
            }
        }
    }

    @Test
    fun `reroute error`() {
        val (rerouteControllerAdapter, wrapper) = provideRerouteControllerAdapter()
        every {
            wrapper.navigationRerouteController
                .reroute(any<NavigationRerouteController.RoutesCallback>())
        } answers {
            firstArg<NavigationRerouteController.RoutesCallback>()
                .onNewRoutes(emptyList(), RouterOrigin.Onboard)
        }
        val slotExpected = slot<Expected<RerouteError, RerouteInfo>>()
        val mockRerouteCallback = mockk<RerouteCallback>(relaxUnitFun = true) {
            every { run(capture(slotExpected)) } just runs
        }

        rerouteControllerAdapter.reroute("url.com", mockRerouteCallback)

        assertTrue(slotExpected.isCaptured)
        assertTrue(slotExpected.captured.isError)
        with(slotExpected.captured.error!!) {
            assertEquals(
                RerouteControllerAdapter.ERROR_EMPTY_NAVIGATION_ROUTES_LIST,
                message
            )
            assertEquals(
                RerouteErrorType.ROUTER_ERROR,
                type
            )
        }
        verifyNoOne {
            wrapper.platformRerouteObserver.onNewRoutes(any())
        }
    }

    @Test
    fun `cancel reroute`() {
        val (rerouteControllerAdapter, wrapper) = provideRerouteControllerAdapter()
        every { wrapper.navigationRerouteController.interrupt() } just runs

        rerouteControllerAdapter.cancel()

        verify(exactly = 1) {
            wrapper.navigationRerouteController.interrupt()
        }
        verifyNoOne {
            wrapper.platformRerouteObserver.onNewRoutes(any())
        }
    }
}
