package com.mapbox.navigation.ui.maps.camera.data

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class MapboxFollowingCameraFramingStrategyTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val pointAdapter = PointArrayTestAdapter()
    private val doubleAdapter = DoubleArrayTestAdapter()
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
    private val sut = MapboxFollowingCameraFramingStrategy

    @Test
    fun `test getPointsToFrameOnCurrentStep, intersections disabled, nothing traveled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { distanceTraveled } returns 0f
            every { distanceRemaining } returns 93.4f
            every { stepPoints } returns listOf(
                Point.fromLngLat(-77.157347, 38.783004),
                Point.fromLngLat(-77.157471, 38.78217),
            )
            every { stepIndex } returns 0
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 0
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.157347, 38.783004),
            Point.fromLngLat(-77.157471, 38.78217),
        )
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns legProgress
        }

        val actual = sut.getPointsToFrameOnCurrentStep(
            routeProgress = routeProgress,
            followingFrameOptions = FollowingFrameOptions().apply {
                intersectionDensityCalculation.enabled = false
                intersectionDensityCalculation.averageDistanceMultiplier = 7.0
            },
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
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
                Point.fromLngLat(-77.157471, 38.78217),
            )
            every { stepIndex } returns 0
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 0
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.15743994716026, 38.78237885723322),
            Point.fromLngLat(-77.157471, 38.78217),
        )
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns legProgress
        }

        val actual = sut.getPointsToFrameOnCurrentStep(
            routeProgress = routeProgress,
            followingFrameOptions = FollowingFrameOptions().apply {
                intersectionDensityCalculation.enabled = false
                intersectionDensityCalculation.averageDistanceMultiplier = 7.0
            },
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
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
                Point.fromLngLat(-77.168762, 38.777048),
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
            Point.fromLngLat(-77.168762, 38.777048),
        )
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns legProgress
        }

        val actual = sut.getPointsToFrameOnCurrentStep(
            routeProgress = routeProgress,
            followingFrameOptions = FollowingFrameOptions().apply {
                intersectionDensityCalculation.enabled = false
                intersectionDensityCalculation.averageDistanceMultiplier = 7.0
            },
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
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
                Point.fromLngLat(-77.168762, 38.777048),
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
            Point.fromLngLat(-77.16853194636113, 38.77703799791736),
        )
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns legProgress
        }

        val actual = sut.getPointsToFrameOnCurrentStep(
            routeProgress = routeProgress,
            followingFrameOptions = FollowingFrameOptions().apply {
                intersectionDensityCalculation.enabled = true
                intersectionDensityCalculation.averageDistanceMultiplier = 0.5
            },
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
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
                Point.fromLngLat(-77.157471, 38.78217),
            )
            every { stepIndex } returns 0
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 0
        }
        val expected: List<Point> = emptyList()
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns legProgress
        }

        val actual = sut.getPointsToFrameOnCurrentStep(
            routeProgress = routeProgress,
            followingFrameOptions = FollowingFrameOptions().apply {
                intersectionDensityCalculation.enabled = false
                intersectionDensityCalculation.averageDistanceMultiplier = 7.0
            },
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
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
                Point.fromLngLat(22.0, 13.0),
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
            Point.fromLngLat(21.0, 12.0),
        )
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns legProgress
        }

        val actual = sut.getPointsToFrameOnCurrentStep(
            routeProgress = routeProgress,
            followingFrameOptions = FollowingFrameOptions().apply {
                intersectionDensityCalculation.enabled = false
                intersectionDensityCalculation.averageDistanceMultiplier = 7.0
            },
            averageIntersectionDistancesOnRoute = averageIntersectionDistancesOnRoute,
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameAfterCurrentManeuver - disabled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { stepIndex } returns 1
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 1
        }
        val expected: List<Point> = emptyList()
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns legProgress
        }

        val actual = sut.getPointsToFrameAfterCurrentManeuver(
            routeProgress = routeProgress,
            followingFrameOptions = FollowingFrameOptions().apply {
                frameGeometryAfterManeuver.enabled = false
            },
            postManeuverFramingPoints = postManeuverFramingPoints,
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameAfterCurrentManeuver - enabled, empty maneuvers`() {
        val stepProgress: RouteStepProgress = mockk {
            every { stepIndex } returns 1
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 1
        }
        val expected: List<Point> = emptyList()
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns legProgress
        }

        val actual = sut.getPointsToFrameAfterCurrentManeuver(
            routeProgress = routeProgress,
            followingFrameOptions = FollowingFrameOptions().apply {
                frameGeometryAfterManeuver.enabled = true
            },
            postManeuverFramingPoints = emptyList(),
        )

        assertArrays1(expected, actual, pointAdapter)
    }

    @Test
    fun `test getPointsToFrameAfterCurrentManeuver - enabled`() {
        val stepProgress: RouteStepProgress = mockk {
            every { stepIndex } returns 2
        }
        val legProgress: RouteLegProgress = mockk {
            every { currentStepProgress } returns stepProgress
            every { legIndex } returns 1
        }
        val expected: List<Point> = listOf(
            Point.fromLngLat(-77.168728, 38.777728),
            Point.fromLngLat(-77.168619, 38.777744),
            Point.fromLngLat(-77.168499, 38.777768),
            Point.fromLngLat(-77.167878, 38.777921),
            Point.fromLngLat(-77.1676211, 38.7779783),
        )
        val routeProgress: RouteProgress = mockk {
            every { currentLegProgress } returns legProgress
        }

        val actual = sut.getPointsToFrameAfterCurrentManeuver(
            routeProgress = routeProgress,
            followingFrameOptions = FollowingFrameOptions().apply {
                frameGeometryAfterManeuver.enabled = true
            },
            postManeuverFramingPoints = postManeuverFramingPoints,
        )

        assertArrays1(expected, actual, pointAdapter)
    }
}
