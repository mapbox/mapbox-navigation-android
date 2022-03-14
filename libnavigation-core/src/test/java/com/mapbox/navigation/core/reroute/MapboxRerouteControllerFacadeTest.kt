package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toNavigationRoutes
import com.mapbox.navigation.testing.verifyOnce
import com.mapbox.navigator.RerouteError
import com.mapbox.navigator.RerouteInfo
import com.mapbox.navigator.RerouteObserver
import com.mapbox.navigator.RouterOrigin
import io.mockk.CapturingSlot
import io.mockk.Ordering
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class MapboxRerouteControllerFacadeTest {

    private lateinit var rerouteObserver: RerouteObserver

    private companion object {
        private fun provideMapboxRerouteControllerFacade(
            platformRerouteObserver: RerouteControllersManager.Observer = mockk(),
            nativeRerouteController: NativeExtendedRerouteControllerInterface =
                mockk(relaxUnitFun = true),
        ): Pair<MapboxRerouteControllerFacade, MocksWrapper> {

            val slotRerouteObserver = slot<RerouteObserver>()
            every {
                nativeRerouteController.addRerouteObserver(capture(slotRerouteObserver))
            } just runs

            return MapboxRerouteControllerFacade(
                platformRerouteObserver,
                nativeRerouteController
            ) to MocksWrapper(
                platformRerouteObserver,
                nativeRerouteController,
                slotRerouteObserver,
            )
        }

        private data class MocksWrapper(
            val platformRerouteObserver: RerouteControllersManager.Observer,
            val nativeRerouteController: NativeExtendedRerouteControllerInterface,
            val slotRerouteObserver: CapturingSlot<RerouteObserver>,
        )

        private fun mockkStaticMappersAndNavigationRoute(inMockContext: () -> Unit) {
            mockkObject(NavigationRoute) {
                mockkStatic(DirectionsResponse::class, RouteOptions::class) {
                    mockkStatic("com.mapbox.navigation.base.route.NavigationRouteEx") {
                        inMockContext()
                    }
                }
            }
        }
    }

    @Test
    fun sanity() {
        val (rerouteControllerFacade, wrapper) = provideMapboxRerouteControllerFacade()

        assertEquals(RerouteState.Idle, rerouteControllerFacade.state)
        verify(exactly = 1) { wrapper.nativeRerouteController.addRerouteObserver(any()) }
        assertTrue(wrapper.slotRerouteObserver.isCaptured)
    }

    @Test
    fun `state is changed based on RouterObserver`() {
        mockkStaticMappersAndNavigationRoute {
            val (rerouteControllerFacade, wrapper) = provideMapboxRerouteControllerFacade()
            every { wrapper.platformRerouteObserver.onNewRoutes(any()) } returns mockk()
            val mockkNavRoutes = listOf<NavigationRoute>(mockk())
            every { RouteOptions.fromUrl(any()) } returns mockk()
            every { DirectionsResponse.fromJson(any(), any()) } returns mockk {
                every { routes().toNavigationRoutes(any()) } returns mockkNavRoutes
            }
            val errorMessage1 = "Error message 1"
            val mockRerouteStateObserver: RerouteController.RerouteStateObserver =
                mockk(relaxUnitFun = true)

            rerouteControllerFacade.registerRerouteStateObserver(mockRerouteStateObserver)
            with(wrapper.slotRerouteObserver.captured) {
                onRerouteDetected("")
                onRerouteReceived("", "https://any.url", RouterOrigin.ONBOARD)

                onRerouteDetected("")
                onRerouteCancelled()

                onRerouteDetected("")
                onRerouteFailed(mockk { every { message } returns errorMessage1 })
            }

            assertTrue(wrapper.slotRerouteObserver.isCaptured)
            verify(ordering = Ordering.SEQUENCE) {
                mockRerouteStateObserver.onRerouteStateChanged(RerouteState.Idle)

                mockRerouteStateObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                mockRerouteStateObserver.onRerouteStateChanged(
                    RerouteState.RouteFetched(
                        com.mapbox.navigation.base.route.RouterOrigin.Onboard
                    )
                )
                mockRerouteStateObserver.onRerouteStateChanged(RerouteState.Idle)

                mockRerouteStateObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                mockRerouteStateObserver.onRerouteStateChanged(RerouteState.Interrupted)
                mockRerouteStateObserver.onRerouteStateChanged(RerouteState.Idle)

                mockRerouteStateObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                mockRerouteStateObserver.onRerouteStateChanged(any<RerouteState.Failed>())
                mockRerouteStateObserver.onRerouteStateChanged(RerouteState.Idle)
            }
        }
    }

    @Test
    fun `switch to alternative`() {
        mockkStaticMappersAndNavigationRoute {
            val (rerouteControllerFacade, wrapper) = provideMapboxRerouteControllerFacade()
            every { NavigationRoute.create(any<String>(), any(), any()) } returns listOf(mockk())
            every { wrapper.platformRerouteObserver.onNewRoutes(any()) } returns mockk()
            val mockRerouteStateObserver: RerouteController.RerouteStateObserver =
                mockk(relaxUnitFun = true)

            rerouteControllerFacade.registerRerouteStateObserver(mockRerouteStateObserver)

            with(wrapper.slotRerouteObserver.captured) {
                onSwitchToAlternative(
                    mockk {
                        every { routerOrigin } returns RouterOrigin.ONBOARD
                        every { requestUri } returns ""
                        every { responseJson } returns ""
                    }
                )
            }

            assertTrue(wrapper.slotRerouteObserver.isCaptured)
            verifyOnce {
                mockRerouteStateObserver.onRerouteStateChanged(RerouteState.Idle)
            }
            verifyOnce {
                wrapper.platformRerouteObserver.onNewRoutes(any())
            }
        }
    }

    @Test
    fun `on reroute`() {
        data class Case(
            val descriptions: String,
            val expected: Expected<RerouteError, RerouteInfo> =
                ExpectedFactory.createValue(
                    RerouteInfo(
                        "", "https://any.url", RouterOrigin.ONBOARD,
                    )
                ),
            val preAction: (MapboxRerouteControllerFacade, MocksWrapper) -> Unit,
            val postAction: (MapboxRerouteControllerFacade, MocksWrapper) -> Unit,
            val checks: (String, MocksWrapper, NavigationRerouteController.RoutesCallback) -> Unit,
        )
        listOf(
            Case(
                "Plain Reroute",
                preAction = { rerouteFacade, wrapper -> },
                postAction = { rerouteFacade, wrapper -> },
                checks = { description, wrapper, rerouteCallback ->
                    verify(exactly = 1) { rerouteCallback.onNewRoutes(any(), any()) }
                    verify(exactly = 0) { wrapper.nativeRerouteController.cancel() }
                },
            ),
            Case(
                "Reroute when another reroute runs",
                preAction = { rerouteFacade, wrapper ->
                    rerouteFacade.state = RerouteState.FetchingRoute
                },
                postAction = { rerouteFacade, wrapper -> },
                checks = { description, wrapper, rerouteCallback ->
                    verify(exactly = 1) { wrapper.nativeRerouteController.cancel() }
                    verify(exactly = 1) { rerouteCallback.onNewRoutes(any(), any()) }
                },
            ),
            Case(
                "Reroute error",
                expected = ExpectedFactory.createError(mockk()),
                preAction = { rerouteFacade, wrapper -> },
                postAction = { rerouteFacade, wrapper -> },
                checks = { description, wrapper, rerouteCallback ->
                    verify(exactly = 0) { rerouteCallback.onNewRoutes(any(), any()) }
                    verify(exactly = 0) { wrapper.nativeRerouteController.cancel() }
                },
            ),
        ).forEach { (description, expected, preAction, postAction, checks) ->
            mockkStaticMappersAndNavigationRoute {
                val mockkNavRoutes = listOf<NavigationRoute>(mockk())
                every { RouteOptions.fromUrl(any()) } returns mockk()
                every { DirectionsResponse.fromJson(any(), any()) } returns mockk {
                    every { routes().toNavigationRoutes(any()) } returns mockkNavRoutes
                }
                val (rerouteControllerFacade, wrapper) = provideMapboxRerouteControllerFacade()
                every { wrapper.nativeRerouteController.forceReroute() } just runs
                every { wrapper.nativeRerouteController.cancel() } just runs
                every {
                    wrapper.nativeRerouteController.setRerouteCallbackListener(any())
                } answers {
                    firstArg<((Expected<RerouteError, RerouteInfo>) -> Unit)?>()?.invoke(expected)
                }
                val mockNavRerouteCallback: NavigationRerouteController.RoutesCallback =
                    mockk(relaxUnitFun = true)

                preAction(rerouteControllerFacade, wrapper)
                rerouteControllerFacade.reroute(mockNavRerouteCallback)
                postAction(rerouteControllerFacade, wrapper)

                verify(ordering = Ordering.ORDERED) {
                    wrapper.nativeRerouteController.setRerouteCallbackListener(any())
                    wrapper.nativeRerouteController.forceReroute()
                }
                checks(description, wrapper, mockNavRerouteCallback)
            }
        }
    }

    @Test
    fun setRerouteOptionsAdapter() {
        val (rerouteControllerFacade, wrapper) = provideMapboxRerouteControllerFacade()
        val mockRerouteOptionsAdapter: RerouteOptionsAdapter = mockk()
        every {
            wrapper.nativeRerouteController.setRerouteOptionsAdapter(mockRerouteOptionsAdapter)
        } just runs

        rerouteControllerFacade.setRerouteOptionsAdapter(mockRerouteOptionsAdapter)

        verify(exactly = 1) {
            wrapper.nativeRerouteController.setRerouteOptionsAdapter(mockRerouteOptionsAdapter)
        }
    }

    @Test
    fun `interrupt reroute request`() {
        val (rerouteControllerFacade, wrapper) = provideMapboxRerouteControllerFacade()
        every { wrapper.nativeRerouteController.cancel() } just runs

        rerouteControllerFacade.interrupt()

        verifyOnce {
            wrapper.nativeRerouteController.cancel()
        }
    }

    @Test
    fun `register new observer`() {
        val (rerouteControllerFacade, _) = provideMapboxRerouteControllerFacade()
        val mockRerouteStateObserver: RerouteController.RerouteStateObserver = mockk {
            every { onRerouteStateChanged(any()) } just runs
        }

        rerouteControllerFacade.registerRerouteStateObserver(mockRerouteStateObserver)

        verifyOnce {
            mockRerouteStateObserver.onRerouteStateChanged(any())
        }
    }
}
