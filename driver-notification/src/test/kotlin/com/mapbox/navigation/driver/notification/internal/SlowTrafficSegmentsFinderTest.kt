package com.mapbox.navigation.driver.notification.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.internal.utils.Constants
import com.mapbox.navigation.base.internal.utils.Constants.CongestionRange.HEAVY_CONGESTION_RANGE
import com.mapbox.navigation.base.internal.utils.Constants.CongestionRange.SEVERE_CONGESTION_RANGE
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SlowTrafficSegmentsFinderTest {
    private val lowCongestion = Constants.CongestionRange.LOW_CONGESTION_RANGE.middle
    private val heavyCongestion = HEAVY_CONGESTION_RANGE.middle
    private val severeCongestion = SEVERE_CONGESTION_RANGE.middle
    private val finder = SlowTrafficSegmentsFinder()

    @Test
    fun `finds single slow segment in middle of leg`() = runBlocking {
        val annotation = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                lowCongestion,
                severeCongestion,
                severeCongestion,
                severeCongestion,
                lowCongestion,
                lowCongestion,
            )
            every { distance() } returns listOf(50.0, 60.0, 70.0, 80.0, 90.0, 40.0, 30.0)
            every { duration() } returns listOf(5.0, 6.0, 15.0, 18.0, 20.0, 4.0, 3.0)
            every { freeflowSpeed() } returns listOf(36, 36, 17, 16, 15, 36, 36)
        }
        val routeProgress = prepareRouteProgressFrom(annotation)

        val segments = finder.findSlowTrafficSegments(
            routeProgress = routeProgress,
            targetCongestionsRanges = listOf(SEVERE_CONGESTION_RANGE),
        )

        Assert.assertEquals(1, segments.size)

        val segment = segments.first()
        Assert.assertEquals(0, segment.legIndex)
        Assert.assertEquals(2..4, segment.geometryRange)
        // The distance before the segment starts is 50.0 + 60.0
        Assert.assertEquals(110.0, segment.distanceToSegmentMeters, 0.01)

        Assert.assertEquals(
            SEVERE_CONGESTION_RANGE,
            segment.congestionRange,
        )
        // Total distance of the slow part: 70.0 + 80.0 + 90.0
        Assert.assertEquals(240.0, segment.distanceMeters, 0.01)
        // Total duration of the slow part: 15.0 + 18.0 + 20.0
        Assert.assertEquals(53.seconds, segment.duration)

        Assert.assertEquals(54, segment.freeFlowDuration.inWholeSeconds)
    }

    @Test
    fun `finds multiple distinct slow segments in single leg`() = runBlocking {
        val annotation = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                severeCongestion, // first segment
                severeCongestion, // first segment
                lowCongestion,
                lowCongestion,
                severeCongestion, // second segment
                lowCongestion,
            )
            every { distance() } returns listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0)
            every { duration() } returns listOf(1.0, 12.0, 13.0, 4.0, 5.0, 16.0, 7.0)
            every { freeflowSpeed() } returns listOf(36, 15, 15, 36, 36, 15, 36)
        }
        val routeProgress = prepareRouteProgressFrom(annotation)

        val segments = finder.findSlowTrafficSegments(
            routeProgress = routeProgress,
            targetCongestionsRanges = listOf(SEVERE_CONGESTION_RANGE),
        )

        Assert.assertEquals(2, segments.size)

        val firstSegment = segments[0]
        Assert.assertEquals(0, firstSegment.legIndex)
        Assert.assertEquals(1..2, firstSegment.geometryRange)
        Assert.assertEquals(10.0, firstSegment.distanceToSegmentMeters, 0.01)
        Assert.assertEquals(50.0, firstSegment.distanceMeters, 0.01)
        Assert.assertEquals(25.seconds, firstSegment.duration)

        val secondSegment = segments[1]
        Assert.assertEquals(0, secondSegment.legIndex)
        Assert.assertEquals(5..5, secondSegment.geometryRange)
        Assert.assertEquals(150.0, secondSegment.distanceToSegmentMeters, 0.01)
        Assert.assertEquals(60.0, secondSegment.distanceMeters, 0.01)
        Assert.assertEquals(16.seconds, secondSegment.duration)
    }

    @Test
    fun `finds many segment spanning for multiple congestion traits`() = runBlocking {
        val annotation = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                heavyCongestion, // start of segment
                heavyCongestion, // end of segment
                severeCongestion, // start of segment
                severeCongestion, // end of segment
                lowCongestion,
            )
            every { distance() } returns listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0)
            every { duration() } returns listOf(1.0, 8.0, 9.0, 15.0, 18.0, 6.0)
            every { freeflowSpeed() } returns listOf(36, 25, 25, 15, 15, 36)
        }
        val routeProgress = prepareRouteProgressFrom(annotation)

        val segments = finder.findSlowTrafficSegments(
            routeProgress = routeProgress,
            targetCongestionsRanges = listOf(
                HEAVY_CONGESTION_RANGE,
                SEVERE_CONGESTION_RANGE,
            ),
        )

        Assert.assertEquals(2, segments.size)

        Assert.assertEquals(HEAVY_CONGESTION_RANGE, segments.first().congestionRange)
        Assert.assertEquals(0, segments.first().legIndex)
        Assert.assertEquals(1..2, segments.first().geometryRange)
        Assert.assertEquals(10.0, segments.first().distanceToSegmentMeters, 0.01)
        Assert.assertEquals(50.0, segments.first().distanceMeters, 0.01)
        Assert.assertEquals(17.seconds, segments.first().duration)

        Assert.assertEquals(SEVERE_CONGESTION_RANGE, segments.last().congestionRange)
        Assert.assertEquals(0, segments.last().legIndex)
        Assert.assertEquals(3..4, segments.last().geometryRange)
        Assert.assertEquals(60.0, segments.last().distanceToSegmentMeters, 0.01)
        Assert.assertEquals(90.0, segments.last().distanceMeters, 0.01)
        Assert.assertEquals(33.seconds, segments.last().duration)
    }

    @Test
    fun `gives single summary for continuous segments spanning multiple congestion traits`() = runBlocking {
        val annotation = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                heavyCongestion, // start of segment
                heavyCongestion,
                severeCongestion,
                severeCongestion, // end of segment
                lowCongestion,
            )
            every { distance() } returns listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0)
            every { duration() } returns listOf(1.0, 8.0, 9.0, 15.0, 18.0, 6.0)
            every { freeflowSpeed() } returns listOf(36, 25, 25, 15, 15, 36)
        }
        val routeProgress = prepareRouteProgressFrom(annotation)

        val summaries = finder.findAndSummarizeSlowTrafficSegments(
            routeProgress = routeProgress,
            targetCongestionsRanges = listOf(
                HEAVY_CONGESTION_RANGE,
                SEVERE_CONGESTION_RANGE,
            ),
        )

        Assert.assertEquals(1, summaries.size)
        val summary = summaries.first()
        Assert.assertEquals(1..4, summary.geometryRange)
        Assert.assertEquals(10.0, summary.distanceToSegmentMeters, 0.01)
        Assert.assertEquals(2, summary.traits.size)

        val moderateTraits =
            summary.traits.first { it.congestionRange == HEAVY_CONGESTION_RANGE }
        Assert.assertEquals(50.0, moderateTraits.distanceMeters, 0.01)
        Assert.assertEquals(17.seconds, moderateTraits.duration)

        val severeTraits =
            summary.traits.first { it.congestionRange == SEVERE_CONGESTION_RANGE }
        Assert.assertEquals(90.0, severeTraits.distanceMeters, 0.01)
        Assert.assertEquals(33.seconds, severeTraits.duration)
    }

    @Test
    fun `finds slow segment that ends at the very end of leg`() = runBlocking {
        val annotation = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                lowCongestion,
                severeCongestion,
                severeCongestion,
            )
            every { distance() } returns listOf(50.0, 60.0, 70.0, 80.0)
            every { duration() } returns listOf(5.0, 6.0, 15.0, 18.0)
            every { freeflowSpeed() } returns listOf(36, 36, 17, 16)
        }
        val routeProgress = prepareRouteProgressFrom(annotation)

        val segments = finder.findSlowTrafficSegments(
            routeProgress = routeProgress,
            targetCongestionsRanges = listOf(SEVERE_CONGESTION_RANGE),
        )

        Assert.assertEquals(1, segments.size)
        val segment = segments.first()
        Assert.assertEquals(0, segment.legIndex)
        Assert.assertEquals(2..3, segment.geometryRange)
        Assert.assertEquals(110.0, segment.distanceToSegmentMeters, 0.01)
        Assert.assertEquals(150.0, segment.distanceMeters, 0.01)
        Assert.assertEquals(33.seconds, segment.duration)
    }

    @Test
    fun `finds multiple segments across multiple legs`() = runBlocking {
        val annotation1 = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                severeCongestion,
                lowCongestion,
            )
            every { distance() } returns listOf(100.0, 200.0, 300.0)
            every { duration() } returns listOf(10.0, 40.0, 30.0)
            every { freeflowSpeed() } returns listOf(36, 15, 36)
        }
        val annotation2 = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                lowCongestion,
                severeCongestion,
            )
            every { distance() } returns listOf(50.0, 60.0, 70.0)
            every { duration() } returns listOf(5.0, 6.0, 20.0)
            every { freeflowSpeed() } returns listOf(36, 36, 15)
        }
        val routeProgress = prepareRouteProgressFrom(annotation1, annotation2)

        val segments = finder.findSlowTrafficSegments(
            routeProgress = routeProgress,
            targetCongestionsRanges = listOf(SEVERE_CONGESTION_RANGE),
        )

        Assert.assertEquals(2, segments.size)

        val firstSegment = segments[0]
        Assert.assertEquals(0, firstSegment.legIndex)
        Assert.assertEquals(1..1, firstSegment.geometryRange)
        Assert.assertEquals(100.0, firstSegment.distanceToSegmentMeters, 0.01)
        Assert.assertEquals(200.0, firstSegment.distanceMeters, 0.01)

        val secondSegment = segments[1]
        Assert.assertEquals(1, secondSegment.legIndex)
        Assert.assertEquals(2..2, secondSegment.geometryRange)
        val leg1TotalDistance = 100.0 + 200.0 + 300.0
        val leg2PreSegmentDistance = 50.0 + 60.0
        Assert.assertEquals(
            leg1TotalDistance + leg2PreSegmentDistance,
            secondSegment.distanceToSegmentMeters,
            0.01,
        )
        Assert.assertEquals(70.0, secondSegment.distanceMeters, 0.01)
    }

    @Test
    fun `respects segments limit and returns fewer segments than available`() = runBlocking {
        val annotation = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                severeCongestion, // segment 1
                lowCongestion,
                severeCongestion, // segment 2
                lowCongestion,
                severeCongestion, // segment 3 (should be ignored)
                lowCongestion,
            )
            every { distance() } returns listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0)
            every { duration() } returns listOf(1.0, 12.0, 3.0, 14.0, 5.0, 16.0, 7.0)
            every { freeflowSpeed() } returns listOf(36, 15, 36, 15, 36, 15, 36)
        }
        val routeProgress = prepareRouteProgressFrom(annotation)

        val segments = finder.findSlowTrafficSegments(
            routeProgress = routeProgress,
            targetCongestionsRanges = listOf(SEVERE_CONGESTION_RANGE),
            segmentsLimit = 2,
        )

        Assert.assertEquals(2, segments.size)
        // Verify it's the first segment
        Assert.assertEquals(1..1, segments[0].geometryRange)
        // Verify it's the second segment
        Assert.assertEquals(3..3, segments[1].geometryRange)
    }

    @Test
    fun `respects legs limit and ignores traffic in later legs`() = runBlocking {
        val annotation1 = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                lowCongestion,
                lowCongestion,
            )
            every { distance() } returns listOf(100.0, 200.0, 300.0)
            every { duration() } returns listOf(10.0, 20.0, 30.0)
            every { freeflowSpeed() } returns listOf(36, 36, 36)
        }
        val annotation2 = mockk<LegAnnotation> {
            every { congestionNumeric() } returns listOf(
                lowCongestion,
                severeCongestion,
                lowCongestion,
            )
            every { distance() } returns listOf(50.0, 60.0, 70.0)
            every { duration() } returns listOf(5.0, 16.0, 7.0)
            every { freeflowSpeed() } returns listOf(36, 15, 36)
        }
        val routeProgress = prepareRouteProgressFrom(annotation1, annotation2)

        val segments = finder.findSlowTrafficSegments(
            routeProgress = routeProgress,
            targetCongestionsRanges = listOf(SEVERE_CONGESTION_RANGE),
            legsLimit = 1,
        )

        Assert.assertTrue(segments.isEmpty())
    }

    private fun prepareRouteProgressFrom(vararg annotation: LegAnnotation): RouteProgress {
        val legs = annotation.map {
            mockk<RouteLeg> { every { annotation() } returns it }
        }
        val route = mockk<DirectionsRoute> { every { legs() } returns legs }
        val legProgress = mockk<RouteLegProgress> {
            every { legIndex } returns 0
            every { geometryIndex } returns 0
        }
        val routeProgress = mockk<RouteProgress> {
            every { this@mockk.route } returns route
            every { currentLegProgress } returns legProgress
        }
        return routeProgress
    }
}

private val IntRange.middle: Int
    get() = first + (last - first) / 2
