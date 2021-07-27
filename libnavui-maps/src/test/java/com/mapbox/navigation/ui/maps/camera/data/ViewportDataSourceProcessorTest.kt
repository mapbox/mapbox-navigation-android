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
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getMapAnchoredPaddingFromUserPadding
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getPitchFallbackFromRouteProgress
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getPointsToFrameAfterCurrentManeuver
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getPointsToFrameOnCurrentStep
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getRemainingPointsOnRoute
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getScreenBoxForFraming
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.getSmootherBearingForMap
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteForPostManeuverFramingGeometry
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRouteIntersections
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.processRoutePoints
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor.simplifyCompleteRoutePoints
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportDataSourceProcessorTest {

    private val pointAdapter =
        object : ArrayTestAdapter<Point, LinkedTreeMap<String, String>> {
            override fun encode(value: Point): String {
                return value.toJson()
            }

            override fun decode(value: LinkedTreeMap<String, String>): Point {
                val coordinates = value["coordinates"] as ArrayList<Double>
                return Point.fromLngLat(coordinates[0], coordinates[1])
            }

            override fun assertEqual(expected: Point, actual: Point) {
                assertEquals(expected.longitude(), actual.longitude(), 0.0000001)
                assertEquals(expected.latitude(), actual.latitude(), 0.0000001)
            }
        }

    private val doubleAdapter = object : ArrayTestAdapter<Double, Double> {
        override fun encode(value: Double): String {
            return value.toString()
        }

        override fun decode(value: Double): Double {
            return value
        }

        override fun assertEqual(expected: Double, actual: Double) {
            assertEquals(expected, actual, 0.0000001)
        }
    }

    private val multiLegRoute = DirectionsRoute.fromJson(
        FileUtils.loadJsonFixture("multileg_route.json")
    )

    private val completeRoutePoints = decodeArrays3(
        Gson().fromJson(
            FileUtils.loadJsonFixture("multileg_route_all_points_per_step.json"),
            List::class.java
        ) as List<List<List<LinkedTreeMap<String, String>>>>,
        pointAdapter
    )

    private val simplifiedCompleteRoutePoints = decodeArrays3(
        Gson().fromJson(
            FileUtils.loadJsonFixture("multileg_route_simplified_all_points_per_step.json"),
            List::class.java
        ) as List<List<List<LinkedTreeMap<String, String>>>>,
        pointAdapter
    )

    private val postManeuverFramingPoints = decodeArrays3(
        Gson().fromJson(
            FileUtils.loadJsonFixture("multileg_route_post_maneuver_framing_geometry.json"),
            List::class.java
        ) as List<List<List<LinkedTreeMap<String, String>>>>,
        pointAdapter
    )

    private val averageIntersectionDistancesOnRoute = decodeArrays2(
        Gson().fromJson(
            FileUtils.loadJsonFixture("multileg_route_average_intersection_distances.json"),
            List::class.java
        ) as List<List<Double>>,
        doubleAdapter
    )

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
            completeRoutePoints = completeRoutePoints
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
            completeRoutePoints = completeRoutePoints
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
            completeRoutePoints = completeRoutePoints
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
            completeRoutePoints = completeRoutePoints
        )

        assertArrays2(expected, actual, doubleAdapter)
    }

    @Test
    fun `test simplifyCompleteRoutePoints - disabled`() {
        val expected: List<List<List<Point>>> = completeRoutePoints

        val actual = simplifyCompleteRoutePoints(
            enabled = false,
            simplificationFactor = 25,
            completeRoutePoints = completeRoutePoints
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test simplifyCompleteRoutePoints - enabled`() {
        val expected: List<List<List<Point>>> = simplifiedCompleteRoutePoints

        val actual = simplifyCompleteRoutePoints(
            enabled = true,
            simplificationFactor = 25,
            completeRoutePoints = completeRoutePoints
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test simplifyCompleteRoutePoints - enabled, factor zero`() {
        val expected: List<List<List<Point>>> = completeRoutePoints

        val actual = simplifyCompleteRoutePoints(
            enabled = true,
            simplificationFactor = 0,
            completeRoutePoints = completeRoutePoints
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test simplifyCompleteRoutePoints - enabled, factor negative`() {
        val expected: List<List<List<Point>>> = completeRoutePoints

        val actual = simplifyCompleteRoutePoints(
            enabled = true,
            simplificationFactor = -2,
            completeRoutePoints = completeRoutePoints
        )

        assertArrays3(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameOnCurrentStep, intersections disabled, nothing traveled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { distanceTraveled } returns 0f
            every { distanceRemaining } returns 93.4f
            every { stepPoints } returns listOf(
                Point.fromLngLat(-77.157347, 38.783004),
                Point.fromLngLat(-77.157471, 38.78217)
            )
            every { stepIndex } returns 0
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 0
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.157347, 38.783004),
            Point.fromLngLat(-77.157471, 38.78217)
        )

        val actual = getPointsToFrameOnCurrentStep(
            intersectionDensityCalculationEnabled = false,
            intersectionDensityAverageDistanceMultiplier = 7.0,
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameOnCurrentStep, intersections disabled, portion traveled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { distanceTraveled } returns 70f
            every { distanceRemaining } returns 23.4f
            every { stepPoints } returns listOf(
                Point.fromLngLat(-77.157347, 38.783004),
                Point.fromLngLat(-77.157471, 38.78217)
            )
            every { stepIndex } returns 0
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 0
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.15743994716026, 38.78237885723322),
            Point.fromLngLat(-77.157471, 38.78217)
        )

        val actual = getPointsToFrameOnCurrentStep(
            intersectionDensityCalculationEnabled = false,
            intersectionDensityAverageDistanceMultiplier = 7.0,
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameOnCurrentStep, intersections disabled, leg traveled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { distanceTraveled } returns 60.4f
            every { distanceRemaining } returns 100.4f
            every { stepPoints } returns listOf(
                Point.fromLngLat(-77.166911, 38.776967),
                Point.fromLngLat(-77.168279, 38.777027),
                Point.fromLngLat(-77.168762, 38.777048)
            )
            every { stepIndex } returns 1
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 1
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.16760644581015, 38.77699750401471),
            Point.fromLngLat(-77.168279, 38.777027),
            Point.fromLngLat(-77.168762, 38.777048)
        )

        val actual = getPointsToFrameOnCurrentStep(
            intersectionDensityCalculationEnabled = false,
            intersectionDensityAverageDistanceMultiplier = 7.0,
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameOnCurrentStep, intersections enabled, portion traveled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { distanceTraveled } returns 60.4f
            every { distanceRemaining } returns 100.4f
            every { stepPoints } returns listOf(
                Point.fromLngLat(-77.166911, 38.776967),
                Point.fromLngLat(-77.168279, 38.777027),
                Point.fromLngLat(-77.168762, 38.777048)
            )
            every { stepIndex } returns 1
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 1
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.16760644581015, 38.77699750401471),
            Point.fromLngLat(-77.168279, 38.777027),
            Point.fromLngLat(-77.16853194636113, 38.77703799791736)
        )

        val actual = getPointsToFrameOnCurrentStep(
            intersectionDensityCalculationEnabled = true,
            intersectionDensityAverageDistanceMultiplier = 0.5,
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameOnCurrentStep, intersections disabled, fully traveled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { distanceTraveled } returns 93.4f
            every { distanceRemaining } returns 0f
            every { stepPoints } returns listOf(
                Point.fromLngLat(-77.157347, 38.783004),
                Point.fromLngLat(-77.157471, 38.78217)
            )
            every { stepIndex } returns 0
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 0
        }
        val expected: List<Point> = emptyList()

        val actual = getPointsToFrameOnCurrentStep(
            intersectionDensityCalculationEnabled = false,
            intersectionDensityAverageDistanceMultiplier = 7.0,
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameOnCurrentStep, intersections disabled, cloverleaf`() {
        val stepProgress: RouteStepProgress = mockk {
            every { distanceTraveled } returns 0f
            every { distanceRemaining } returns 20000000f
            every { stepPoints } returns listOf(
                Point.fromLngLat(20.0, 11.0),
                Point.fromLngLat(20.0, 12.0),
                Point.fromLngLat(21.0, 12.0),
                Point.fromLngLat(21.0, 9.0),
                Point.fromLngLat(19.0, 9.0),
                Point.fromLngLat(19.0, 13.0),
                Point.fromLngLat(22.0, 13.0)
            )
            every { stepIndex } returns 0
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 0
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(20.0, 11.0),
            Point.fromLngLat(20.0, 12.0),
            Point.fromLngLat(21.0, 12.0)
        )

        val actual = getPointsToFrameOnCurrentStep(
            intersectionDensityCalculationEnabled = false,
            intersectionDensityAverageDistanceMultiplier = 7.0,
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPitchFallbackFromRouteProgress - pitch near maneuver disabled`() {
        val expected = 45.0

        val actual = getPitchFallbackFromRouteProgress(
            pitchNearManeuversEnabled = false,
            triggerDistanceForPitchZero = 180.0,
            defaultPitch = 45.0,
            distanceRemainingOnStep = 150f
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test getPitchFallbackFromRouteProgress - pitch near maneuver enabled, not reached`() {
        val expected = 45.0

        val actual = getPitchFallbackFromRouteProgress(
            pitchNearManeuversEnabled = true,
            triggerDistanceForPitchZero = 180.0,
            defaultPitch = 45.0,
            distanceRemainingOnStep = 200f
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test getPitchFallbackFromRouteProgress - pitch near maneuver enabled, reached`() {
        val expected = 0.0

        val actual = getPitchFallbackFromRouteProgress(
            pitchNearManeuversEnabled = true,
            triggerDistanceForPitchZero = 180.0,
            defaultPitch = 45.0,
            distanceRemainingOnStep = 150f
        )

        assertEquals(expected, actual, 0.0000001)
    }

    @Test
    fun `test getPointsToFrameAfterCurrentManeuver - disabled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { stepIndex } returns 1
        }
        val legProgress: RouteLegProgress = mockk {
            every { legIndex } returns 1
        }
        val expected: List<Point> = emptyList()

        val actual = getPointsToFrameAfterCurrentManeuver(
            frameGeometryAfterManeuverEnabled = false,
            generatedPostManeuverFramingPoints = postManeuverFramingPoints,
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameAfterCurrentManeuver - enabled, empty maneuvers`() {
        val stepProgress: RouteStepProgress = mockk {
            every { stepIndex } returns 1
        }
        val legProgress: RouteLegProgress = mockk {
            every { legIndex } returns 1
        }
        val expected: List<Point> = emptyList()

        val actual = getPointsToFrameAfterCurrentManeuver(
            frameGeometryAfterManeuverEnabled = true,
            generatedPostManeuverFramingPoints = emptyList(),
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameAfterCurrentManeuver - enabled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { stepIndex } returns 2
        }
        val legProgress: RouteLegProgress = mockk {
            every { legIndex } returns 1
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.168728, 38.777728),
            Point.fromLngLat(-77.168619, 38.777744),
            Point.fromLngLat(-77.168499, 38.777768),
            Point.fromLngLat(-77.167878, 38.777921),
            Point.fromLngLat(-77.1676211, 38.7779783)
        )

        val actual = getPointsToFrameAfterCurrentManeuver(
            frameGeometryAfterManeuverEnabled = true,
            generatedPostManeuverFramingPoints = postManeuverFramingPoints,
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getRemainingPointsOnRoute - empty current step`() {
        val stepProgress: RouteStepProgress = mockk {
            every { stepIndex } returns 4
        }
        val legProgress: RouteLegProgress = mockk {
            every { legIndex } returns 1
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.153415, 38.771307),
            Point.fromLngLat(-77.153451, 38.771105),
            Point.fromLngLat(-77.153461, 38.770924),
            Point.fromLngLat(-77.153468, 38.77091)
        )

        val actual = getRemainingPointsOnRoute(
            simplifiedCompleteRoutePoints = completeRoutePoints,
            pointsToFrameOnCurrentStep = emptyList(),
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getRemainingPointsOnRoute`() {
        val stepProgress: RouteStepProgress = mockk {
            every { stepIndex } returns 4
        }
        val legProgress: RouteLegProgress = mockk {
            every { legIndex } returns 1
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.123, 38.77091),
            Point.fromLngLat(-77.456, 38.77091),
            Point.fromLngLat(-77.153415, 38.771307),
            Point.fromLngLat(-77.153451, 38.771105),
            Point.fromLngLat(-77.153461, 38.770924),
            Point.fromLngLat(-77.153468, 38.77091)
        )

        val actual = getRemainingPointsOnRoute(
            simplifiedCompleteRoutePoints = completeRoutePoints,
            pointsToFrameOnCurrentStep = listOf(
                Point.fromLngLat(-77.123, 38.77091),
                Point.fromLngLat(-77.456, 38.77091)
            ),
            currentLegProgress = legProgress,
            currentStepProgress = stepProgress
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - empty`() {
        val mapSize = Size(1000f, 1000f)
        val padding = EdgeInsets(0.0, 0.0, 0.0, 0.0)
        val expected = EdgeInsets(1000.0, 500.0, 0.0, 500.0)

        val actual = getMapAnchoredPaddingFromUserPadding(mapSize, padding)

        assertEquals(expected, actual)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - padded`() {
        val mapSize = Size(1000f, 1000f)
        val padding = EdgeInsets(300.0, 150.0, 200.0, 100.0)
        val expected = EdgeInsets(800.0, 525.0, 200.0, 475.0)

        val actual = getMapAnchoredPaddingFromUserPadding(mapSize, padding)

        assertEquals(expected, actual)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - invalid horizontally`() {
        val mapSize = Size(1000f, 1000f)

        val padding1 = EdgeInsets(0.0, 1100.0, 0.0, 0.0)
        val expected1 = EdgeInsets(500.0, 500.0, 500.0, 500.0)
        val actual1 = getMapAnchoredPaddingFromUserPadding(mapSize, padding1)
        assertEquals(expected1, actual1)

        val padding2 = EdgeInsets(0.0, 0.0, 0.0, 1100.0)
        val expected2 = EdgeInsets(500.0, 500.0, 500.0, 500.0)
        val actual2 = getMapAnchoredPaddingFromUserPadding(mapSize, padding2)
        assertEquals(expected2, actual2)

        val padding3 = EdgeInsets(0.0, 600.0, 0.0, 600.0)
        val expected3 = EdgeInsets(500.0, 500.0, 500.0, 500.0)
        val actual3 = getMapAnchoredPaddingFromUserPadding(mapSize, padding3)
        assertEquals(expected3, actual3)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - invalid vertically`() {
        val mapSize = Size(1000f, 1000f)

        val padding1 = EdgeInsets(1100.0, 0.0, 0.0, 0.0)
        val expected1 = EdgeInsets(500.0, 500.0, 500.0, 500.0)
        val actual1 = getMapAnchoredPaddingFromUserPadding(mapSize, padding1)
        assertEquals(expected1, actual1)

        val padding2 = EdgeInsets(0.0, 0.0, 1100.0, 0.0)
        val expected2 = EdgeInsets(500.0, 500.0, 500.0, 500.0)
        val actual2 = getMapAnchoredPaddingFromUserPadding(mapSize, padding2)
        assertEquals(expected2, actual2)

        val padding3 = EdgeInsets(600.0, 0.0, 600.0, 0.0)
        val expected3 = EdgeInsets(500.0, 500.0, 500.0, 500.0)
        val actual3 = getMapAnchoredPaddingFromUserPadding(mapSize, padding3)
        assertEquals(expected3, actual3)
    }

    @Test
    fun `test getMapAnchoredPaddingFromUserPadding - matches map size`() {
        val mapSize = Size(1000f, 1000f)

        val padding1 = EdgeInsets(250.0, 300.0, 750.0, 0.0)
        val expected1 = EdgeInsets(250.0, 650.0, 750.0, 350.0)
        val actual1 = getMapAnchoredPaddingFromUserPadding(mapSize, padding1)
        assertEquals(expected1, actual1)

        val padding2 = EdgeInsets(200.0, 250.0, 0.0, 750.0)
        val expected2 = EdgeInsets(1000.0, 250.0, 0.0, 750.0)
        val actual2 = getMapAnchoredPaddingFromUserPadding(mapSize, padding2)
        assertEquals(expected2, actual2)
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
                Point.fromLngLat(-77.123, 38.77091)
            ),
            vehicleBearing = 95.0
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
                Point.fromLngLat(-77.123, 38.77091)
            ),
            vehicleBearing = 95.0
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
                Point.fromLngLat(-77.123, 38.77091)
            ),
            vehicleBearing = 110.0
        )

        assertEquals(expected, actual, 0.0000001)
    }

    private fun <V, D> assertArrays1(
        expected: List<V>,
        actual: List<V>,
        adapter: ArrayTestAdapter<V, D>
    ) {
        assertTrue(expected.size == actual.size)
        expected.forEachIndexed { index, expectedValue ->
            val actualValue = actual[index]
            adapter.assertEqual(expectedValue, actualValue)
        }
    }

    private fun <V, D> assertArrays2(
        expected: List<List<V>>,
        actual: List<List<V>>,
        adapter: ArrayTestAdapter<V, D>
    ) {
        assertTrue(expected.size == actual.size)
        expected.forEachIndexed { index, nestedExpected ->
            assertArrays1(nestedExpected, actual[index], adapter)
        }
    }

    private fun <V, D> assertArrays3(
        expected: List<List<List<V>>>,
        actual: List<List<List<V>>>,
        adapter: ArrayTestAdapter<V, D>
    ) {
        assertTrue(expected.size == actual.size)
        expected.forEachIndexed { index, nestedExpected ->
            assertArrays2(nestedExpected, actual[index], adapter)
        }
    }

    private fun <V, D> decodeArrays1(list: List<D>, adapter: ArrayTestAdapter<V, D>): List<V> {
        return list.map { value ->
            adapter.decode(value)
        }
    }

    private fun <V, D> decodeArrays2(
        list: List<List<D>>,
        adapter: ArrayTestAdapter<V, D>
    ): List<List<V>> {
        return list.map { nestedList ->
            decodeArrays1(nestedList, adapter)
        }
    }

    private fun <V, D> decodeArrays3(
        list: List<List<List<D>>>,
        adapter: ArrayTestAdapter<V, D>
    ): List<List<List<V>>> {
        return list.map { nestedList ->
            decodeArrays2(nestedList, adapter)
        }
    }

    /* below functions are used to update test suite if necessary */

    private fun <V, D> encodeArrays1(list: List<V>, adapter: ArrayTestAdapter<V, D>): String {
        val builder = StringBuilder()
        builder.append("[")
        list.forEachIndexed { index, value ->
            if (index > 0) {
                builder.append(",")
            }
            builder.append(adapter.encode(value))
        }
        builder.append("]")
        return builder.toString()
    }

    private fun <V, D> encodeArrays2(list: List<List<V>>, adapter: ArrayTestAdapter<V, D>): String {
        val builder = StringBuilder()
        builder.append("[")
        list.forEachIndexed { index, nestedList ->
            if (index > 0) {
                builder.append(",")
            }
            builder.append(encodeArrays1(nestedList, adapter))
        }
        builder.append("]")
        return builder.toString()
    }

    private fun <V, D> encodeArrays3(
        list: List<List<List<V>>>,
        adapter: ArrayTestAdapter<V, D>
    ): String {
        val builder = StringBuilder()
        builder.append("[")
        list.forEachIndexed { index, nestedList ->
            if (index > 0) {
                builder.append(",")
            }
            builder.append(encodeArrays2(nestedList, adapter))
        }
        builder.append("]")
        return builder.toString()
    }
}

interface ArrayTestAdapter<V, D> {
    fun encode(value: V): String
    fun decode(value: D): V
    fun assertEqual(expected: V, actual: V)
}
