package com.mapbox.navigation.route.internal.onboard

import android.content.Context
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.route.internal.util.httpUrl
import com.mapbox.navigation.route.offboard.RouteBuilderProvider
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.NavigationException
import com.mapbox.navigation.utils.internal.RequestMap
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.RouterError
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapboxOnboardRouterTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val routerCallback: Router.Callback = mockk(relaxUnitFun = true)
    private val routerResultSuccess: Expected<RouterError, String> = mockk {
        every { isValue } returns true
        every { isError } returns false
        every { value } returns SUCCESS_RESPONSE
        every { error } returns null
    }
    private val routerResultFailure: Expected<RouterError, String> = mockk {
        every { isValue } returns false
        every { isError } returns true
        every { value } returns null
        every { error } returns RouterError(FAILURE_MESSAGE, FAILURE_CODE)
    }
    private val routerOptions: RouteOptions = provideDefaultRouteOptions()
    private val logger: Logger = mockk(relaxUnitFun = true)
    private val context = mockk<Context>()
    private val mapboxDirections = mockk<MapboxDirections>(relaxed = true)
    private val mapboxDirectionsBuilder = mockk<MapboxDirections.Builder>(relaxed = true)

    private var onboardRouter: MapboxOnboardRouter = MapboxOnboardRouter(navigator, context, logger)

    private val url =
        MapboxDirections.builder()
            .routeOptions(routerOptions)
            .build()
            .httpUrl()
            .toUrl()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher

        mockkObject(RouteBuilderProvider)
        every {
            RouteBuilderProvider.getBuilder(null)
        } returns mapboxDirectionsBuilder
        every { mapboxDirectionsBuilder.interceptor(any()) } returns mapboxDirectionsBuilder
        every { mapboxDirectionsBuilder.routeOptions(any()) } returns mapboxDirectionsBuilder
        every { mapboxDirectionsBuilder.build() } returns mapboxDirections
        every { mapboxDirections.httpUrl() } returns url.toHttpUrlOrNull()!!
    }

    @After
    fun cleanUp() {
        unmockkObject(ThreadController)
        unmockkObject(RouteBuilderProvider)
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(onboardRouter)
    }

    @Test
    fun checkCallbackCalledOnFailure() = coroutineRule.runBlockingTest {
        val exceptionSlot = slot<NavigationException>()
        coEvery { navigator.getRoute(any()) } returns routerResultFailure

        onboardRouter.getRoute(routerOptions, routerCallback)

        coVerify { navigator.getRoute(url.toString()) }
        verify { routerCallback.onFailure(capture(exceptionSlot)) }
        assertEquals(ERROR_MESSAGE, exceptionSlot.captured.message)
    }

    @Test
    fun checkCallbackCalledOnSuccess() = coroutineRule.runBlockingTest {
        coEvery { navigator.getRoute(any()) } returns routerResultSuccess

        onboardRouter.getRoute(routerOptions, routerCallback)

        coVerify { navigator.getRoute(url.toString()) }
        verify { routerCallback.onResponse(DirectionsResponse.fromJson(SUCCESS_RESPONSE).routes()) }
    }

    @Test
    fun checkCallbackCalledOnCancel() {
        coEvery { navigator.getRoute(any()) } coAnswers {
            awaitCancellation()
        }

        val latch = CountDownLatch(1)
        val callback: Router.Callback = object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                fail()
            }

            override fun onFailure(throwable: Throwable) {
                fail()
            }

            override fun onCanceled() {
                latch.countDown()
            }
        }
        onboardRouter.getRoute(routerOptions, callback)
        onboardRouter.cancelAll()

        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("onCanceled not called")
        }
    }

    @Test
    fun checkCallbackCalledOnCancel2() = coroutineRule.runBlockingTest {
        coEvery { navigator.getRoute(any()) } coAnswers { throw CancellationException() }

        onboardRouter.getRoute(routerOptions, routerCallback)

        verify { routerCallback.onCanceled() }
    }

    @Test
    fun checkCallbackCalledOnCancel3() = runBlocking {
        // cancellable code should run on a separate dispatcher to allow call `cancel` after launch.
        // if we use the same dispatcher, it will be blocked until coroutine finish. `cancel` will have no affect
        // job's state will be `isActive == false`, `isCancelled == false`.
        every { ThreadController.IODispatcher } returns Dispatchers.Default

        coEvery { navigator.getRoute(any()) } coAnswers {
            // delay on a separate dispatcher, it doesn't affect the main thread.
            // we just suspend the function until it's cancelled from the main thread.
            // if we don't suspend it, the function will return immediately, before `cancel` is called.
            // job's state will be `isActive == false`, `isCancelled == false`.
            delay(1000)
            routerResultSuccess
        }

        val scope = coroutineRule.coroutineScope
        val job = scope.launch { onboardRouter.getRoute("") }
        scope.cancel()

        assertFalse(job.isActive)
        assertTrue(job.isCancelled)
    }

    @Test
    fun `cancel a request when multiple are running`() {
        coEvery { navigator.getRoute(any()) } coAnswers {
            awaitCancellation()
        }

        val firstLatch = CountDownLatch(1)
        val firstCallback: Router.Callback = object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                fail()
            }

            override fun onFailure(throwable: Throwable) {
                fail()
            }

            override fun onCanceled() {
                firstLatch.countDown()
            }
        }

        val secondLatch = CountDownLatch(1)
        val secondCallback: Router.Callback = object : Router.Callback {
            override fun onResponse(routes: List<DirectionsRoute>) {
                fail()
            }

            override fun onFailure(throwable: Throwable) {
                fail()
            }

            override fun onCanceled() {
                secondLatch.countDown()
            }
        }

        val firstId = onboardRouter.getRoute(routerOptions, firstCallback)
        onboardRouter.cancelRouteRequest(firstId)

        if (!firstLatch.await(5, TimeUnit.SECONDS)) {
            fail("onCanceled not called on first request")
        }

        assertTrue(secondLatch.count > 0)

        val secondId = onboardRouter.getRoute(routerOptions, secondCallback)
        onboardRouter.cancelRouteRequest(secondId)

        if (!secondLatch.await(5, TimeUnit.SECONDS)) {
            fail("onCanceled not called on second request")
        }
    }

    @Test
    fun `request list cleared on success`() = coroutineRule.runBlockingTest {
        mockkConstructor(RequestMap::class)
        val idSlot = slot<Long>()
        every { anyConstructed<RequestMap<Job>>().put(capture(idSlot), any()) } just Runs
        onboardRouter = MapboxOnboardRouter(navigator, context, logger)
        coEvery { navigator.getRoute(any()) } returns routerResultSuccess

        onboardRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 1) { anyConstructed<RequestMap<Job>>().remove(idSlot.captured) }
        unmockkConstructor(RequestMap::class)
    }

    @Test
    fun `request list cleared on failure`() = coroutineRule.runBlockingTest {
        mockkConstructor(RequestMap::class)
        val idSlot = slot<Long>()
        every { anyConstructed<RequestMap<Job>>().put(capture(idSlot), any()) } just Runs
        onboardRouter = MapboxOnboardRouter(navigator, context, logger)
        coEvery { navigator.getRoute(any()) } returns routerResultFailure

        onboardRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 1) { anyConstructed<RequestMap<Job>>().remove(idSlot.captured) }
        unmockkConstructor(RequestMap::class)
    }

    @Test
    fun `request list cleared on cancel`() = coroutineRule.runBlockingTest {
        mockkConstructor(RequestMap::class)
        val idSlot = slot<Long>()
        every { anyConstructed<RequestMap<Job>>().put(capture(idSlot), any()) } just Runs
        onboardRouter = MapboxOnboardRouter(navigator, context, logger)
        coEvery { navigator.getRoute(any()) } coAnswers { throw CancellationException() }

        onboardRouter.getRoute(routerOptions, routerCallback)

        verify(exactly = 1) { anyConstructed<RequestMap<Job>>().remove(idSlot.captured) }
        unmockkConstructor(RequestMap::class)
    }

    @Test
    fun checkModelOnSuccess() = coroutineRule.runBlockingTest {
        val routesSlot = slot<List<DirectionsRoute>>()
        coEvery { navigator.getRoute(any()) } returns routerResultSuccess

        onboardRouter.getRoute(routerOptions, routerCallback)

        verify { routerCallback.onResponse(capture(routesSlot)) }

        val delta = 0.000001
        // route
        assertEquals(ROUTE_SIZE, routesSlot.captured.size)
        val route = routesSlot.captured[0]
        assertEquals(ROUTE_DISTANCE, route.distance(), delta)
        assertEquals(ROUTE_DURATION, route.duration(), delta)
        assertEquals(ROUTE_WEIGHT_NAME, route.weightName())
        assertEquals(ROUTE_VOICE_LANGUAGE, route.voiceLanguage())
        assertEquals(ROUTE_WEIGHT, route.weight())

        // leg
        assertEquals(LEG_SIZE, route.legs()!!.size)
        val leg = route.legs()!![0]
        assertEquals(LEG_SUMMARY, leg.summary())
        assertEquals(LEG_DURATION, leg.duration())
        assertEquals(LEG_DISTANCE, leg.distance())

        // step
        assertEquals(STEP_SIZE, leg.steps()!!.size)
        val step = leg.steps()!![0]
        assertEquals(STEP_DRIVING_SIDE, step.drivingSide())
        assertEquals(STEP_GEOMETRY, step.geometry())
        assertEquals(STEP_MODE, step.mode())
        assertEquals(STEP_WEIGHT, step.weight(), delta)
        assertEquals(STEP_DURATION, step.duration(), delta)
        assertEquals(STEP_NAME, step.name())
        assertEquals(STEP_DISTANCE, step.distance(), delta)

        // intersection
        assertEquals(INTERSECTION_SIZE, step.intersections()!!.size)
        val intersection = step.intersections()!![0]
        assertEquals(INTERSECTION_OUT, intersection.out())
        assertEquals(INTERSECTION_ENTRY, intersection.entry()!![0])
        assertEquals(INTERSECTION_BEARINGS, intersection.bearings()!![0])
        assertEquals(INTERSECTION_LONGITUDE, intersection.location().longitude(), delta)
        assertEquals(INTERSECTION_LATITUDE, intersection.location().latitude(), delta)

        // maneuver
        val maneuver = step.maneuver()
        assertEquals(MANEUVER_BEARING_AFTER, maneuver.bearingAfter())
        assertEquals(MANEUVER_BEARING_BEFORE, maneuver.bearingBefore())
        assertEquals(MANEUVER_MODIFIER, maneuver.modifier())
        assertEquals(MANEUVER_TYPE, maneuver.type())
        assertEquals(MANEUVER_INSTRUCTION, maneuver.instruction())
        assertEquals(MANEUVER_LONGITUDE, maneuver.location().longitude(), delta)
        assertEquals(MANEUVER_LATITUDE, maneuver.location().latitude(), delta)

        // voiceInstructions
        assertEquals(VOICE_INSTRUCTIONS_SIZE, step.voiceInstructions()!!.size)
        val voiceInstruction = step.voiceInstructions()!![0]
        assertEquals(VOICE_INSTRUCTIONS_DISTANCE, voiceInstruction.distanceAlongGeometry())
        assertEquals(VOICE_INSTRUCTIONS_ANNOUNCEMENT, voiceInstruction.announcement())
        assertEquals(VOICE_INSTRUCTIONS_SSML_ANNOUNCEMENT, voiceInstruction.ssmlAnnouncement())

        // bannerInstructions
        assertEquals(BANNER_INSTRUCTIONS_SIZE, step.bannerInstructions()!!.size)
        val bannerInstruction = step.bannerInstructions()!![0]
        assertEquals(BANNER_INSTRUCTIONS_DISTANCE, bannerInstruction.distanceAlongGeometry(), delta)
        assertEquals(BANNER_INSTRUCTIONS_SECONDARY, bannerInstruction.secondary())
        assertEquals(BANNER_INSTRUCTIONS_TYPE, bannerInstruction.primary().type())
        assertEquals(BANNER_INSTRUCTIONS_MODIFIER, bannerInstruction.primary().modifier())
        assertEquals(BANNER_INSTRUCTIONS_TEXT, bannerInstruction.primary().text())

        // components
        assertEquals(COMPONENT_SIZE, bannerInstruction.primary().components()!!.size)
        val component = bannerInstruction.primary().components()!![0]
        assertEquals(COMPONENT_ABBREVIATION, component.abbreviation())
        assertEquals(COMPONENT_ABBREVIATION_PRIORITY, component.abbreviationPriority())
        assertEquals(COMPONENT_TEXT, component.text())
        assertEquals(COMPONENT_TYPE, component.type())
    }

    @Test
    fun `route refresh is disabled`() {
        val callback = mockk<RouteRefreshCallback>(relaxUnitFun = true)
        onboardRouter.getRouteRefresh(mockk(), 0, callback)

        verify {
            callback.onError(
                RouteRefreshError(message = "Route refresh is not available when offline.")
            )
        }
    }

    private fun provideDefaultRouteOptions(): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .apply {
                accessToken(ACCESS_TOKEN)
                coordinates(origin, waypoints, destination)
            }.build()
    }

    companion object {
        private const val ACCESS_TOKEN = "pk.1234"

        private val origin = Point.fromLngLat(.0, .0)
        private val waypoints = listOf(Point.fromLngLat(.42, .11))
        private val destination = Point.fromLngLat(1.83, 1232.01)

        private const val TILE_PATH = "tiles"
        private const val TAG = "MbxOnboardRouter"

        private const val ROUTE_SIZE = 1
        private const val ROUTE_DISTANCE = 50369.7
        private const val ROUTE_DURATION = 2248.5
        private const val ROUTE_WEIGHT_NAME = "routability"
        private const val ROUTE_VOICE_LANGUAGE = "en-US"
        private const val ROUTE_WEIGHT = 2383.1

        private const val LEG_SIZE = 1
        private const val LEG_SUMMARY = "PA 283 West, Fishburn Road"
        private const val LEG_DURATION = 2248.5
        private const val LEG_DISTANCE = 50369.7

        private const val STEP_SIZE = 1
        private const val STEP_DRIVING_SIDE = "right"
        private const val STEP_GEOMETRY = "s`_kkA`y|opCcAsReEcy@Eq@]oDa@gEEu@uKgwB"
        private const val STEP_MODE = "driving"
        private const val STEP_WEIGHT = 63.8
        private const val STEP_DURATION = 50.8
        private const val STEP_NAME = "East Fulton Street"
        private const val STEP_DISTANCE = 293.2

        private const val INTERSECTION_SIZE = 3
        private const val INTERSECTION_OUT = 0
        private const val INTERSECTION_ENTRY = true
        private const val INTERSECTION_BEARINGS = 82
        private const val INTERSECTION_LONGITUDE = -76.299169
        private const val INTERSECTION_LATITUDE = 40.042522

        private const val MANEUVER_BEARING_AFTER = 82.0
        private const val MANEUVER_BEARING_BEFORE = 0.0
        private const val MANEUVER_MODIFIER = "left"
        private const val MANEUVER_TYPE = "depart"
        private const val MANEUVER_INSTRUCTION = "Head east on East Fulton Street"
        private const val MANEUVER_LONGITUDE = -76.299169
        private const val MANEUVER_LATITUDE = 40.042522

        private const val VOICE_INSTRUCTIONS_SIZE = 2
        private const val VOICE_INSTRUCTIONS_DISTANCE = 293.2
        private const val VOICE_INSTRUCTIONS_ANNOUNCEMENT =
            "Head east on East Fulton Street, then turn right onto North Ann Street"
        private const val VOICE_INSTRUCTIONS_SSML_ANNOUNCEMENT =
            "<speak><amazon:effect name=\"drc\"><prosody rate=\"1.08\">Head east on East Fulton" +
                " Street, then turn right onto North Ann Street</prosody></amazon:effect></speak>"

        private const val BANNER_INSTRUCTIONS_SIZE = 1
        private const val BANNER_INSTRUCTIONS_DISTANCE = 293.2
        private val BANNER_INSTRUCTIONS_SECONDARY = null
        private const val BANNER_INSTRUCTIONS_TYPE = "turn"
        private const val BANNER_INSTRUCTIONS_MODIFIER = "right"
        private const val BANNER_INSTRUCTIONS_TEXT = "North Ann Street"

        private const val COMPONENT_SIZE = 2
        private const val COMPONENT_ABBREVIATION = "N"
        private const val COMPONENT_ABBREVIATION_PRIORITY = 1
        private const val COMPONENT_TEXT = "North"
        private const val COMPONENT_TYPE = "text"

        private const val ERROR_MESSAGE =
            "Error occurred fetching offline route: No suitable edges near location - Code: 171"
        private const val FAILURE_MESSAGE = "No suitable edges near location"
        private const val FAILURE_CODE = 171
        private const val SUCCESS_RESPONSE = "{\n" +
            "  \"routes\": [\n" +
            "    {\n" +
            "      \"geometry\": \"\",\n" +
            "      \"legs\": [\n" +
            "        {\n" +
            "          \"summary\": \"PA 283 West, Fishburn Road\",\n" +
            "          \"weight\": 2383.1,\n" +
            "          \"duration\": 2248.5,\n" +
            "          \"steps\": [\n" +
            "            {\n" +
            "              \"intersections\": [\n" +
            "                {\n" +
            "                  \"out\": 0,\n" +
            "                  \"entry\": [\n" +
            "                    true\n" +
            "                  ],\n" +
            "                  \"bearings\": [\n" +
            "                    82\n" +
            "                  ],\n" +
            "                  \"location\": [\n" +
            "                    -76.299169,\n" +
            "                    40.042522\n" +
            "                  ]\n" +
            "                },\n" +
            "                {\n" +
            "                  \"out\": 0,\n" +
            "                  \"in\": 1,\n" +
            "                  \"entry\": [\n" +
            "                    true,\n" +
            "                    false,\n" +
            "                    true\n" +
            "                  ],\n" +
            "                  \"bearings\": [\n" +
            "                    75,\n" +
            "                    255,\n" +
            "                    345\n" +
            "                  ],\n" +
            "                  \"location\": [\n" +
            "                    -76.298855,\n" +
            "                    40.042556\n" +
            "                  ]\n" +
            "                },\n" +
            "                {\n" +
            "                  \"out\": 0,\n" +
            "                  \"in\": 2,\n" +
            "                  \"entry\": [\n" +
            "                    true,\n" +
            "                    true,\n" +
            "                    false,\n" +
            "                    true\n" +
            "                  ],\n" +
            "                  \"bearings\": [\n" +
            "                    75,\n" +
            "                    165,\n" +
            "                    255,\n" +
            "                    345\n" +
            "                  ],\n" +
            "                  \"location\": [\n" +
            "                    -76.297812,\n" +
            "                    40.042673\n" +
            "                  ]\n" +
            "                }\n" +
            "              ],\n" +
            "              \"driving_side\": \"right\",\n" +
            "              \"geometry\": \"s`_kkA`y|opCcAsReEcy@Eq@]oDa@gEEu@uKgwB\",\n" +
            "              \"mode\": \"driving\",\n" +
            "              \"maneuver\": {\n" +
            "                \"bearing_after\": 82,\n" +
            "                \"bearing_before\": 0,\n" +
            "                \"location\": [\n" +
            "                  -76.299169,\n" +
            "                  40.042522\n" +
            "                ],\n" +
            "                \"modifier\": \"left\",\n" +
            "                \"type\": \"depart\",\n" +
            "                \"instruction\": \"Head east on East Fulton Street\"\n" +
            "              },\n" +
            "              \"weight\": 63.8,\n" +
            "              \"duration\": 50.8,\n" +
            "              \"name\": \"East Fulton Street\",\n" +
            "              \"distance\": 293.2,\n" +
            "              \"voiceInstructions\": [\n" +
            "                {\n" +
            "                  \"distanceAlongGeometry\": 293.2,\n" +
            "                  \"announcement\": \"Head east on East Fulton Street, then turn " +
            "right onto North Ann Street\",\n" +
            "                  \"ssmlAnnouncement\": \"<speak><amazon:effect name=\\\"drc\\\">" +
            "<prosody rate=\\\"1.08\\\">Head east on East Fulton Street, then turn right onto " +
            "North Ann Street</prosody></amazon:effect></speak>\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"distanceAlongGeometry\": 86.6,\n" +
            "                  \"announcement\": \"Turn right onto North Ann Street, then turn " +
            "left onto East Chestnut Street (PA 23 East)\",\n" +
            "                  \"ssmlAnnouncement\": \"<speak><amazon:effect name=\\\"drc\\\">" +
            "<prosody rate=\\\"1.08\\\">Turn right onto North Ann Street, then turn left onto " +
            "East Chestnut Street (PA <say-as interpret-as=\\\"address\\\">23</say-as> " +
            "East)</prosody></amazon:effect></speak>\"\n" +
            "                }\n" +
            "              ],\n" +
            "              \"bannerInstructions\": [\n" +
            "                {\n" +
            "                  \"distanceAlongGeometry\": 293.2,\n" +
            "                  \"primary\": {\n" +
            "                    \"type\": \"turn\",\n" +
            "                    \"modifier\": \"right\",\n" +
            "                    \"components\": [\n" +
            "                      {\n" +
            "                        \"text\": \"North\",\n" +
            "                        \"type\": \"text\",\n" +
            "                        \"abbr\": \"N\",\n" +
            "                        \"abbr_priority\": 1\n" +
            "                      },\n" +
            "                      {\n" +
            "                        \"text\": \"Ann Street\",\n" +
            "                        \"type\": \"text\",\n" +
            "                        \"abbr\": \"Ann St\",\n" +
            "                        \"abbr_priority\": 0\n" +
            "                      }\n" +
            "                    ],\n" +
            "                    \"text\": \"North Ann Street\"\n" +
            "                  },\n" +
            "                  \"secondary\": null\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          ],\n" +
            "          \"distance\": 50369.7\n" +
            "        }\n" +
            "      ],\n" +
            "      \"weight_name\": \"routability\",\n" +
            "      \"weight\": 2383.1,\n" +
            "      \"duration\": 2248.5,\n" +
            "      \"distance\": 50369.7,\n" +
            "      \"voiceLocale\": \"en-US\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"waypoints\": [\n" +
            "    {\n" +
            "      \"name\": \"East Fulton Street\",\n" +
            "      \"location\": [\n" +
            "        -76.299169,\n" +
            "        40.042522\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"West Chocolate Avenue\",\n" +
            "      \"location\": [\n" +
            "        -76.654634,\n" +
            "        40.283943\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"code\": \"Ok\",\n" +
            "  \"uuid\": \"cjeacbr8s21bk47lggcvce7lv\"\n" +
            "}"
    }
}
