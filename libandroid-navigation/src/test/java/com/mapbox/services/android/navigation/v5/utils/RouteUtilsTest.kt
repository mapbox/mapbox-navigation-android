package com.mapbox.services.android.navigation.v5.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.BaseTest
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteUtilsTest : BaseTest() {

    @Test
    fun isArrivalEvent_returnsTrueWhenRouteProgressStateIsArrived() {
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.currentState() } returns RouteProgressState.ROUTE_ARRIVED
        val routeUtils = RouteUtils()

        val isArrivalEvent = routeUtils.isArrivalEvent(routeProgress)

        assertTrue(isArrivalEvent)
    }

    @Test
    fun isArrivalEvent_returnsFalseWhenRouteProgressStateIsNotArrived() {
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.currentState() } returns RouteProgressState.LOCATION_TRACKING
        val routeUtils = RouteUtils()

        val isArrivalEvent = routeUtils.isArrivalEvent(routeProgress)

        assertFalse(isArrivalEvent)
    }

    @Test
    fun findCurrentBannerInstructions_returnsNullWithNullCurrentStep() {
        val currentStep: LegStep? = null
        val stepDistanceRemaining = 0.0
        val routeUtils = RouteUtils()

        val currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
            currentStep, stepDistanceRemaining
        )

        assertNull(currentBannerInstructions)
    }

    @Test
    @Throws(Exception::class)
    fun findCurrentBannerInstructions_returnsNullWithCurrentStepEmptyInstructions() {
        val (_, _, _, currentLegProgress) = buildDefaultTestRouteProgress()
        val currentStep = currentLegProgress!!.currentStep()
        val stepDistanceRemaining = currentLegProgress.currentStepProgress()!!.distanceRemaining()!!
        val currentInstructions = currentStep!!.bannerInstructions()
        currentInstructions!!.clear()
        val routeUtils = RouteUtils()

        val currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
            currentStep, stepDistanceRemaining
        )

        assertNull(currentBannerInstructions)
    }

    @Test
    @Throws(Exception::class)
    fun findCurrentBannerInstructions_returnsCorrectCurrentInstruction() {
        val (_, _, _, currentLegProgress) = buildDefaultTestRouteProgress()
        val currentStep = currentLegProgress?.currentStep() ?: throw Exception()
        val stepDistanceRemaining = currentLegProgress.currentStepProgress()?.distanceRemaining() ?: throw Exception()
        val routeUtils = RouteUtils()

        val currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
            currentStep, stepDistanceRemaining
        )
        val bannerInstructions = currentStep.bannerInstructions()

        assertNotNull(bannerInstructions)
        assertEquals(bannerInstructions?.get(0), currentBannerInstructions)
    }

    @Test
    @Throws(Exception::class)
    fun findCurrentBannerInstructions_adjustedDistanceRemainingReturnsCorrectInstruction() {
        var routeProgress = buildDefaultTestRouteProgress()
        routeProgress = routeProgress.toBuilder()
            .stepDistanceRemaining(50.0)
            .build()
        val currentStep = routeProgress.currentLegProgress()?.currentStep() ?: throw Exception()
        val stepDistanceRemaining = routeProgress.currentLegProgress()?.currentStepProgress()?.distanceRemaining()
            ?: throw Exception()
        val routeUtils = RouteUtils()

        val currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
            currentStep, stepDistanceRemaining
        )
        val bannerInstructions = currentStep.bannerInstructions()

        assertNotNull(bannerInstructions)
        assertEquals(bannerInstructions?.get(0), currentBannerInstructions)
    }

    @Test
    @Throws(Exception::class)
    fun findCurrentBannerInstructions_adjustedDistanceRemainingRemovesCorrectInstructions() {
        var routeProgress = buildDefaultTestRouteProgress()
        routeProgress = routeProgress.toBuilder()
            .stepIndex(1)
            .stepDistanceRemaining(500.0)
            .build()
        val currentStep = routeProgress.currentLegProgress()?.currentStep() ?: throw Exception()
        val stepDistanceRemaining = routeProgress.currentLegProgress()?.currentStepProgress()?.distanceRemaining()
            ?: throw Exception()
        val routeUtils = RouteUtils()

        val currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
            currentStep, stepDistanceRemaining
        )
        val bannerInstructions = currentStep.bannerInstructions()

        assertNotNull(bannerInstructions)
        assertEquals(bannerInstructions?.get(0), currentBannerInstructions)
    }

    @Test
    fun calculateRemainingWaypoints_whenNoMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointIndices() } returns null
        val routeProgress = buildRouteProgress(routeOptions, 1)
        val routeUtils = RouteUtils()

        val remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress)

        assertNotNull(remainingWaypoints)
        assertEquals(1, remainingWaypoints?.size)
        assertEquals(Point.fromLngLat(4.56789, 0.12345), remainingWaypoints?.get(0))
    }

    @Test
    fun calculateRemainingWaypoints_whenOneMiddleWaypoint() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointIndices() } returns "0;3;6"
        val routeProgress = buildRouteProgress(routeOptions, 2)
        val routeUtils = RouteUtils()

        val remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress)

        assertNotNull(remainingWaypoints)
        assertEquals(4, remainingWaypoints?.size)
        assertEquals(Point.fromLngLat(7.89012, 3.45678), remainingWaypoints?.get(0))
        assertEquals(Point.fromLngLat(9.01234, 5.67890), remainingWaypoints?.get(1))
        assertEquals(Point.fromLngLat(2.34567, 8.90123), remainingWaypoints?.get(2))
        assertEquals(Point.fromLngLat(4.56789, 0.12345), remainingWaypoints?.get(3))
    }

    @Test
    fun calculateRemainingWaypoints_whenTwoMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointIndices() } returns "0;2;4;6"
        val routeProgress = buildRouteProgress(routeOptions, 2)
        val routeUtils = RouteUtils()

        val remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress)

        assertNotNull(remainingWaypoints)
        assertEquals(3, remainingWaypoints?.size)
        assertEquals(Point.fromLngLat(9.01234, 5.67890), remainingWaypoints?.get(0))
        assertEquals(Point.fromLngLat(2.34567, 8.90123), remainingWaypoints?.get(1))
        assertEquals(Point.fromLngLat(4.56789, 0.12345), remainingWaypoints?.get(2))
    }

    @Test
    fun calculateRemainingWaypoints_whenTwoMiddleOneByOneWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointIndices() } returns "0;3;4;6"
        val routeProgress = buildRouteProgress(routeOptions, 3)
        val routeUtils = RouteUtils()

        val remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress)

        assertNotNull(remainingWaypoints)
        assertEquals(4, remainingWaypoints?.size)
        assertEquals(Point.fromLngLat(7.89012, 3.45678), remainingWaypoints?.get(0))
        assertEquals(Point.fromLngLat(9.01234, 5.67890), remainingWaypoints?.get(1))
        assertEquals(Point.fromLngLat(2.34567, 8.90123), remainingWaypoints?.get(2))
        assertEquals(Point.fromLngLat(4.56789, 0.12345), remainingWaypoints?.get(3))
    }

    @Test
    fun calculateRemainingWaypoints_handlesNullOptions() {
        val routeProgress = buildRouteProgress(null, 2)
        val routeUtils = RouteUtils()

        val remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress)

        assertNull(remainingWaypoints)
    }

    @Test
    fun calculateRemainingWaypointsIndices_whenNoMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointIndices() } returns "0;6"
        val routeProgress = buildRouteProgress(routeOptions, 1)
        val routeUtils = RouteUtils()

        val remainingWaypointIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress)

        assertNotNull(remainingWaypointIndices)
        assertEquals(2, remainingWaypointIndices?.size)
        assertEquals(0, remainingWaypointIndices?.get(0))
        assertEquals(1, remainingWaypointIndices?.get(1))
    }

    @Test
    fun calculateRemainingWaypointsIndices_whenOneMiddleWaypoint() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointIndices() } returns "0;3;6"
        val routeProgress = buildRouteProgress(routeOptions, 2)
        val routeUtils = RouteUtils()

        val remainingWaypointIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress)

        assertNotNull(remainingWaypointIndices)
        assertEquals(3, remainingWaypointIndices?.size)
        assertEquals(0, remainingWaypointIndices?.get(0))
        assertEquals(1, remainingWaypointIndices?.get(1))
        assertEquals(4, remainingWaypointIndices?.get(2))
    }

    @Test
    fun calculateRemainingWaypointsIndices_whenTwoMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointIndices() } returns "0;2;4;6"
        val routeProgress = buildRouteProgress(routeOptions, 2)
        val routeUtils = RouteUtils()

        val remainingWaypointIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress)

        assertNotNull(remainingWaypointIndices)
        assertEquals(3, remainingWaypointIndices?.size)
        assertEquals(0, remainingWaypointIndices?.get(0))
        assertEquals(1, remainingWaypointIndices?.get(1))
        assertEquals(3, remainingWaypointIndices?.get(2))
    }

    @Test
    fun calculateRemainingWaypointsIndices_whenTwoMiddleOneByOneWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointIndices() } returns "0;3;4;6"
        val routeProgress = buildRouteProgress(routeOptions, 3)
        val routeUtils = RouteUtils()

        val remainingWaypointIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress)

        assertNotNull(remainingWaypointIndices)
        assertEquals(4, remainingWaypointIndices?.size)
        assertEquals(0, remainingWaypointIndices?.get(0))
        assertEquals(1, remainingWaypointIndices?.get(1))
        assertEquals(2, remainingWaypointIndices?.get(2))
        assertEquals(4, remainingWaypointIndices?.get(3))
    }

    @Test
    fun calculateRemainingWaypointsIndices_handlesNullOptions() {
        val routeProgress = buildRouteProgress(null, 2)
        val routeUtils = RouteUtils()

        val remainingWaypointsIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress)

        assertNull(remainingWaypointsIndices)
    }

    @Test
    fun calculateRemainingWaypointNames_whenNoMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointNames() } returns "first;seventh"
        val routeProgress = buildRouteProgress(routeOptions, 1)
        val routeUtils = RouteUtils()

        val remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress)

        assertNotNull(remainingWaypointNames)
        assertEquals(2, remainingWaypointNames?.size)
        assertEquals("first", remainingWaypointNames?.get(0))
        assertEquals("seventh", remainingWaypointNames?.get(1))
    }

    @Test
    fun calculateRemainingWaypointNames_whenOneMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointNames() } returns "first;fourth;seventh"
        val routeProgress = buildRouteProgress(routeOptions, 2)
        val routeUtils = RouteUtils()

        val remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress)

        assertNotNull(remainingWaypointNames)
        assertEquals(3, remainingWaypointNames?.size)
        assertEquals("first", remainingWaypointNames?.get(0))
        assertEquals("fourth", remainingWaypointNames?.get(1))
        assertEquals("seventh", remainingWaypointNames?.get(2))
    }

    @Test
    fun calculateRemainingWaypointNames_whenTwoMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointNames() } returns "first;third;fifth;seventh"
        val routeProgress = buildRouteProgress(routeOptions, 2)
        val routeUtils = RouteUtils()

        val remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress)

        assertNotNull(remainingWaypointNames)
        assertEquals(3, remainingWaypointNames?.size)
        assertEquals("first", remainingWaypointNames?.get(0))
        assertEquals("fifth", remainingWaypointNames?.get(1))
        assertEquals("seventh", remainingWaypointNames?.get(2))
    }

    @Test
    fun calculateRemainingWaypointNames_whenTwoMiddleOneByOneWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.waypointNames() } returns "first;second;fifth;seventh"
        val routeProgress = buildRouteProgress(routeOptions, 3)
        val routeUtils = RouteUtils()

        val remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress)

        assertNotNull(remainingWaypointNames)
        assertEquals(4, remainingWaypointNames?.size)
        assertEquals("first", remainingWaypointNames?.get(0))
        assertEquals("second", remainingWaypointNames?.get(1))
        assertEquals("fifth", remainingWaypointNames?.get(2))
        assertEquals("seventh", remainingWaypointNames?.get(3))
    }

    @Test
    fun calculateRemainingWaypointNames_handlesNullOptions() {
        val routeProgress = buildRouteProgress(null, 2)
        val routeUtils = RouteUtils()

        val remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress)

        assertNull(remainingWaypointNames)
    }

    @Test
    fun calculateRemainingApproaches_whenNoMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.approaches() } returns "curb;unrestricted"
        val routeProgress = buildRouteProgress(routeOptions, 1)
        val routeUtils = RouteUtils()

        val remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress)

        assertNotNull(remainingApproaches)
        assertEquals(2, remainingApproaches?.size)
        assertEquals("curb", remainingApproaches?.get(0))
        assertEquals("unrestricted", remainingApproaches?.get(1))
    }

    @Test
    fun calculateRemainingApproaches_whenOneMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.approaches() } returns "curb;curb;unrestricted"
        val routeProgress = buildRouteProgress(routeOptions, 2)
        val routeUtils = RouteUtils()

        val remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress)

        assertNotNull(remainingApproaches)
        assertEquals(3, remainingApproaches?.size)
        assertEquals("curb", remainingApproaches?.get(0))
        assertEquals("curb", remainingApproaches?.get(1))
        assertEquals("unrestricted", remainingApproaches?.get(2))
    }

    @Test
    fun calculateRemainingApproaches_whenTwoMiddleWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.approaches() } returns "curb;curb;curb;unrestricted"
        val routeProgress = buildRouteProgress(routeOptions, 2)
        val routeUtils = RouteUtils()

        val remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress)

        assertNotNull(remainingApproaches)
        assertEquals(3, remainingApproaches?.size)
        assertEquals("curb", remainingApproaches?.get(0))
        assertEquals("curb", remainingApproaches?.get(1))
        assertEquals("unrestricted", remainingApproaches?.get(2))
    }

    @Test
    fun calculateRemainingApproaches_whenTwoMiddleOneByOneWaypoints() {
        val routeOptions = buildRouteOptions()
        every { routeOptions.approaches() } returns "curb;curb;curb;unrestricted"
        val routeProgress = buildRouteProgress(routeOptions, 3)
        val routeUtils = RouteUtils()

        val remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress)

        assertNotNull(remainingApproaches)
        assertEquals(4, remainingApproaches?.size)
        assertEquals("curb", remainingApproaches?.get(0))
        assertEquals("curb", remainingApproaches?.get(1))
        assertEquals("curb", remainingApproaches?.get(2))
        assertEquals("unrestricted", remainingApproaches?.get(3))
    }

    @Test
    fun calculateRemainingApproaches_handlesNullOptions() {
        val routeProgress = buildRouteProgress(null, 2)
        val routeUtils = RouteUtils()

        val remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress)

        assertNull(remainingApproaches)
    }

    private fun buildCoordinateList() = listOf(
        Point.fromLngLat(1.23456, 7.89012),
        Point.fromLngLat(3.45678, 9.01234),
        Point.fromLngLat(5.67890, 1.23456),
        Point.fromLngLat(7.89012, 3.45678),
        Point.fromLngLat(9.01234, 5.67890),
        Point.fromLngLat(2.34567, 8.90123),
        Point.fromLngLat(4.56789, 0.12345)
    )

    private fun buildRouteOptions(): RouteOptions {
        val routeOptions = mockk<RouteOptions>(relaxed = true)
        every { routeOptions.coordinates() } returns buildCoordinateList()
        return routeOptions
    }

    private fun buildRouteProgress(routeOptions: RouteOptions?, remainingWaypointsCount: Int): RouteProgress {
        val route = mockk<DirectionsRoute>(relaxed = true)
        every { route.routeOptions() } returns routeOptions
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.remainingWaypoints() } returns remainingWaypointsCount
        every { routeProgress.directionsRoute() } returns route
        return routeProgress
    }
}
