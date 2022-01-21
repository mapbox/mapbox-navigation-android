package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.NativeRouteProcessingListener
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesControllerInterface
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class RouteAlternativesControllerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val controllerInterface: RouteAlternativesControllerInterface = mockk(relaxed = true)
    private val navigator: MapboxNativeNavigator = mockk {
        every { createRouteAlternativesController() } returns controllerInterface
    }
    private val tripSession: TripSession = mockk(relaxUnitFun = true)

    private fun createRouteAlternativesController(
        options: RouteAlternativesOptions = RouteAlternativesOptions.Builder().build()
    ) = RouteAlternativesController(
        options,
        navigator,
        tripSession,
        ThreadController(),
    )

    @Test
    fun `should set refreshIntervalSeconds from options`() {
        // Capture the route options set
        val nativeOptions = slot<com.mapbox.navigator.RouteAlternativesOptions>()
        every { controllerInterface.setRouteAlternativesOptions(capture(nativeOptions)) } just runs

        // Construct a native route alternatives interface
        createRouteAlternativesController(
            options = RouteAlternativesOptions.Builder()
                .intervalMillis(59_135L)
                .build()
        )

        // Verify the conversion was accurate
        assertEquals(
            59.0,
            nativeOptions.captured.requestIntervalSeconds,
            0.001
        )
    }

    @Test
    fun `should set minTimeBeforeManeuverSeconds from options`() {
        // Capture the route options set
        val nativeOptions = slot<com.mapbox.navigator.RouteAlternativesOptions>()
        every { controllerInterface.setRouteAlternativesOptions(capture(nativeOptions)) } just runs

        // Construct a native route alternatives interface
        createRouteAlternativesController(
            options = RouteAlternativesOptions.Builder()
                .avoidManeuverSeconds(15)
                .build()
        )

        // Verify the conversion was accurate
        assertEquals(
            15.0,
            nativeOptions.captured.minTimeBeforeManeuverSeconds,
            0.001
        )
    }

    @Test
    fun `should add a single nav-native observer when registering listeners`() {
        val routeAlternativesController = createRouteAlternativesController()

        routeAlternativesController.register(mockk(relaxed = true))
        routeAlternativesController.register(mockk(relaxed = true))

        verify(exactly = 1) { controllerInterface.addObserver(any()) }
        verify(exactly = 0) { controllerInterface.removeObserver(any()) }
    }

    @Test
    fun `should removeObserver from nav-native interface when all observers are removed`() {
        val routeAlternativesController = createRouteAlternativesController()

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        val secondObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        routeAlternativesController.register(secondObserver)
        routeAlternativesController.unregister(firstObserver)
        routeAlternativesController.unregister(secondObserver)

        verify(exactly = 1) { controllerInterface.addObserver(any()) }
        verify(exactly = 1) { controllerInterface.removeObserver(any()) }
    }

    @Test
    fun `should removeAllObservers from nav-native interface when unregisterAll is called`() {
        val routeAlternativesController = createRouteAlternativesController()

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        val secondObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        routeAlternativesController.register(secondObserver)
        routeAlternativesController.unregisterAll()

        verify(exactly = 1) { controllerInterface.addObserver(any()) }
        verify(exactly = 1) { controllerInterface.removeAllObservers() }
    }

    @Test
    fun `should broadcast alternative routes changes from nav-native`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress> {
                every { route } returns mockk(relaxed = true)
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
            val secondObserver: RouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(firstObserver)
            routeAlternativesController.register(secondObserver)
            val alternativeRouteJson = FileUtils.loadJsonFixture(
                "route_alternative_from_native.json"
            )
            nativeObserver.captured.onRouteAlternativesChanged(
                listOf(
                    mockk {
                        every { route.response } returns alternativeRouteJson
                        every {
                            route.routerOrigin
                        } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                        every { isNew } returns true
                    }
                ),
                emptyList()
            )

            val firstRoutesSlot = slot<List<DirectionsRoute>>()
            verify(exactly = 1) {
                firstObserver.onRouteAlternatives(routeProgress, capture(firstRoutesSlot), any())
            }
            assertEquals(1, firstRoutesSlot.captured.size)
            val secondRoutesSlot = slot<List<DirectionsRoute>>()
            verify(exactly = 1) {
                secondObserver.onRouteAlternatives(routeProgress, capture(secondRoutesSlot), any())
            }
            assertEquals(1, secondRoutesSlot.captured.size)
        }

    @Test
    fun `should not broadcast current route with alternative`() = coroutineRule.runBlockingTest {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk(relaxed = true) {
                every { duration() } returns 200.0
            }
        }

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        val alternativeRouteJson = FileUtils.loadJsonFixture(
            "route_alternative_from_native.json"
        )
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                mockk {
                    every { route.response } returns alternativeRouteJson
                    every { route.routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                    every { isNew } returns true
                }
            ),
            emptyList()
        )

        val routeProgressSlot = slot<RouteProgress>()
        val alternativesSlot = slot<List<DirectionsRoute>>()
        val routerOriginSlot = slot<RouterOrigin>()
        verify(exactly = 1) {
            firstObserver.onRouteAlternatives(
                capture(routeProgressSlot),
                capture(alternativesSlot),
                capture(routerOriginSlot)
            )
        }
        assertEquals(200.0, routeProgressSlot.captured.route.duration(), 0.01)
        assertEquals(383.222, alternativesSlot.captured[0].duration(), 0.01)
        assertEquals(RouterOrigin.Onboard, routerOriginSlot.captured)
        assertFalse(alternativesSlot.captured.contains(routeProgressSlot.captured.route))
    }

    @Test
    fun `should set RouteOptions to alternative routes`() = coroutineRule.runBlockingTest {
        val originalCoordinates = "-122.270375,37.801429;-122.271496, 37.799063"
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk {
                every { routeOptions() } returns mockk {
                    every { coordinates() } returns originalCoordinates
                }
            }
        }

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        val alternativeRouteJson = FileUtils.loadJsonFixture(
            "route_alternative_from_native.json"
        )
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                mockk {
                    mockk {
                        every { route.response } returns alternativeRouteJson
                        every {
                            route.routerOrigin
                        } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                        every { isNew } returns true
                    }
                }
            ),
            emptyList()
        )

        val routeProgressSlot = slot<RouteProgress>()
        val alternativesSlot = slot<List<DirectionsRoute>>()
        val routerOriginSlot = slot<RouterOrigin>()
        verify(exactly = 1) {
            firstObserver.onRouteAlternatives(
                capture(routeProgressSlot),
                capture(alternativesSlot),
                capture(routerOriginSlot)
            )
        }

        assertEquals(
            originalCoordinates, routeProgressSlot.captured.route.routeOptions()?.coordinates()
        )
        assertEquals(
            originalCoordinates, alternativesSlot.captured[0].routeOptions()?.coordinates()
        )
    }

    /**
     * workaround for a problem in Nav Native where calling #setRoute invokes alternative observer
     */
    @Test
    fun `should not broadcast alternative routes right after routes are set`() {
        val routeProcessingListenerSlot = slot<NativeRouteProcessingListener>()
        every {
            tripSession.registerNativeRouteProcessingListener(capture(routeProcessingListenerSlot))
        } just Runs
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        val routeProgress = mockk<RouteProgress> {
            every { route } returns mockk(relaxed = true)
        }
        every { tripSession.getRouteProgress() } returns routeProgress

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        val secondObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        routeAlternativesController.register(secondObserver)
        val alternativeRouteJson = FileUtils.loadJsonFixture(
            "route_alternative_from_native.json"
        )

        routeProcessingListenerSlot.captured.onNativeRouteProcessingStarted()
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                mockk {
                    mockk {
                        every { route.response } returns alternativeRouteJson
                        every {
                            route.routerOrigin
                        } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                        every { isNew } returns true
                    }
                }
            ),
            emptyList()
        )
        verify(exactly = 0) { firstObserver.onRouteAlternatives(routeProgress, any(), any()) }
        verify(exactly = 0) { secondObserver.onRouteAlternatives(routeProgress, any(), any()) }

        // subsequent updates should go through
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                mockk {
                    mockk {
                        every { route.response } returns alternativeRouteJson
                        every {
                            route.routerOrigin
                        } returns com.mapbox.navigator.RouterOrigin.ONLINE
                        every { isNew } returns true
                    }
                }
            ),
            emptyList()
        )
        verify(exactly = 1) { firstObserver.onRouteAlternatives(routeProgress, any(), any()) }
        verify(exactly = 1) { secondObserver.onRouteAlternatives(routeProgress, any(), any()) }
    }

    @Test
    fun `should set route index and UUID of alternative routes for refresh`() {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk(relaxed = true)
        }

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        val alternativeRouteJson = FileUtils.loadJsonFixture(
            "route_alternative_from_native.json"
        )
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                mockk {
                    mockk {
                        every { route.response } returns alternativeRouteJson
                        every {
                            route.routerOrigin
                        } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                        every { isNew } returns true
                    }
                }
            ),
            emptyList()
        )

        val routeProgressSlot = slot<RouteProgress>()
        val alternativesSlot = slot<List<DirectionsRoute>>()
        val routerOriginSlot = slot<RouterOrigin>()
        verify(exactly = 1) {
            firstObserver.onRouteAlternatives(
                capture(routeProgressSlot),
                capture(alternativesSlot),
                capture(routerOriginSlot)
            )
        }
        assertEquals("1", alternativesSlot.captured[0].routeIndex())
        assertEquals(
            "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==",
            alternativesSlot.captured[0].requestUuid()
        )
    }

    @Test
    fun `broadcasts correct alternatives origin`() = coroutineRule.runBlockingTest {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk(relaxed = true) {
                every { duration() } returns 200.0
            }
        }

        val observer: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(observer)

        val firstAlternative: RouteAlternative = mockk {
            every { route.response } returns FileUtils.loadJsonFixture(
                "route_alternative_from_native.json"
            )
            every { route.routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONBOARD
            every { isNew } returns true
        }
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                firstAlternative
            ),
            emptyList()
        )
        val secondAlternative: RouteAlternative = mockk {
            every { route.response } returns FileUtils.loadJsonFixture(
                "route_alternative_from_native.json"
            )
            every { route.routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
            every { isNew } returns true
        }
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                secondAlternative
            ),
            listOf(
                firstAlternative.apply {
                    every { isNew } returns false
                }
            )
        )

        val routeProgressSlots = mutableListOf<RouteProgress>()
        val alternativesSlots = mutableListOf<List<DirectionsRoute>>()
        val routerOriginSlots = mutableListOf<RouterOrigin>()
        verify(exactly = 2) {
            observer.onRouteAlternatives(
                capture(routeProgressSlots),
                capture(alternativesSlots),
                capture(routerOriginSlots),
            )
        }
        assertEquals(RouterOrigin.Offboard, routerOriginSlots.last())
    }

    @Test
    fun `broadcasts cached origin of previous update if there were no new routes`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            every { tripSession.getRouteProgress() } returns mockk {
                every { route } returns mockk(relaxed = true) {
                    every { duration() } returns 200.0
                }
            }

            val observer: RouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(observer)

            val firstAlternative: RouteAlternative = mockk {
                every { route.response } returns FileUtils.loadJsonFixture(
                    "route_alternative_from_native.json"
                )
                every { route.routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
                every { isNew } returns true
            }
            nativeObserver.captured.onRouteAlternativesChanged(
                listOf(
                    firstAlternative
                ),
                emptyList()
            )
            nativeObserver.captured.onRouteAlternativesChanged(
                emptyList(),
                listOf(
                    firstAlternative.apply {
                        every { isNew } returns false
                    }
                )
            )

            val routeProgressSlots = mutableListOf<RouteProgress>()
            val alternativesSlots = mutableListOf<List<DirectionsRoute>>()
            val routerOriginSlots = mutableListOf<RouterOrigin>()
            verify(exactly = 2) {
                observer.onRouteAlternatives(
                    capture(routeProgressSlots),
                    capture(alternativesSlots),
                    capture(routerOriginSlots),
                )
            }
            assertEquals(RouterOrigin.Offboard, routerOriginSlots.last())
        }

    @Test
    fun `should notify callback when on-demand alternatives refresh finishes`() {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RefreshAlternativesCallback>()
        val alternative: RouteAlternative = mockk {
            every { route.response } returns FileUtils.loadJsonFixture(
                "route_alternative_from_native.json"
            )
            every { route.routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
            every { isNew } returns true
        }
        val expected = ExpectedFactory.createValue<String, List<RouteAlternative>>(
            listOf(alternative)
        )
        every { controllerInterface.refreshImmediately(capture(nativeObserver)) } answers {
            nativeObserver.captured.run(expected)
        }
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk(relaxed = true)
        }

        val callback = mockk<RouteAlternativesRequestCallback>(relaxUnitFun = true)
        routeAlternativesController.triggerAlternativeRequest(callback)

        val routeProgressSlot = slot<RouteProgress>()
        val alternativesSlot = slot<List<DirectionsRoute>>()
        val routerOriginSlot = slot<RouterOrigin>()
        verify(exactly = 1) {
            callback.onRouteAlternativeRequestFinished(
                capture(routeProgressSlot),
                capture(alternativesSlot),
                capture(routerOriginSlot)
            )
        }
        assertEquals("1", alternativesSlot.captured[0].routeIndex())
        assertEquals(
            "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==",
            alternativesSlot.captured[0].requestUuid()
        )
    }

    @Test
    fun `should notify callback when on-demand alternatives refresh fails - no progress`() {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RefreshAlternativesCallback>()
        val alternative: RouteAlternative = mockk {
            every { route.response } returns FileUtils.loadJsonFixture(
                "route_alternative_from_native.json"
            )
            every { route.routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
            every { isNew } returns true
        }
        val expected = ExpectedFactory.createValue<String, List<RouteAlternative>>(
            listOf(alternative)
        )
        every { controllerInterface.refreshImmediately(capture(nativeObserver)) } answers {
            nativeObserver.captured.run(expected)
        }
        every { tripSession.getRouteProgress() } returns null

        val callback = mockk<RouteAlternativesRequestCallback>(relaxUnitFun = true)
        routeAlternativesController.triggerAlternativeRequest(callback)

        verify(exactly = 1) {
            callback.onRouteAlternativesAborted(
                """
                    |Route progress not available, ignoring alternatives update.
                    |Continuous alternatives are only available in active guidance.
                """.trimMargin(),
            )
        }
    }

    @Test
    fun `should notify callback when on-demand alternatives refresh fails - NN failure`() {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RefreshAlternativesCallback>()
        val expected = ExpectedFactory.createError<String, List<RouteAlternative>>(
            "error"
        )
        every { controllerInterface.refreshImmediately(capture(nativeObserver)) } answers {
            nativeObserver.captured.run(expected)
        }
        every { tripSession.getRouteProgress() } returns mockk {
            every { route } returns mockk(relaxed = true)
        }

        val callback = mockk<RouteAlternativesRequestCallback>(relaxUnitFun = true)
        routeAlternativesController.triggerAlternativeRequest(callback)

        verify(exactly = 1) {
            callback.onRouteAlternativesAborted("error")
        }
    }
}
