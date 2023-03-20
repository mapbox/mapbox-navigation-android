package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.startTripSessionAndWaitForActiveGuidanceState
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.startTripSessionAndWaitForFreeDriveState
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.stopTripSessionAndWaitForIdleState
import com.mapbox.navigation.instrumentation_tests.utils.http.HttpServiceEvent
import com.mapbox.navigation.instrumentation_tests.utils.http.HttpServiceEventsObserver
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.parameters
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider.toNavigationRoutes
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

class TripSessionsBillingTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation

    private val route = RoutesProvider.dc_very_short(context).toNavigationRoutes()[0]
    private val anotherRoute = RoutesProvider.dc_short_with_alternative(context).toNavigationRoutes()[0]

    private lateinit var httpEventsObserver: HttpServiceEventsObserver

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        route.routeOptions.coordinatesList().first().run {
            latitude = latitude()
            longitude = longitude()
        }
        bearing = 190f
    }

    private fun stayOnInitialPosition() {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                route.routeOptions.coordinatesList().first().run {
                    this@generateLocationUpdate.latitude = latitude()
                    this@generateLocationUpdate.longitude = longitude()
                }

                bearing = 190f
            },
            times = 120
        )
    }

    @Before
    fun setup() {
        httpEventsObserver = HttpServiceEventsObserver.register()
        assertTrue(httpEventsObserver.billingRequests().isEmpty())

        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .accessToken(getMapboxAccessTokenFromResources(context))
                    .routingTilesOptions(
                        RoutingTilesOptions.Builder()
                            .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                            .build()
                    )
                    .build()
            )
        }
    }

    @After
    fun cleanUp() {
        HttpServiceEventsObserver.unregister()
    }

    /**
     * Waits for exactly [count] elements that match [predicate] to appear
     * in [HttpServiceEventsObserver] to avoid tests flakiness,
     * than waits for 1 second for more http events to arrive.
     * This is made in order to catch bugs
     * where more than expected billing events sent to the server.
     *
     * @return All billing [HttpServiceEvent.Request]'s appeared within waiting time.
     * Number of returned items can be more that [count].
     * Returned elements might not match the [predicate].
     */
    private suspend fun getAllBillingEventsWhenHasEvents(
        count: Int,
        eventsName: String,
        predicate: suspend (HttpServiceEvent) -> Boolean = { true }
    ): List<HttpServiceEvent.Request> {
        try {
            httpEventsObserver
                .billingRequestsFlow()
                .filter(predicate)
                .take(count)
                .toList()
        } catch (e: CancellationException) {
            fail("Timed out waiting for $count $eventsName event(s)")
        }

        delay(1000)
        return httpEventsObserver.billingRequests()
    }

    @Test
    fun testBillingSessionSentForOneSecondLongActiveGuidanceTrip() = sdkTest {
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(route))
        mapboxNavigation.startTripSessionAndWaitForActiveGuidanceState()

        delay(TimeUnit.SECONDS.toMillis(SESSION_DURATION_SECONDS))

        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        val billingRequests = getAllBillingEventsWhenHasEvents(1, "Active Guidance") {
            it.isActiveGuidanceSession
        }

        assertSessionEvents(BillingEventType.ACTIVE_GUIDANCE, billingRequests)
        assertSessionDuration(SESSION_DURATION_SECONDS, billingRequests.first().duration)
    }

    @Test
    fun testBillingSessionsWhenNewRouteSet() = sdkTest {
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(route))

        stayOnInitialPosition()

        mapboxNavigation.startTripSessionAndWaitForActiveGuidanceState()

        mapboxNavigation.routeProgressUpdates().first()

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(anotherRoute))

        delay(TimeUnit.SECONDS.toMillis(2))

        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        val billingRequests = getAllBillingEventsWhenHasEvents(1, "Active Guidance") {
            it.isActiveGuidanceSession
        }

        assertSessionEvents(BillingEventType.ACTIVE_GUIDANCE, billingRequests)
        assertSessionDuration(3, billingRequests[0].duration)
    }

    @Test
    fun testBillingSessionsWhenNewRouteSetImmediately() = sdkTest(timeout = Long.MAX_VALUE) {
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(route))
        mapboxNavigation.startTripSessionAndWaitForActiveGuidanceState()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(anotherRoute))

        delay(TimeUnit.SECONDS.toMillis(SESSION_DURATION_SECONDS))

        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        val billingRequests = getAllBillingEventsWhenHasEvents(2, "Active Guidance") {
            it.isActiveGuidanceSession
        }

        assertSessionEvents(
            listOf(
                BillingEventType.ACTIVE_GUIDANCE,
                BillingEventType.ACTIVE_GUIDANCE
            ),
            billingRequests
        )
        assertSessionDuration(0, billingRequests[0].duration)
        assertSessionDuration(SESSION_DURATION_SECONDS, billingRequests[1].duration)
    }

    @Test
    fun testBillingSessionSentForEmptyActiveGuidanceTrip() = sdkTest {
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(route))
        mapboxNavigation.startTripSessionAndWaitForActiveGuidanceState()
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        val billingRequests = getAllBillingEventsWhenHasEvents(1, "Active Guidance") {
            it.isActiveGuidanceSession
        }

        assertSessionEvents(BillingEventType.ACTIVE_GUIDANCE, billingRequests)
        assertSessionDuration(0, billingRequests.first().duration)
    }

    @Test
    fun testOneBillingSessionSentForOneSecondLongPausedAndResumedActiveGuidanceTrip() = sdkTest {
        val sessionLengthMillis = TimeUnit.SECONDS.toMillis(SESSION_DURATION_SECONDS)

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(route))
        mapboxNavigation.startTripSessionAndWaitForActiveGuidanceState()
        delay(sessionLengthMillis / 2)
        mapboxNavigation.stopTripSessionAndWaitForIdleState()

        delay(TimeUnit.SECONDS.toMillis(SESSION_PAUSE_DURATION_SECONDS))

        mapboxNavigation.startTripSessionAndWaitForActiveGuidanceState()
        delay(sessionLengthMillis / 2)
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        val billingRequests = getAllBillingEventsWhenHasEvents(1, "Active Guidance") {
            it.isActiveGuidanceSession
        }

        assertSessionEvents(BillingEventType.ACTIVE_GUIDANCE, billingRequests)
        assertSessionDuration(SESSION_DURATION_SECONDS, billingRequests.first().duration)
    }

    @Test
    fun testOneBillingSessionSentForEmptyPausedAndResumedActiveGuidanceTrip() = sdkTest {
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(route))
        mapboxNavigation.startTripSessionAndWaitForActiveGuidanceState()
        mapboxNavigation.stopTripSessionAndWaitForIdleState()

        delay(TimeUnit.SECONDS.toMillis(SESSION_PAUSE_DURATION_SECONDS))

        mapboxNavigation.startTripSessionAndWaitForActiveGuidanceState()
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        val billingRequests = getAllBillingEventsWhenHasEvents(1, "Active Guidance") {
            it.isActiveGuidanceSession
        }

        assertSessionEvents(BillingEventType.ACTIVE_GUIDANCE, billingRequests)
        assertSessionDuration(0, billingRequests.first().duration)
    }

    @Test
    fun testBillingSessionSentForOneSecondLongFreeDrive() = sdkTest {
        mapboxNavigation.startTripSessionAndWaitForFreeDriveState()
        delay(TimeUnit.SECONDS.toMillis(SESSION_DURATION_SECONDS))
        mapboxNavigation.stopTripSessionAndWaitForIdleState()
        mapboxNavigation.onDestroy()

        val billingRequests = getAllBillingEventsWhenHasEvents(1, "Free Drive") {
            it.isFreeDriveSession
        }

        assertSessionEvents(BillingEventType.FREE_DRIVE, billingRequests)
        assertSessionDuration(SESSION_DURATION_SECONDS, billingRequests.first().duration)
    }

    @Test
    fun testBillingSessionSentForEmptyFreeDrive() = sdkTest {
        mapboxNavigation.startTripSessionAndWaitForFreeDriveState()
        mapboxNavigation.stopTripSessionAndWaitForIdleState()
        mapboxNavigation.onDestroy()

        val billingRequests = getAllBillingEventsWhenHasEvents(1, "Free Drive") {
            it.isFreeDriveSession
        }

        assertSessionEvents(BillingEventType.FREE_DRIVE, billingRequests)
        assertSessionDuration(0, billingRequests.first().duration)
    }

    @Test
    fun testOneBillingSessionSentForOneSecondLongPausedAndResumedFreeDrive() = sdkTest {
        val sessionLengthMillis = TimeUnit.SECONDS.toMillis(SESSION_DURATION_SECONDS)

        mapboxNavigation.startTripSessionAndWaitForFreeDriveState()
        delay(sessionLengthMillis / 2)
        mapboxNavigation.stopTripSessionAndWaitForIdleState()

        delay(TimeUnit.SECONDS.toMillis(SESSION_PAUSE_DURATION_SECONDS))

        mapboxNavigation.startTripSessionAndWaitForFreeDriveState()
        delay(sessionLengthMillis / 2)
        mapboxNavigation.stopTripSessionAndWaitForIdleState()

        mapboxNavigation.onDestroy()

        val billingRequests = getAllBillingEventsWhenHasEvents(1, "Free Drive") {
            it.isFreeDriveSession
        }

        assertSessionEvents(BillingEventType.FREE_DRIVE, billingRequests)
        assertSessionDuration(SESSION_DURATION_SECONDS, billingRequests.first().duration)
    }

    @Test
    fun testBillingSessionsForMixedFreeDriveAndActiveGuidanceTrip() = sdkTest {
        mapboxNavigation.startTripSessionAndWaitForFreeDriveState()
        delay(1000)
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(route))
        delay(2000)
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()
        delay(3000)
        mapboxNavigation.stopTripSessionAndWaitForIdleState()
        mapboxNavigation.onDestroy()

        val billingRequests = getAllBillingEventsWhenHasEvents(3, "any")

        assertSessionEvents(
            listOf(
                BillingEventType.FREE_DRIVE,
                BillingEventType.ACTIVE_GUIDANCE,
                BillingEventType.FREE_DRIVE
            ),
            billingRequests
        )

        assertSessionDuration(1, billingRequests[0].duration)
        assertSessionDuration(2, billingRequests[1].duration)
        assertSessionDuration(3, billingRequests[2].duration)
    }

    private companion object {

        enum class BillingEventType {
            ACTIVE_GUIDANCE,
            FREE_DRIVE;

            companion object {
                fun fromHttpServiceEvent(event: HttpServiceEvent): BillingEventType {
                    return when {
                        event.isActiveGuidanceSession -> ACTIVE_GUIDANCE
                        event.isFreeDriveSession -> FREE_DRIVE
                        else -> error("$event is not a billing event")
                    }
                }
            }
        }

        const val SESSION_DURATION_SECONDS = 2L
        const val SESSION_PAUSE_DURATION_SECONDS = 1L
        const val ACTIVE_GUIDANCE_SKU_PREFIX = "10a"
        const val FREE_DRIVE_SKU_PREFIX = "10b"

        const val SESSIONS_ENDPOINT_PATH = "/sdk-sessions"

        val HttpServiceEvent.skuParameter: String?
            get() = url.parameters()["sku"]

        val HttpServiceEvent.duration: Long?
            get() = url.parameters()["duration"]?.toLong()

        val HttpServiceEvent.isActiveGuidanceSession: Boolean
            get() = skuParameter?.startsWith(ACTIVE_GUIDANCE_SKU_PREFIX) == true

        val HttpServiceEvent.isFreeDriveSession: Boolean
            get() = skuParameter?.startsWith(FREE_DRIVE_SKU_PREFIX) == true

        val HttpServiceEvent.isBillingEvent: Boolean
            get() = url.path.startsWith(SESSIONS_ENDPOINT_PATH) &&
                (isFreeDriveSession || isActiveGuidanceSession)

        fun HttpServiceEventsObserver.billingRequests(): List<HttpServiceEvent.Request> {
            return eventsFlow.replayCache
                .filterIsInstance(HttpServiceEvent.Request::class.java)
                .filter { it.isBillingEvent }
        }

        fun HttpServiceEventsObserver.billingRequestsFlow(): Flow<HttpServiceEvent.Request> {
            return onRequestEventsFlow
                .filter { it.isBillingEvent }
        }

        fun assertSessionEvents(expected: BillingEventType, actual: List<HttpServiceEvent>) {
            return assertSessionEvents(listOf(expected), actual)
        }

        fun assertSessionEvents(expected: List<BillingEventType>, actual: List<HttpServiceEvent>) {
            assertEquals(
                expected,
                actual.map { BillingEventType.fromHttpServiceEvent(it) }
            )
        }

        private fun assertSessionDuration(expectedDurationSeconds: Long, duration: Long?) {
            assertNotNull("Session duration was null", duration)
            requireNotNull(duration)

            if (duration < expectedDurationSeconds) {
                fail(
                    "Session duration is less than expected. " +
                        "Expected: $expectedDurationSeconds, but was $duration"
                )
            }

            // Allow at most one second for session time calculation error
            if (duration - expectedDurationSeconds > 1) {
                fail(
                    "Session duration is too long. " +
                        "Expected $expectedDurationSeconds, but was $duration"
                )
            }
        }
    }
}
