package com.mapbox.navigation.core.navigator

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RoadFactory
import com.mapbox.navigation.base.internal.factory.RouteLegProgressFactory
import com.mapbox.navigation.base.internal.factory.RouteProgressFactory
import com.mapbox.navigation.base.internal.factory.RouteStepProgressFactory.buildRouteStepProgressObject
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigator.BannerInstruction
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
import com.mapbox.navigator.VoiceInstruction
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class NavigatorMapperTest {

    private val enhancedLocation: Location = mockk(relaxed = true)
    private val keyPoints: List<Location> = mockk(relaxed = true)
    private val route: DirectionsRoute = mockk(relaxed = true)

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `route progress result sanity`() {
        val bannerInstructions = nativeBannerInstructions.mapToDirectionsApi()
        val expected = RouteProgressFactory.buildRouteProgressObject(
            route = directionsRoute,
            bannerInstructions = bannerInstructions,
            voiceInstructions = navigationStatus.voiceInstruction?.mapToDirectionsApi(),
            currentState = navigationStatus.routeState.convertState(),
            currentLegProgress = RouteLegProgressFactory.buildRouteLegProgressObject(
                legIndex = navigationStatus.legIndex,
                routeLeg = directionsRoute.legs()!!.first(),
                distanceRemaining = 180f,
                durationRemaining = 2.0,
                distanceTraveled = 20f,
                fractionTraveled = 2f,
                upcomingStep = directionsRoute.legs()!!.first().steps()!![2],
                currentStepProgress = buildRouteStepProgressObject(
                    stepIndex = 1,
                    intersectionIndex = 1,
                    instructionIndex = 1,
                    step = directionsRoute.legs()!!.first().steps()!![1],
                    stepPoints = PolylineUtils.decode(
                        directionsRoute.legs()!!.first().steps()!![1].geometry()!!,
                        6
                    ),
                    distanceRemaining = 35f,
                    distanceTraveled = 30f,
                    fractionTraveled = 50f,
                    durationRemaining = 3.0
                )
            ),
            distanceRemaining = 80f,
            durationRemaining = 1.0,
            distanceTraveled = 10f,
            fractionTraveled = 1f,
            upcomingStepPoints = PolylineUtils.decode(
                directionsRoute.legs()!!.first().steps()!![2].geometry()!!,
                6
            ),
            inTunnel = true,
            stale = true,
            remainingWaypoints = 1,
            upcomingRoadObjects = listOf(),
        )

        val result = getRouteProgressFrom(
            directionsRoute,
            navigationStatus,
            remainingWaypoints = 1,
            bannerInstructions,
            instructionIndex = 1,
            lastVoiceInstruction = null,
        )

        assertEquals(expected, result)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `location matcher result sanity`() {
        val road: Road = RoadFactory.buildRoadObject(navigationStatus)
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
                every { layer } returns null
                every { roadName } returns "Central Avenue"
                every { shieldName } returns "I880"
            }
        )
        val expected = LocationMatcherResult(
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
            roadEdgeMatchProbability = 1f,
            zLevel = null,
            road = road
        )

        val result = tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)

        assertEquals(expected, result)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `location matcher result when close to being off road`() {
        val road: Road = RoadFactory.buildRoadObject(navigationStatus)
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
                every { layer } returns null
                every { roadName } returns "Central Avenue"
                every { shieldName } returns "I880"
            }
        )
        val expected = LocationMatcherResult(
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
            roadEdgeMatchProbability = 1f,
            zLevel = null,
            road = road
        )

        val result = tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)

        assertEquals(expected, result)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `location matcher result when off road`() {
        val road: Road = RoadFactory.buildRoadObject(navigationStatus)
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
                every { layer } returns null
                every { roadName } returns "Central Avenue"
                every { shieldName } returns "I880"
            }
        )
        val expected = LocationMatcherResult(
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
            roadEdgeMatchProbability = 1f,
            zLevel = null,
            road = road
        )

        val result = tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)

        assertEquals(expected, result)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `location matcher result teleport`() {
        val road: Road = RoadFactory.buildRoadObject(navigationStatus)
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
                every { layer } returns null
                every { roadName } returns "Central Avenue"
                every { shieldName } returns "I880"
            }
        )
        val expected = LocationMatcherResult(
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
            roadEdgeMatchProbability = 1f,
            zLevel = null,
            road = road
        )

        val result = tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)

        assertEquals(expected, result)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `location matcher result no edge matches`() {
        val navigationStatus = mockk<NavigationStatus> {
            every { offRoadProba } returns 1f
            every { speedLimit } returns createSpeedLimit()
            every { mapMatcherOutput } returns mockk {
                every { isTeleport } returns false
                every { matches } returns listOf()
            }
            every { layer } returns null
            every { roadName } returns navigationStatus.roadName
            every { shieldName } returns navigationStatus.shieldName
            every { shields } returns navigationStatus.shields
            every { imageBaseurl } returns navigationStatus.imageBaseurl
        }
        val road: Road = RoadFactory.buildRoadObject(navigationStatus)
        val tripStatus = TripStatus(
            route,
            navigationStatus
        )
        val expected = LocationMatcherResult(
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
            roadEdgeMatchProbability = 0f,
            zLevel = null,
            road = road
        )

        val result = tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)

        assertEquals(expected, result)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `location matcher result zLevel`() {
        val road: Road = RoadFactory.buildRoadObject(navigationStatus)
        val tripStatus = TripStatus(
            route,
            mockk {
                every { offRoadProba } returns 1f
                every { speedLimit } returns createSpeedLimit()
                every { mapMatcherOutput } returns mockk {
                    every { isTeleport } returns false
                    every { matches } returns listOf(
                        mockk {
                            every { proba } returns 1f
                        }
                    )
                }
                every { layer } returns 2
                every { roadName } returns "Central Avenue"
                every { shieldName } returns "I880"
            }
        )
        val expected = LocationMatcherResult(
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
            roadEdgeMatchProbability = 1f,
            zLevel = 2,
            road = road
        )

        val result = tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)

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
            0,
            mockk(relaxed = true),
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
            0,
            mockk(relaxed = true),
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
            0,
            mockk(relaxed = true),
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
            0,
            lastVoiceInstruction = mockk(relaxed = true),
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
            0,
            lastVoiceInstruction = mockk(relaxed = true),
        )

        assertFalse(routeProgress!!.stale)
    }

    @Test
    fun `route init info is null when route info is null`() {
        assertNull(getRouteInitInfo(null))
    }

    @Test
    @Ignore("https://github.com/mapbox/mapbox-navigation-native/issues/3456")
    fun `alerts are present in the route init info is they are delivered from native`() {
        val routeInfo = RouteInfo(1, listOf(tunnel.toUpcomingRouteAlert()))

        val result = getRouteInitInfo(routeInfo)!!

        assertEquals(1, result.roadObjects.size)
        assertEquals(RoadObjectType.TUNNEL, result.roadObjects[0].roadObject.objectType)
    }

    @Test
    @Ignore("https://github.com/mapbox/mapbox-navigation-native/issues/3456")
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
            0,
            mockk(relaxed = true),
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

    private val nativeBannerInstructions = mockk<BannerInstruction> {
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
        every { view } returns null
        every { index } returns 0
    }
    private val nativeVoiceInstructions = mockk<VoiceInstruction> {
        every { announcement } returns "announcement"
        every { remainingStepDistance } returns 111f
        every { ssmlAnnouncement } returns "ssmlAnnouncement"
    }
    private val navigationStatus: NavigationStatus = mockk {
        every { intersectionIndex } returns 1
        every { stepIndex } returns 1
        every { legIndex } returns 0
        every { activeGuidanceInfo?.routeProgress?.remainingDistance } returns 80.0
        every { activeGuidanceInfo?.routeProgress?.remainingDuration } returns 1000
        every { activeGuidanceInfo?.routeProgress?.distanceTraveled } returns 10.0
        every { activeGuidanceInfo?.routeProgress?.fractionTraveled } returns 1.0
        every { activeGuidanceInfo?.legProgress?.remainingDistance } returns 180.0
        every { activeGuidanceInfo?.legProgress?.remainingDuration } returns 2000
        every { activeGuidanceInfo?.legProgress?.distanceTraveled } returns 20.0
        every { activeGuidanceInfo?.legProgress?.fractionTraveled } returns 2.0
        every { activeGuidanceInfo?.stepProgress?.remainingDistance } returns 35.0
        every { activeGuidanceInfo?.stepProgress?.remainingDuration } returns 3000
        every { activeGuidanceInfo?.stepProgress?.distanceTraveled } returns 30.0
        every { activeGuidanceInfo?.stepProgress?.fractionTraveled } returns 50.0
        every { routeState } returns RouteState.TRACKING
        every { stale } returns true
        every { bannerInstruction } returns nativeBannerInstructions
        every { voiceInstruction } returns nativeVoiceInstructions
        every { inTunnel } returns true
        every { upcomingRouteAlerts } returns emptyList()
        every { roadName } returns "Central Avenue"
        every { shieldName } returns "I880"
        every { imageBaseurl } returns "https://mapbox.shields.com/"
        every { shields } returns listOf()
    }

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
    }
}
