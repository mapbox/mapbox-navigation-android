package com.mapbox.navigation.route.internal

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin.Offboard
import com.mapbox.navigation.base.route.RouterOrigin.Onboard
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.mapToRoutingMode
import com.mapbox.navigation.route.internal.util.ACCESS_TOKEN_QUERY_PARAM
import com.mapbox.navigation.route.internal.util.TestRouteFixtures
import com.mapbox.navigation.route.internal.util.redactQueryParam
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.MockLoggerRule
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteRefreshOptions
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterErrorType
import com.mapbox.navigator.RouterInterface
import com.mapbox.navigator.RouterOrigin
import com.mapbox.navigator.RouterRefreshCallback
import com.mapbox.navigator.RoutingProfile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RouterWrapperTests {

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var routerWrapper: RouterWrapper
    private val mapboxNativeNavigator: MapboxNativeNavigator = mockk(relaxed = true)
    private val router: RouterInterface = mockk(relaxed = true)
    private val accessToken = "pk.123"
    private val route: DirectionsRoute = mockk(relaxed = true)
    private val routerCallback: RouterCallback = mockk(relaxed = true)
    private val routerRefreshCallback: RouteRefreshCallback = mockk(relaxed = true)
    private val routerOptions: RouteOptions = provideDefaultRouteOptions()
    private val routeUrl = routerOptions.toUrl(accessToken).toString()

    private val testRouteFixtures = TestRouteFixtures()

    private val routerResultSuccess: Expected<RouterError, String> = mockk {
        every { isValue } returns true
        every { isError } returns false
        every { value } returns testRouteFixtures.loadTwoLegRoute()
        every { error } returns null

        val valueSlot = slot<Expected.Transformer<String, Unit>>()
        every { fold(any(), capture(valueSlot)) } answers {
            valueSlot.captured.invoke(this@mockk.value!!)
        }
    }
    private val routerResultFailure: Expected<RouterError, String> = mockk {
        every { isValue } returns false
        every { isError } returns true
        every { value } returns null
        every { error } returns RouterError(
            FAILURE_MESSAGE,
            FAILURE_CODE,
            FAILURE_TYPE,
            REQUEST_ID
        )

        val errorSlot = slot<Expected.Transformer<RouterError, Unit>>()
        every { fold(capture(errorSlot), any()) } answers {
            errorSlot.captured.invoke(this@mockk.error!!)
        }
    }
    private val routerResultCancelled: Expected<RouterError, String> = mockk {
        every { isValue } returns false
        every { isError } returns true
        every { value } returns null
        every { error } returns RouterError(
            CANCELLED_MESSAGE,
            FAILURE_CODE,
            CANCELED_TYPE,
            REQUEST_ID
        )

        val errorSlot = slot<Expected.Transformer<RouterError, Unit>>()
        every { fold(capture(errorSlot), any()) } answers {
            errorSlot.captured.invoke(this@mockk.error!!)
        }
    }
    private val routerResultSuccessEmptyRoutes: Expected<RouterError, String> = mockk {
        every { isValue } returns true
        every { isError } returns false
        every { value } returns testRouteFixtures.loadEmptyRoutesResponse()
        every { error } returns null

        val valueSlot = slot<Expected.Transformer<String, Unit>>()
        every { fold(any(), capture(valueSlot)) } answers {
            valueSlot.captured.invoke(this@mockk.value!!)
        }
    }
    private val routerResultSuccessErroneousValue: Expected<RouterError, String> =
        ExpectedFactory.createValue(
            "{\"message\":\"should be >= 1\",\"code\":\"InvalidInput\"}"
        )

    private val routerRefreshSuccess: Expected<RouterError, String> = mockk {
        every { isValue } returns true
        every { isError } returns false
        every { value } returns testRouteFixtures.loadRefreshForMultiLegRoute()
        every { error } returns null

        val valueSlot = slot<Expected.Transformer<String, Unit>>()
        every { fold(any(), capture(valueSlot)) } answers {
            valueSlot.captured.invoke(this@mockk.value!!)
        }
    }
    private val routerRefreshSuccessSecondLeg: Expected<RouterError, String> = mockk {
        every { isValue } returns true
        every { isError } returns false
        every { value } returns testRouteFixtures.loadRefreshForMultiLegRouteSecondLeg()
        every { error } returns null

        val valueSlot = slot<Expected.Transformer<String, Unit>>()
        every { fold(any(), capture(valueSlot)) } answers {
            valueSlot.captured.invoke(this@mockk.value!!)
        }
    }
    private val nativeOriginOnline: RouterOrigin = RouterOrigin.ONLINE
    private val nativeOriginOnboard: RouterOrigin = RouterOrigin.ONBOARD
    private val getRouteSlot = slot<com.mapbox.navigator.RouterCallback>()
    private val refreshRouteSlot = slot<RouterRefreshCallback>()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
        every { ThreadController.DefaultDispatcher } returns coroutineRule.testDispatcher

        every { mapboxNativeNavigator.router } returns router
        every { router.getRoute(any(), capture(getRouteSlot)) } returns 0L
        every { router.getRouteRefresh(any(), capture(refreshRouteSlot)) } returns 0L

        every { route.requestUuid() } returns UUID
        every { route.routeIndex() } returns "index"
        every { route.routeOptions() } returns routerOptions

        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
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
                            every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }

        routerWrapper = RouterWrapper(
            accessToken,
            mapboxNativeNavigator.router,
            ThreadController()
        )
    }

    @After
    fun cleanUp() {
        unmockkObject(ThreadController)
        unmockkObject(NativeRouteParserWrapper)
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(routerWrapper)
    }

    @Test
    fun `get route is called with expected url`() {
        routerWrapper.getRoute(routerOptions, routerCallback)

        verify { router.getRoute(routeUrl, any()) }
    }

    @Test
    fun `check callback called on failure with redacted token`() {
        routerWrapper.getRoute(routerOptions, routerCallback)
        getRouteSlot.captured.run(routerResultFailure, nativeOriginOnline)

        val expected = listOf(
            RouterFailure(
                url = routeUrl.toHttpUrlOrNull()!!.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)
                    .toUrl(),
                routerOrigin = Offboard,
                message = FAILURE_MESSAGE,
                code = FAILURE_CODE,
                throwable = null
            )
        )

        verify { router.getRoute(routeUrl, any()) }
        verify { routerCallback.onFailure(expected, routerOptions) }
    }

    @Test
    fun `check callback called on success and contains original options`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, routerCallback)
            getRouteSlot.captured.run(routerResultSuccess, nativeOriginOnboard)

            verify { router.getRoute(routeUrl, any()) }

            val expected = DirectionsResponse.fromJson(
                testRouteFixtures.loadTwoLegRoute(),
                routerOptions,
                UUID
            ).routes()

            verify(exactly = 1) { routerCallback.onRoutesReady(expected, Onboard) }
        }

    @Test
    fun `check on failure is called on success response with no routes`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, routerCallback)
            getRouteSlot.captured.run(routerResultSuccessEmptyRoutes, nativeOriginOnboard)

            verify { router.getRoute(routeUrl, any()) }

            val expected = RouterFailure(
                url = routeUrl.toHttpUrlOrNull()!!.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)
                    .toUrl(),
                routerOrigin = Onboard,
                message = "failed for response: ${routerResultSuccessEmptyRoutes.value}",
                throwable = IllegalStateException(
                    "no routes returned, collection is empty"
                )
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) { routerCallback.onFailure(capture(failures), routerOptions) }
            val failure: RouterFailure = failures.captured[0]
            assertEquals(expected.message, failure.message)
            assertEquals(expected.code, failure.code)
            assertEquals(expected.routerOrigin, failure.routerOrigin)
            assertEquals(expected.url, failure.url)
            assertEquals(expected.throwable!!.message, failure.throwable!!.message)
        }

    @Test
    fun `check on failure is called on erroneous success response`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, routerCallback)
            getRouteSlot.captured.run(routerResultSuccessErroneousValue, nativeOriginOnboard)

            verify { router.getRoute(routeUrl, any()) }

            val expected = RouterFailure(
                url = routeUrl.toHttpUrlOrNull()!!.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)
                    .toUrl(),
                routerOrigin = Onboard,
                message = "failed for response: ${routerResultSuccessErroneousValue.value}",
                throwable = IllegalStateException(
                    "java.lang.IllegalStateException: Property \"routes\" has not been set"
                )
            )

            val failures = slot<List<RouterFailure>>()
            verify(exactly = 1) { routerCallback.onFailure(capture(failures), routerOptions) }
            val failure: RouterFailure = failures.captured[0]
            assertEquals(expected.message, failure.message)
            assertEquals(expected.code, failure.code)
            assertEquals(expected.routerOrigin, failure.routerOrigin)
            assertEquals(expected.url, failure.url)
            assertEquals(expected.throwable!!.message, failure.throwable!!.message)
        }

    @Test
    fun `check callback called on cancel`() = coroutineRule.runBlockingTest {
        every { router.cancelRouteRequest(any()) } answers {
            getRouteSlot.captured.run(routerResultCancelled, nativeOriginOnboard)
        }

        routerWrapper.getRoute(routerOptions, routerCallback)
        routerWrapper.cancelRouteRequest(REQUEST_ID)

        verify { routerCallback.onCanceled(routerOptions, Onboard) }
    }

    @Test
    fun `check callback called on cancelAll`() = coroutineRule.runBlockingTest {
        every { router.cancelAll() } answers {
            getRouteSlot.captured.run(routerResultCancelled, nativeOriginOnline)
        }

        routerWrapper.getRoute(routerOptions, routerCallback)
        routerWrapper.cancelAll()

        verify { routerCallback.onCanceled(routerOptions, Offboard) }
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
    fun `route refresh fails with null requestUuid`() {
        val route: DirectionsRoute = DirectionsRoute.builder()
            .requestUuid(null)
            .distance(100.0)
            .duration(100.0)
            .routeIndex("1")
            .routeOptions(routerOptions)
            .build()

        routerWrapper.getRouteRefresh(route, 0, routerRefreshCallback)

        val expectedErrorMessage =
            """
               Route refresh failed because of a empty or null param:
               requestUuid = null
            """.trimIndent()

        val errorSlot = slot<RouteRefreshError>()
        verify(exactly = 1) { routerRefreshCallback.onError(capture(errorSlot)) }
        verify(exactly = 0) { router.getRouteRefresh(any(), any()) }
        assertEquals("Route refresh failed", errorSlot.captured.message)
        assertEquals(expectedErrorMessage, errorSlot.captured.throwable?.message)
    }

    @Test
    fun `route refresh set right params`() {
        mockkStatic("com.mapbox.navigation.base.route.NavigationRouteEx") {
            val route: DirectionsRoute = mockk(relaxed = true) {
                every { requestUuid() } returns UUID
                every { routeIndex() } returns "1"
                every { routeOptions() } returns routerOptions
                every { distance() } returns 0.0
                every { duration() } returns 0.0
            }
            every { route.toNavigationRoute() } returns mockk {
                every { directionsResponse.uuid() } returns UUID
                every { routeOptions } returns routerOptions
                every { directionsRoute } returns route
                every { routeIndex } returns 1
            }

            routerWrapper.getRouteRefresh(route, 0, routerRefreshCallback)

            val expectedRefreshOptions = RouteRefreshOptions(
                UUID,
                1,
                0,
                RoutingProfile(routerOptions.profile().mapToRoutingMode(), routerOptions.user())
            )

            verify(exactly = 1) {
                router.getRouteRefresh(
                    expectedRefreshOptions,
                    any()
                )
            }
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
                    Point.fromLngLat(17.032132688234814, 51.10720758039439)
                )
            )
            .build()
        val route: DirectionsRoute = DirectionsResponse.fromJson(
            testRouteFixtures.loadMultiLegRouteForRefresh(),
            options
        ).routes().first()

        routerWrapper.getRouteRefresh(route, 0, routerRefreshCallback)
        refreshRouteSlot.captured.run(routerRefreshSuccess, nativeOriginOnboard)

        val expected = DirectionsResponse.fromJson(
            testRouteFixtures.loadRefreshedMultiLegRoute(),
            options
        ).routes().first()

        verify(exactly = 1) { routerRefreshCallback.onRefresh(expected) }
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
                    Point.fromLngLat(17.032132688234814, 51.10720758039439)
                )
            )
            .build()
        val route: DirectionsRoute = DirectionsResponse.fromJson(
            testRouteFixtures.loadMultiLegRouteForRefresh(),
            options
        ).routes().first()

        routerWrapper.getRouteRefresh(route, 1, routerRefreshCallback)
        refreshRouteSlot.captured.run(routerRefreshSuccessSecondLeg, nativeOriginOnboard)

        val expected = DirectionsResponse.fromJson(
            testRouteFixtures.loadRefreshedMultiLegRouteSecondLeg(),
            options
        ).routes().first()

        verify(exactly = 1) { routerRefreshCallback.onRefresh(expected) }
    }

    @Test
    fun `route refresh failure`() {
        val route: DirectionsRoute = DirectionsRoute.builder()
            .requestUuid(UUID)
            .distance(100.0)
            .duration(100.0)
            .routeIndex("1")
            .routeOptions(routerOptions)
            .build()

        val legIndex = 0
        routerWrapper.getRouteRefresh(route, legIndex, routerRefreshCallback)
        refreshRouteSlot.captured.run(routerResultFailure, nativeOriginOnboard)

        val errorMessage =
            """
               Route refresh failed.
               message = $FAILURE_MESSAGE
               code = $FAILURE_CODE
               type = $FAILURE_TYPE
               requestId = $REQUEST_ID
               legIndex = $legIndex
            """.trimIndent()

        val errorSlot = slot<RouteRefreshError>()

        verify(exactly = 1) { routerRefreshCallback.onError(capture(errorSlot)) }
        assertEquals("Route refresh failed", errorSlot.captured.message)
        assertEquals(errorMessage, errorSlot.captured.throwable?.message)
    }

    private fun provideDefaultRouteOptions(): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .apply {
                coordinates(Point.fromLngLat(.0, .0), null, Point.fromLngLat(.0, .0))
            }.build()
    }

    private companion object {
        private const val CANCELLED_MESSAGE = "Cancelled"
        private const val FAILURE_MESSAGE = "No suitable edges near location"
        private const val FAILURE_CODE = 171
        private val FAILURE_TYPE = RouterErrorType.UNKNOWN
        private val CANCELED_TYPE = RouterErrorType.REQUEST_CANCELLED
        private const val REQUEST_ID = 19L
        private const val UUID = "cjeacbr8s21bk47lggcvce7lv"
    }
}
