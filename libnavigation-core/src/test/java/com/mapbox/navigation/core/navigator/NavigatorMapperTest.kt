package com.mapbox.navigation.core.navigator

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RoadFactory
import com.mapbox.navigation.base.internal.factory.RouteIndicesFactory
import com.mapbox.navigation.base.internal.factory.RouteLegProgressFactory
import com.mapbox.navigation.base.internal.factory.RouteProgressFactory
import com.mapbox.navigation.base.internal.factory.RouteStepProgressFactory.buildRouteStepProgressObject
import com.mapbox.navigation.base.internal.factory.SpeedLimitInfoFactory
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.route.LegWaypoint
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
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
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.SpeedLimitSign
import com.mapbox.navigator.SpeedLimitUnit
import com.mapbox.navigator.UpcomingRouteAlert
import com.mapbox.navigator.UpcomingRouteAlertUpdate
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

    private val directionsRoute = DirectionsRoute.fromJson(
        FileUtils.loadJsonFixture("multileg_route.json")
    )

    private val route: NavigationRoute = mockk(relaxed = true) {
        every { directionsRoute } returns this@NavigatorMapperTest.directionsRoute
    }
    private val roadName = com.mapbox.navigator.RoadName("Central Av", "en", null, null)

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `route progress result sanity`() {
        val bannerInstructions = navigationStatus.getCurrentBannerInstructions(route)
        val upcomingRoadObjects = listOf<UpcomingRoadObject>(mockk())
        val legDestination = mockk<LegWaypoint>()
        val expected = RouteProgressFactory.buildRouteProgressObject(
            route = route,
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
                ),
                geometryIndex = legGeometryIndex,
                legDestination = legDestination
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
            upcomingRoadObjects = upcomingRoadObjects,
            alternativeRouteId = "alternative_id",
            currentRouteGeometryIndex = routeGeometryIndex,
            inParkingAisle = true,
            alternativeRoutesIndices = mapOf(
                "id#2" to RouteIndicesFactory.buildRouteIndices(2, 4, 6, 8, 10),
                "id#3" to RouteIndicesFactory.buildRouteIndices(3, 7, 5, 11, 9),
            )
        )

        val result = getRouteProgressFrom(
            route,
            navigationStatus,
            remainingWaypoints = 1,
            bannerInstructions,
            instructionIndex = 1,
            lastVoiceInstruction = null,
            upcomingRoadObjects,
            legDestination
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
                every { roads } returns listOf(roadName)
                every { isFallback } returns false
                every { inTunnel } returns false
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
            speedLimitInfo = SpeedLimitInfoFactory.createSpeedLimitInfo(
                10,
                SpeedUnit.KILOMETERS_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD,
            ),
            roadEdgeMatchProbability = 1f,
            zLevel = null,
            road = road,
            isDegradedMapMatching = false,
            inTunnel = false,
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
                every { roads } returns listOf(roadName)
                every { isFallback } returns false
                every { inTunnel } returns false
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
            speedLimitInfo = SpeedLimitInfoFactory.createSpeedLimitInfo(
                10,
                SpeedUnit.KILOMETERS_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD,
            ),
            roadEdgeMatchProbability = 1f,
            zLevel = null,
            road = road,
            isDegradedMapMatching = false,
            inTunnel = false,
        )

        val result = tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)

        assertEquals(expected, result)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `location matcher result when off road and degraded map matching`() {
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
                every { roads } returns listOf(roadName)
                every { isFallback } returns true
                every { inTunnel } returns false
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
            speedLimitInfo = SpeedLimitInfoFactory.createSpeedLimitInfo(
                10,
                SpeedUnit.KILOMETERS_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD,
            ),
            roadEdgeMatchProbability = 1f,
            zLevel = null,
            road = road,
            isDegradedMapMatching = true,
            inTunnel = false,
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
                every { roads } returns listOf(roadName)
                every { isFallback } returns false
                every { inTunnel } returns false
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
            speedLimitInfo = SpeedLimitInfoFactory.createSpeedLimitInfo(
                10,
                SpeedUnit.KILOMETERS_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD,
            ),
            roadEdgeMatchProbability = 1f,
            zLevel = null,
            road = road,
            isDegradedMapMatching = false,
            inTunnel = false,
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
            every { roads } returns listOf(roadName)
            every { isFallback } returns false
            every { inTunnel } returns false
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
            speedLimitInfo = SpeedLimitInfoFactory.createSpeedLimitInfo(
                10,
                SpeedUnit.KILOMETERS_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD,
            ),
            roadEdgeMatchProbability = 0f,
            zLevel = null,
            road = road,
            isDegradedMapMatching = false,
            inTunnel = false,
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
                every { roads } returns listOf(roadName)
                every { isFallback } returns false
                every { inTunnel } returns false
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
            speedLimitInfo = SpeedLimitInfoFactory.createSpeedLimitInfo(
                10,
                SpeedUnit.KILOMETERS_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD,
            ),
            roadEdgeMatchProbability = 1f,
            zLevel = 2,
            road = road,
            isDegradedMapMatching = false,
            inTunnel = false,
        )

        val result = tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)

        assertEquals(expected, result)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `location matcher result inTunnel`() {
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
                every { roads } returns listOf(roadName)
                every { isFallback } returns false
                every { inTunnel } returns true
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
            speedLimitInfo = SpeedLimitInfoFactory.createSpeedLimitInfo(
                10,
                SpeedUnit.KILOMETERS_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD,
            ),
            roadEdgeMatchProbability = 1f,
            zLevel = null,
            road = road,
            isDegradedMapMatching = false,
            inTunnel = true,
        )

        val result = tripStatus.getLocationMatcherResult(enhancedLocation, keyPoints, road)

        assertEquals(expected, result)
    }

    @Test
    fun `route progress is null when status is invalid`() {
        every { navigationStatus.routeState } returns RouteState.INVALID

        val routeProgress = getRouteProgressFrom(
            route,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0,
            mockk(relaxed = true),
            emptyList(),
            mockk(),
        )

        assertNull(routeProgress)
    }

    @Test
    fun `route progress minimum requirements`() {
        val routeProgress = getRouteProgressFrom(
            route,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0,
            mockk(relaxed = true),
            emptyList(),
            mockk(),
        )

        assertNotNull(routeProgress)
    }

    @Test
    fun `route progress state stale`() {
        every { navigationStatus.stale } returns true
        val routeProgress = getRouteProgressFrom(
            route,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0,
            lastVoiceInstruction = mockk(relaxed = true),
            emptyList(),
            mockk(),
        )

        assertTrue(routeProgress!!.stale)
    }

    @Test
    fun `route progress state not stale`() {
        every { navigationStatus.stale } returns false
        val routeProgress = getRouteProgressFrom(
            route,
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0,
            lastVoiceInstruction = mockk(relaxed = true),
            emptyList(),
            mockk(),
        )

        assertFalse(routeProgress!!.stale)
    }

    @Test
    @Ignore("https://github.com/mapbox/mapbox-navigation-native/issues/3456")
    fun `parsing multiple tunnel entrances returns multiple alerts`() {
        val firstEntrance = tunnel.toUpcomingRouteAlert(100.0)
        val secondEntrance = tunnel.toUpcomingRouteAlert(200.0)
        every { navigationStatus.upcomingRouteAlertUpdates } returns listOf(
            UpcomingRouteAlertUpdate(tunnel.id, 100.0),
            UpcomingRouteAlertUpdate(tunnel.id, 200.0),
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true),
            mockk(relaxed = true),
            0,
            mockk(relaxed = true),
            emptyList(),
            mockk(),
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

    @Test
    fun `banner components mapper correctly sets remaining distance`() {
        val result = navigationStatus.getCurrentBannerInstructions(route)

        assertEquals(
            111f.toDouble(),
            result?.distanceAlongGeometry()
        )
    }

    @Test
    fun `prepareSpeedLimit kmph non null`() {
        every {
            navigationStatus.speedLimit
        } returns createSpeedLimit(speed = 80, unit = SpeedLimitUnit.KILOMETRES_PER_HOUR)
        val actual = navigationStatus.prepareSpeedLimit()!!

        assertEquals(80, actual.speedKmph)
    }

    @Test
    fun `prepareSpeedLimitInfo kmph non null`() {
        every {
            navigationStatus.speedLimit
        } returns createSpeedLimit(
            speed = 80,
            unit = SpeedLimitUnit.KILOMETRES_PER_HOUR,
            sign = SpeedLimitSign.VIENNA
        )
        val actual = navigationStatus.prepareSpeedLimitInfo()

        assertEquals(
            SpeedLimitInfoFactory.createSpeedLimitInfo(
                80,
                SpeedUnit.KILOMETERS_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.VIENNA,
            ),
            actual
        )
    }

    @Test
    fun `prepareSpeedLimit kmph null`() {
        every {
            navigationStatus.speedLimit
        } returns createSpeedLimit(speed = null, unit = SpeedLimitUnit.KILOMETRES_PER_HOUR)
        val actual = navigationStatus.prepareSpeedLimit()!!

        assertNull(actual.speedKmph)
    }

    @Test
    fun `prepareSpeedLimitInfo kmph null`() {
        every {
            navigationStatus.speedLimit
        } returns createSpeedLimit(
            speed = null,
            unit = SpeedLimitUnit.KILOMETRES_PER_HOUR,
            sign = SpeedLimitSign.MUTCD
        )
        val actual = navigationStatus.prepareSpeedLimitInfo()

        assertEquals(
            SpeedLimitInfoFactory.createSpeedLimitInfo(
                null,
                SpeedUnit.KILOMETERS_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD,
            ),
            actual
        )
    }

    @Test
    fun `prepareSpeedLimit mph non null`() {
        every {
            navigationStatus.speedLimit
        } returns createSpeedLimit(speed = 60, unit = SpeedLimitUnit.MILES_PER_HOUR)
        val actual = navigationStatus.prepareSpeedLimit()!!

        assertEquals(97, actual.speedKmph)
    }

    @Test
    fun `prepareSpeedLimitInfo mph non null`() {
        every {
            navigationStatus.speedLimit
        } returns createSpeedLimit(
            speed = 60,
            unit = SpeedLimitUnit.MILES_PER_HOUR,
            sign = SpeedLimitSign.MUTCD
        )
        val actual = navigationStatus.prepareSpeedLimitInfo()

        assertEquals(
            SpeedLimitInfoFactory.createSpeedLimitInfo(
                60,
                SpeedUnit.MILES_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD,
            ),
            actual
        )
    }

    @Test
    fun `prepareSpeedLimit mph null`() {
        every {
            navigationStatus.speedLimit
        } returns createSpeedLimit(speed = null, unit = SpeedLimitUnit.MILES_PER_HOUR)
        val actual = navigationStatus.prepareSpeedLimit()!!

        assertNull(actual.speedKmph)
    }

    @Test
    fun `prepareSpeedLimitInfo mph null`() {
        every {
            navigationStatus.speedLimit
        } returns createSpeedLimit(
            speed = null,
            unit = SpeedLimitUnit.MILES_PER_HOUR,
            sign = SpeedLimitSign.VIENNA
        )
        val actual = navigationStatus.prepareSpeedLimitInfo()

        assertEquals(
            SpeedLimitInfoFactory.createSpeedLimitInfo(
                null,
                SpeedUnit.MILES_PER_HOUR,
                com.mapbox.navigation.base.speed.model.SpeedLimitSign.VIENNA,
            ),
            actual
        )
    }

    private fun createSpeedLimit(
        speed: Int? = 10,
        unit: SpeedLimitUnit = SpeedLimitUnit.KILOMETRES_PER_HOUR,
        sign: SpeedLimitSign = SpeedLimitSign.MUTCD,
    ): com.mapbox.navigator.SpeedLimit {
        return com.mapbox.navigator.SpeedLimit(speed, unit, sign)
    }

    private val nativeBannerInstructions = mockk<BannerInstruction> {
        every { remainingStepDistance } returns 111f
        every { primary } returns mockk {
            every { components } returns listOf(
                mockk(relaxed = true) {
                    every { imageBaseUrl } returns "legacyShieldUrl"
                    every { shield } returns mockk(relaxed = true) {
                        every { baseUrl } returns "designBaseUrl"
                    }
                }
            )
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
    private val routeGeometryIndex = 12
    private val legGeometryIndex = 7
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
        every { roads } returns listOf(roadName)
        every { locatedAlternativeRouteId } returns "alternative_id"
        every { geometryIndex } returns routeGeometryIndex
        every { shapeIndex } returns legGeometryIndex
        every { inParkingAisle } returns true
        every { alternativeRouteIndices } returns listOf(
            com.mapbox.navigator.RouteIndices("id#2", 2, 4, 6, 8, 10),
            com.mapbox.navigator.RouteIndices("id#3", 3, 7, 5, 11, 9),
        )
    }

    val routeAlertLocation: RouteAlertLocation = mockk()

    private val tunnel = RoadObject(
        ID,
        LENGTH,
        MatchedRoadObjectLocation.valueOf(routeAlertLocation),
        com.mapbox.navigator.RoadObjectType.TUNNEL,
        RoadObjectProvider.MAPBOX,
        RoadObjectMetadata.valueOf(com.mapbox.navigator.TunnelInfo("Ted Williams Tunnel", "id")),
        false,
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
