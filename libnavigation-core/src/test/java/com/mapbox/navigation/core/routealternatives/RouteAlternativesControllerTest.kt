package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.MockLoggerRule
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesControllerInterface
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteIntersection
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL
import java.util.UUID

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class RouteAlternativesControllerTest {

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val genericURL = URL("https://mock_request_url")

    private val controllerInterface: RouteAlternativesControllerInterface = mockk(relaxed = true)
    private val navigator: MapboxNativeNavigator = mockk {
        every { routeAlternativesController } returns controllerInterface
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

    @Before
    fun setup() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher

        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any())
        } answers {
            val routesCount =
                JSONObject(this.firstArg<String>())
                    .getJSONArray("routes")
                    .length()
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeId } returns "$it"
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
        unmockkObject(NativeRouteParserWrapper)
    }

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

        routeAlternativesController.register(mockk<RouteAlternativesObserver>(relaxed = true))
        routeAlternativesController.register(mockk<RouteAlternativesObserver>(relaxed = true))

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
            mockkStatic(RouteOptions::fromUrl)
            every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>()
            every { tripSession.getRouteProgress() } returns routeProgress

            val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
            val secondObserver: RouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(firstObserver)
            routeAlternativesController.register(secondObserver)
            nativeObserver.captured.onRouteAlternativesChanged(
                listOf(
                    createNativeAlternativeMock()
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

            unmockkStatic(RouteOptions::fromUrl)
        }

    @Test
    fun `should not broadcast current route with alternative`() = coroutineRule.runBlockingTest {
        mockkStatic(RouteOptions::fromUrl)
        every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk {
            every { navigationRoute } returns mockk {
                every { routeOptions } returns mockk()
                every { directionsRoute } returns mockk(relaxed = true) {
                    every { duration() } returns 200.0
                }
            }
        }

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                createNativeAlternativeMock()
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
            200.0,
            routeProgressSlot.captured.navigationRoute.directionsRoute.duration(),
            0.01
        )
        assertEquals(383.222, alternativesSlot.captured[0].duration(), 0.01)
        assertEquals(RouterOrigin.Onboard, routerOriginSlot.captured)
        assertFalse(
            alternativesSlot.captured.contains(
                routeProgressSlot.captured.navigationRoute.directionsRoute
            )
        )

        unmockkStatic(RouteOptions::fromUrl)
    }

    @Test
    fun `should set RouteOptions to alternative routes`() = coroutineRule.runBlockingTest {
        val mockRouteOptions = mockk<RouteOptions>()
        mockkStatic(RouteOptions::fromUrl)
        every { RouteOptions.fromUrl(eq(genericURL)) } returns mockRouteOptions
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk()

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                createNativeAlternativeMock()
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
            mockRouteOptions,
            alternativesSlot.captured.first().routeOptions()
        )

        unmockkStatic(RouteOptions::fromUrl)
    }

    @Test
    fun `should set route index and UUID of alternative routes for refresh`() {
        mockkStatic(RouteOptions::fromUrl)
        every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk()

        val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                createNativeAlternativeMock()
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
        assertEquals("0", alternativesSlot.captured[0].routeIndex())
        assertEquals(
            "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==",
            alternativesSlot.captured[0].requestUuid()
        )

        unmockkStatic(RouteOptions::fromUrl)
    }

    @Test
    fun `broadcasts correct alternatives origin`() = coroutineRule.runBlockingTest {
        mockkStatic(RouteOptions::fromUrl)
        every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk()

        val observer: RouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(observer)

        val firstAlternative: RouteAlternative = createNativeAlternativeMock()
        nativeObserver.captured.onRouteAlternativesChanged(
            listOf(
                firstAlternative
            ),
            emptyList()
        )
        val secondAlternative: RouteAlternative = createNativeAlternativeMock().apply {
            every { route.routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
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

        unmockkStatic(RouteOptions::fromUrl)
    }

    @Test
    fun `broadcasts cached origin of previous update if there were no new routes`() =
        coroutineRule.runBlockingTest {
            mockkStatic(RouteOptions::fromUrl)
            every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            every { tripSession.getRouteProgress() } returns mockk()

            val observer: RouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(observer)

            val firstAlternative: RouteAlternative = createNativeAlternativeMock().apply {
                every { route.routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
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

            unmockkStatic(RouteOptions::fromUrl)
        }

    @Test
    fun `should notify callback when on-demand alternatives refresh finishes`() {
        mockkStatic(RouteOptions::fromUrl)
        every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RefreshAlternativesCallback>()
        val alternative: RouteAlternative = createNativeAlternativeMock()
        val expected = ExpectedFactory.createValue<String, List<RouteAlternative>>(
            listOf(alternative)
        )
        every { controllerInterface.refreshImmediately(capture(nativeObserver)) } answers {
            nativeObserver.captured.run(expected)
        }
        every { tripSession.getRouteProgress() } returns mockk {
            every { navigationRoute } returns mockk {
                every { routeOptions } returns mockk()
                every { directionsRoute } returns mockk(relaxed = true)
            }
        }

        val callback = mockk<NavigationRouteAlternativesRequestCallback>(relaxUnitFun = true)
        routeAlternativesController.triggerAlternativeRequest(callback)

        val routeProgressSlot = slot<RouteProgress>()
        val alternativesSlot = slot<List<NavigationRoute>>()
        val routerOriginSlot = slot<RouterOrigin>()
        verify(exactly = 1) {
            callback.onRouteAlternativeRequestFinished(
                capture(routeProgressSlot),
                capture(alternativesSlot),
                capture(routerOriginSlot)
            )
        }
        assertEquals(0, alternativesSlot.captured[0].routeIndex)
        assertEquals(
            "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==",
            alternativesSlot.captured[0].directionsResponse.uuid()
        )

        unmockkStatic(RouteOptions::fromUrl)
    }

    @Test
    fun `should notify callback when on-demand alternatives refresh fails - no progress`() {
        mockkStatic(RouteOptions::fromUrl)
        every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RefreshAlternativesCallback>()
        val alternative: RouteAlternative = createNativeAlternativeMock()
        val expected = ExpectedFactory.createValue<String, List<RouteAlternative>>(
            listOf(alternative)
        )
        every { controllerInterface.refreshImmediately(capture(nativeObserver)) } answers {
            nativeObserver.captured.run(expected)
        }
        every { tripSession.getRouteProgress() } returns null

        val callback = mockk<NavigationRouteAlternativesRequestCallback>(relaxUnitFun = true)
        routeAlternativesController.triggerAlternativeRequest(callback)

        verify(exactly = 1) {
            callback.onRouteAlternativesRequestError(
                RouteAlternativesError(
                    message = """
                        |Route progress not available, ignoring alternatives update.
                        |Continuous alternatives are only available in active guidance.
                    """.trimMargin()
                )
            )
        }

        unmockkStatic(RouteOptions::fromUrl)
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
        every { tripSession.getRouteProgress() } returns mockk()

        val callback = mockk<NavigationRouteAlternativesRequestCallback>(relaxUnitFun = true)
        routeAlternativesController.triggerAlternativeRequest(callback)

        verify(exactly = 1) {
            callback.onRouteAlternativesRequestError(
                RouteAlternativesError(
                    message = "error"
                )
            )
        }
    }

    @Test
    fun `processing job should be canceled if it doesn't keep up`() =
        coroutineRule.runBlockingTest {
            mockkStatic(RouteOptions::fromUrl)
            every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>()
            every { tripSession.getRouteProgress() } returns routeProgress

            val observer: RouteAlternativesObserver = mockk(relaxed = true)

            routeAlternativesController.register(observer)
            pauseDispatcher {
                nativeObserver.captured.onRouteAlternativesChanged(
                    listOf(
                        createNativeAlternativeMock()
                    ),
                    emptyList()
                )
                nativeObserver.captured.onRouteAlternativesChanged(
                    listOf(
                        createNativeAlternativeMock().apply {
                            every {
                                route.routerOrigin
                            } returns com.mapbox.navigator.RouterOrigin.ONLINE
                        }
                    ),
                    emptyList()
                )
            }

            val originSlot = slot<RouterOrigin>()
            verify(exactly = 1) {
                observer.onRouteAlternatives(any(), any(), capture(originSlot))
            }
            assertEquals(RouterOrigin.Offboard, originSlot.captured)

            unmockkStatic(RouteOptions::fromUrl)
        }

    @Test
    fun `metadata for alternative available when generated by native observer`() =
        coroutineRule.runBlockingTest {

            mockkStatic(RouteOptions::fromUrl)
            every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>()
            every { tripSession.getRouteProgress() } returns routeProgress

            val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(firstObserver)
            nativeObserver.captured.onRouteAlternativesChanged(
                listOf(
                    createNativeAlternativeMock()
                ),
                emptyList()
            )

            val firstRoutesSlot = slot<List<DirectionsRoute>>()
            verify(exactly = 1) {
                firstObserver.onRouteAlternatives(routeProgress, capture(firstRoutesSlot), any())
            }
            val alternativeRoute = firstRoutesSlot.captured.first()

            val metadata =
                routeAlternativesController.getMetadataFor(alternativeRoute.toNavigationRoute())

            assertEquals(platformForkAlt, metadata!!.forkIntersectionOfAlternativeRoute)
            assertEquals(platformForkMain, metadata.forkIntersectionOfPrimaryRoute)
            assertEquals(platformInfoStart, metadata.infoFromStartOfPrimary)
            assertEquals(platformInfoFork, metadata.infoFromFork)

            unmockkStatic(RouteOptions::fromUrl)
        }

    @Test
    fun `metadata for alternative available when set after processing routes`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val routeProgress = mockk<RouteProgress>()
            every { tripSession.getRouteProgress() } returns routeProgress

            val navigationRoute = mockk<NavigationRoute>(relaxed = true) {
                every { id } returns "route_id"
            }
            routeAlternativesController.processAlternativesMetadata(
                routes = listOf(navigationRoute),
                nativeAlternatives = listOf(
                    createNativeAlternativeMock().apply {
                        every { route.routeId } returns "route_id"
                    }
                )
            )
            val metadata = routeAlternativesController.getMetadataFor(navigationRoute)

            assertEquals(platformForkAlt, metadata!!.forkIntersectionOfAlternativeRoute)
            assertEquals(platformForkMain, metadata.forkIntersectionOfPrimaryRoute)
            assertEquals(platformInfoStart, metadata.infoFromStartOfPrimary)
            assertEquals(platformInfoFork, metadata.infoFromFork)

            unmockkStatic(RouteOptions::fromUrl)
        }

    @Test
    fun `metadata cleared on routes update`() = coroutineRule.runBlockingTest {
        val routeAlternativesController = createRouteAlternativesController()
        val routeProgress = mockk<RouteProgress>()
        every { tripSession.getRouteProgress() } returns routeProgress

        val navigationRoute = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "route_id"
        }
        routeAlternativesController.processAlternativesMetadata(
            routes = listOf(navigationRoute),
            nativeAlternatives = listOf(
                createNativeAlternativeMock().apply {
                    every { route.routeId } returns "route_id"
                }
            )
        )
        routeAlternativesController.processAlternativesMetadata(
            routes = emptyList(),
            nativeAlternatives = emptyList()
        )
        val metadata = routeAlternativesController.getMetadataFor(navigationRoute)

        assertNull(metadata)

        unmockkStatic(RouteOptions::fromUrl)
    }

    @Test
    fun `metadata cleared on alternatives update from native observer`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>()
            every { tripSession.getRouteProgress() } returns routeProgress
            routeAlternativesController.register(
                mockk<NavigationRouteAlternativesObserver>(relaxUnitFun = true)
            )

            val navigationRoute = mockk<NavigationRoute>(relaxed = true) {
                every { id } returns "route_id"
            }
            val nativeAlternative = createNativeAlternativeMock().apply {
                every { route.routeId } returns "route_id"
            }
            routeAlternativesController.processAlternativesMetadata(
                routes = listOf(navigationRoute),
                nativeAlternatives = listOf(nativeAlternative)
            )
            nativeObserver.captured.onRouteAlternativesChanged(
                emptyList(),
                listOf(nativeAlternative)
            )
            val metadata = routeAlternativesController.getMetadataFor(navigationRoute)

            assertNull(metadata)
        }

    @Test
    fun `native updates ignored when controller paused, and resumed successfully`() =
        coroutineRule.runBlockingTest {
            mockkStatic(RouteOptions::fromUrl)
            every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>()
            every { tripSession.getRouteProgress() } returns routeProgress
            val firstObserver: RouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(firstObserver)

            routeAlternativesController.pauseUpdates()
            nativeObserver.captured.onRouteAlternativesChanged(
                listOf(
                    createNativeAlternativeMock()
                ),
                emptyList()
            )

            verify(exactly = 0) {
                firstObserver.onRouteAlternatives(any(), any(), any())
            }

            routeAlternativesController.resumeUpdates()
            nativeObserver.captured.onRouteAlternativesChanged(
                listOf(
                    createNativeAlternativeMock()
                ),
                emptyList()
            )

            verify(exactly = 1) {
                firstObserver.onRouteAlternatives(any(), any(), any())
            }

            unmockkStatic(RouteOptions::fromUrl)
        }

    private val nativeInfoFork = com.mapbox.navigator.AlternativeRouteInfo(
        100.0, // distance
        200.0, // duration
    )
    private val nativeInfoStart = com.mapbox.navigator.AlternativeRouteInfo(
        300.0, // distance
        400.0, // duration
    )
    private val nativeAltForkPoint = mockk<Point>()
    private val nativeForkAlt = RouteIntersection(
        nativeAltForkPoint,
        1, // geometryIndex
        2, // segmentIndex
        3, // legIndex
    )
    private val nativeMainForkPoint = mockk<Point>()
    private val nativeForkMain = RouteIntersection(
        nativeMainForkPoint,
        4, // geometryIndex
        5, // segmentIndex
        6, // legIndex
    )

    private val platformInfoFork = AlternativeRouteInfo(
        distance = 100.0, // distance
        duration = 200.0, // duration
    )
    private val platformInfoStart = AlternativeRouteInfo(
        distance = 300.0, // distance
        duration = 400.0, // duration
    )
    private val platformForkAlt = AlternativeRouteIntersection(
        nativeAltForkPoint,
        geometryIndexInRoute = 1, // geometryIndex
        geometryIndexInLeg = 2, // segmentIndex
        legIndex = 3, // legIndex
    )
    private val platformForkMain = AlternativeRouteIntersection(
        nativeMainForkPoint,
        geometryIndexInRoute = 4, // geometryIndex
        geometryIndexInLeg = 5, // segmentIndex
        legIndex = 6, // legIndex
    )

    private fun createNativeAlternativeMock(): RouteAlternative {
        return mockk {
            every { route.routeId } returns UUID.randomUUID().toString()
            every { route.responseJson } returns FileUtils.loadJsonFixture(
                "route_alternative_from_native.json"
            )
            every { route.requestUri } returns genericURL.toString()
            every { route.routeIndex } returns 0
            every {
                route.routerOrigin
            } returns com.mapbox.navigator.RouterOrigin.ONBOARD
            every { isNew } returns true
            every { alternativeRouteFork } returns nativeForkAlt
            every { mainRouteFork } returns nativeForkMain
            every { infoFromFork } returns nativeInfoFork
            every { infoFromStart } returns nativeInfoStart
        }
    }
}
