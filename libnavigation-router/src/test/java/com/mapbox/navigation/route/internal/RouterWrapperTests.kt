package com.mapbox.navigation.route.internal

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.internal.RouteRefreshRequestData
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterRefreshCallback
import com.mapbox.navigation.base.route.NavigationRouterRefreshError
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin.Offboard
import com.mapbox.navigation.base.route.RouterOrigin.Onboard
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.mapToRoutingMode
import com.mapbox.navigation.route.internal.util.ACCESS_TOKEN_QUERY_PARAM
import com.mapbox.navigation.route.internal.util.TestRouteFixtures
import com.mapbox.navigation.route.internal.util.redactQueryParam
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.toDataRef
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.GetRouteOptions
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
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val routeParserRule = NativeRouteParserRule()

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var routerWrapper: RouterWrapper
    private val mapboxNativeNavigator: MapboxNativeNavigator = mockk(relaxed = true)
    private val router: RouterInterface = mockk(relaxed = true)
    private val accessToken = "pk.123"
    private val route: DirectionsRoute = mockk(relaxed = true)
    private val routerCallback: RouterCallback = mockk(relaxed = true)
    private val routerRefreshCallback: NavigationRouterRefreshCallback = mockk(relaxed = true)
    private val routerOptions: RouteOptions = provideDefaultRouteOptions()
    private val routeUrl = routerOptions.toUrl(accessToken).toString()
    private val evData = mapOf("aaa" to "bbb")

    // these data is used in expected files
    private val routeRefreshRequestData = RouteRefreshRequestData(0, 100, 10, evData)

    private val testRouteFixtures = TestRouteFixtures()

    private val routerResultSuccess: Expected<RouterError, DataRef> = ExpectedFactory.createValue(
        testRouteFixtures.loadTwoLegRoute().toDataRef()
    )
    private val routerResultFailure: Expected<RouterError, String> = ExpectedFactory.createError(
        RouterError(
            FAILURE_MESSAGE,
            FAILURE_CODE,
            FAILURE_TYPE,
            REQUEST_ID,
            null
        )
    )
    private val routerResultFailureDataRef: Expected<RouterError, DataRef> = ExpectedFactory
        .createError(
            RouterError(
                FAILURE_MESSAGE,
                FAILURE_CODE,
                FAILURE_TYPE,
                REQUEST_ID,
                null
            )
        )
    private val routerResultCancelled: Expected<RouterError, DataRef> = ExpectedFactory
        .createError(
            RouterError(
                CANCELLED_MESSAGE,
                FAILURE_CODE,
                CANCELED_TYPE,
                REQUEST_ID,
                null
            )
        )
    private val routerResultSuccessEmptyRoutes: Expected<RouterError, DataRef> = ExpectedFactory
        .createValue(testRouteFixtures.loadEmptyRoutesResponse().toDataRef())
    private val routerResultSuccessErroneousValue: Expected<RouterError, DataRef> =
        ExpectedFactory.createValue(
            "{\"message\":\"should be >= 1\",\"code\":\"InvalidInput\"}".toDataRef()
        )

    private val routerRefreshSuccess: Expected<RouterError, String> = ExpectedFactory.createValue(
        testRouteFixtures.loadRefreshForMultiLegRoute()
    )
    private val routerRefreshSuccessSecondLeg: Expected<RouterError, String> = ExpectedFactory
        .createValue(testRouteFixtures.loadRefreshForMultiLegRouteSecondLeg())
    private val nativeOriginOnline: RouterOrigin = RouterOrigin.ONLINE
    private val nativeOriginOnboard: RouterOrigin = RouterOrigin.ONBOARD
    private val getRouteSlot = slot<com.mapbox.navigator.RouterDataRefCallback>()
    private val refreshRouteSlot = slot<RouterRefreshCallback>()
    private val routeSlot = slot<NavigationRoute>()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
        every { ThreadController.DefaultDispatcher } returns coroutineRule.testDispatcher

        every { mapboxNativeNavigator.router } returns router
        every { router.getRoute(any(), any(), capture(getRouteSlot)) } returns 0L
        every { router.getRouteRefresh(any(), capture(refreshRouteSlot)) } returns 0L

        every { route.requestUuid() } returns UUID
        every { route.routeIndex() } returns "index"
        every { route.routeOptions() } returns routerOptions

        routerWrapper = RouterWrapper(
            accessToken,
            mapboxNativeNavigator.router,
            ThreadController(),
        )
    }

    @After
    fun cleanUp() {
        unmockkObject(ThreadController)
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(routerWrapper)
    }

    @Test
    fun `get route is called with expected url and options`() {
        routerWrapper.getRoute(routerOptions, routerCallback)
        val requestOptions = GetRouteOptions(null)

        verify {
            router.getRoute(
                routeUrl,
                requestOptions,
                any<com.mapbox.navigator.RouterDataRefCallback>()
            )
        }
    }

    @Test
    fun `check callback called on failure with redacted token`() {
        routerWrapper.getRoute(routerOptions, routerCallback)
        getRouteSlot.captured.run(routerResultFailureDataRef, nativeOriginOnline)

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

        verify {
            router.getRoute(
                routeUrl,
                any(),
                any<com.mapbox.navigator.RouterDataRefCallback>()
            )
        }
        verify { routerCallback.onFailure(expected, routerOptions) }
    }

    @Test
    fun `check callback called on success and contains original options`() =
        coroutineRule.runBlockingTest {
            routerWrapper.getRoute(routerOptions, routerCallback)
            getRouteSlot.captured.run(routerResultSuccess, nativeOriginOnboard)

            verify {
                router.getRoute(
                    routeUrl,
                    any(),
                    any<com.mapbox.navigator.RouterDataRefCallback>()
                )
            }

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

            verify {
                router.getRoute(
                    routeUrl,
                    any(),
                    any<com.mapbox.navigator.RouterDataRefCallback>()
                )
            }

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

            verify {
                router.getRoute(
                    routeUrl,
                    any(),
                    any<com.mapbox.navigator.RouterDataRefCallback>()
                )
            }

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
        val route = createNavigationRoute(
            DirectionsRoute.builder()
                .requestUuid(null)
                .distance(100.0)
                .duration(100.0)
                .routeIndex("0")
                .routeOptions(routerOptions)
                .build()
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
        mockkStatic("com.mapbox.navigation.base.route.NavigationRouteEx") {
            val routeOptions = provideDefaultRouteOptions()
            val route = NavigationRoute.create(
                DirectionsResponse.builder()
                    .code("200")
                    .uuid(UUID)
                    .routes(listOf(createDirectionsRoute(routeIndex = "0")))
                    .build(),
                routeOptions,
                Offboard
            ).first()

            val legIndex = 12
            val routeGeometryIndex = 23
            val legGeometryIndex = 19
            val requestData = RouteRefreshRequestData(
                legIndex,
                routeGeometryIndex,
                legGeometryIndex,
                evData
            )
            routerWrapper.getRouteRefresh(route, requestData, routerRefreshCallback)

            val expectedRefreshOptions = RouteRefreshOptions(
                UUID,
                0,
                legIndex,
                RoutingProfile(
                    routeOptions.profile().mapToRoutingMode(),
                    routeOptions.user()
                ),
                routerOptions.baseUrl(),
                accessToken,
                routeGeometryIndex,
                HashMap(evData)
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
        val route = NavigationRoute.create(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options
            ),
            options,
            com.mapbox.navigation.base.route.RouterOrigin.Custom()
        ).first()

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        refreshRouteSlot.captured.run(routerRefreshSuccess, nativeOriginOnboard, hashMapOf())

        val expected = NavigationRoute.create(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadRefreshedMultiLegRoute(),
                options
            ),
            options,
            com.mapbox.navigation.base.route.RouterOrigin.Custom()
        ).first()

        verify(exactly = 1) { routerRefreshCallback.onRefreshReady(capture(routeSlot)) }
        checkRefreshedNavigationRouteWithWithWaypoints(expected, routeSlot.captured)
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
        val route = NavigationRoute.create(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadMultiLegRouteForRefresh(),
                options
            ),
            options,
            com.mapbox.navigation.base.route.RouterOrigin.Custom()
        ).first()

        routerWrapper.getRouteRefresh(
            route,
            RouteRefreshRequestData(
                1,
                routeRefreshRequestData.routeGeometryIndex,
                routeRefreshRequestData.legGeometryIndex,
                evData
            ),
            routerRefreshCallback
        )
        refreshRouteSlot.captured.run(
            routerRefreshSuccessSecondLeg,
            nativeOriginOnboard,
            hashMapOf()
        )

        val expected = NavigationRoute.create(
            DirectionsResponse.fromJson(
                testRouteFixtures.loadRefreshedMultiLegRouteSecondLeg(),
                options
            ),
            options,
            com.mapbox.navigation.base.route.RouterOrigin.Custom()
        ).first()

        verify(exactly = 1) { routerRefreshCallback.onRefreshReady(capture(routeSlot)) }
        checkRefreshedNavigationRouteWithWithWaypoints(expected, routeSlot.captured)
    }

    private fun checkRefreshedNavigationRouteWithWithWaypoints(
        expected: NavigationRoute,
        actual: NavigationRoute
    ) {
        assertEquals(expected, actual) // directions route equality
        assertEquals(expected.directionsResponse.waypoints(), actual.directionsResponse.waypoints())
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
                .build()
        )

        routerWrapper.getRouteRefresh(route, routeRefreshRequestData, routerRefreshCallback)
        refreshRouteSlot.captured.run(routerResultFailure, nativeOriginOnboard, hashMapOf())

        val errorMessage =
            """
               Route refresh failed.
               message = $FAILURE_MESSAGE
               code = $FAILURE_CODE
               type = $FAILURE_TYPE
               requestId = $REQUEST_ID
               routeRefreshRequestData = $routeRefreshRequestData
            """.trimIndent()

        val errorSlot = slot<NavigationRouterRefreshError>()

        verify(exactly = 1) { routerRefreshCallback.onFailure(capture(errorSlot)) }
        assertEquals("Route refresh failed", errorSlot.captured.message)
        assertEquals(errorMessage, errorSlot.captured.throwable?.message)
    }

    private fun provideDefaultRouteOptions(): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .baseUrl("https://my.url.com")
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
