package com.mapbox.navigation.core.navigator

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RouteStepProgressFactory.buildRouteStepProgressObject
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigator.MatchedRoadObjectLocation
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.RoadObject
import com.mapbox.navigator.RoadObjectMetadata
import com.mapbox.navigator.RoadObjectProvider
import com.mapbox.navigator.RouteAlertLocation
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.SpeedLimitSign
import com.mapbox.navigator.SpeedLimitUnit
import com.mapbox.navigator.UpcomingRouteAlert
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

// https://github.com/mapbox/mapbox-navigation-android/issues/4492
@Ignore
class NavigatorMapperTest {

    private val enhancedLocation: Location = mockk(relaxed = true)
    private val keyPoints: List<Location> = mockk(relaxed = true)
    private val route: DirectionsRoute = mockk(relaxed = true)
    private val shape: Geometry = Point.fromLngLat(LONGITUDE, LATITUDE)

    @Test
    fun `map matcher result sanity`() {
        val tripStatus = TripStatus(
            route,
            mockk {
                every { offRoadProba } returns 0f
                every { speedLimit } returns createSpeedLimit()
                every { mapMatcherOutput } returns mockk {
                    every { isTeleport } returns false
                    every { matches } returns listOf(
                        mockk {
                            every { proba } returns 1f
                        }
                    )
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = false,
            offRoadProbability = 0f,
            isTeleport = false,
            speedLimit = SpeedLimit(
                10,
                com.mapbox.navigation.base.speed.model.SpeedLimitUnit.KILOMETRES_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD
            ),
            roadEdgeMatchProbability = 1f
        )

        val result = tripStatus.getMapMatcherResult(enhancedLocation, keyPoints)

        assertEquals(expected, result)
    }

    @Test
    fun `map matcher result when close to being off road`() {
        val tripStatus = TripStatus(
            route,
            mockk {
                every { offRoadProba } returns 0.5f
                every { speedLimit } returns createSpeedLimit()
                every { mapMatcherOutput } returns mockk {
                    every { isTeleport } returns false
                    every { matches } returns listOf(
                        mockk {
                            every { proba } returns 1f
                        }
                    )
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = false,
            offRoadProbability = 0.5f,
            isTeleport = false,
            speedLimit = SpeedLimit(
                10,
                com.mapbox.navigation.base.speed.model.SpeedLimitUnit.KILOMETRES_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD
            ),
            roadEdgeMatchProbability = 1f
        )

        val result = tripStatus.getMapMatcherResult(enhancedLocation, keyPoints)

        assertEquals(expected, result)
    }

    @Test
    fun `map matcher result when off road`() {
        val tripStatus = TripStatus(
            route,
            mockk {
                every { offRoadProba } returns 0.500009f
                every { speedLimit } returns createSpeedLimit()
                every { mapMatcherOutput } returns mockk {
                    every { isTeleport } returns false
                    every { matches } returns listOf(
                        mockk {
                            every { proba } returns 1f
                        }
                    )
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = true,
            offRoadProbability = 0.500009f,
            isTeleport = false,
            speedLimit = SpeedLimit(
                10,
                com.mapbox.navigation.base.speed.model.SpeedLimitUnit.KILOMETRES_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD
            ),
            roadEdgeMatchProbability = 1f
        )

        val result = tripStatus.getMapMatcherResult(enhancedLocation, keyPoints)

        assertEquals(expected, result)
    }

    @Test
    fun `map matcher result teleport`() {
        val tripStatus = TripStatus(
            route,
            mockk {
                every { offRoadProba } returns 0f
                every { speedLimit } returns createSpeedLimit()
                every { mapMatcherOutput } returns mockk {
                    every { isTeleport } returns true
                    every { matches } returns listOf(
                        mockk {
                            every { proba } returns 1f
                        }
                    )
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = false,
            offRoadProbability = 0f,
            isTeleport = true,
            speedLimit = SpeedLimit(
                10,
                com.mapbox.navigation.base.speed.model.SpeedLimitUnit.KILOMETRES_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD
            ),
            roadEdgeMatchProbability = 1f
        )

        val result = tripStatus.getMapMatcherResult(enhancedLocation, keyPoints)

        assertEquals(expected, result)
    }

    @Test
    fun `map matcher result no edge matches`() {
        val tripStatus = TripStatus(
            route,
            mockk {
                every { offRoadProba } returns 1f
                every { speedLimit } returns createSpeedLimit()
                every { mapMatcherOutput } returns mockk {
                    every { isTeleport } returns false
                    every { matches } returns listOf()
                }
            }
        )
        val expected = MapMatcherResult(
            enhancedLocation,
            keyPoints,
            isOffRoad = true,
            offRoadProbability = 1f,
            isTeleport = false,
            speedLimit = SpeedLimit(
                10,
                com.mapbox.navigation.base.speed.model.SpeedLimitUnit.KILOMETRES_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD
            ),
            roadEdgeMatchProbability = 0f
        )

        val result = tripStatus.getMapMatcherResult(enhancedLocation, keyPoints)

        assertEquals(expected, result)
    }

    @Test
    fun `route progress is null when route is null`() {
        val navigationStatus: NavigationStatus = mockk {
            every { routeState } returns RouteState.TRACKING
        }

        val routeProgress = getRouteProgressFrom(
            null,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0
        )

        assertNull(routeProgress)
    }

    @Test
    fun `route progress is null when status is invalid`() {
        every { navigationStatus.routeState } returns RouteState.INVALID

        val routeProgress = getRouteProgressFrom(
            directionsRoute,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0
        )

        assertNull(routeProgress)
    }

    @Test
    fun `route progress minimum requirements`() {
        val routeProgress = getRouteProgressFrom(
            directionsRoute,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0
        )

        assertNotNull(routeProgress)
    }

    @Test
    fun `route progress state stale`() {
        every { navigationStatus.stale } returns true
        val routeProgress = getRouteProgressFrom(
            directionsRoute,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0
        )

        assertTrue(routeProgress!!.stale)
    }

    @Test
    fun `route progress state not stale`() {
        every { navigationStatus.stale } returns false
        val routeProgress = getRouteProgressFrom(
            directionsRoute,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0
        )

        assertFalse(routeProgress!!.stale)
    }

    @Test
    fun `route init info is null when route info is null`() {
        assertNull(getRouteInitInfo(null))
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `step progress correctly created`() {
        val stepProgress = buildRouteStepProgressObject(
            1,
            1,
            null,
            directionsRoute.legs()!![0].steps()!![1],
            PolylineUtils.decode("sla~hA|didrCoDvx@", 6),
            15f,
            10f,
            50f,
            300.0 / 1000.0
        )

        val routeProgress = getRouteProgressFrom(
            directionsRoute,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0
        )

        assertEquals(stepProgress, routeProgress!!.currentLegProgress!!.currentStepProgress)
    }

    @Test
    fun `alerts are present in the route init info is they are delivered from native`() {
        val routeInfo = RouteInfo(1, listOf(tunnel.toUpcomingRouteAlert()))

        val result = getRouteInitInfo(routeInfo)!!

        assertEquals(1, result.roadObjects.size)
        assertEquals(RoadObjectType.TUNNEL, result.roadObjects[0].roadObject.objectType)
    }

    @Test
    fun `parsing multiple tunnel entrances returns multiple alerts`() {
        val firstEntrance = tunnel.toUpcomingRouteAlert(100.0)
        val secondEntrance = tunnel.toUpcomingRouteAlert(200.0)
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            firstEntrance,
            secondEntrance
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0
        )
        val upcomingRouteAlerts = routeProgress!!.upcomingRoadObjects

        assertEquals(
            firstEntrance.distanceToStart,
            upcomingRouteAlerts[0].distanceToStart!!,
            .0001
        )
        assertEquals(
            secondEntrance.distanceToStart,
            upcomingRouteAlerts[1].distanceToStart!!,
            .0001
        )
    }

    private fun createSpeedLimit(): com.mapbox.navigator.SpeedLimit {
        return com.mapbox.navigator.SpeedLimit(
            10,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.MUTCD
        )
    }

    private val directionsRoute = DirectionsRoute.fromJson(
        FileUtils.loadJsonFixture("multileg_route.json")
    )

    private val navigationStatus: NavigationStatus = mockk {
        every { intersectionIndex } returns 1
        every { stepIndex } returns 1
        every { legIndex } returns 0
        every { activeGuidanceInfo?.routeProgress?.remainingDistance } returns 80.0
        every { activeGuidanceInfo?.routeProgress?.remainingDuration } returns 10000
        every { activeGuidanceInfo?.routeProgress?.distanceTraveled } returns 10.0
        every { activeGuidanceInfo?.routeProgress?.fractionTraveled } returns 1.0
        every { activeGuidanceInfo?.legProgress?.remainingDistance } returns 80.0
        every { activeGuidanceInfo?.legProgress?.remainingDuration } returns 10000
        every { activeGuidanceInfo?.legProgress?.distanceTraveled } returns 10.0
        every { activeGuidanceInfo?.legProgress?.fractionTraveled } returns 1.0
        every { activeGuidanceInfo?.stepProgress?.remainingDistance } returns 15.0
        every { activeGuidanceInfo?.stepProgress?.remainingDuration } returns 300
        every { activeGuidanceInfo?.stepProgress?.distanceTraveled } returns 10.0
        every { activeGuidanceInfo?.stepProgress?.fractionTraveled } returns 50.0
        every { routeState } returns RouteState.TRACKING
        every { stale } returns false
        every { bannerInstruction } returns mockk {
            every { remainingStepDistance } returns 111f
            every { primary } returns mockk {
                every { components } returns listOf()
                every { degrees } returns 45
                every { drivingSide } returns "drivingSide"
                every { modifier } returns "modifier"
                every { text } returns "text"
                every { type } returns "type"
            }
            every { secondary } returns null
            every { sub } returns null
            every { index } returns 0
        }
        every { voiceInstruction } returns mockk {
            every { announcement } returns "announcement"
            every { remainingStepDistance } returns 111f
            every { ssmlAnnouncement } returns "ssmlAnnouncement"
        }
        every { inTunnel } returns true
        every { upcomingRouteAlerts } returns emptyList()
    }

    // https://github.com/mapbox/mapbox-navigation-android/issues/4492
    val routeAlertLocation: RouteAlertLocation = mockk()

    private val tunnel = RoadObject(
        ID,
        LENGTH,
        MatchedRoadObjectLocation.valueOf(routeAlertLocation),
        com.mapbox.navigator.RoadObjectType.TUNNEL,
        RoadObjectProvider.MAPBOX,
        RoadObjectMetadata.valueOf(com.mapbox.navigator.TunnelInfo("Ted Williams Tunnel"))
    )

    private fun RoadObject.toUpcomingRouteAlert(
        distanceToStart: Double = DISTANCE_TO_START
    ) = UpcomingRouteAlert(this, distanceToStart)

    companion object {
        private const val ID = "roadObjectId"
        private const val DISTANCE_TO_START = 1234.0
        private const val LENGTH = 456.0
        private const val LATITUDE = 5353.3
        private const val LONGITUDE = 2020.20
    }
}
