package com.mapbox.navigation.ui.maps.camera.data

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Size
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getMapAnchoredPaddingFromUserPadding
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getRemainingPointsOnRoute
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getScreenBoxForFraming
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getSmootherBearingForMap
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.isFramingManeuver
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteForPostManeuverFramingGeometry
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteIntersections
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.simplifyCompleteRoutePoints
import com.mapbox.navigation.ui.maps.internal.camera.OverviewMode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ViewportDataSourceProcessorTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val pointAdapter = PointArrayTestAdapter()
    private val doubleAdapter = DoubleArrayTestAdapter()

    private val multiLegRoute = DirectionsRoute.fromJson(
        FileUtils.loadJsonFixture("multileg_route.json"),
    )

    private val completeRoutePoints = decodeArrays3(
        Gson().fromJson(
            FileUtils.loadJsonFixture("multileg_route_all_points_per_step.json"),
            List::class.java,
        ) as List<List<List<LinkedTreeMap<String, String>>>>,
        pointAdapter,
    )

    private val simplifiedCompleteRoutePoints = decodeArrays3(
        Gson().fromJson(
            FileUtils.loadJsonFixture("multileg_route_simplified_all_points_per_step.json"),
            List::class.java,
        ) as List<List<List<LinkedTreeMap<String, String>>>>,
        pointAdapter,
    )

    private val postManeuverFramingPoints = decodeArrays3(
        Gson().fromJson(
            FileUtils.loadJsonFixture("multileg_route_post_maneuver_framing_geometry.json"),
            List::class.java,
        ) as List<List<List<LinkedTreeMap<String, String>>>>,
        pointAdapter,
    )

    private val averageIntersectionDistancesOnRoute = decodeArrays2(
        Gson().fromJson(
            FileUtils.loadJsonFixture("multileg_route_average_intersection_distances.json"),
            List::class.java,
        ) as List<List<Double>>,
        doubleAdapter,
    )

    private val defaultFocalPoint = FollowingFrameOptions.FocalPoint(0.5, 1.0)

    @Test
    fun `test processRoutePoints`() {
        val expected = completeRoutePoints

        val actual = processRoutePoints(multiLegRoute)

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test processRouteForPostManeuverFramingGeometry - disable`() {
        val expected: List<List<List<Point>>> = emptyList()

        val actual = processRouteForPostManeuverFramingGeometry(
            enabled = false,
            distanceToCoalesceCompoundManeuvers = 150.0,
            distanceToFrameAfterManeuver = 100.0,
            route = multiLegRoute,
            completeRoutePoints = completeRoutePoints,
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test processRouteForPostManeuverFramingGeometry - enabled`() {
        val expected = postManeuverFramingPoints

        val actual = processRouteForPostManeuverFramingGeometry(
            enabled = true,
            distanceToCoalesceCompoundManeuvers = 150.0,
            distanceToFrameAfterManeuver = 100.0,
            route = multiLegRoute,
            completeRoutePoints = completeRoutePoints,
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test processRouteIntersections - disabled`() {
        val expected: List<List<Double>> = emptyList()

        val actual = processRouteIntersections(
            enabled = false,
            minimumMetersForIntersectionDensity = 20.0,
            route = multiLegRoute,
            completeRoutePoints = completeRoutePoints,
        )

        assertArrays2(expected, actual, doubleAdapter)
    }

    @Test
    fun `test processRouteIntersections - enabled`() {
        val expected = averageIntersectionDistancesOnRoute

        val actual = processRouteIntersections(
            enabled = true,
            minimumMetersForIntersectionDensity = 20.0,
            route = multiLegRoute,
            completeRoutePoints = completeRoutePoints,
        )

        assertArrays2(expected, actual, doubleAdapter)
    }

    @Test
    fun `test simplifyCompleteRoutePoints - disabled`() {
        val expected: List<List<List<Point>>> = completeRoutePoints

        val actual = simplifyCompleteRoutePoints(
            enabled = false,
            simplificationFactor = 25,
            completeRoutePoints = completeRoutePoints,
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test simplifyCompleteRoutePoints - enabled`() {
        val expected: List<List<List<Point>>> = simplifiedCompleteRoutePoints

        val actual = simplifyCompleteRoutePoints(
            enabled = true,
            simplificationFactor = 25,
            completeRoutePoints = completeRoutePoints,
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test simplifyCompleteRoutePoints - enabled, factor zero`() {
        val expected: List<List<List<Point>>> = completeRoutePoints

        val actual = simplifyCompleteRoutePoints(
            enabled = true,
            simplificationFactor = 0,
            completeRoutePoints = completeRoutePoints,
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test simplifyCompleteRoutePoints - enabled, factor negative`() {
        val expected: List<List<List<Point>>> = completeRoutePoints

        val actual = simplifyCompleteRoutePoints(
            enabled = true,
            simplificationFactor = -2,
            completeRoutePoints = completeRoutePoints,
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test isFramingManeuver - pitch near maneuver, false`() {
        val maneuvers = listOf("continue", "merge", "on ramp", "off ramp", "fork")
        maneuvers.forEach {
            val actual = isFramingManeuver(
                routeProgressWith(
                    upcomingManeuverType = it,
                    distanceToUpcomingManeuver = 150f,
                ),
                FollowingFrameOptions(),
            )

            assertEquals(
                "Camera pitch should not update to 0 for maneuver `$it`",
                false,
                actual,
            )
        }
    }

    @Test
    fun `test isFramingManeuver - pitch near maneuver disabled`() {
        val expected = false

        val actual = isFramingManeuver(
            routeProgressWith(
                upcomingManeuverType = "fork",
                distanceToUpcomingManeuver = 150f,
            ),
            FollowingFrameOptions().apply {
                pitchNearManeuvers.enabled = false
                pitchNearManeuvers.triggerDistanceFromManeuver = 180.0
                pitchNearManeuvers.excludedManeuvers = emptyList()
            },
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `test isFramingManeuver - pitch near maneuver enabled, not reached`() {
        val expected = false

        val actual = isFramingManeuver(
            routeProgressWith(
                upcomingManeuverType = "fork",
                distanceToUpcomingManeuver = 200f,
            ),
            FollowingFrameOptions().apply {
                pitchNearManeuvers.enabled = true
                pitchNearManeuvers.triggerDistanceFromManeuver = 180.0
                pitchNearManeuvers.excludedManeuvers = emptyList()
            },
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `test isFramingManeuver - pitch near maneuver enabled, reached`() {
        val expected = true

        val actual = isFramingManeuver(
            routeProgressWith(
                upcomingManeuverType = "fork",
                distanceToUpcomingManeuver = 150f,
            ),
            FollowingFrameOptions().apply {
                pitchNearManeuvers.enabled = true
                pitchNearManeuvers.triggerDistanceFromManeuver = 180.0
                pitchNearManeuvers.excludedManeuvers = emptyList()
            },
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `test isFramingManeuver - pitch near maneuver enabled, reached but excluded`() {
        val expected = false

        val actual = isFramingManeuver(
            routeProgressWith(
                upcomingManeuverType = "fork",
                distanceToUpcomingManeuver = 150f,
            ),
            FollowingFrameOptions().apply {
                defaultPitch = 45.0
                pitchNearManeuvers.enabled = true
                pitchNearManeuvers.triggerDistanceFromManeuver = 180.0
                pitchNearManeuvers.excludedManeuvers = listOf("fork")
            },
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `test getRemainingPointsOnRoute - empty current step`() {
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.153415, 38.771307),
            Point.fromLngLat(-77.153451, 38.771105),
            Point.fromLngLat(-77.153461, 38.770924),
            Point.fromLngLat(-77.153468, 38.77091),
        )

        val actual = getRemainingPointsOnRoute(
            simplifiedCompleteRoutePoints = completeRoutePoints,
            pointsToFrameOnCurrentStep = emptyList(),
            overviewMode = OverviewMode.ACTIVE_LEG,
            legIndex = 1,
            stepIndex = 4,
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getRemainingPointsOnRoute activeLeg`() {
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.123, 38.77091),
            Point.fromLngLat(-77.456, 38.77091),
            Point.fromLngLat(-77.153415, 38.771307),
            Point.fromLngLat(-77.153451, 38.771105),
            Point.fromLngLat(-77.153461, 38.770924),
            Point.fromLngLat(-77.153468, 38.77091),
        )

        val actual = getRemainingPointsOnRoute(
            simplifiedCompleteRoutePoints = completeRoutePoints,
            pointsToFrameOnCurrentStep = listOf(
                Point.fromLngLat(-77.123, 38.77091),
                Point.fromLngLat(-77.456, 38.77091),
            ),
            overviewMode = OverviewMode.ACTIVE_LEG,
            legIndex = 1,
            stepIndex = 4,
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getRemainingPointsOnRoute entireRoute on the last leg`() {
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.123, 38.77091),
            Point.fromLngLat(-77.456, 38.77091),
            Point.fromLngLat(-77.153415, 38.771307),
            Point.fromLngLat(-77.153451, 38.771105),
            Point.fromLngLat(-77.153461, 38.770924),
            Point.fromLngLat(-77.153468, 38.77091),
        )

        val actual = getRemainingPointsOnRoute(
            simplifiedCompleteRoutePoints = completeRoutePoints,
            pointsToFrameOnCurrentStep = listOf(
                Point.fromLngLat(-77.123, 38.77091),
                Point.fromLngLat(-77.456, 38.77091),
            ),
            overviewMode = OverviewMode.ENTIRE_ROUTE,
            legIndex = 1,
            stepIndex = 4,
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getRemainingPointsOnRoute entireRoute on non-last leg`() {
        val stepProgress: RouteStepProgress = mockk {
            every { stepIndex } returns 4
        }
        val legProgress: RouteLegProgress = mockk {
            every { legIndex } returns 0
        }

        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.123, 38.456),
            Point.fromLngLat(-77.456, 38.789),

            Point.fromLngLat(-77.168762, 38.777048),
            Point.fromLngLat(-77.168279, 38.777027),
            Point.fromLngLat(-77.166911, 38.776967),

            Point.fromLngLat(-77.166911, 38.776967),
            Point.fromLngLat(-77.166957, 38.776341),
            Point.fromLngLat(-77.166966, 38.776291),
            Point.fromLngLat(-77.166979, 38.776222),
            Point.fromLngLat(-77.167009, 38.776127),
            Point.fromLngLat(-77.167058, 38.776014),
            Point.fromLngLat(-77.167132, 38.77589),
            Point.fromLngLat(-77.167202, 38.7758),
            Point.fromLngLat(-77.167276, 38.775717),
        ) + completeRoutePoints[1].flatten()

        val actual = getRemainingPointsOnRoute(
            simplifiedCompleteRoutePoints = completeRoutePoints,
            pointsToFrameOnCurrentStep = listOf(
                Point.fromLngLat(-77.123, 38.456),
                Point.fromLngLat(-77.456, 38.789),
            ),
            overviewMode = OverviewMode.ENTIRE_ROUTE,
            legIndex = 0,
            stepIndex = 4,
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - empty`() {
        val mapSize = Size(1000f, 1000f)
        val padding = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        val expected = EdgeInsets(1000.0, 500.0, 0.0, 500.0)

        val actual = getMapAnchoredPaddingFromUserPadding(mapSize, padding, defaultFocalPoint)

        assertEquals(expected, actual)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - padded`() {
        val mapSize = Size(1000f, 1000f)
        val padding = EdgeInsets(300.0, 150.0, 200.0, 100.0)
        val expected = EdgeInsets(800.0, 525.0, 200.0, 475.0)

        val actual = getMapAnchoredPaddingFromUserPadding(mapSize, padding, defaultFocalPoint)

        assertEquals(expected, actual)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - invalid horizontally`() {
        val mapSize = Size(1000f, 1000f)
        val expected = EdgeInsets(0.0, 0.0, 0.0, 0.0)

        val padding1 = EdgeInsets(0.0, 1100.0, 0.0, 0.0)
        val actual1 = getMapAnchoredPaddingFromUserPadding(mapSize, padding1, defaultFocalPoint)
        assertEquals(expected, actual1)

        val padding2 = EdgeInsets(0.0, 0.0, 0.0, 1100.0)
        val actual2 = getMapAnchoredPaddingFromUserPadding(mapSize, padding2, defaultFocalPoint)
        assertEquals(expected, actual2)

        val padding3 = EdgeInsets(0.0, 600.0, 0.0, 600.0)
        val actual3 = getMapAnchoredPaddingFromUserPadding(mapSize, padding3, defaultFocalPoint)
        assertEquals(expected, actual3)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - invalid vertically`() {
        val mapSize = Size(1000f, 1000f)
        val expected = EdgeInsets(0.0, 0.0, 0.0, 0.0)

        val padding1 = EdgeInsets(1100.0, 0.0, 0.0, 0.0)
        val actual1 = getMapAnchoredPaddingFromUserPadding(mapSize, padding1, defaultFocalPoint)
        assertEquals(expected, actual1)

        val padding2 = EdgeInsets(0.0, 0.0, 1100.0, 0.0)
        val actual2 = getMapAnchoredPaddingFromUserPadding(mapSize, padding2, defaultFocalPoint)
        assertEquals(expected, actual2)

        val padding3 = EdgeInsets(600.0, 0.0, 600.0, 0.0)
        val actual3 = getMapAnchoredPaddingFromUserPadding(mapSize, padding3, defaultFocalPoint)
        assertEquals(expected, actual3)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - matches map size`() {
        val mapSize = Size(1000f, 1000f)

        val padding1 = EdgeInsets(250.0, 300.0, 750.0, 0.0)
        val expected1 = EdgeInsets(250.0, 650.0, 750.0, 350.0)
        val actual1 = getMapAnchoredPaddingFromUserPadding(mapSize, padding1, defaultFocalPoint)
        assertEquals(expected1, actual1)

        val padding2 = EdgeInsets(200.0, 250.0, 0.0, 750.0)
        val expected2 = EdgeInsets(1000.0, 250.0, 0.0, 750.0)
        val actual2 = getMapAnchoredPaddingFromUserPadding(mapSize, padding2, defaultFocalPoint)
        assertEquals(expected2, actual2)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - focal point offset`() {
        val mapSize = Size(1000f, 1000f)
        val padding = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        val expected = EdgeInsets(500.0, 500.0, 500.0, 500.0)
        val focalPoint = FollowingFrameOptions.FocalPoint(0.5, 0.5)

        val actual = getMapAnchoredPaddingFromUserPadding(mapSize, padding, focalPoint)

        assertEquals(expected, actual)
    }

    @Test
    fun `test getScreenBoxForFraming`() {
        val mapSize = Size(1000f, 1000f)
        val padding = EdgeInsets(300.0, 150.0, 200.0, 100.0)
        val expected = ScreenBox(ScreenCoordinate(150.0, 300.0), ScreenCoordinate(900.0, 800.0))

        val actual = getScreenBoxForFraming(mapSize, padding)

        assertEquals(expected, actual)
    }

    @Test
    fun `test getSmootherBearingForMap - disabled`() {
        val expected = 95.0

        val actual = getSmootherBearingForMap(
            enabled = false,
            bearingDiffMax = 10.0,
            currentMapCameraBearing = 30.0,
            pointsForBearing = listOf(
                Point.fromLngLat(-77.456, 38.77091),
                Point.fromLngLat(-77.123, 38.77091),
            ),
            vehicleBearing = 95.0,
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test getSmootherBearingForMap - enabled, within threshold`() {
        val expected = 89.89573618159727

        val actual = getSmootherBearingForMap(
            enabled = true,
            bearingDiffMax = 10.0,
            currentMapCameraBearing = 30.0,
            pointsForBearing = listOf(
                Point.fromLngLat(-77.456, 38.77091),
                Point.fromLngLat(-77.123, 38.77091),
            ),
            vehicleBearing = 95.0,
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test getSmootherBearingForMap - enabled, outside of threshold, negative diff`() {
        val expected = 100.0

        val actual = getSmootherBearingForMap(
            enabled = true,
            bearingDiffMax = 10.0,
            currentMapCameraBearing = 120.0,
            pointsForBearing = listOf(
                Point.fromLngLat(-77.456, 38.77091),
                Point.fromLngLat(-77.123, 38.77091),
            ),
            vehicleBearing = 110.0,
        )

        assertEquals(expected, actual, 0.0000001)
    }
}
