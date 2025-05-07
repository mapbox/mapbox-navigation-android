package com.mapbox.navigation.driver.notification.traffic

import android.os.SystemClock
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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

    @Test
    fun `verify slow traffic notification sampling`() = runBlocking {
        val routeLeg = createRouteLeg()
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        val currentLegProgress = mockk<RouteLegProgress>(relaxed = true)
        every { routeProgress.currentLegProgress } answers { currentLegProgress }
        every { currentLegProgress.routeLeg } answers { routeLeg }
        every { currentLegProgress.geometryIndex } answers { 0 }
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
        )

        provider.onAttached(mapboxNavigation)

        // SlowTrafficNotificationProvider doesn't generate any notification.
        // Limiting waiting with timeout
        withTimeoutOrNull(3000) {
            provider.trackNotifications().toList()
        }
        provider.onDetached(mapboxNavigation)
        verify(exactly = 3) { routeProgress.currentLegProgress }
    }

    @Test
    fun `verify slow traffic notification`() = runTest {
        // Input data and constants, all next checks are use these variables
        val startCongestionIndex = 2
        val slowTrafficCongestion = 60..100
        val routeLeg = createRouteLeg(
            annotation = createRouteLegAnnotation(
                congestionNumeric = listOf(20, 50, 60, 70, 80, 90, 20),
                distance = listOf(10.0, 11.0, 11.0, 11.0, 12.0, 11.0, 11.0),
                duration = listOf(4.0, 4.0, 10.0, 10.0, 10.0, 5.0, 6.0),
                freeFlowSpeed = listOf(10, 10, 10, 10, 11, 11, 11),
            ),
        )
        val slowTrafficCongestionRange =
            startCongestionIndex until (routeLeg.annotation()?.congestionNumeric()?.size ?: 0)

        val routeProgress = mockk<RouteProgress>(relaxed = true)
        val currentLegProgress = mockk<RouteLegProgress>(relaxed = true)
        every { routeProgress.currentLegProgress } answers { currentLegProgress }
        every { currentLegProgress.routeLeg } answers { routeLeg }

        every { currentLegProgress.geometryIndex } answers { startCongestionIndex }
        every { any<MapboxNavigation>().flowRouteProgress() } answers { flowOf(routeProgress) }

        val provider = SlowTrafficNotificationProvider(
            SlowTrafficNotificationOptions.Builder()
                .slowTrafficCongestionRange(slowTrafficCongestion)
                .slowTrafficPeriodCheck(800.milliseconds)
                .trafficDelay(15.seconds)
                .build(),
        )

        provider.onAttached(mapboxNavigation)
        val slowTrafficNotification =
            provider.trackNotifications().firstOrNull() as? SlowTrafficNotification

        assertEquals(slowTrafficCongestionRange, slowTrafficNotification?.slowTrafficGeometryRange)
        assertEquals(currentLegProgress.legIndex, slowTrafficNotification?.legIndex)

        assertEquals(
            routeLeg.annotation()?.duration()?.subList(
                slowTrafficCongestionRange.first,
                slowTrafficCongestionRange.last,
            )?.sum()?.seconds,
            slowTrafficNotification?.slowTrafficRangeDuration,
        )

        assertEquals(
            routeLeg.annotation()?.distance()?.subList(
                slowTrafficCongestionRange.first,
                slowTrafficCongestionRange.last,
            )?.sum(),
            slowTrafficNotification?.slowTrafficRangeDistance,
        )

        assertEquals(15L, slowTrafficNotification?.freeFlowRangeDuration?.inWholeSeconds)
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
