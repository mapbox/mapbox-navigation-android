package com.mapbox.navigation.driver.notification.traffic

import android.os.SystemClock
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.driver.notification.internal.SlowTrafficSegment
import com.mapbox.navigation.driver.notification.internal.SlowTrafficSegmentTraits
import com.mapbox.navigation.driver.notification.internal.SlowTrafficSegmentsFinder
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class SlowTrafficNotificationProviderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val segmentsFinder = mockk<SlowTrafficSegmentsFinder>(relaxed = true)

    @Test
    fun `verify slow traffic notification sampling`() = runBlocking {
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { any<MapboxNavigation>().flowRouteProgress() } answers {
            flow {
                emit(routeProgress)
                delay(1000.milliseconds)
                emit(routeProgress)
                delay(400.milliseconds)
                emit(routeProgress)
                delay(600.milliseconds)
                emit(routeProgress)
            }
        }

        val provider = SlowTrafficNotificationProvider(
            SlowTrafficNotificationOptions.Builder()
                .slowTrafficCongestionRange(IntRange(60, 100))
                .slowTrafficPeriodCheck(800.milliseconds)
                .trafficDelay(2.minutes)
                .build(),
        ).apply {
            slowTrafficSegmentsFinder = segmentsFinder
        }

        provider.onAttached(mapboxNavigation)

        // SlowTrafficNotificationProvider doesn't generate any notification.
        // Limiting waiting with timeout
        withTimeoutOrNull(3000) {
            provider.trackNotifications().toList()
        }
        provider.onDetached(mapboxNavigation)
        coVerify(exactly = 3) {
            segmentsFinder.findSlowTrafficSegments(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test(timeout = 1000)
    fun `verify slow traffic notification`() = runBlocking {
        val slowTrafficCongestion = 60..100
        val segment = SlowTrafficSegment(
            legIndex = 0,
            geometryRange = 2..5,
            distanceToSegmentMeters = 21.0,
            traits = setOf(
                SlowTrafficSegmentTraits(
                    congestionRange = slowTrafficCongestion,
                    freeFlowDuration = 100.seconds,
                    duration = 400.seconds,
                    distanceMeters = 100.0,
                ),
            ),
        )
        coEvery {
            val result = segmentsFinder.findSlowTrafficSegments(any(), any(), any(), any())
            result
        } returns listOf(segment)

        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { any<MapboxNavigation>().flowRouteProgress() } answers { flowOf(routeProgress) }

        val provider = SlowTrafficNotificationProvider(
            SlowTrafficNotificationOptions.Builder()
                .slowTrafficCongestionRange(slowTrafficCongestion)
                .slowTrafficPeriodCheck(800.milliseconds)
                .trafficDelay(15.seconds)
                .build(),
        ).apply {
            slowTrafficSegmentsFinder = segmentsFinder
        }

        provider.onAttached(mapboxNavigation)
        val slowTrafficNotification =
            provider.trackNotifications().firstOrNull() as? SlowTrafficNotification

        assertEquals(segment.geometryRange, slowTrafficNotification?.slowTrafficGeometryRange)
        assertEquals(segment.legIndex, slowTrafficNotification?.legIndex)
        assertEquals(
            segment.traits.first().duration,
            slowTrafficNotification?.slowTrafficRangeDuration,
        )
        assertEquals(
            segment.traits.first().distanceMeters,
            slowTrafficNotification?.slowTrafficRangeDistance,
        )
        assertEquals(
            segment.traits.first().freeFlowDuration,
            slowTrafficNotification?.freeFlowRangeDuration,
        )
    }

    companion object {

        @JvmStatic
        @BeforeClass
        fun setup() {
            mockkStatic(MapboxNavigation::flowRouteProgress)
            mockkStatic(SystemClock::elapsedRealtime)
            every { SystemClock.elapsedRealtime() } answers { System.currentTimeMillis() }
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            unmockkStatic(MapboxNavigation::flowRouteProgress)
            unmockkStatic(SystemClock::elapsedRealtime)
        }
    }
}
