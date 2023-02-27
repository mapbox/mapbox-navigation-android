package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.startTripSessionAndWaitForFreeDriveState
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.stopTripSessionAndWaitForIdleState
import com.mapbox.navigation.instrumentation_tests.utils.http.HttpServiceEvent
import com.mapbox.navigation.instrumentation_tests.utils.http.HttpServiceEventsObserver
import com.mapbox.navigation.instrumentation_tests.utils.parameters
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider.toNavigationRoutes
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.TimeUnit

class TripSessionsBillingTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    private lateinit var mapboxNavigation: MapboxNavigation

    private val route = RoutesProvider.dc_very_short(context).toNavigationRoutes()[0]

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        route.routeOptions.coordinatesList().first().run {
            latitude = latitude()
            longitude = longitude()
        }
        bearing = 190f
    }

    @Before
    fun setup() {
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

    @Test
    fun testBillingSessionSentForOneSecondLongActiveGuidanceTrip() = sdkTest {
        val observer = HttpServiceEventsObserver.register()

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(route))
        mapboxNavigation.startTripSession()

        delay(TimeUnit.SECONDS.toMillis(SESSION_DURATION_SECONDS))

        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        delay(HTTP_EVENT_APPEARANCE_TIMEOUT_MILLIS)

        val billingRequests = observer.billingRequests()
        assertEquals(1, billingRequests.size)
        assertTrue(billingRequests.first().isActiveGuidanceSession)

        assertSessionDuration(SESSION_DURATION_SECONDS, billingRequests.first().duration)
    }

    @Test
    fun testBillingSessionSentForEmptyActiveGuidanceTrip() = sdkTest {
        val observer = HttpServiceEventsObserver.register()

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(listOf(route))
        mapboxNavigation.startTripSession()
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()

        delay(HTTP_EVENT_APPEARANCE_TIMEOUT_MILLIS)

        val billingRequests = observer.billingRequests()
        assertEquals(1, billingRequests.size)
        assertTrue(billingRequests.first().isActiveGuidanceSession)

        assertSessionDuration(0, billingRequests.first().duration)
    }

    @Test
    fun testBillingSessionSentForOneSecondLongFreeDrive() = sdkTest {
        val observer = HttpServiceEventsObserver.register()

        mapboxNavigation.startTripSessionAndWaitForFreeDriveState()
        delay(TimeUnit.SECONDS.toMillis(SESSION_DURATION_SECONDS))
        mapboxNavigation.stopTripSessionAndWaitForIdleState()
        mapboxNavigation.onDestroy()

        delay(HTTP_EVENT_APPEARANCE_TIMEOUT_MILLIS)

        val billingRequests = observer.billingRequests()
        assertEquals(1, billingRequests.size)
        assertTrue(billingRequests.first().isFreeDriveSession)

        assertSessionDuration(SESSION_DURATION_SECONDS, billingRequests.first().duration)
    }

    @Test
    fun testBillingSessionSentForEmptyFreeDrive() = sdkTest {
        val observer = HttpServiceEventsObserver.register()

        mapboxNavigation.startTripSessionAndWaitForFreeDriveState()
        mapboxNavigation.stopTripSessionAndWaitForIdleState()
        mapboxNavigation.onDestroy()

        delay(HTTP_EVENT_APPEARANCE_TIMEOUT_MILLIS)

        val billingRequests = observer.billingRequests()
        assertEquals(1, billingRequests.size)
        assertTrue(billingRequests.first().isFreeDriveSession)

        assertSessionDuration(0, billingRequests.first().duration)
    }

    @Test
    fun testOneBillingSessionSentForPausedAndResumedFreeDrive() = sdkTest {
        val observer = HttpServiceEventsObserver.register()

        mapboxNavigation.startTripSessionAndWaitForFreeDriveState()
        mapboxNavigation.stopTripSessionAndWaitForIdleState()

        delay(1000)

        mapboxNavigation.startTripSessionAndWaitForFreeDriveState()
        mapboxNavigation.stopTripSessionAndWaitForIdleState()

        mapboxNavigation.onDestroy()

        delay(HTTP_EVENT_APPEARANCE_TIMEOUT_MILLIS)

        val billingRequests = observer.billingRequests()
        assertEquals(1, billingRequests.size)
        assertTrue(billingRequests.first().isFreeDriveSession)

        assertSessionDuration(0, billingRequests.first().duration)
    }

    private companion object {

        const val HTTP_EVENT_APPEARANCE_TIMEOUT_MILLIS = 500L
        const val SESSION_DURATION_SECONDS = 1L
        const val ACTIVE_GUIDANCE_SKU_PREFIX = "10a"
        const val FREE_DRIVE_SKU_PREFIX = "10b"

        const val SESSIONS_ENDPOINT_PATH = "/sdk-sessions"

        val HttpServiceEvent.isSessionEvent: Boolean
            get() = url.path.startsWith(SESSIONS_ENDPOINT_PATH)

        val HttpServiceEvent.skuParameter: String?
            get() = url.parameters()["sku"]

        val HttpServiceEvent.isActiveGuidanceSession: Boolean
            get() = skuParameter?.startsWith(ACTIVE_GUIDANCE_SKU_PREFIX) == true

        val HttpServiceEvent.isFreeDriveSession: Boolean
            get() = skuParameter?.startsWith(FREE_DRIVE_SKU_PREFIX) == true

        val HttpServiceEvent.duration: Long?
            get() = url.parameters()["duration"]?.toLong()

        fun HttpServiceEventsObserver.billingRequests(): List<HttpServiceEvent.Request> {
            return onRequestEvents
                .filter {
                    it.isSessionEvent && (it.isFreeDriveSession || it.isActiveGuidanceSession)
                }
        }

        private fun assertSessionDuration(expectedDurationSeconds: Long?, duration: Long?) {
            if (expectedDurationSeconds == null) {
                assertNull(duration)
                return
            }

            assertNotNull("Session duration was null", duration)
            requireNotNull(duration)

            // Allow at most one second for session time calculation error
            val approxSame = (duration >= expectedDurationSeconds) &&
                (duration - expectedDurationSeconds < 1)
            if (!approxSame) {
                fail("Expected session duration $expectedDurationSeconds, but was $duration")
            }
        }
    }
}
