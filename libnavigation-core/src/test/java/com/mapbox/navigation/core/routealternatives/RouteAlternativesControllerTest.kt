package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.factories.createRouteInterface
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesControllerInterface
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

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class RouteAlternativesControllerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val nativeRouteParserRule = NativeRouteParserRule()

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
        every { ThreadController.DefaultDispatcher } returns coroutineRule.testDispatcher
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
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

            val firstObserver: NavigationRouteAlternativesObserver = mockk(relaxed = true)
            val secondObserver: NavigationRouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(firstObserver)
            routeAlternativesController.register(secondObserver)
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(
                    createNativeAlternativeMock()
                ),
                emptyList()
            )

            val firstRoutesSlot = slot<List<NavigationRoute>>()
            verify(exactly = 1) {
                firstObserver.onRouteAlternatives(routeProgress, capture(firstRoutesSlot), any())
            }
            assertEquals(1, firstRoutesSlot.captured.size)
            val secondRoutesSlot = slot<List<NavigationRoute>>()
            verify(exactly = 1) {
                secondObserver.onRouteAlternatives(routeProgress, capture(secondRoutesSlot), any())
            }
            assertEquals(1, secondRoutesSlot.captured.size)

            unmockkStatic(RouteOptions::fromUrl)
        }

    @Test
    fun `should broadcast alternative routes changes from nav-native with online primary route`() =
        coroutineRule.runBlockingTest {
            mockkStatic(RouteOptions::fromUrl)
            every { RouteOptions.fromUrl(eq(genericURL)) } returns mockk()
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>()
            every { tripSession.getRouteProgress() } returns routeProgress

            val firstObserver: NavigationRouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(firstObserver)
            val testOnlinePrimaryRoute = createRouteInterface(responseUUID = "test")
            nativeObserver.captured.onRouteAlternativesUpdated(
                testOnlinePrimaryRoute,
                listOf(
                    createNativeAlternativeMock()
                ),
                emptyList()
            )

            val firstRoutesSlot = slot<List<NavigationRoute>>()
            verify(exactly = 1) {
                firstObserver.onRouteAlternatives(routeProgress, capture(firstRoutesSlot), any())
            }
            assertEquals(2, firstRoutesSlot.captured.size)
            assertEquals(testOnlinePrimaryRoute.routeId, firstRoutesSlot.captured.first().id)

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

        val firstObserver: NavigationRouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
            listOf(
                createNativeAlternativeMock()
            ),
            emptyList()
        )

        val routeProgressSlot = slot<RouteProgress>()
        val alternativesSlot = slot<List<NavigationRoute>>()
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
        assertEquals(
            383.222,
            alternativesSlot.captured[0].directionsRoute.duration(),
            0.01
        )
        assertEquals(RouterOrigin.Onboard, routerOriginSlot.captured)
        assertFalse(
            alternativesSlot.captured.contains(
                routeProgressSlot.captured.navigationRoute
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

        val firstObserver: NavigationRouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
            listOf(
                createNativeAlternativeMock()
            ),
            emptyList()
        )

        val routeProgressSlot = slot<RouteProgress>()
        val alternativesSlot = slot<List<NavigationRoute>>()
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
            alternativesSlot.captured.first().directionsRoute.routeOptions()
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

        val firstObserver: NavigationRouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(firstObserver)
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
            listOf(
                createNativeAlternativeMock()
            ),
            emptyList()
        )

        val routeProgressSlot = slot<RouteProgress>()
        val alternativesSlot = slot<List<NavigationRoute>>()
        val routerOriginSlot = slot<RouterOrigin>()
        verify(exactly = 1) {
            firstObserver.onRouteAlternatives(
                capture(routeProgressSlot),
                capture(alternativesSlot),
                capture(routerOriginSlot)
            )
        }
        assertEquals("0", alternativesSlot.captured[0].directionsRoute.routeIndex())
        assertEquals(
            "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==",
            alternativesSlot.captured[0].directionsRoute.requestUuid()
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

        val observer: NavigationRouteAlternativesObserver = mockk(relaxed = true)
        routeAlternativesController.register(observer)

        val firstAlternative: RouteAlternative = createNativeAlternativeMock()
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
            listOf(
                firstAlternative
            ),
            emptyList()
        )
        val secondAlternative: RouteAlternative = createNativeAlternativeMock(
            routerOrigin = com.mapbox.navigator.RouterOrigin.ONLINE
        )
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
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
        val alternativesSlots = mutableListOf<List<NavigationRoute>>()
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

            val observer: NavigationRouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(observer)

            val firstAlternative: RouteAlternative = createNativeAlternativeMock(
                routerOrigin = com.mapbox.navigator.RouterOrigin.ONLINE
            )
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(
                    firstAlternative
                ),
                emptyList()
            )
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                emptyList(),
                listOf(
                    firstAlternative.apply {
                        every { isNew } returns false
                    }
                )
            )

            val routeProgressSlots = mutableListOf<RouteProgress>()
            val alternativesSlots = mutableListOf<List<NavigationRoute>>()
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

            val observer: NavigationRouteAlternativesObserver = mockk(relaxed = true)

            routeAlternativesController.register(observer)
            pauseDispatcher {
                nativeObserver.captured.onRouteAlternativesUpdated(
                    null,
                    listOf(
                        createNativeAlternativeMock()
                    ),
                    emptyList()
                )
                nativeObserver.captured.onRouteAlternativesUpdated(
                    null,
                    listOf(
                        createNativeAlternativeMock(
                            routerOrigin = com.mapbox.navigator.RouterOrigin.ONLINE
                        )
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

            val firstObserver: NavigationRouteAlternativesObserver = mockk(relaxed = true)
            routeAlternativesController.register(firstObserver)
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(
                    createNativeAlternativeMock(alternativeId = 4)
                ),
                emptyList()
            )

            val firstRoutesSlot = slot<List<NavigationRoute>>()
            verify(exactly = 1) {
                firstObserver.onRouteAlternatives(routeProgress, capture(firstRoutesSlot), any())
            }
            val alternativeRoute = firstRoutesSlot.captured.first()

            val metadata =
                routeAlternativesController.getMetadataFor(alternativeRoute)

            assertEquals(platformForkAlt, metadata!!.forkIntersectionOfAlternativeRoute)
            assertEquals(platformForkMain, metadata.forkIntersectionOfPrimaryRoute)
            assertEquals(platformInfoStart, metadata.infoFromStartOfPrimary)
            assertEquals(platformInfoFork, metadata.infoFromFork)
            assertEquals(4, metadata.alternativeId)

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
                every { route.routeInfo } returns mockk(relaxed = true)
            }
            routeAlternativesController.processAlternativesMetadata(
                routes = listOf(navigationRoute),
                nativeAlternatives = listOf(nativeAlternative)
            )
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                emptyList(),
                listOf(nativeAlternative)
            )
            val metadata = routeAlternativesController.getMetadataFor(navigationRoute)

            assertNull(metadata)
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

    private fun createNativeAlternativeMock(
        alternativeId: Int = 0,
        routerOrigin: com.mapbox.navigator.RouterOrigin = com.mapbox.navigator.RouterOrigin.ONBOARD
    ): RouteAlternative {
        val nativeRoute = createRouteInterface(
            responseJson = FileUtils.loadJsonFixture(
                "route_alternative_from_native.json"
            ),
            requestURI = genericURL.toString(),
            routerOrigin = routerOrigin
        )
        return mockk {
            every { route } returns nativeRoute
            every { isNew } returns true
            every { alternativeRouteFork } returns nativeForkAlt
            every { mainRouteFork } returns nativeForkMain
            every { infoFromFork } returns nativeInfoFork
            every { infoFromStart } returns nativeInfoStart
            every { id } returns alternativeId
        }
    }
}
