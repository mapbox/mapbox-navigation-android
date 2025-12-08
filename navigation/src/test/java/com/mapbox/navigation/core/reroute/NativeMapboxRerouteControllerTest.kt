package com.mapbox.navigation.core.reroute

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.internal.utils.createRouteParsingManager
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.performance.RouteParsingTracking
import com.mapbox.navigation.core.internal.router.util.TestRouteFixtures
import com.mapbox.navigation.navigator.internal.RerouteEventsProvider
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRerouteError
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.factories.createRouterError
import com.mapbox.navigation.testing.factories.toDataRef
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.ForceRerouteCallback
import com.mapbox.navigator.ForceRerouteReason
import com.mapbox.navigator.RerouteControllerInterface
import com.mapbox.navigator.RerouteDetectorInterface
import com.mapbox.navigator.RerouteErrorType
import com.mapbox.navigator.RerouteInfo
import com.mapbox.navigator.RerouteObserver
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterOrigin
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val TEST_REROUTE_URL = createRouteOptions().toUrl("***").toString()

class NativeMapboxRerouteControllerTest {

    private val testRouteFixtures = TestRouteFixtures()

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    @get:Rule
    val routeParserRule = NativeRouteParserRule()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.DefaultDispatcher } returns UnconfinedTestDispatcher()
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
    }

    @Test
    fun `reroute is cancelled`() {
        val observerRegistration = RerouteEventsRegistration()
        val updateRoutes = mockk<UpdateRoutes>()
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
            updateRoutes = updateRoutes,
        )
        val states = controller.recordRerouteState()
        val statesFromSecondSubscriber = controller.recordRerouteState()

        observerRegistration.observer.apply {
            onRerouteDetected(TEST_REROUTE_URL)
            onRerouteCancelled()
        }
        assertEquals(
            listOf(
                RerouteState.Idle,
                RerouteState.FetchingRoute,
                RerouteState.Interrupted,
                RerouteState.Idle,
            ),
            states,
        )
        assertEquals(
            states,
            statesFromSecondSubscriber,
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }

    @Test
    fun `reroute is successful`() {
        val updateRoutes = mockk<UpdateRoutes>()
        val observerRegistration = RerouteEventsRegistration()
        val parsingTracking = mockk<RouteParsingTracking>(relaxed = true)
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
            updateRoutes = updateRoutes,
            routeParsingTracking = parsingTracking,
        )
        val states = controller.recordRerouteState()

        observerRegistration.observer.apply {
            onRerouteDetected(TEST_REROUTE_URL)
            onRerouteReceived(
                testRouteFixtures.loadTwoLegRoute().toDataRef(),
                TEST_REROUTE_URL,
                RouterOrigin.ONBOARD,
            )
        }
        assertEquals(
            listOf(
                RerouteState.Idle,
                RerouteState.FetchingRoute,
                RerouteState.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.OFFLINE),
                RerouteState.Idle,
            ),
            states,
        )
        verify(exactly = 1) {
            updateRoutes(match { it.map { it.id } == listOf("cjeacbr8s21bk47lggcvce7lv#0") }, eq(0))
        }
        verify {
            parsingTracking.routeResponseIsParsed(any())
        }
    }

    @Test
    fun `reroute is successful after parent is destroyed`() {
        val updateRoutes = mockk<UpdateRoutes>()
        val observerRegistration = RerouteEventsRegistration()
        val parentScope = TestScope(UnconfinedTestDispatcher())
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
            scope = parentScope,
            updateRoutes = updateRoutes,
        )
        val states = controller.recordRerouteState()

        observerRegistration.observer.apply {
            onRerouteDetected(TEST_REROUTE_URL)
            parentScope.cancel()
            onRerouteReceived(
                testRouteFixtures.loadTwoLegRoute().toDataRef(),
                TEST_REROUTE_URL,
                RouterOrigin.ONBOARD,
            )
        }

        assertEquals(
            listOf(
                RerouteState.Idle,
                RerouteState.FetchingRoute,
            ),
            states,
        )
        verify(exactly = 0) { updateRoutes.invoke(any(), any()) }
    }

    @Test
    fun `java parsing of successful reroute is failed`() {
        val observerRegistration = RerouteEventsRegistration()
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
        )
        val states = controller.recordRerouteState()

        observerRegistration.observer.apply {
            onRerouteDetected(TEST_REROUTE_URL)
            onRerouteReceived(
                "wrong route".toDataRef(),
                TEST_REROUTE_URL,
                RouterOrigin.ONLINE,
            )
        }

        assertEquals(
            listOf(
                RerouteState.Idle::class.java,
                RerouteState.FetchingRoute::class.java,
                RerouteState.Failed::class.java,
                RerouteState.Idle::class.java,
            ),
            states.map { it.javaClass },
        )
        assertFalse(
            (states[2] as RerouteState.Failed).isRetryable,
        )
    }

    @Test
    fun `reroute is failed`() {
        val observerRegistration = RerouteEventsRegistration()
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
        )
        val states = controller.recordRerouteState()

        observerRegistration.observer.apply {
            onRerouteDetected(TEST_REROUTE_URL)
            onRerouteFailed(
                createRerouteError(
                    errorType = RerouteErrorType.ROUTER_ERROR,
                    routerErrors = listOf(
                        createRouterError(
                            isRetryable = true,
                            routerOrigin = RouterOrigin.ONBOARD,
                            stringUrl = TEST_REROUTE_URL,
                        ),
                    ),
                ),
            )
        }

        assertEquals(
            listOf(
                RerouteState.Idle::class.java,
                RerouteState.FetchingRoute::class.java,
                RerouteState.Failed::class.java,
                RerouteState.Idle::class.java,
            ),
            states.map { it.javaClass },
        )
        val failure = states[2] as RerouteState.Failed
        assertTrue(failure.isRetryable)
        assertEquals(
            1,
            failure.reasons?.size,
        )
        val failureReason = failure.reasons!![0]
        assertEquals(
            com.mapbox.navigation.base.route.RouterOrigin.OFFLINE,
            failureReason.routerOrigin,
        )
        assertEquals(
            TEST_REROUTE_URL,
            failureReason.url.toString(),
        )
    }

    @Test
    fun `switch to alternative route is successful`() {
        var currentLegIndex = 0
        var currentRoutes = createNavigationRoutes(
            response = createDirectionsResponse(
                uuid = "test",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                ),
            ),
        )
        val observerRegistration = RerouteEventsRegistration()
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
            getCurrentRoutes = { currentRoutes },
            updateRoutes = { routes, legIndex ->
                currentRoutes = routes
                currentLegIndex = legIndex
            },
        )
        val states = controller.recordRerouteState()

        observerRegistration.observer.apply {
            onSwitchToAlternative(
                currentRoutes[1].nativeRoute(),
                1,
            )
        }
        assertEquals(
            listOf(
                RerouteState.Idle,
                RerouteState.FetchingRoute,
                RerouteState.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteState.Idle,
            ),
            states,
        )
        assertEquals(
            listOf("test#1", "test#0"),
            currentRoutes.map { it.id },
        )
        assertEquals(1, currentLegIndex)
    }

    @Test
    fun `switch to alternative route failed`() {
        val testRoutes = createNavigationRoutes(
            response = createDirectionsResponse(
                uuid = "test",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                ),
            ),
        )
        val updateRoutes = mockk<UpdateRoutes>()
        val observerRegistration = RerouteEventsRegistration()
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
            getCurrentRoutes = { listOf(testRoutes[0]) },
            updateRoutes = updateRoutes,
        )
        val states = controller.recordRerouteState()

        observerRegistration.observer.apply {
            onSwitchToAlternative(
                testRoutes[1].nativeRoute(),
                1,
            )
        }
        assertEquals(
            listOf(
                RerouteState.Idle,
            ),
            states,
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }

    @Test
    fun `user requests reroute`() {
        val updateRoutes = mockk<UpdateRoutes>()
        val observerRegistration = RerouteEventsRegistration()
        val rerouteDetector = TestRerouteDetector()
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
            updateRoutes = updateRoutes,
            rerouteDetector = rerouteDetector,
        )
        val states = controller.recordRerouteState()

        var newRoutes: List<NavigationRoute>? = null
        var newRoutesOrigin: String? = null
        controller.reroute { routes, origin ->
            newRoutes = routes
            newRoutesOrigin = origin
        }
        rerouteDetector.latestCallback?.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        assertEquals(
            listOf("cjeacbr8s21bk47lggcvce7lv#0"),
            newRoutes?.map { it.id },
        )
        assertEquals(com.mapbox.navigation.base.route.RouterOrigin.ONLINE, newRoutesOrigin)
        assertEquals(
            listOf(
                RerouteState.Idle,
                RerouteState.FetchingRoute,
                RerouteState.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteState.Idle,
            ),
            states,
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }

    @Test
    fun `user requests reroute but it fails`() {
        val updateRoutes = mockk<UpdateRoutes>()
        val observerRegistration = RerouteEventsRegistration()
        val rerouteDetector = TestRerouteDetector()
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
            updateRoutes = updateRoutes,
            rerouteDetector = rerouteDetector,
        )
        val states = controller.recordRerouteState()

        val rerouteCallback = mockk<RerouteController.RoutesCallback>()
        controller.reroute(rerouteCallback)
        rerouteDetector.latestCallback?.run(
            ExpectedFactory.createError(
                createRerouteError(
                    errorType = RerouteErrorType.ROUTER_ERROR,
                    routerErrors = listOf(
                        createRouterError(
                            type = RouterErrorType.ROUTE_CREATION_ERROR,
                            isRetryable = false,
                            routerOrigin = RouterOrigin.ONLINE,
                        ),
                        createRouterError(
                            type = RouterErrorType.REQUEST_CANCELLED,
                            isRetryable = false,
                            routerOrigin = RouterOrigin.ONBOARD,
                        ),
                    ),
                ),
            ),
        )

        verify(exactly = 0) { rerouteCallback.onNewRoutes(any(), any()) }
        assertEquals(
            listOf(
                RerouteState.Idle::class.java,
                RerouteState.FetchingRoute::class.java,
                RerouteState.Failed::class.java,
                RerouteState.Idle::class.java,
            ),
            states.map { it.javaClass },
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }

    @Test
    fun `user requests reroute but it's cancelled`() {
        val updateRoutes = mockk<UpdateRoutes>()
        val observerRegistration = RerouteEventsRegistration()
        val rerouteDetector = TestRerouteDetector()
        val controller = createNativeMapboxRerouteController(
            rerouteEventsRegistration = observerRegistration,
            updateRoutes = updateRoutes,
            rerouteDetector = rerouteDetector,
        )
        val states = controller.recordRerouteState()

        val rerouteCallback = mockk<RerouteController.RoutesCallback>()
        controller.reroute(rerouteCallback)
        rerouteDetector.latestCallback?.run(
            ExpectedFactory.createError(
                createRerouteError(
                    errorType = RerouteErrorType.CANCELLED,
                    routerErrors = listOf(
                        createRouterError(
                            type = RouterErrorType.REQUEST_CANCELLED,
                            routerOrigin = RouterOrigin.ONLINE,
                        ),
                        createRouterError(
                            type = RouterErrorType.ROUTE_CREATION_ERROR,
                            routerOrigin = RouterOrigin.ONBOARD,
                        ),
                    ),
                ),
            ),
        )

        verify(exactly = 0) { rerouteCallback.onNewRoutes(any(), any()) }
        assertEquals(
            listOf(
                RerouteState.Idle::class.java,
                RerouteState.FetchingRoute::class.java,
                RerouteState.Interrupted::class.java,
                RerouteState.Idle::class.java,
            ),
            states.map { it.javaClass },
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }
}

