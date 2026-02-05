@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.internal.router

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxServices
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.internal.RouterFailureFactory
import com.mapbox.navigation.base.internal.route.isExpired
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterFailureType
import com.mapbox.navigation.base.route.RouterOrigin.Companion.OFFLINE
import com.mapbox.navigation.base.route.RouterOrigin.Companion.ONLINE
import com.mapbox.navigation.core.internal.router.util.TestRouteFixtures
import com.mapbox.navigation.navigator.internal.mapToRoutingMode
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.TestSystemClock
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouterError
import com.mapbox.navigation.testing.factories.createTestNavigationRoutesParsing
import com.mapbox.navigation.testing.factories.toDataRef
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.GetRouteOptions
import com.mapbox.navigator.RouteRefreshOptions
import com.mapbox.navigator.RouterDataRefCallback
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RouterOrigin
import com.mapbox.navigator.RouterRefreshCallback
import com.mapbox.navigator.RoutingProfile
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RouterWrapperTests {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val routeParserRule = NativeRouteParserRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val clock = TestSystemClock()

    private lateinit var routerWrapper: RouterWrapper
    private val router: RouterInterface = mockk(relaxed = true)
    private val accessToken = "pk.123"
    private val route: DirectionsRoute = mockk(relaxed = true)
    private val navigationRouterCallback: NavigationRouterCallback = mockk(relaxed = true)
    private val routerRefreshCallback: NavigationRouterRefreshCallback = mockk(relaxed = true)
    private val routerOptions: RouteOptions = provideDefaultRouteOptions()
    private val nativeSignature = mockk<com.mapbox.navigator.GetRouteSignature>()
    private val signature = mockk<GetRouteSignature>() {
        every { toNativeSignature() } returns nativeSignature
    }
    private val routeUrl = routerOptions.toUrl(accessToken).toString()
    private val evData = mapOf("aaa" to "bbb")

    // these data is used in expected files
    private val routeRefreshRequestData = RouteRefreshRequestData(0, 100, 10, evData)

    private val testRouteFixtures = TestRouteFixtures()

    private val routerResultSuccess: Expected<List<RouterError>, DataRef> =
        ExpectedFactory.createValue(
            testRouteFixtures.loadTwoLegRoute().toDataRef(),
        )

    private val routerResultFailure: Expected<List<RouterError>, DataRef> =
        ExpectedFactory.createError(
            listOf(
                createRouterError(
                    FAILURE_MESSAGE,
                    FAILURE_TYPE,
                    REQUEST_ID,
                    REFRESH_TTL,
                ),
            ),
        )

    private val routerResultCancelled: Expected<List<RouterError>, DataRef> = ExpectedFactory
        .createError(
            listOf(
                createRouterError(
                    CANCELLED_MESSAGE,
                    CANCELED_TYPE,
                    REQUEST_ID,
                    REFRESH_TTL,
                ),
            ),
        )

    private val routerResultSuccessEmptyRoutes: Expected<List<RouterError>, DataRef> =
        ExpectedFactory
            .createValue(testRouteFixtures.loadEmptyRoutesResponse().toDataRef())

    private val routerResultSuccessErroneousValue: Expected<List<RouterError>, DataRef> =
        ExpectedFactory.createValue(
            "{\"message\":\"should be >= 1\",\"code\":\"InvalidInput\"}".toDataRef(),
        )

    private val routerRefreshSuccess: Expected<List<RouterError>, DataRef> =
        ExpectedFactory.createValue(
            testRouteFixtures.loadRefreshForMultiLegRoute().toDataRef(),
        )

    private val routerRefreshSuccessSecondLeg: Expected<List<RouterError>, DataRef> =
        ExpectedFactory.createValue(
            testRouteFixtures.loadRefreshForMultiLegRouteSecondLeg().toDataRef(),
        )

    private val nativeOriginOnline: RouterOrigin = RouterOrigin.ONLINE
    private val nativeOriginOnboard: RouterOrigin = RouterOrigin.ONBOARD
    private val getRouteSlot = slot<com.mapbox.navigator.RouterDataRefCallback>()
    private val refreshRouteSlot = slot<RouterRefreshCallback>()
    private val routeSlot = slot<NavigationRoute>()
    private val refreshResponseSlot = slot<DataRef>()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
        every { ThreadController.DefaultDispatcher } returns coroutineRule.testDispatcher

        mockkStatic(MapboxOptionsUtil::class)
        every {
            MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        } returns accessToken

        every { router.getRoute(any(), any(), any(), capture(getRouteSlot)) } returns 0L
        every { router.getRouteRefresh(any(), capture(refreshRouteSlot)) } returns 0L

        every { route.requestUuid() } returns UUID
        every { route.routeIndex() } returns "index"
        every { route.routeOptions() } returns routerOptions

        routerWrapper = RouterWrapper(
            router,
            ThreadController(),
            createTestNavigationRoutesParsing(
                parsingDispatcher = coroutineRule.testDispatcher,
            ),
        )
    }

    @After
    fun cleanUp() {
        unmockkObject(ThreadController)
        unmockkStatic(MapboxOptionsUtil::class)
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(routerWrapper)
    }

    @Test
    fun `get route is called with expected url and options`() {
        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        val requestOptions = GetRouteOptions(null)

        verify {
            router.getRoute(
                routeUrl,
                requestOptions,
                nativeSignature,
                any<com.mapbox.navigator.RouterDataRefCallback>(),
            )
        }
    }

    @Test
    fun `get route uses latest token`() {
        every {
            MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        } returns "new.token"
        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        val requestOptions = GetRouteOptions(null)

        verify {
            router.getRoute(
                routerOptions.toUrl("new.token").toString(),
                requestOptions,
                nativeSignature,
                any<com.mapbox.navigator.RouterDataRefCallback>(),
            )
        }
    }

    @Test
    fun `check callback called on failure with redacted token`() {
        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        getRouteSlot.captured.run(routerResultFailure, nativeOriginOnline)

        verify {
            router.getRoute(
                routeUrl,
                any(),
                nativeSignature,
                any<com.mapbox.navigator.RouterDataRefCallback>(),
            )
        }
        verify {
            navigationRouterCallback.onFailure(
                match {
                    val failure = it.singleOrNull() ?: return@match false
                    failure.url == routeUrl.toHttpUrlOrNull()!!
                        .redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)
                        .toUrl() &&
                        failure.message == FAILURE_MESSAGE &&
                        failure.type == FAILURE_SDK_TYPE &&
                        failure.routerOrigin == ONLINE
                },
                routerOptions,
            )
        }
    }

    @Test
    fun `check callback called on success and contains original options`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(routerResultSuccess, nativeOriginOnboard)

            verify {
                router.getRoute(
                    routeUrl,
                    any(),
                    nativeSignature,
                    any<com.mapbox.navigator.RouterDataRefCallback>(),
                )
            }

            val expected = DirectionsResponse.fromJson(
                testRouteFixtures.loadTwoLegRoute(),
                routerOptions,
                UUID,
            ).routes()

            verify(exactly = 1) {
                navigationRouterCallback.onRoutesReady(
                    match { it.map { it.directionsRoute } == expected },
                    OFFLINE,
                )
            }
        }

    @Test
    fun `route expiration data is updated on success`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createValue(
                    testRouteFixtures.loadTwoLegRouteWithRefreshTtl().toDataRef(),
                ),
                nativeOriginOnboard,
            )
            val routesCaptor = slot<List<NavigationRoute>>()
            verify {
                navigationRouterCallback.onRoutesReady(capture(routesCaptor), any())
            }

            clock.advanceTimeBy(9.seconds)
            assertFalse(routesCaptor.captured[0].isExpired())
            clock.advanceTimeBy(2.seconds)
            assertTrue(routesCaptor.captured[0].isExpired())
            assertEquals("cjeacbr8s21bk47lggcvce7lv#0", routesCaptor.captured[0].id)
        }

    @Test
    fun `route expiration data is updated on success when no refresh ttl`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createValue(
                    testRouteFixtures.loadTwoLegRoute().toDataRef(),
                ),
                nativeOriginOnboard,
            )

            val routesCaptor = slot<List<NavigationRoute>>()
            verify {
                navigationRouterCallback.onRoutesReady(capture(routesCaptor), any())
            }

            clock.advanceTimeBy(10_000.seconds)
            assertFalse(routesCaptor.captured[0].isExpired())
            assertEquals("cjeacbr8s21bk47lggcvce7lv#0", routesCaptor.captured[0].id)
        }

    @Test
    fun `route expiration data is updated on success for multiple routes`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createValue(
                    testRouteFixtures.loadTwoRoutes().toDataRef(),
                ),
                nativeOriginOnboard,
            )

            val routesCaptor = slot<List<NavigationRoute>>()
            verify {
                navigationRouterCallback.onRoutesReady(capture(routesCaptor), any())
            }

            clock.advanceTimeBy(4.seconds)
            assertFalse(routesCaptor.captured[1].isExpired())
            clock.advanceTimeBy(2.seconds)
            assertTrue(routesCaptor.captured[1].isExpired())

            clock.advanceTimeBy(3.seconds)
            assertFalse(routesCaptor.captured[0].isExpired())
            clock.advanceTimeBy(2.seconds)
            assertTrue(routesCaptor.captured[0].isExpired())

            assertEquals("cjeacbr8s21bk47lggcvce7lv#0", routesCaptor.captured[0].id)
            assertEquals("cjeacbr8s21bk47lggcvce7lv#1", routesCaptor.captured[1].id)
        }

    @Test
    fun `check on failure is called on success response with no routes`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(routerResultSuccessEmptyRoutes, nativeOriginOnboard)

            verify {
                router.getRoute(
                    routeUrl,
                    any(),
                    nativeSignature,
                    any<com.mapbox.navigator.RouterDataRefCallback>(),
                )
            }

            val expected = RouterFailureFactory.create(
                url = routeUrl.toHttpUrlOrNull()!!.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)
                    .toUrl(),
                routerOrigin = OFFLINE,
                message = "Failed to parse response",
                type = RouterFailureType.RESPONSE_PARSING_ERROR,
                throwable = IllegalStateException(
                    "no routes returned, collection is empty",
                ),
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertEquals(expected.message, failure.message)
            assertEquals(expected.type, failure.type)
            assertEquals(expected.routerOrigin, failure.routerOrigin)
            assertEquals(expected.url, failure.url)
            assertEquals(expected.throwable!!.message, failure.throwable!!.message)
            assertFalse(failure.isRetryable)
        }

    @Test
    fun `check on failure is called on erroneous success response`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(routerResultSuccessErroneousValue, nativeOriginOnboard)

            verify {
                router.getRoute(
                    routeUrl,
                    any(),
                    nativeSignature,
                    any<com.mapbox.navigator.RouterDataRefCallback>(),
                )
            }

            val expected = RouterFailureFactory.create(
                url = routeUrl.toHttpUrlOrNull()!!.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)
                    .toUrl(),
                routerOrigin = OFFLINE,
                message = "Failed to parse response",
                type = RouterFailureType.RESPONSE_PARSING_ERROR,
                throwable = IllegalStateException(
                    "java.lang.IllegalStateException: Missing required properties:  routes",
                ),
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertEquals(expected.message, failure.message)
            assertEquals(expected.type, failure.type)
            assertEquals(expected.routerOrigin, failure.routerOrigin)
            assertEquals(expected.url, failure.url)
            assertEquals(expected.throwable!!.message, failure.throwable!!.message)
            assertFalse(failure.isRetryable)
        }

    @Test
    fun `route request network failure`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createError(
                    listOf(
                        createRouterError(
                            type = RouterErrorType.NETWORK_ERROR,
                        ),
                    ),
                ),
                nativeOriginOnboard,
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertTrue(failure.isRetryable)
            assertEquals(RouterFailureType.NETWORK_ERROR, failure.type)
        }

    @Test
    fun `route request directions api input error`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createError(
                    listOf(
                        createRouterError(
                            type = RouterErrorType.INPUT_ERROR,
                        ),
                    ),
                ),
                nativeOriginOnboard,
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertFalse(failure.isRetryable)
            assertEquals(RouterFailureType.INPUT_ERROR, failure.type)
        }

    @Test
    fun `route request directions api auth error`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createError(
                    listOf(
                        createRouterError(
                            type = RouterErrorType.AUTHENTICATION_ERROR,
                        ),
                    ),
                ),
                nativeOriginOnboard,
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertFalse(failure.isRetryable)
            assertEquals(RouterFailureType.AUTHENTICATION_ERROR, failure.type)
        }

    @Test
    fun `route request directions api route creation error`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createError(
                    listOf(
                        createRouterError(
                            type = RouterErrorType.ROUTE_CREATION_ERROR,
                        ),
                    ),
                ),
                nativeOriginOnboard,
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertFalse(failure.isRetryable)
            assertEquals(RouterFailureType.ROUTE_CREATION_ERROR, failure.type)
        }

    @Test
    fun `route request directions api route expiry error`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createError(
                    listOf(
                        createRouterError(
                            message = "some test error message.",
                            type = RouterErrorType.ROUTE_NOT_FOUND_ON_SERVER,
                        ),
                    ),
                ),
                nativeOriginOnboard,
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertFalse(failure.isRetryable)
            assertEquals(RouterFailureType.ROUTE_EXPIRY_ERROR, failure.type)
        }

    @Test
    fun `route request unknown error`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createError(
                    listOf(
                        createRouterError(
                            type = RouterErrorType.UNKNOWN,
                        ),
                    ),
                ),
                nativeOriginOnboard,
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertFalse(failure.isRetryable)
            assertEquals(RouterFailureType.UNKNOWN_ERROR, failure.type)
        }

    @Test
    fun `route request throttling error`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createError(
                    listOf(
                        createRouterError(
                            type = RouterErrorType.THROTTLING_ERROR,
                        ),
                    ),
                ),
                nativeOriginOnboard,
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertFalse(failure.isRetryable)
            assertEquals(RouterFailureType.THROTTLING_ERROR, failure.type)
        }

    @Test
    fun `check callback called on cancel`() = coroutineRule.runBlockingTest {
        every { router.cancelRouteRequest(any()) } answers {
            getRouteSlot.captured.run(routerResultCancelled, nativeOriginOnboard)
        }

        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        routerWrapper.cancelRouteRequest(REQUEST_ID)

        verify { navigationRouterCallback.onCanceled(routerOptions, OFFLINE) }
    }

    @Test
    fun `check cancel and NN callback afterwards - invoke only cancel`() {
        every { router.getRoute(any(), any(), any(), capture(getRouteSlot)) } returns REQUEST_ID
        every { router.cancelRouteRequest(any()) } answers {}

        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        routerWrapper.cancelRouteRequest(REQUEST_ID)

        verify(exactly = 1) { navigationRouterCallback.onCanceled(routerOptions, OFFLINE) }

        clearMocks(navigationRouterCallback, answers = false)

        getRouteSlot.captured.run(routerResultSuccess, nativeOriginOnline)

        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onRoutesReady(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
    }

    @Test
    fun `check callback called on cancelAll`() = coroutineRule.runBlockingTest {
        every { router.cancelAll() } answers {
            getRouteSlot.captured.run(routerResultCancelled, nativeOriginOnline)
        }

        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        routerWrapper.cancelAll()

        verify { navigationRouterCallback.onCanceled(routerOptions, OFFLINE) }
    }

    @Test
    fun `check cancelAll and NN callback afterwards - invoke only cancel`() {
        every { router.getRoute(any(), any(), any(), capture(getRouteSlot)) } returns REQUEST_ID
        every { router.cancelAll() } answers {}

        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        routerWrapper.cancelAll()

        verify(exactly = 1) { navigationRouterCallback.onCanceled(routerOptions, OFFLINE) }

        clearMocks(navigationRouterCallback, answers = false)

        getRouteSlot.captured.run(routerResultSuccess, nativeOriginOnline)

        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onRoutesReady(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
    }

    @Test
    fun `cancel a specific route request when multiple are running`() {
        val requestIdOne = 1L
        val requestIdTwo = 2L
        val refreshIdOne = 3L
        val refreshIdTwo = 4L

        verify(exactly = 0) { router.cancelRouteRequest(any()) }

        routerWrapper.cancelRouteRequest(requestIdOne)
        routerWrapper.cancelRouteRequest(requestIdTwo)
        routerWrapper.cancelRouteRequest(refreshIdOne)
        routerWrapper.cancelRouteRequest(refreshIdTwo)

        verify(exactly = 1) { router.cancelRouteRequest(requestIdOne) }
        verify(exactly = 1) { router.cancelRouteRequest(requestIdTwo) }
        verify(exactly = 1) { router.cancelRouteRequest(refreshIdOne) }
        verify(exactly = 1) { router.cancelRouteRequest(refreshIdTwo) }
    }

    @Test
    fun `check refresh cancel and NN callback afterwards - invoke only cancel`() {
        val options = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(17.035958238636283, 51.123073179658476),
                    Point.fromLngLat(17.033342297413395, 51.11608871549779),
                    Point.fromLngLat(17.030364743939824, 51.11309150868635),
                    Point.fromLngLat(17.032132688234814, 51.10720758039439),
                ),
            )
            .build()
        val route = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options,
            ),
            options,
            ONLINE,
        ).first()

        every { router.getRouteRefresh(any(), capture(refreshRouteSlot)) } returns REQUEST_ID
        every { router.cancelRouteRequest(any()) } answers {}

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        routerWrapper.cancelRouteRefreshRequest(REQUEST_ID)

        verify(exactly = 1) {
            routerRefreshCallback.onFailure(any())
        }

        clearMocks(routerRefreshCallback, answers = false)

        refreshRouteSlot.captured.run(routerRefreshSuccess, nativeOriginOnline, hashMapOf())

        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { routerRefreshCallback.onRefreshReady(any(), any()) }
    }

    @Test
    fun `check refresh cancelAll and NN callback afterwards - invoke only cancel`() {
        val options = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(17.035958238636283, 51.123073179658476),
                    Point.fromLngLat(17.033342297413395, 51.11608871549779),
                    Point.fromLngLat(17.030364743939824, 51.11309150868635),
                    Point.fromLngLat(17.032132688234814, 51.10720758039439),
                ),
            )
            .build()
        val route = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options,
            ),
            options,
            ONLINE,
        ).first()

        every { router.getRouteRefresh(any(), capture(refreshRouteSlot)) } returns REQUEST_ID
        every { router.cancelAll() } answers {}

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        routerWrapper.cancelAll()

        verify(exactly = 1) { routerRefreshCallback.onFailure(any()) }

        clearMocks(routerRefreshCallback, answers = false)

        refreshRouteSlot.captured.run(routerRefreshSuccess, nativeOriginOnline, hashMapOf())

        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { routerRefreshCallback.onRefreshReady(any(), any()) }
    }

    @Test
    fun `check cancel route request if has running refresh with the same id`() {
        val options = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(17.035958238636283, 51.123073179658476),
                    Point.fromLngLat(17.033342297413395, 51.11608871549779),
                    Point.fromLngLat(17.030364743939824, 51.11309150868635),
                    Point.fromLngLat(17.032132688234814, 51.10720758039439),
                ),
            )
            .build()
        val route = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options,
            ),
            options,
            ONLINE,
        ).first()

        every { router.getRoute(any(), any(), any(), capture(getRouteSlot)) } returns REQUEST_ID
        every { router.getRouteRefresh(any(), capture(refreshRouteSlot)) } returns REQUEST_ID
        every { router.cancelRouteRequest(any()) } answers {}
        every { router.cancelRouteRefreshRequest(any()) } answers {}

        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        routerWrapper.cancelRouteRequest(REQUEST_ID)

        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 1) { navigationRouterCallback.onCanceled(routerOptions, OFFLINE) }

        clearMocks(routerRefreshCallback, navigationRouterCallback, answers = false)

        getRouteSlot.captured.run(routerResultSuccess, nativeOriginOnline)

        verify(exactly = 0) { routerRefreshCallback.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onRoutesReady(any(), any()) }

        clearMocks(routerRefreshCallback, navigationRouterCallback, answers = false)

        refreshRouteSlot.captured.run(routerRefreshSuccess, nativeOriginOnline, hashMapOf())

        verify(exactly = 1) { routerRefreshCallback.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onRoutesReady(any(), any()) }
    }

    @Test
    fun `check cancel route refresh request if has running route with the same id`() {
        val options = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(17.035958238636283, 51.123073179658476),
                    Point.fromLngLat(17.033342297413395, 51.11608871549779),
                    Point.fromLngLat(17.030364743939824, 51.11309150868635),
                    Point.fromLngLat(17.032132688234814, 51.10720758039439),
                ),
            )
            .build()
        val route = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options,
            ),
            options,
            ONLINE,
        ).first()

        every { router.getRoute(any(), any(), any(), capture(getRouteSlot)) } returns REQUEST_ID
        every { router.getRouteRefresh(any(), capture(refreshRouteSlot)) } returns REQUEST_ID
        every { router.cancelRouteRequest(any()) } answers {}
        every { router.cancelRouteRefreshRequest(any()) } answers {}

        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        routerWrapper.cancelRouteRefreshRequest(REQUEST_ID)

        verify(exactly = 1) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }

        clearMocks(routerRefreshCallback, navigationRouterCallback, answers = false)
        refreshRouteSlot.captured.run(routerRefreshSuccess, nativeOriginOnline, hashMapOf())

        verify(exactly = 0) { routerRefreshCallback.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onRoutesReady(any(), any()) }

        clearMocks(routerRefreshCallback, navigationRouterCallback, answers = false)

        getRouteSlot.captured.run(routerResultSuccess, nativeOriginOnline)

        verify(exactly = 0) { routerRefreshCallback.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
        verify(exactly = 1) { navigationRouterCallback.onRoutesReady(any(), any()) }
    }

    @Test
    fun `cancelAll for multiple running requests of different types`() {
        val options = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(17.035958238636283, 51.123073179658476),
                    Point.fromLngLat(17.033342297413395, 51.11608871549779),
                    Point.fromLngLat(17.030364743939824, 51.11309150868635),
                    Point.fromLngLat(17.032132688234814, 51.10720758039439),
                ),
            )
            .build()
        val route = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options,
            ),
            options,
            ONLINE,
        ).first()
        val navigationRouterCallback2: NavigationRouterCallback = mockk(relaxed = true)
        val routerRefreshCallback2: NavigationRouterRefreshCallback = mockk(relaxed = true)

        val getRouteSlots = mutableListOf<RouterDataRefCallback>()
        val refreshRouteSlots = mutableListOf<RouterRefreshCallback>()
        every {
            router.getRoute(
                any(),
                any(),
                any(),
                capture(getRouteSlots),
            )
        } answers { Random.nextLong() }
        every {
            router.getRouteRefresh(
                any(),
                capture(refreshRouteSlots),
            )
        } answers { Random.nextLong() }
        every { router.cancelAll() } answers {}

        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback2)
        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback2)
        routerWrapper.cancelAll()

        verify(exactly = 1) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 1) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 1) { routerRefreshCallback2.onFailure(any()) }
        verify(exactly = 1) { navigationRouterCallback2.onCanceled(any(), any()) }

        clearMocks(
            routerRefreshCallback,
            navigationRouterCallback,
            routerRefreshCallback2,
            navigationRouterCallback2,
            answers = false,
        )

        refreshRouteSlots[0].run(routerRefreshSuccess, nativeOriginOnline, hashMapOf())

        verify(exactly = 0) { routerRefreshCallback.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { routerRefreshCallback2.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback2.onFailure(any()) }
        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onRoutesReady(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onRoutesReady(any(), any()) }

        clearMocks(
            routerRefreshCallback,
            navigationRouterCallback,
            routerRefreshCallback2,
            navigationRouterCallback2,
            answers = false,
        )

        getRouteSlots[0].run(routerResultSuccess, nativeOriginOnline)

        verify(exactly = 0) { routerRefreshCallback.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { routerRefreshCallback2.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback2.onFailure(any()) }
        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onRoutesReady(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onRoutesReady(any(), any()) }

        clearMocks(
            routerRefreshCallback,
            navigationRouterCallback,
            routerRefreshCallback2,
            navigationRouterCallback2,
            answers = false,
        )

        getRouteSlots[1].run(routerResultSuccess, nativeOriginOnline)

        verify(exactly = 0) { routerRefreshCallback.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { routerRefreshCallback2.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback2.onFailure(any()) }
        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onRoutesReady(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onRoutesReady(any(), any()) }

        clearMocks(
            routerRefreshCallback,
            navigationRouterCallback,
            routerRefreshCallback2,
            navigationRouterCallback2,
            answers = false,
        )

        refreshRouteSlots[1].run(routerRefreshSuccess, nativeOriginOnline, hashMapOf())

        verify(exactly = 0) { routerRefreshCallback.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback.onFailure(any()) }
        verify(exactly = 0) { routerRefreshCallback2.onRefreshReady(any(), any()) }
        verify(exactly = 0) { routerRefreshCallback2.onFailure(any()) }
        verify(exactly = 0) { navigationRouterCallback.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback.onRoutesReady(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onCanceled(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onFailure(any(), any()) }
        verify(exactly = 0) { navigationRouterCallback2.onRoutesReady(any(), any()) }
    }

    @Test
    fun `route refresh fails with null requestUuid`() {
        val route = createNavigationRoute(
            directionsRoute = DirectionsRoute.builder()
                .requestUuid(null)
                .distance(100.0)
                .duration(100.0)
                .routeIndex("0")
                .routeOptions(routerOptions)
                .build(),
        )

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)

        val expectedErrorMessage =
            """
               Route refresh failed because of a empty or null param:
               requestUuid = null
            """.trimIndent()

        val errorSlot = slot<NavigationRouterRefreshError>()
        verify(exactly = 1) { routerRefreshCallback.onFailure(capture(errorSlot)) }
        verify(exactly = 0) { router.getRouteRefresh(any(), any()) }
        assertEquals("Route refresh failed", errorSlot.captured.message)
        assertEquals(expectedErrorMessage, errorSlot.captured.throwable?.message)
    }

    @Test
    fun `route refresh set right params`() {
        val newToken = "new.token"
        val routeOptions = provideDefaultRouteOptions()
        val route = createNavigationRoutes(
            response = DirectionsResponse.builder()
                .code("200")
                .uuid(UUID)
                .routes(listOf(createDirectionsRoute(routeIndex = "0")))
                .build(),
            options = routeOptions,
            routerOrigin = ONLINE,
        ).first()

        val legIndex = 12
        val routeGeometryIndex = 23
        val legGeometryIndex = 19
        val requestData = RouteRefreshRequestData(
            legIndex,
            routeGeometryIndex,
            legGeometryIndex,
            evData,
        )
        every {
            MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        } returns newToken
        routerWrapper.getRouteRefresh(route, requestData, routerRefreshCallback)

        val expectedRefreshOptions = RouteRefreshOptions(
            UUID,
            0,
            legIndex,
            RoutingProfile(
                routeOptions.profile().mapToRoutingMode(),
                routeOptions.user(),
            ),
            routerOptions.baseUrl(),
            routeGeometryIndex,
            HashMap(evData),
        )

        verify(exactly = 1) {
            router.getRouteRefresh(
                expectedRefreshOptions,
                any(),
            )
        }
    }

    @Test
    fun `route refresh successful`() = runBlockingTest {
        val options = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(17.035958238636283, 51.123073179658476),
                    Point.fromLngLat(17.033342297413395, 51.11608871549779),
                    Point.fromLngLat(17.030364743939824, 51.11309150868635),
                    Point.fromLngLat(17.032132688234814, 51.10720758039439),
                ),
            )
            .build()
        val route = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options,
            ),
            options,
            ONLINE,
        ).first()

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        refreshRouteSlot.captured.run(routerRefreshSuccess, nativeOriginOnboard, hashMapOf())

        val expected = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadRefreshedMultiLegRoute(),
                options,
            ),
            options,
            ONLINE,
        ).first()

        verify(exactly = 1) {
            routerRefreshCallback.onRefreshReady(
                capture(routeSlot),
                capture(refreshResponseSlot),
            )
        }
        checkRefreshedNavigationRouteWithWithWaypoints(expected, routeSlot.captured)
        assertNotNull(routeSlot.captured.routeRefreshMetadata)
        assertTrue(routeSlot.captured.routeRefreshMetadata!!.isUpToDate)
        assertEquals(routerRefreshSuccess.value, refreshResponseSlot.captured)
    }

    @Test
    fun `route refresh successful starting from second leg`() = runBlockingTest {
        val options = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(17.035958238636283, 51.123073179658476),
                    Point.fromLngLat(17.033342297413395, 51.11608871549779),
                    Point.fromLngLat(17.030364743939824, 51.11309150868635),
                    Point.fromLngLat(17.032132688234814, 51.10720758039439),
                ),
            )
            .build()
        val route = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options,
            ),
            options,
            ONLINE,
        ).first()

        routerWrapper.getRouteRefresh(
            route,
            RouteRefreshRequestData(
                1,
                routeRefreshRequestData.routeGeometryIndex,
                routeRefreshRequestData.legGeometryIndex,
                evData,
            ),
            routerRefreshCallback,
        )
        refreshRouteSlot.captured.run(
            routerRefreshSuccessSecondLeg,
            nativeOriginOnboard,
            hashMapOf(),
        )

        val expected = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadRefreshedMultiLegRouteSecondLeg(),
                options,
            ),
            options,
            ONLINE,
        ).first()

        verify(exactly = 1) { routerRefreshCallback.onRefreshReady(capture(routeSlot), any()) }
        checkRefreshedNavigationRouteWithWithWaypoints(expected, routeSlot.captured)
    }

    private fun checkRefreshedNavigationRouteWithWithWaypoints(
        expected: NavigationRoute,
        actual: NavigationRoute,
    ) {
        assertEquals(expected.directionsRoute, actual.directionsRoute)
        assertEquals(expected, actual) // directions route equality
        assertEquals(expected.waypoints, actual.waypoints)
    }

    @Test
    fun `route refresh failure`() {
        val route = createNavigationRoute(
            DirectionsRoute.builder()
                .requestUuid(UUID)
                .distance(100.0)
                .duration(100.0)
                .routeIndex("0")
                .routeOptions(routerOptions)
                .build(),
        )

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        refreshRouteSlot.captured.run(routerResultFailure, nativeOriginOnboard, hashMapOf())

        val errorMessage =
            """
               Route refresh failed.
               requestUuid = $UUID
               message = $FAILURE_MESSAGE
               type = $FAILURE_TYPE
               requestId = $REQUEST_ID
               refreshTTL = $REFRESH_TTL
               routeRefreshRequestData = $routeRefreshRequestData
            """.trimIndent()

        val errorSlot = slot<NavigationRouterRefreshError>()

        verify(exactly = 1) { routerRefreshCallback.onFailure(capture(errorSlot)) }
        assertEquals("Route refresh failed", errorSlot.captured.message)
        assertEquals(errorMessage, errorSlot.captured.throwable?.message)
    }

    @Test
    fun `route refresh failure with refresh ttl updates routes expiration data`() {
        val route = createNavigationRoute(
            DirectionsRoute.builder()
                .requestUuid(UUID)
                .distance(100.0)
                .duration(100.0)
                .routeIndex("0")
                .routeOptions(routerOptions)
                .build(),
        )

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        refreshRouteSlot.captured.run(
            ExpectedFactory.createError<List<RouterError>, DataRef>(
                listOf(
                    createRouterError(
                        FAILURE_MESSAGE,
                        FAILURE_TYPE,
                        REQUEST_ID,
                        REFRESH_TTL,
                    ),
                ),
            ),
            nativeOriginOnboard,
            hashMapOf(),
        )

        clock.advanceTimeBy(REFRESH_TTL.seconds - 1.seconds)
        assertFalse(route.isExpired())
        clock.advanceTimeBy(2.seconds)
        assertTrue(route.isExpired())
    }

    @Test
    fun `route refresh failure without refresh ttl does not update routes expiration data`() {
        val route = createNavigationRoute(
            DirectionsRoute.builder()
                .requestUuid(UUID)
                .distance(100.0)
                .duration(100.0)
                .routeIndex("0")
                .routeOptions(routerOptions)
                .build(),
        )

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        refreshRouteSlot.captured.run(
            ExpectedFactory.createError<List<RouterError>, DataRef>(
                listOf(
                    createRouterError(
                        FAILURE_MESSAGE,
                        FAILURE_TYPE,
                        REQUEST_ID,
                        null,
                    ),
                ),
            ),
            nativeOriginOnboard,
            hashMapOf(),
        )

        clock.advanceTimeBy(10_000.seconds)
        assertFalse(route.isExpired())
    }

    @Test
    fun `route refresh successful with refresh ttl updates route expiration data`() =
        runBlockingTest {
            val options = RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(17.035958238636283, 51.123073179658476),
                        Point.fromLngLat(17.033342297413395, 51.11608871549779),
                        Point.fromLngLat(17.030364743939824, 51.11309150868635),
                        Point.fromLngLat(17.032132688234814, 51.10720758039439),
                    ),
                )
                .build()
            val route = createNavigationRoutes(
                DirectionsResponse.fromJson(
                    testRouteFixtures.loadMultiLegRouteForRefresh(),
                    options,
                ),
                options,
                ONLINE,
            ).first()

            routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
            refreshRouteSlot.captured.run(
                ExpectedFactory.createValue(
                    testRouteFixtures.loadRefreshForMultiLegRouteWithRefreshTtl().toDataRef(),
                ),
                nativeOriginOnboard,
                hashMapOf(),
            )

            val routeCaptor = slot<NavigationRoute>()
            verify { routerRefreshCallback.onRefreshReady(capture(routeCaptor), any()) }

            clock.advanceTimeBy(49.seconds)
            assertFalse(routeCaptor.captured.isExpired())
            clock.advanceTimeBy(2.seconds)
            assertTrue(routeCaptor.captured.isExpired())
        }

    @Test
    fun `route refresh successful without refresh ttl does not update route expiration data`() =
        runBlockingTest {
            val options = RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(17.035958238636283, 51.123073179658476),
                        Point.fromLngLat(17.033342297413395, 51.11608871549779),
                        Point.fromLngLat(17.030364743939824, 51.11309150868635),
                        Point.fromLngLat(17.032132688234814, 51.10720758039439),
                    ),
                )
                .build()
            val route = createNavigationRoutes(
                DirectionsResponse.fromJson(
                    testRouteFixtures.loadMultiLegRouteForRefresh(),
                    options,
                ),
                options,
                ONLINE,
                100L,
            ).first()

            routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
            refreshRouteSlot.captured.run(routerRefreshSuccess, nativeOriginOnboard, hashMapOf())

            val routeCaptor = slot<NavigationRoute>()
            verify { routerRefreshCallback.onRefreshReady(capture(routeCaptor), any()) }

            clock.advanceTimeBy(10_000.seconds)
            assertFalse(routeCaptor.captured.isExpired())
        }

    @Test
    fun `check on failure callback is called on getRoute when router reset`() {
        routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
        routerWrapper.resetRouter(mockk())
        getRouteSlot.captured.run(routerResultSuccess, nativeOriginOnline)

        verify {
            router.getRoute(
                routeUrl,
                any(),
                nativeSignature,
                any<com.mapbox.navigator.RouterDataRefCallback>(),
            )
        }

        val expected = RouterFailureFactory.create(
            url = routeUrl.toHttpUrlOrNull()!!.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM).toUrl(),
            routerOrigin = ONLINE,
            message = "Failed to get a route",
            type = RouterFailureType.ROUTER_RECREATION_ERROR,
            isRetryable = true,
        )

        val failures = slot<List<RouterFailure>>()
        verify(exactly = 1) {
            navigationRouterCallback.onFailure(capture(failures), routerOptions)
        }
        val failure: RouterFailure = failures.captured[0]
        assertEquals(expected.message, failure.message)
        assertEquals(expected.type, failure.type)
        assertEquals(expected.routerOrigin, failure.routerOrigin)
        assertEquals(expected.url, failure.url)
        assertTrue(failure.isRetryable)
    }

    @Test
    fun `check on failure callback is called on getRouteRefresh when router reset`() = runTest {
        val options = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(17.035958238636283, 51.123073179658476),
                    Point.fromLngLat(17.033342297413395, 51.11608871549779),
                    Point.fromLngLat(17.030364743939824, 51.11309150868635),
                    Point.fromLngLat(17.032132688234814, 51.10720758039439),
                ),
            )
            .build()
        val route = createNavigationRoutes(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options,
            ),
            options,
            ONLINE,
        ).first()

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        routerWrapper.resetRouter(mockk())
        refreshRouteSlot.captured.run(routerRefreshSuccess, nativeOriginOnboard, hashMapOf())

        val errorSlot = slot<NavigationRouterRefreshError>()
        verify(exactly = 1) { routerRefreshCallback.onFailure(capture(errorSlot)) }
        assertEquals("Failed to refresh a route", errorSlot.captured.message)
    }

    private fun provideDefaultRouteOptions(): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .baseUrl("https://my.url.com")
            .apply {
                coordinates(Point.fromLngLat(.0, .0), null, Point.fromLngLat(.0, .0))
            }.build()
    }

    @Test
    fun `route request missing tiles error`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, signature, navigationRouterCallback)
            getRouteSlot.captured.run(
                ExpectedFactory.createError(
                    listOf(
                        createRouterError(
                            type = RouterErrorType.MISSING_TILES_ERROR,
                        ),
                    ),
                ),
                nativeOriginOnboard,
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) {
                navigationRouterCallback.onFailure(capture(failures), routerOptions)
            }
            val failure: RouterFailure = failures.captured[0]
            assertFalse(failure.isRetryable)
            assertEquals(RouterFailureType.MISSING_TILES_ERROR, failure.type)
        }

    private companion object {

        private const val CANCELLED_MESSAGE = "Cancelled"
        private const val FAILURE_MESSAGE = "No suitable edges near location"
        private val FAILURE_TYPE = RouterErrorType.UNKNOWN
        private const val FAILURE_SDK_TYPE = RouterFailureType.UNKNOWN_ERROR
        private val CANCELED_TYPE = RouterErrorType.REQUEST_CANCELLED
        private const val REQUEST_ID = 19L
        private const val REFRESH_TTL = 100
        private const val UUID = "cjeacbr8s21bk47lggcvce7lv"
    }
}
