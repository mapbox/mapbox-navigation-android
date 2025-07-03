package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.route.isExpired
import com.mapbox.navigation.base.internal.utils.RouteParsingManager
import com.mapbox.navigation.base.internal.utils.createRouteParsingManager
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.findRoute
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.TestSystemClock
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteInterface
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteAlternativesControllerInterface
import com.mapbox.navigator.RouteAlternativesObserver
import com.mapbox.navigator.RouteIntersection
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class RouteAlternativesControllerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val nativeRouteParserRule = NativeRouteParserRule()

    @get:Rule
    val clock = TestSystemClock()

    private val routeRequestUrl = createRouteOptions().toUrl("***")

    private val controllerInterface: RouteAlternativesControllerInterface = mockk(relaxed = true)
    private val navigator: MapboxNativeNavigator = mockk {
        every { routeAlternativesController } returns controllerInterface
    }
    private val tripSession: TripSession = mockk(relaxed = true)

    private val directionSession: DirectionsSession = mockk(relaxed = true)

    private fun createRouteAlternativesController(
        options: RouteAlternativesOptions = RouteAlternativesOptions.Builder().build(),
        routeParsingManager: RouteParsingManager = createParsingManagerForTest(),
    ) = RouteAlternativesController(
        options,
        navigator,
        tripSession,
        ThreadController(),
        routeParsingManager,
        directionSession,
    )

    @Before
    fun setup() {
        mockkObject(ThreadController)

        mockkStatic(DirectionsSession::findRoute)
        every { directionSession.findRoute(any()) } returns null

        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
        every { ThreadController.DefaultDispatcher } returns coroutineRule.testDispatcher
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
        unmockkStatic(DirectionsSession::findRoute)
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
                .build(),
        )

        // Verify the conversion was accurate
        assertEquals(
            59.toShort(),
            nativeOptions.captured.requestIntervalSeconds,
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
                .build(),
        )

        // Verify the conversion was accurate
        assertEquals(
            15.0f,
            nativeOptions.captured.minTimeBeforeManeuverSeconds,
            0.001f,
        )
    }

    @Test
    fun `should removeObserver from nav-native interface when all observers are removed`() {
        val routeAlternativesController = createRouteAlternativesController()

        routeAlternativesController.setRouteUpdateSuggestionListener { }
        routeAlternativesController.setRouteUpdateSuggestionListener(null)

        verify(exactly = 1) { controllerInterface.addObserver(any()) }
        verify(exactly = 1) { controllerInterface.removeObserver(any()) }
    }

    @Test
    fun `should removeObserver from nav-native interface on pause`() {
        val routeAlternativesController = createRouteAlternativesController()

        routeAlternativesController.setRouteUpdateSuggestionListener { }
        routeAlternativesController.pause()

        verify(exactly = 1) { controllerInterface.addObserver(any()) }
        verify(exactly = 1) { controllerInterface.removeObserver(any()) }
    }

    @Test
    fun `should addObserver to nav-native interface on resume after pause`() {
        val routeAlternativesController = createRouteAlternativesController()

        routeAlternativesController.setRouteUpdateSuggestionListener { }
        routeAlternativesController.pause()
        routeAlternativesController.resume()

        verify(exactly = 2) { controllerInterface.addObserver(any()) }
        verify(exactly = 1) { controllerInterface.removeObserver(any()) }
    }

    @Test
    fun `should removeAllObservers from nav-native interface when unregisterAll is called`() {
        val routeAlternativesController = createRouteAlternativesController()

        routeAlternativesController.setRouteUpdateSuggestionListener { }
        routeAlternativesController.unregisterAll()

        verify(exactly = 1) { controllerInterface.addObserver(any()) }
        verify(exactly = 1) { controllerInterface.removeAllObservers() }
    }

    @Test
    fun `should set expiration time when new routes are received with refresh ttl`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns createNavigationRoute()
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            val testOnlinePrimaryRoute = createRouteInterface(
                responseJson = createDirectionsResponse(
                    routes = listOf(createDirectionsRoute(refreshTtl = 5)),
                ).toJson(),
                responseUUID = "test",
                routeGeometry = emptyList(),
            )
            nativeObserver.captured.onRouteAlternativesUpdated(
                testOnlinePrimaryRoute,
                listOf(
                    createNativeAlternativeMock(),
                ),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestions",
                routesUpdateSuggestion,
            )
            val newAlternatives = routesUpdateSuggestion!!.newRoutes.drop(1)

            clock.advanceTimeBy(4.seconds)
            assertFalse(newAlternatives[0].isExpired())
            clock.advanceTimeBy(2.seconds)
            assertTrue(newAlternatives[0].isExpired())
            clock.advanceTimeBy(3.seconds)
            assertFalse(newAlternatives[1].isExpired())
            clock.advanceTimeBy(2.seconds)
            assertTrue(newAlternatives[1].isExpired())
        }

    @Test
    fun `should not set expiration time when new routes are received without refresh ttl`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns createNavigationRoute()
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routeUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routeUpdateSuggestion = it
            }
            val testOnlinePrimaryRoute = createRouteInterface(
                responseJson = createDirectionsResponse(
                    routes = listOf(createDirectionsRoute(refreshTtl = null)),
                ).toJson(),
                responseUUID = "test",
                routeGeometry = emptyList(),
            )
            nativeObserver.captured.onRouteAlternativesUpdated(
                testOnlinePrimaryRoute,
                listOf(
                    createNativeAlternativeMock(
                        fileName = "route_alternative_from_native_without_refresh_ttl.json",
                    ),
                ),
                emptyList(),
            )

            assertNotNull(
                "no routes update where suggested",
                routeUpdateSuggestion,
            )
            val suggestedAlternatives = routeUpdateSuggestion!!.newRoutes.drop(1)

            clock.advanceTimeBy(4.seconds)
            assertFalse(suggestedAlternatives[0].isExpired())
            clock.advanceTimeBy(2.seconds)
            assertFalse(suggestedAlternatives[0].isExpired())
            clock.advanceTimeBy(3.seconds)
            assertFalse(suggestedAlternatives[1].isExpired())
            clock.advanceTimeBy(2.seconds)
            assertFalse(suggestedAlternatives[1].isExpired())
        }

    @Test
    fun `should not broadcast current route with alternative`() = coroutineRule.runBlockingTest {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
            every { navigationRoute } returns mockk {
                every { directionsRoute } returns mockk(relaxed = true) {
                    every { duration() } returns 200.0
                }
                every { origin } returns RouterOrigin.ONLINE
            }
        }

        var updateSuggestion: UpdateRouteSuggestion? = null
        routeAlternativesController.setRouteUpdateSuggestionListener {
            updateSuggestion = it
        }
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
            listOf(
                createNativeAlternativeMock(),
            ),
            emptyList(),
        )

        assertNotNull(
            "no route update suggestion received",
            updateSuggestion,
        )
        val newRoutes = updateSuggestion!!.newRoutes
        val primaryRoute = newRoutes[0]
        val alternativeRoutes = newRoutes.drop(1)
        assertEquals(
            200.0,
            primaryRoute.directionsRoute.duration(),
            0.01,
        )
        assertEquals(
            383.222,
            alternativeRoutes[0].directionsRoute.duration(),
            0.01,
        )
        clock.advanceTimeBy(9.seconds)
        assertFalse(alternativeRoutes[0].isExpired())
        clock.advanceTimeBy(2.seconds)
        assertTrue(alternativeRoutes[0].isExpired())

        assertFalse(
            alternativeRoutes.contains(
                primaryRoute,
            ),
        )
    }

    @Test
    fun `should set RouteOptions to alternative routes`() = coroutineRule.runBlockingTest {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
            every { navigationRoute } returns createNavigationRoute()
        }

        var updateRouteSuggestion: UpdateRouteSuggestion? = null
        routeAlternativesController.setRouteUpdateSuggestionListener {
            updateRouteSuggestion = it
        }
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
            listOf(
                createNativeAlternativeMock(),
            ),
            emptyList(),
        )

        assertNotNull(
            "no update routes suggestion received",
            updateRouteSuggestion,
        )
        val newRoutes = updateRouteSuggestion!!.newRoutes
        val alternatives = newRoutes.drop(1)

        assertEquals(
            routeRequestUrl,
            alternatives.first().directionsRoute.routeOptions()?.toUrl("***"),
        )
    }

    @Test
    fun `should set route index and UUID of alternative routes for refresh`() {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
            every { navigationRoute } returns createNavigationRoute()
        }

        var updateRouteSuggestion: UpdateRouteSuggestion? = null
        routeAlternativesController.setRouteUpdateSuggestionListener {
            updateRouteSuggestion = it
        }
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
            listOf(
                createNativeAlternativeMock(),
            ),
            emptyList(),
        )

        assertNotNull(
            "no routes update suggestions were received",
            updateRouteSuggestion,
        )
        val alternativeRoutes = updateRouteSuggestion!!.newRoutes.drop(1)
        assertEquals("0", alternativeRoutes[0].directionsRoute.routeIndex())
        assertEquals(
            "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==",
            alternativeRoutes[0].directionsRoute.requestUuid(),
        )
    }

    @Test
    fun `broadcasts correct alternatives origin`() = coroutineRule.runBlockingTest {
        val routeAlternativesController = createRouteAlternativesController()
        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
        every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
            every { navigationRoute } returns createNavigationRoute()
        }

        var updateRouteSuggestion: UpdateRouteSuggestion? = null
        routeAlternativesController.setRouteUpdateSuggestionListener {
            updateRouteSuggestion = it
        }

        val firstAlternative: RouteAlternative = createNativeAlternativeMock()
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
            listOf(
                firstAlternative,
            ),
            emptyList(),
        )
        val secondAlternative: RouteAlternative = createNativeAlternativeMock(
            routerOrigin = com.mapbox.navigator.RouterOrigin.ONLINE,
        )
        nativeObserver.captured.onRouteAlternativesUpdated(
            null,
            listOf(
                secondAlternative,
            ),
            listOf(
                firstAlternative.apply {
                    every { isNew } returns false
                },
            ),
        )

        assertNotNull(
            "no routes update suggestions were received",
            updateRouteSuggestion,
        )
        val alternativeRoutes = updateRouteSuggestion!!.newRoutes.drop(1)
        assertEquals(RouterOrigin.ONLINE, alternativeRoutes.last().origin)
    }

    @Test
    fun `processing job should be canceled if it doesn't keep up`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns createNavigationRoute()
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            val updateRouteSuggestions = mutableListOf<UpdateRouteSuggestion>()
            routeAlternativesController.setRouteUpdateSuggestionListener {
                updateRouteSuggestions.add(it)
            }

            pauseDispatcher {
                nativeObserver.captured.onRouteAlternativesUpdated(
                    null,
                    listOf(
                        createNativeAlternativeMock(
                            alternativeId = 1,
                            fileName = "route_response_new_for_refresh.json",
                            routeIndex = 0,
                        ),
                    ),
                    emptyList(),
                )
                nativeObserver.captured.onRouteAlternativesUpdated(
                    null,
                    listOf(
                        createNativeAlternativeMock(
                            alternativeId = 0,
                            fileName = "route_alternative_from_native.json",
                            routeIndex = 0,
                        ),
                    ),
                    emptyList(),
                )
            }

            assertEquals(
                "only one routes update is expected",
                1,
                updateRouteSuggestions.size,
            )
            assertEquals(
                "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==#0",
                updateRouteSuggestions.single().newRoutes[1].id,
            )
        }

    @Test
    fun `metadata for alternative available when generated by native observer`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
                every { navigationRoute } returns createNavigationRoute()
            }

            var updateRouteSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                updateRouteSuggestion = it
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(createNativeAlternativeMock()),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestions were received",
                updateRouteSuggestion,
            )
            val alternativeRoute = updateRouteSuggestion!!.newRoutes[1]

            val metadata =
                routeAlternativesController.getMetadataFor(alternativeRoute)

            assertEquals(platformForkAlt, metadata!!.forkIntersectionOfAlternativeRoute)
            assertEquals(platformForkMain, metadata.forkIntersectionOfPrimaryRoute)
            assertEquals(platformInfoStart, metadata.infoFromStartOfPrimary)
            assertEquals(platformInfoFork, metadata.infoFromFork)
        }

    @Test
    fun `metadata for alternative available when set after processing routes`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val routeProgress = mockk<RouteProgress>(relaxed = true)
            every { tripSession.getRouteProgress() } returns routeProgress

            val navigationRoute = mockk<NavigationRoute>(relaxed = true) {
                every { id } returns "route_id"
            }
            routeAlternativesController.processAlternativesMetadata(
                routes = listOf(navigationRoute),
                nativeAlternatives = listOf(
                    createNativeAlternativeMock().apply {
                        every { route.routeId } returns "route_id"
                    },
                ),
            )
            val metadata = routeAlternativesController.getMetadataFor(navigationRoute)

            assertEquals(platformForkAlt, metadata!!.forkIntersectionOfAlternativeRoute)
            assertEquals(platformForkMain, metadata.forkIntersectionOfPrimaryRoute)
            assertEquals(platformInfoStart, metadata.infoFromStartOfPrimary)
            assertEquals(platformInfoFork, metadata.infoFromFork)
        }

    @Test
    fun `metadata cleared on routes update`() = coroutineRule.runBlockingTest {
        val routeAlternativesController = createRouteAlternativesController()
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { tripSession.getRouteProgress() } returns routeProgress

        val navigationRoute = mockk<NavigationRoute>(relaxed = true) {
            every { id } returns "route_id"
        }
        routeAlternativesController.processAlternativesMetadata(
            routes = listOf(navigationRoute),
            nativeAlternatives = listOf(
                createNativeAlternativeMock().apply {
                    every { route.routeId } returns "route_id"
                },
            ),
        )
        routeAlternativesController.processAlternativesMetadata(
            routes = emptyList(),
            nativeAlternatives = emptyList(),
        )
        val metadata = routeAlternativesController.getMetadataFor(navigationRoute)

        assertNull(metadata)
    }

    @Test
    fun `metadata cleared on alternatives update from native observer`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            val routeProgress = mockk<RouteProgress>(relaxed = true)
            every { tripSession.getRouteProgress() } returns routeProgress
            routeAlternativesController.setRouteUpdateSuggestionListener { }

            val navigationRoute = mockk<NavigationRoute>(relaxed = true) {
                every { id } returns "route_id"
            }
            val nativeAlternative = createNativeAlternativeMock().apply {
                every { route.routeId } returns "route_id"
                every { route.routeInfo } returns mockk(relaxed = true)
            }
            routeAlternativesController.processAlternativesMetadata(
                routes = listOf(navigationRoute),
                nativeAlternatives = listOf(nativeAlternative),
            )
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                emptyList(),
                listOf(nativeAlternative),
            )
            val metadata = routeAlternativesController.getMetadataFor(navigationRoute)

            assertNull(metadata)
        }

    @Test
    fun `alternative routes are updated`() =
        coroutineRule.runBlockingTest {
            val parsingManager = createParsingManagerForTest()
            val routeAlternativesController = createRouteAlternativesController(
                routeParsingManager = parsingManager,
            )
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

            val testPrimaryRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "testPrimaryRoute"),
            )
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns testPrimaryRoute
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            var parsingPreparation = 0
            parsingManager.setPrepareForParsingAction {
                parsingPreparation++
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(
                    createNativeAlternativeMock(
                        alternativeId = 1,
                        fileName = "route_response_new_for_refresh.json",
                        routeIndex = 0,
                    ),
                    createNativeAlternativeMock(
                        alternativeId = 0,
                        fileName = "route_alternative_from_native.json",
                        routeIndex = 0,
                    ),
                    createNativeAlternativeMock(
                        alternativeId = 1,
                        fileName = "route_response_new_for_refresh.json",
                        routeIndex = 1,
                    ),
                ),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestion",
                routesUpdateSuggestion,
            )
            assertEquals(
                SuggestionType.AlternativesUpdated,
                routesUpdateSuggestion!!.type,
            )
            val newRoutes = routesUpdateSuggestion!!.newRoutes
            assertEquals(
                listOf(
                    "testPrimaryRoute#0",
                    "route_response_new_for_refresh#0",
                    "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==#0",
                    "route_response_new_for_refresh#1",
                ),
                newRoutes.map { it.id },
            )
            assertEquals(
                "no parsing preparation expected for short routes",
                0,
                parsingPreparation,
            )
        }

    @Test
    fun `alternative long routes are updated`() =
        coroutineRule.runBlockingTest {
            val parsingManager = createParsingManagerForTest()
            val routeAlternativesController = createRouteAlternativesController(
                routeParsingManager = parsingManager,
            )
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

            val testPrimaryRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "testPrimaryRoute"),
            )
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns testPrimaryRoute
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            var parsingPreparation = 0
            parsingManager.setPrepareForParsingAction {
                parsingPreparation++
                assertNull(
                    "parsing preparation happened after alternatives routes were suggested",
                    routesUpdateSuggestion,
                )
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(
                    createNativeAlternativeMock(
                        alternativeId = 1,
                        fileName = "long_route_7k.json",
                        routeIndex = 0,
                    ),
                ),
                emptyList(),
            )

            assertEquals(
                1,
                parsingPreparation,
            )
            assertNotNull(
                "no routes update suggestion",
                routesUpdateSuggestion,
            )
            assertEquals(
                SuggestionType.AlternativesUpdated,
                routesUpdateSuggestion!!.type,
            )
            val newRoutes = routesUpdateSuggestion!!.newRoutes
            assertEquals(
                listOf(
                    "testPrimaryRoute#0",
                    "Hx9dSjQIDnHkThyjoziZBodVBvaSGynKcAZEd2Ha5O05s3pKsvYkAQ==#0",
                ),
                newRoutes.map { it.id },
            )
        }

    @Test
    fun `suggest alternatives cleanup for online primary route`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

            val testPrimaryRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "testPrimaryRoute"),
            )
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns testPrimaryRoute
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                emptyList(),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestion",
                routesUpdateSuggestion,
            )
            assertEquals(
                SuggestionType.AlternativesUpdated,
                routesUpdateSuggestion!!.type,
            )
            val newRoutes = routesUpdateSuggestion!!.newRoutes
            assertEquals(
                listOf(
                    "testPrimaryRoute#0",
                ),
                newRoutes.map { it.id },
            )
        }

    @Test
    fun `ignore offline alternatives if current route is online`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

            val testPrimaryRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "testPrimaryRoute"),
            )
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns testPrimaryRoute
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(
                    createNativeAlternativeMock(
                        alternativeId = 0,
                        fileName = "route_alternative_from_native.json",
                        routeIndex = 0,
                        routerOrigin = com.mapbox.navigator.RouterOrigin.ONBOARD,
                    ),
                ),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestion",
                routesUpdateSuggestion,
            )
            assertEquals(
                SuggestionType.AlternativesUpdated,
                routesUpdateSuggestion!!.type,
            )
            val newRoutes = routesUpdateSuggestion!!.newRoutes
            assertEquals(
                listOf(
                    "testPrimaryRoute#0",
                ),
                newRoutes.map { it.id },
            )
        }

    @Test
    fun `suggest alternatives cleanup for offline primary route`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

            val testPrimaryRoute = createNavigationRoute(
                createDirectionsRoute(
                    requestUuid = "testPrimaryRoute",
                ),
                routerOrigin = RouterOrigin.OFFLINE,
            )
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns testPrimaryRoute
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                emptyList(),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestion",
                routesUpdateSuggestion,
            )
            assertEquals(
                SuggestionType.AlternativesUpdated,
                routesUpdateSuggestion!!.type,
            )
            val newRoutes = routesUpdateSuggestion!!.newRoutes
            assertEquals(
                listOf(
                    "testPrimaryRoute#0",
                ),
                newRoutes.map { it.id },
            )
        }

    @Test
    fun `offline primary route is replaced by an online alternative`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

            val testPrimaryRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "testOfflinePrimaryRoute"),
                routerOrigin = RouterOrigin.OFFLINE,
            )
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns testPrimaryRoute
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(
                    createNativeAlternativeMock(
                        alternativeId = 1,
                        fileName = "route_response_new_for_refresh.json",
                        routeIndex = 0,
                        routerOrigin = com.mapbox.navigator.RouterOrigin.ONLINE,
                    ),
                    createNativeAlternativeMock(
                        alternativeId = 1,
                        fileName = "route_response_new_for_refresh.json",
                        routeIndex = 1,
                        routerOrigin = com.mapbox.navigator.RouterOrigin.ONLINE,
                    ),
                ),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestion",
                routesUpdateSuggestion,
            )
            assertEquals(
                SuggestionType.SwitchToOnlineAlternative,
                routesUpdateSuggestion!!.type,
            )
            val newRoutes = routesUpdateSuggestion!!.newRoutes
            assertEquals(
                listOf(
                    "route_response_new_for_refresh#0",
                    "route_response_new_for_refresh#1",
                ),
                newRoutes.map { it.id },
            )
        }

    @Test
    fun `offline alternatives are updated`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

            val testPrimaryRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "testOfflinePrimaryRoute"),
                routerOrigin = RouterOrigin.OFFLINE,
            )
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns testPrimaryRoute
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(
                    createNativeAlternativeMock(
                        alternativeId = 1,
                        fileName = "route_response_new_for_refresh.json",
                        routeIndex = 0,
                        routerOrigin = com.mapbox.navigator.RouterOrigin.ONBOARD,
                    ),
                    createNativeAlternativeMock(
                        alternativeId = 2,
                        fileName = "route_response_new_for_refresh.json",
                        routeIndex = 1,
                        routerOrigin = com.mapbox.navigator.RouterOrigin.ONBOARD,
                    ),
                ),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestion",
                routesUpdateSuggestion,
            )
            assertEquals(
                SuggestionType.AlternativesUpdated,
                routesUpdateSuggestion!!.type,
            )
            val newRoutes = routesUpdateSuggestion!!.newRoutes
            assertEquals(
                listOf(
                    "testOfflinePrimaryRoute#0",
                    "route_response_new_for_refresh#0",
                    "route_response_new_for_refresh#1",
                ),
                newRoutes.map { it.id },
            )
        }

    @Test
    fun `offline primary route is replaced by an online alternative with the same geometry`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

            val testPrimaryRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "testOfflinePrimaryRoute"),
                routerOrigin = RouterOrigin.OFFLINE,
            )
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns testPrimaryRoute
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                createNativeAlternativeMock(
                    alternativeId = 1,
                    fileName = "route_response_new_for_refresh.json",
                    routeIndex = 1,
                    routerOrigin = com.mapbox.navigator.RouterOrigin.ONLINE,
                ).route,
                listOf(
                    createNativeAlternativeMock(
                        alternativeId = 1,
                        fileName = "route_response_new_for_refresh.json",
                        routeIndex = 0,
                        routerOrigin = com.mapbox.navigator.RouterOrigin.ONLINE,
                    ),
                ),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestion",
                routesUpdateSuggestion,
            )
            assertEquals(
                SuggestionType.SwitchToOnlineAlternative,
                routesUpdateSuggestion!!.type,
            )
            val newRoutes = routesUpdateSuggestion!!.newRoutes
            assertEquals(
                listOf(
                    "route_response_new_for_refresh#1",
                    "route_response_new_for_refresh#0",
                ),
                newRoutes.map { it.id },
            )
        }

    @Test
    fun `turn off alternatives suggestions`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            every { controllerInterface.addObserver(any()) } just runs
            every { controllerInterface.removeObserver(any()) } just runs

            routeAlternativesController.setRouteUpdateSuggestionListener {
            }
            routeAlternativesController.setRouteUpdateSuggestionListener(null)

            verify(exactly = 1) { controllerInterface.addObserver(any()) }
            verify(exactly = 1) { controllerInterface.removeObserver(any()) }
        }

    @Test
    fun `reset route update suggestion listener`() =
        coroutineRule.runBlockingTest {
            val routeAlternativesController = createRouteAlternativesController()
            val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
            every { controllerInterface.addObserver(capture(nativeObserver)) } just runs
            every { controllerInterface.removeObserver(capture(nativeObserver)) } just runs

            val testPrimaryRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "testPrimaryRoute"),
            )
            val routeProgress = mockk<RouteProgress>(relaxed = true) {
                every { navigationRoute } returns testPrimaryRoute
            }
            every { tripSession.getRouteProgress() } returns routeProgress

            var routesUpdateSuggestion: UpdateRouteSuggestion? = null
            routeAlternativesController.setRouteUpdateSuggestionListener {
                fail("initial observer shouldn't be called")
            }
            routeAlternativesController.setRouteUpdateSuggestionListener {
                routesUpdateSuggestion = it
            }
            nativeObserver.captured.onRouteAlternativesUpdated(
                null,
                listOf(
                    createNativeAlternativeMock(
                        alternativeId = 1,
                        fileName = "route_alternative_from_native.json",
                        routeIndex = 0,
                    ),
                ),
                emptyList(),
            )

            assertNotNull(
                "no routes update suggestion",
                routesUpdateSuggestion,
            )
            assertEquals(
                SuggestionType.AlternativesUpdated,
                routesUpdateSuggestion!!.type,
            )
            val newRoutes = routesUpdateSuggestion!!.newRoutes
            assertEquals(
                listOf(
                    "testPrimaryRoute#0",
                    "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==#0",
                ),
                newRoutes.map { it.id },
            )

            verify(exactly = 1) { controllerInterface.addObserver(any()) }
            verify(exactly = 0) { controllerInterface.removeObserver(any()) }
        }

    @Test
    fun `during alternatives parsing should look up for existing route`() = coroutineRule.runBlockingTest {
        fun createRouteWithId(
            newRouteId: String,
        ) = createNativeAlternativeMock().apply {
            val spyk = spyk(route) {
                every { routeId } returns newRouteId
            }
            every { route } returns spyk
        }

        val nativeRoute = createRouteWithId("route")
        val firstAlternative = createRouteWithId("alternative_route_1")
        val secondAlternative = createRouteWithId("alternative_route_2")

        every { directionSession.findRoute(any()) } returns null

        val routeAlternativesController = createRouteAlternativesController()

        val nativeObserver = slot<RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

        var ignore: UpdateRouteSuggestion? = null
        routeAlternativesController.setRouteUpdateSuggestionListener {
            ignore = it
        }

        nativeObserver.captured.onRouteAlternativesUpdated(
            nativeRoute.route,
            listOf(firstAlternative, secondAlternative),
            emptyList(),
        )

        listOf(
            nativeRoute.route.routeId,
            firstAlternative.route.routeId,
            secondAlternative.route.routeId,
        ).forEach { routeId ->
            verify(exactly = 1) {
                directionSession.findRoute(routeId)
            }
        }
    }

    @Test
    fun `during alternatives parsing if route already exist should not parse again`() = coroutineRule.runBlockingTest {
        fun createRouteWithId(
            newRouteId: String,
            newResponseUUID: String,
        ) = createNativeAlternativeMock().apply {
            val spyk = spyk(route) {
                every { routeId } returns newRouteId
                every { responseUuid } returns newResponseUUID
            }
            every { route } returns spyk
        }

        val nativeRoute = createRouteWithId("route", "uuid1").route
        val firstAlternative = createRouteWithId("alternative_route_1", "uuid2")
        val secondAlternative = createRouteWithId("alternative_route_2", "uuid2")

        val firstAlternativeRoute = firstAlternative.route
        val secondAlternativeRoute = secondAlternative.route

        every { directionSession.findRoute(eq("route")) } returns null
        every { directionSession.findRoute(eq("alternative_route_1")) } returns null
        every {
            directionSession.findRoute(eq("alternative_route_2"))
        } returns mockk<NavigationRoute>(
            relaxed = true,
        )

        val routeParsingMock = spyk(createParsingManagerForTest()) {
            val fromJson = DirectionsResponse.fromJson(
                FileUtils.loadJsonFixture("route_alternative_from_native.json"),
            )
            every { parseRouteToDirections(any()) } returns fromJson
        }

        val routeAlternativesController = createRouteAlternativesController(
            routeParsingManager = routeParsingMock,
        )

        val nativeObserver = slot<com.mapbox.navigator.RouteAlternativesObserver>()
        every { controllerInterface.addObserver(capture(nativeObserver)) } just runs

        var ignore: UpdateRouteSuggestion? = null
        routeAlternativesController.setRouteUpdateSuggestionListener {
            ignore = it
        }

        nativeObserver.captured.onRouteAlternativesUpdated(
            nativeRoute,
            listOf(firstAlternative, secondAlternative),
            emptyList(),
        )

        listOf(
            nativeRoute.routeId,
            firstAlternativeRoute.routeId,
            secondAlternativeRoute.routeId,
        ).forEach { routeId ->
            verify(exactly = 1) {
                directionSession.findRoute(routeId)
            }
        }

        verify(exactly = 1) {
            routeParsingMock.parseRouteToDirections(refEq(nativeRoute))
        }

        verify(exactly = 1) {
            routeParsingMock.parseRouteToDirections(refEq(firstAlternativeRoute))
        }

        verify(exactly = 0) {
            routeParsingMock.parseRouteToDirections(refEq(secondAlternativeRoute))
        }
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
        routerOrigin: com.mapbox.navigator.RouterOrigin = com.mapbox.navigator.RouterOrigin.ONLINE,
        fileName: String = "route_alternative_from_native.json",
        routeIndex: Int = 0,
    ): RouteAlternative {
        val responseJson = FileUtils.loadJsonFixture(fileName)
        val response = DirectionsResponse.fromJson(responseJson)
        val nativeRoute = createRouteInterface(
            responseJson = responseJson,
            requestURI = routeRequestUrl.toString(),
            routerOrigin = routerOrigin,
            responseUUID = response.uuid()!!,
            routeIndex = routeIndex,
            routeGeometry = response.routes()[0].completeGeometryToPoints(),
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

private fun createParsingManagerForTest() = createRouteParsingManager()