private fun createNativeMapboxRerouteController(
    rerouteEventsRegistration: RerouteEventsProvider = RerouteEventsRegistration(),
    rerouteController: RerouteControllerInterface = mockk(relaxed = true),
    rerouteDetector: RerouteDetectorInterface = mockk(relaxed = true),
    scope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
    parsingDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    getCurrentRoutes: () -> List<NavigationRoute> = { emptyList() },
    updateRoutes: UpdateRoutes = { _, _ -> },
    routeParsingTracking: RouteParsingTracking = mockk(relaxed = true),
) = NativeMapboxRerouteController(
    rerouteEventsRegistration,
    rerouteController,
    rerouteDetector,
    getCurrentRoutes,
    updateRoutes,
    scope,
    parsingDispatcher,
    routeParsingTracking,
    createRouteParsingManager(false),
)

private class RerouteEventsRegistration : RerouteEventsProvider {

    lateinit var observer: RerouteObserver

    override fun addRerouteObserver(nativeRerouteObserver: RerouteObserver) {
        observer = nativeRerouteObserver
    }

    override fun removeRerouteObserver(nativeRerouteObserver: RerouteObserver) {
    }
}

private fun RerouteController.recordRerouteState(): List<RerouteState> {
    val states = mutableListOf<RerouteState>()
    this.registerRerouteStateObserver {
        states.add(it)
    }
    return states
}

class TestRerouteDetector : RerouteDetectorInterface {

    var latestCallback: ForceRerouteCallback? = null

    override fun forceReroute(reason: ForceRerouteReason) {
        TODO("Not yet implemented")
    }

    override fun forceReroute(reason: ForceRerouteReason, callback: ForceRerouteCallback) {
        latestCallback = callback
    }

    override fun isReroute(): Boolean {
        TODO("Not yet implemented")
    }

    override fun cancelReroute() {
        TODO("Not yet implemented")
    }
}
