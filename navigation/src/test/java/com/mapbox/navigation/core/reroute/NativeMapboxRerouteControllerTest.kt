package com.mapbox.navigation.core.reroute

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.nativeRoute
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.models.directions.NavigationRouteParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.parsing.models.directions.NavigationRoutesParser
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.router.util.TestRouteFixtures
import com.mapbox.navigation.navigator.internal.MapboxNativeRerouteInterface
import com.mapbox.navigation.navigator.internal.NativeNavigatorRecreationObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRerouteError
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.factories.createRouterError
import com.mapbox.navigation.testing.factories.createTestNavigationRoutesParsing
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
import kotlinx.coroutines.CompletableDeferred
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

@OptIn(ExperimentalMapboxNavigationAPI::class)
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
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val updateRoutes = mockk<UpdateRoutes>() {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
            updateRoutes = updateRoutes,
        )
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()
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
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.Interrupted(),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
        assertEquals(
            states,
            statesFromSecondSubscriber,
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }

    @Test
    fun `reroute is successful`() {
        val updateRoutes = mockk<UpdateRoutes>() {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
            updateRoutes = updateRoutes,
        )
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

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
        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.OFFLINE),
                RerouteStateV2.Deviation.ApplyingRoute(),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
        verify(exactly = 1) {
            updateRoutes(match { it.map { it.id } == listOf("cjeacbr8s21bk47lggcvce7lv#0") }, eq(0))
        }
    }

    @Test
    fun `reroute is successful but got back on route`() {
        val updateRoutes = mockk<UpdateRoutes>() {
            every { this@mockk.invoke(any(), any()) } returns false
        }
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
            updateRoutes = updateRoutes,
        )
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

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
        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.OFFLINE),
                RerouteStateV2.Deviation.RouteIgnored(),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
        verify(exactly = 1) {
            updateRoutes(match { it.map { it.id } == listOf("cjeacbr8s21bk47lggcvce7lv#0") }, eq(0))
        }
    }

    @Test
    fun `reroute is successful after parent is destroyed`() {
        val updateRoutes = mockk<UpdateRoutes>() {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val parentScope = TestScope(UnconfinedTestDispatcher())
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
            scope = parentScope,
            updateRoutes = updateRoutes,
        )
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

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
        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
            ),
            statesV2,
        )
        verify(exactly = 0) { updateRoutes.invoke(any(), any()) }
    }

    @Test
    fun `only latest replan applied when a few requested in parallel`() {
        // When forceReroute is called with a callback, native does NOT trigger onRerouteDetected/
        // onRerouteReceived on observers (notifyObservers = !callback = false in native).
        // Results arrive exclusively via ForceRerouteCallback.
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val pausingParser = PausingNavigationRoutesParser(
            createTestNavigationRoutesParsing(UnconfinedTestDispatcher()),
        )
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            routeParser = pausingParser,
        )

        val statesV2 = controller.recordRerouteStateV2()
        val firstReplanCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )
        val secondReplanCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )

        // First replan: forceReroute is called; save callback before it is overwritten by second call
        controller.rerouteOnParametersChange(firstReplanCallback)
        val firstForceRerouteCallback = rerouteDetector.latestCallback!!

        // Native responds to first forceReroute - parsing starts but is suspended by PausingNavigationRoutesParser
        firstForceRerouteCallback.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONBOARD,
                ),
            ),
        )

        // Second replan starts before first parsing completes - first job is cancelled
        controller.rerouteOnParametersChange(secondReplanCallback)

        // Native responds to second forceReroute - second parsing also suspended
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        // Release both parsing operations to complete
        pausingParser.releaseAll()

        // Only the second (latest) result should deliver routes
        verify(exactly = 0) { firstReplanCallback.onNewRoutes(any()) }
        verify(exactly = 1) { secondReplanCallback.onNewRoutes(any()) }

        // First replan is interrupted by second; second completes successfully
        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.Interrupted(),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
    }

    @Test
    fun `second deviation after completed deviation`() {
        val updateRoutes = mockk<UpdateRoutes> {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
            updateRoutes = updateRoutes,
        )
        val statesV2 = controller.recordRerouteStateV2()

        // First deviation completes fully; activeParsingJob is done but still referenced.
        observerRegistration.observer.apply {
            onRerouteDetected(TEST_REROUTE_URL)
            onRerouteReceived(
                testRouteFixtures.loadTwoLegRoute().toDataRef(),
                TEST_REROUTE_URL,
                RouterOrigin.ONBOARD,
            )
        }

        // Second deviation: interruptParsingIfAny() must NOT emit Interrupted because the job is already done.
        observerRegistration.observer.apply {
            onRerouteDetected(TEST_REROUTE_URL)
            onRerouteReceived(
                testRouteFixtures.loadTwoLegRoute().toDataRef(),
                TEST_REROUTE_URL,
                RouterOrigin.ONLINE,
            )
        }

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.OFFLINE),
                RerouteStateV2.Deviation.ApplyingRoute(),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Deviation.ApplyingRoute(),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
    }

    @Test
    fun `second replan after completed replan`() {
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
        )
        val statesV2 = controller.recordRerouteStateV2()
        val firstCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )
        val secondCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )

        // First replan completes fully; activeParsingJob is done but still referenced.
        controller.rerouteOnParametersChange(firstCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        // Second replan: interruptParsingIfAny() must NOT emit Interrupted because the job is already done.
        controller.rerouteOnParametersChange(secondCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
        verify(exactly = 1) { firstCallback.onNewRoutes(any()) }
        verify(exactly = 1) { secondCallback.onNewRoutes(any()) }
    }

    @Test
    fun `replan after completed deviation`() {
        val updateRoutes = mockk<UpdateRoutes> {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            updateRoutes = updateRoutes,
        )
        val statesV2 = controller.recordRerouteStateV2()
        val replanCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )

        // Deviation completes fully; activeParsingJob is done but still referenced.
        nativeRerouteInterface.observer.apply {
            onRerouteDetected(TEST_REROUTE_URL)
            onRerouteReceived(
                testRouteFixtures.loadTwoLegRoute().toDataRef(),
                TEST_REROUTE_URL,
                RouterOrigin.ONBOARD,
            )
        }

        // Replan after completed deviation: interruptParsingIfAny() must NOT emit Interrupted.
        controller.rerouteOnParametersChange(replanCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.OFFLINE),
                RerouteStateV2.Deviation.ApplyingRoute(),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
        verify(exactly = 1) { replanCallback.onNewRoutes(any()) }
    }

    @Test
    fun `deviation after completed replan`() {
        val updateRoutes = mockk<UpdateRoutes> {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            updateRoutes = updateRoutes,
        )
        val statesV2 = controller.recordRerouteStateV2()
        val replanCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )

        // Replan completes fully; activeParsingJob is done but still referenced.
        controller.rerouteOnParametersChange(replanCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        // Deviation after completed replan: onRerouteReceived calls interruptParsingIfAny()
        // which must NOT emit Interrupted because the job is already done.
        nativeRerouteInterface.observer.apply {
            onRerouteDetected(TEST_REROUTE_URL)
            onRerouteReceived(
                testRouteFixtures.loadTwoLegRoute().toDataRef(),
                TEST_REROUTE_URL,
                RouterOrigin.ONLINE,
            )
        }

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Deviation.ApplyingRoute(),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
        verify(exactly = 1) { replanCallback.onNewRoutes(any()) }
    }

    @Test
    fun `deviation cancels in-flight replan parsing`() {
        // Scenario: replan response already received (Kotlin parsing in progress),
        // then auto-deviation fires. Deviation should win; replan callback must not fire.
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val pausingParser = PausingNavigationRoutesParser(
            createTestNavigationRoutesParsing(UnconfinedTestDispatcher()),
        )
        val updateRoutes = mockk<UpdateRoutes> {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            routeParser = pausingParser,
            updateRoutes = updateRoutes,
        )

        val statesV2 = controller.recordRerouteStateV2()
        val replanCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )

        // Replan kicks off; native responds immediately — parsing starts, suspended
        controller.rerouteOnParametersChange(replanCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONBOARD,
                ),
            ),
        )

        // Auto-deviation fires (native rerouteInProgress_ is false now — replan already responded)
        nativeRerouteInterface.observer.onRerouteDetected(TEST_REROUTE_URL)
        nativeRerouteInterface.observer.onRerouteReceived(
            testRouteFixtures.loadTwoLegRoute().toDataRef(),
            TEST_REROUTE_URL,
            RouterOrigin.ONLINE,
        )

        // Both parsings are released
        pausingParser.releaseAll()

        // Deviation wins; replan callback must not fire
        verify(exactly = 0) { replanCallback.onNewRoutes(any()) }
        verify(exactly = 1) { updateRoutes(any(), any()) }

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.Interrupted(),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Deviation.ApplyingRoute(),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
    }

    @Test
    fun `replan cancels in-flight deviation parsing`() {
        // Scenario: deviation response already received (Kotlin parsing in progress),
        // then rerouteOnParametersChange fires. Replan should win; deviation must not apply routes.
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val pausingParser = PausingNavigationRoutesParser(
            createTestNavigationRoutesParsing(UnconfinedTestDispatcher()),
        )
        val updateRoutes = mockk<UpdateRoutes> {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            routeParser = pausingParser,
            updateRoutes = updateRoutes,
        )

        val statesV2 = controller.recordRerouteStateV2()
        val replanCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )

        // Auto-deviation fires; native responds — parsing starts, suspended
        nativeRerouteInterface.observer.onRerouteDetected(TEST_REROUTE_URL)
        nativeRerouteInterface.observer.onRerouteReceived(
            testRouteFixtures.loadTwoLegRoute().toDataRef(),
            TEST_REROUTE_URL,
            RouterOrigin.ONBOARD,
        )

        // Replan is requested before deviation parsing completes
        controller.rerouteOnParametersChange(replanCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        // Both parsings are released
        pausingParser.releaseAll()

        // Replan wins; deviation must not apply routes
        verify(exactly = 0) { updateRoutes(any(), any()) }
        verify(exactly = 1) { replanCallback.onNewRoutes(any()) }

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.Interrupted(),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
    }

    @Test
    fun `replan cancels in-flight user-triggered reroute parsing`() {
        // Scenario: user-triggered reroute response already received (parsing in progress),
        // then rerouteOnParametersChange fires. Replan should win; user reroute callback must not fire.
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val pausingParser = PausingNavigationRoutesParser(
            createTestNavigationRoutesParsing(UnconfinedTestDispatcher()),
        )
        val updateRoutes = mockk<UpdateRoutes> {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            routeParser = pausingParser,
            updateRoutes = updateRoutes,
        )

        val statesV2 = controller.recordRerouteStateV2()
        val rerouteCallback = mockk<RerouteController.RoutesCallback>(relaxed = true)
        val replanCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )

        // User triggers reroute; native responds — parsing starts, suspended
        controller.reroute(rerouteCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONBOARD,
                ),
            ),
        )

        // Replan is requested before user-reroute parsing completes
        controller.rerouteOnParametersChange(replanCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        // Both parsings are released
        pausingParser.releaseAll()

        // Replan wins; user reroute callback must not fire
        verify(exactly = 0) { rerouteCallback.onNewRoutes(any(), any()) }
        verify(exactly = 1) { replanCallback.onNewRoutes(any()) }

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.Interrupted(),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
    }

    @Test
    fun `deviation cancels in-flight user-triggered reroute parsing`() {
        // Scenario: user-triggered reroute response already received (parsing in progress),
        // then auto-deviation fires. Deviation should win; user reroute callback must not fire.
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val pausingParser = PausingNavigationRoutesParser(
            createTestNavigationRoutesParsing(UnconfinedTestDispatcher()),
        )
        val updateRoutes = mockk<UpdateRoutes> {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            routeParser = pausingParser,
            updateRoutes = updateRoutes,
        )

        val statesV2 = controller.recordRerouteStateV2()
        val rerouteCallback = mockk<RerouteController.RoutesCallback>(relaxed = true)

        // User triggers reroute; native responds — parsing starts, suspended
        controller.reroute(rerouteCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONBOARD,
                ),
            ),
        )

        // Auto-deviation fires before user-reroute parsing completes
        nativeRerouteInterface.observer.onRerouteDetected(TEST_REROUTE_URL)
        nativeRerouteInterface.observer.onRerouteReceived(
            testRouteFixtures.loadTwoLegRoute().toDataRef(),
            TEST_REROUTE_URL,
            RouterOrigin.ONLINE,
        )

        // Both parsings are released
        pausingParser.releaseAll()

        // Deviation wins; user reroute callback must not fire
        verify(exactly = 0) { rerouteCallback.onNewRoutes(any(), any()) }
        verify(exactly = 1) { updateRoutes(any(), any()) }

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.Interrupted(),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Deviation.ApplyingRoute(),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
    }

    @Test
    fun `user reroute cancels in-flight replan parsing`() {
        // Scenario: replan response already received (parsing in progress),
        // then user triggers reroute. User reroute should win; replan callback must not fire.
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val pausingParser = PausingNavigationRoutesParser(
            createTestNavigationRoutesParsing(UnconfinedTestDispatcher()),
        )
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            routeParser = pausingParser,
        )

        val statesV2 = controller.recordRerouteStateV2()
        val replanCallback = mockk<InternalRerouteController.RouteReplanRoutesCallback>(
            relaxed = true,
        )
        val rerouteCallback = mockk<RerouteController.RoutesCallback>(relaxed = true)

        // Replan kicks off; native responds — parsing starts, suspended
        controller.rerouteOnParametersChange(replanCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONBOARD,
                ),
            ),
        )

        // User triggers reroute before replan parsing completes
        controller.reroute(rerouteCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        // Both parsings are released
        pausingParser.releaseAll()

        // User reroute wins; replan callback must not fire
        verify(exactly = 0) { replanCallback.onNewRoutes(any()) }
        verify(exactly = 1) { rerouteCallback.onNewRoutes(any(), any()) }

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.Interrupted(),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
    }

    @Test
    fun `user reroute cancels in-flight deviation parsing`() {
        // Scenario: deviation response already received (parsing in progress),
        // then user triggers reroute. User reroute should win; deviation must not apply routes.
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val rerouteDetector = nativeRerouteInterface.getRerouteDetector() as TestRerouteDetector
        val pausingParser = PausingNavigationRoutesParser(
            createTestNavigationRoutesParsing(UnconfinedTestDispatcher()),
        )
        val updateRoutes = mockk<UpdateRoutes> {
            every { this@mockk.invoke(any(), any()) } returns true
        }
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            routeParser = pausingParser,
            updateRoutes = updateRoutes,
        )

        val statesV2 = controller.recordRerouteStateV2()
        val rerouteCallback = mockk<RerouteController.RoutesCallback>(relaxed = true)

        // Auto-deviation fires; native responds — parsing starts, suspended
        nativeRerouteInterface.observer.onRerouteDetected(TEST_REROUTE_URL)
        nativeRerouteInterface.observer.onRerouteReceived(
            testRouteFixtures.loadTwoLegRoute().toDataRef(),
            TEST_REROUTE_URL,
            RouterOrigin.ONBOARD,
        )

        // User triggers reroute before deviation parsing completes
        controller.reroute(rerouteCallback)
        rerouteDetector.latestCallback!!.run(
            ExpectedFactory.createValue(
                RerouteInfo(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                    TEST_REROUTE_URL,
                    RouterOrigin.ONLINE,
                ),
            ),
        )

        // Both parsings are released
        pausingParser.releaseAll()

        // User reroute wins; deviation must not apply routes
        verify(exactly = 0) { updateRoutes(any(), any()) }
        verify(exactly = 1) { rerouteCallback.onNewRoutes(any(), any()) }

        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.Interrupted(),
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
    }

    @Test
    fun `java parsing of successful reroute is failed`() {
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
        )
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

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

        assertEquals(
            listOf(
                RerouteStateV2.Idle::class.java,
                RerouteStateV2.FetchingRoute::class.java,
                RerouteStateV2.Failed::class.java,
                RerouteStateV2.Idle::class.java,
            ),
            statesV2.map { it.javaClass },
        )
        assertFalse(
            (statesV2[2] as RerouteStateV2.Failed).isRetryable,
        )
    }

    @Test
    fun `reroute is failed`() {
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
        )
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

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

        assertEquals(
            listOf(
                RerouteStateV2.Idle::class.java,
                RerouteStateV2.FetchingRoute::class.java,
                RerouteStateV2.Failed::class.java,
                RerouteStateV2.Idle::class.java,
            ),
            statesV2.map { it.javaClass },
        )
        val failureV2 = statesV2[2] as RerouteStateV2.Failed
        assertTrue(failureV2.isRetryable)
        assertEquals(
            1,
            failureV2.reasons?.size,
        )
        val failureReasonV2 = failureV2.reasons!![0]
        assertEquals(
            com.mapbox.navigation.base.route.RouterOrigin.OFFLINE,
            failureReasonV2.routerOrigin,
        )
        assertEquals(
            TEST_REROUTE_URL,
            failureReasonV2.url.toString(),
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
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
            getCurrentRoutes = { currentRoutes },
            updateRoutes = { routes, legIndex ->
                currentRoutes = routes
                currentLegIndex = legIndex
                true
            },
        )
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

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
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Deviation.ApplyingRoute(),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
        assertEquals(
            listOf("test#1", "test#0"),
            currentRoutes.map { it.id },
        )
        assertEquals(1, currentLegIndex)
    }

    @Test
    fun `switch to alternative route is successful but route is ignored`() {
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
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
            getCurrentRoutes = { currentRoutes },
            updateRoutes = { routes, legIndex ->
                currentRoutes = routes
                currentLegIndex = legIndex
                false
            },
        )
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

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
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Deviation.RouteIgnored(),
                RerouteStateV2.Idle(),
            ),
            statesV2,
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
        val observerRegistration = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = observerRegistration,
            getCurrentRoutes = { listOf(testRoutes[0]) },
            updateRoutes = updateRoutes,
        )
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

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
        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }

    @Test
    fun `user requests reroute`() {
        val updateRoutes = mockk<UpdateRoutes>()
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            updateRoutes = updateRoutes,
        )
        val rerouteDetector =
            nativeRerouteInterface.getRerouteDetector() as? TestRerouteDetector
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

        var newRoutes: List<NavigationRoute>? = null
        var newRoutesOrigin: String? = null
        controller.reroute { routes, origin ->
            newRoutes = routes
            newRoutesOrigin = origin
        }
        rerouteDetector?.latestCallback?.run(
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
        assertEquals(
            listOf(
                RerouteStateV2.Idle(),
                RerouteStateV2.FetchingRoute(),
                RerouteStateV2.RouteFetched(com.mapbox.navigation.base.route.RouterOrigin.ONLINE),
                RerouteStateV2.Idle(),
            ),
            statesV2,
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }

    @Test
    fun `user requests reroute but it fails`() {
        val updateRoutes = mockk<UpdateRoutes>()
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            updateRoutes = updateRoutes,
        )
        val rerouteDetector =
            nativeRerouteInterface.getRerouteDetector() as? TestRerouteDetector
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

        val rerouteCallback = mockk<RerouteController.RoutesCallback>()
        controller.reroute(rerouteCallback)
        rerouteDetector?.latestCallback?.run(
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
        assertEquals(
            listOf(
                RerouteStateV2.Idle::class.java,
                RerouteStateV2.FetchingRoute::class.java,
                RerouteStateV2.Failed::class.java,
                RerouteStateV2.Idle::class.java,
            ),
            statesV2.map { it.javaClass },
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }

    @Test(expected = IllegalStateException::class)
    fun `setEnabled throws when mainThreadAssertion fails`() {
        val controller = createNativeMapboxRerouteController(
            mainThreadAssertion = { throw IllegalStateException("not on main thread") },
        )
        controller.setEnabled(true)
    }

    @Test(expected = IllegalStateException::class)
    fun `rerouteOnParametersChange throws when mainThreadAssertion fails`() {
        val controller = createNativeMapboxRerouteController(
            mainThreadAssertion = { throw IllegalStateException("not on main thread") },
        )
        controller.rerouteOnParametersChange(mockk(relaxed = true))
    }

    @Test(expected = IllegalStateException::class)
    fun `reroute throws when mainThreadAssertion fails`() {
        val controller = createNativeMapboxRerouteController(
            mainThreadAssertion = { throw IllegalStateException("not on main thread") },
        )
        controller.reroute(mockk(relaxed = true))
    }

    @Test(expected = IllegalStateException::class)
    fun `setRerouteOptionsAdapter throws when mainThreadAssertion fails`() {
        val controller = createNativeMapboxRerouteController(
            mainThreadAssertion = { throw IllegalStateException("not on main thread") },
        )
        controller.setRerouteOptionsAdapter(null)
    }

    @Test
    fun `user requests reroute but it's cancelled`() {
        val updateRoutes = mockk<UpdateRoutes>()
        val nativeRerouteInterface = MapboxNativeRerouteInterfaceImpl()
        val controller = createNativeMapboxRerouteController(
            nativeRerouteInterface = nativeRerouteInterface,
            updateRoutes = updateRoutes,
        )
        val rerouteDetector =
            nativeRerouteInterface.getRerouteDetector() as? TestRerouteDetector
        val states = controller.recordRerouteState()
        val statesV2 = controller.recordRerouteStateV2()

        val rerouteCallback = mockk<RerouteController.RoutesCallback>()
        controller.reroute(rerouteCallback)
        rerouteDetector?.latestCallback?.run(
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
        assertEquals(
            listOf(
                RerouteStateV2.Idle::class.java,
                RerouteStateV2.FetchingRoute::class.java,
                RerouteStateV2.Interrupted::class.java,
                RerouteStateV2.Idle::class.java,
            ),
            statesV2.map { it.javaClass },
        )
        verify(exactly = 0) { updateRoutes(any(), any()) }
    }
}

private fun createNativeMapboxRerouteController(
    nativeRerouteInterface: MapboxNativeRerouteInterface = MapboxNativeRerouteInterfaceImpl(),
    scope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
    parsingDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    getCurrentRoutes: () -> List<NavigationRoute> = { emptyList() },
    updateRoutes: UpdateRoutes = { _, _ -> true },
    routeParser: NavigationRoutesParser = createTestNavigationRoutesParsing(parsingDispatcher),
    mainThreadAssertion: () -> Unit = {},
) = NativeMapboxRerouteController(
    nativeRerouteInterface,
    getCurrentRoutes,
    updateRoutes,
    scope,
    routeParser,
    mainThreadAssertion,
)

private class MapboxNativeRerouteInterfaceImpl : MapboxNativeRerouteInterface {

    lateinit var observer: RerouteObserver
    private val rerouteDetector = TestRerouteDetector()

    override fun addRerouteObserver(nativeRerouteObserver: RerouteObserver) {
        observer = nativeRerouteObserver
    }

    override fun removeRerouteObserver(nativeRerouteObserver: RerouteObserver) {
    }

    override fun addNativeNavigatorRecreationObserver(
        nativeNavigatorRecreationObserver: NativeNavigatorRecreationObserver,
    ) {
    }

    override fun getRerouteDetector(): RerouteDetectorInterface? {
        return rerouteDetector
    }

    override fun getRerouteController(): RerouteControllerInterface? {
        return mockk(relaxed = true)
    }

    override fun nativeRerouteEnabled(): Boolean = true
}

private fun RerouteController.recordRerouteState(): List<RerouteState> {
    val states = mutableListOf<RerouteState>()
    this.registerRerouteStateObserver {
        states.add(it)
    }
    return states
}

@OptIn(ExperimentalMapboxNavigationAPI::class)
private fun RerouteController.recordRerouteStateV2(): List<RerouteStateV2> {
    val states = mutableListOf<RerouteStateV2>()
    this.registerRerouteStateV2Observer {
        states.add(it)
    }
    return states
}

private class PausingNavigationRoutesParser(
    private val delegate: NavigationRoutesParser,
) : NavigationRoutesParser {

    private val gates = mutableListOf<CompletableDeferred<Unit>>()

    override suspend fun parseDirectionsResponse(
        response: ResponseToParse,
    ): Result<NavigationRouteParsingSuccessfulResult> {
        val gate = CompletableDeferred<Unit>()
        gates.add(gate)
        gate.await()
        return delegate.parseDirectionsResponse(response)
    }

    fun releaseAll() {
        val toRelease = ArrayList(gates)
        gates.clear()
        toRelease.forEach { it.complete(Unit) }
    }
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
