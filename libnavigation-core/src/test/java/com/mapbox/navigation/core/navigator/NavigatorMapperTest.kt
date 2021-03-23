package com.mapbox.navigation.core.navigator

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import com.mapbox.navigation.core.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.core.trip.model.roadobject.border.CountryBorderCrossing
import com.mapbox.navigation.core.trip.model.roadobject.border.CountryBorderCrossingAdminInfo
import com.mapbox.navigation.core.trip.model.roadobject.border.CountryBorderCrossingInfo
import com.mapbox.navigation.core.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentCongestion
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentImpact
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentInfo
import com.mapbox.navigation.core.trip.model.roadobject.incident.IncidentType.CONSTRUCTION
import com.mapbox.navigation.core.trip.model.roadobject.restrictedarea.RestrictedAreaEntrance
import com.mapbox.navigation.core.trip.model.roadobject.restrictedarea.RestrictedAreaExit
import com.mapbox.navigation.core.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.core.trip.model.roadobject.reststop.RestStopType
import com.mapbox.navigation.core.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.core.trip.model.roadobject.tollcollection.TollCollectionType
import com.mapbox.navigation.core.trip.model.roadobject.tunnel.TunnelEntrance
import com.mapbox.navigation.core.trip.model.roadobject.tunnel.TunnelExit
import com.mapbox.navigation.core.trip.model.roadobject.tunnel.TunnelInfo
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigator.IncidentCongestionDescription
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.RouteAlert
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.SpeedLimitSign
import com.mapbox.navigator.SpeedLimitUnit
import com.mapbox.navigator.UpcomingRouteAlert
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class NavigatorMapperTest {

    private val enhancedLocation: Location = mockk(relaxed = true)
    private val keyPoints: List<Location> = mockk(relaxed = true)
    private val route: DirectionsRoute = mockk(relaxed = true)
    private val routeBufferGeoJson: Geometry = mockk(relaxed = true)

    @Test
    fun `map matcher result sanity`() {
        val tripStatus = TripStatus(
            route,
            routeBufferGeoJson,
            mockk {
                every { offRoadProba } returns 0f
                every { speedLimit } returns createSpeedLimit()
                every { map_matcher_output } returns mockk {
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
            routeBufferGeoJson,
            mockk {
                every { offRoadProba } returns 0.5f
                every { speedLimit } returns createSpeedLimit()
                every { map_matcher_output } returns mockk {
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
            routeBufferGeoJson,
            mockk {
                every { offRoadProba } returns 0.500009f
                every { speedLimit } returns createSpeedLimit()
                every { map_matcher_output } returns mockk {
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
            routeBufferGeoJson,
            mockk {
                every { offRoadProba } returns 0f
                every { speedLimit } returns createSpeedLimit()
                every { map_matcher_output } returns mockk {
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
            routeBufferGeoJson,
            mockk {
                every { offRoadProba } returns 1f
                every { speedLimit } returns createSpeedLimit()
                every { map_matcher_output } returns mockk {
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
        val navigationStatus: NavigationStatus = mockk()

        val routeProgress = getRouteProgressFrom(
            null,
            null,
            navigationStatus,
            mockk(relaxed = true)
        )

        assertNull(routeProgress)
    }

    @Test
    fun `route progress minimum requirements`() {
        val routeProgress = getRouteProgressFrom(
            directionsRoute,
            null,
            navigationStatus,
            mockk(relaxed = true)
        )

        assertNotNull(routeProgress)
    }

    @Test
    fun `route progress state location stale`() {
        every { navigationStatus.stale } returns true
        val routeProgress = getRouteProgressFrom(
            directionsRoute,
            null,
            navigationStatus,
            mockk(relaxed = true)
        )

        assertEquals(RouteProgressState.LOCATION_STALE, routeProgress!!.currentState)
    }

    @Test
    fun `route init info is null when route info is null`() {
        assertNull(getRouteInitInfo(null))
    }

    @Test
    fun `step progress correctly created`() {
        val stepProgress = RouteStepProgress.Builder()
            .step(directionsRoute.legs()!![0].steps()!![1])
            .stepIndex(1)
            .stepPoints(PolylineUtils.decode("sla~hA|didrCoDvx@", 6))
            .distanceRemaining(15f)
            .durationRemaining(300.0 / 1000.0)
            .distanceTraveled(10f)
            .fractionTraveled(50f)
            .intersectionIndex(1)
            .build()

        val routeProgress = getRouteProgressFrom(
            directionsRoute,
            null,
            navigationStatus,
            mockk(relaxed = true)
        )

        assertEquals(stepProgress, routeProgress!!.currentLegProgress!!.currentStepProgress)
    }

    @Test
    fun `alerts are present in the route init info is they are delivered from native`() {
        val routeInfo = RouteInfo(listOf(tunnelEntranceRouteAlert))

        val result = getRouteInitInfo(routeInfo)!!

        assertEquals(2, result.roadObjects.size)
        assertEquals(RoadObjectType.TUNNEL_ENTRANCE, result.roadObjects[0].objectType)
        assertEquals(RoadObjectType.TUNNEL_EXIT, result.roadObjects[1].objectType)
    }

    @Test
    fun `tunnel entrance alert is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            tunnelEntranceRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )

        val expectedFirst = TunnelEntrance.Builder(
            RoadObjectGeometry.Builder(null, Point.fromLngLat(10.0, 20.0), null, null).build(),
            TunnelInfo.Builder("Ted Williams Tunnel").build()
        ).distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE)
            .build()

        val expectedSecond = TunnelExit.Builder(
            RoadObjectGeometry.Builder(null, Point.fromLngLat(33.0, 44.0), null, null).build(),
            TunnelInfo.Builder("Ted Williams Tunnel").build()
        ).distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE + LENGTH)
            .build()

        val upcomingRoadObjectFirst = routeProgress!!.upcomingRoadObjects[0]
        val upcomingRoadObjectSecond = routeProgress.upcomingRoadObjects[1]

        assertEquals(DISTANCE_TO_START, upcomingRoadObjectFirst.distanceToStart, .00001)
        assertEquals(DISTANCE_TO_START + LENGTH, upcomingRoadObjectSecond.distanceToStart, .00001)
        assertEquals(expectedFirst, upcomingRoadObjectFirst.roadObject)
        assertEquals(expectedFirst.hashCode(), upcomingRoadObjectFirst.roadObject.hashCode())
        assertEquals(expectedFirst.toString(), upcomingRoadObjectFirst.roadObject.toString())
        assertEquals(expectedFirst.objectType, RoadObjectType.TUNNEL_ENTRANCE)
        assertEquals(expectedSecond, upcomingRoadObjectSecond.roadObject)
        assertEquals(expectedSecond.hashCode(), upcomingRoadObjectSecond.roadObject.hashCode())
        assertEquals(expectedSecond.toString(), upcomingRoadObjectSecond.roadObject.toString())
        assertEquals(expectedSecond.objectType, RoadObjectType.TUNNEL_EXIT)
    }

    @Test
    fun `parsing multiple tunnel entrances returns multiple alerts`() {
        val firstEntrance = tunnelEntranceRouteAlert.toUpcomingRouteAlert(100.0)
        val secondEntrance = tunnelEntranceRouteAlert.toUpcomingRouteAlert(200.0)
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            firstEntrance,
            secondEntrance
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlerts = routeProgress!!.upcomingRoadObjects

        assertEquals(
            firstEntrance.distanceToStart,
            upcomingRouteAlerts[0].distanceToStart,
            .0001
        )
        assertEquals(
            firstEntrance.distanceToStart + LENGTH,
            upcomingRouteAlerts[1].distanceToStart,
            .0001
        )
        assertEquals(
            secondEntrance.distanceToStart,
            upcomingRouteAlerts[2].distanceToStart,
            .0001
        )
        assertEquals(
            secondEntrance.distanceToStart + LENGTH,
            upcomingRouteAlerts[3].distanceToStart,
            .0001
        )
    }

    @Test
    fun `country border crossing alert is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            countryBorderCrossingRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRoadObjects[0]

        assertEquals(
            DISTANCE_TO_START,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = CountryBorderCrossing.Builder(
            RoadObjectGeometry.Builder(
                null,
                shape = Point.fromLngLat(10.0, 20.0),
                startGeometryIndex = 1,
                endGeometryIndex = 1
            ).build(),
            CountryBorderCrossingInfo.Builder(
                CountryBorderCrossingAdminInfo.Builder("US", "USA").build(),
                CountryBorderCrossingAdminInfo.Builder("CA", "CAN").build()
            ).build()
        )
            .distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE)
            .build()
        assertEquals(expected, upcomingRouteAlert.roadObject)
        assertEquals(expected.hashCode(), upcomingRouteAlert.roadObject.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.roadObject.toString())
        assertEquals(expected.objectType, RoadObjectType.COUNTRY_BORDER_CROSSING)
    }

    @Test
    fun `toll collection alert is parsed correctly (gantry)`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            tollCollectionGantryRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRoadObjects[0]

        assertEquals(
            DISTANCE_TO_START,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = TollCollection.Builder(
            RoadObjectGeometry.Builder(
                null,
                shape = Point.fromLngLat(10.0, 20.0),
                startGeometryIndex = 1,
                endGeometryIndex = 1
            ).build(),
            TollCollectionType.TOLL_GANTRY
        ).distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE)
            .build()
        assertEquals(expected, upcomingRouteAlert.roadObject)
        assertEquals(expected.hashCode(), upcomingRouteAlert.roadObject.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.roadObject.toString())
        assertEquals(expected.objectType, RoadObjectType.TOLL_COLLECTION)
    }

    @Test
    fun `toll collection alert is parsed correctly (booth)`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            tollCollectionBoothRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRoadObjects[0]

        assertEquals(
            DISTANCE_TO_START,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = TollCollection.Builder(
            RoadObjectGeometry.Builder(
                null,
                shape = Point.fromLngLat(10.0, 20.0),
                startGeometryIndex = 1,
                endGeometryIndex = 1
            ).build(),
            TollCollectionType.TOLL_BOOTH
        ).distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE)
            .build()
        assertEquals(expected, upcomingRouteAlert.roadObject)
        assertEquals(expected.hashCode(), upcomingRouteAlert.roadObject.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.roadObject.toString())
        assertEquals(expected.objectType, RoadObjectType.TOLL_COLLECTION)
    }

    @Test
    fun `rest stop alert is parsed correctly (rest)`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            restStopRestRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRoadObject = routeProgress!!.upcomingRoadObjects[0]

        assertEquals(
            DISTANCE_TO_START,
            upcomingRoadObject.distanceToStart,
            .00001
        )
        val expected = RestStop.Builder(
            RoadObjectGeometry.Builder(
                null,
                shape = Point.fromLngLat(10.0, 20.0),
                startGeometryIndex = 1,
                endGeometryIndex = 1
            ).build(),
            RestStopType.REST_AREA
        ).distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE)
            .build()
        assertEquals(expected, upcomingRoadObject.roadObject)
        assertEquals(expected.hashCode(), upcomingRoadObject.roadObject.hashCode())
        assertEquals(expected.toString(), upcomingRoadObject.roadObject.toString())
        assertEquals(expected.objectType, RoadObjectType.REST_STOP)
    }

    @Test
    fun `rest stop alert is parsed correctly (service)`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            restStopServiceRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRoadObject = routeProgress!!.upcomingRoadObjects[0]

        assertEquals(DISTANCE_TO_START, upcomingRoadObject.distanceToStart, .00001)
        val expected = RestStop.Builder(
            RoadObjectGeometry.Builder(
                null,
                shape = Point.fromLngLat(10.0, 20.0),
                startGeometryIndex = 1,
                endGeometryIndex = 1
            ).build(),
            RestStopType.SERVICE_AREA
        ).distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE)
            .build()
        assertEquals(expected, upcomingRoadObject.roadObject)
        assertEquals(expected.hashCode(), upcomingRoadObject.roadObject.hashCode())
        assertEquals(expected.toString(), upcomingRoadObject.roadObject.toString())
        assertEquals(expected.objectType, RoadObjectType.REST_STOP)
    }

    @Test
    fun `restricted area alert is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            restrictedAreaRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRoadObjectFirst = routeProgress!!.upcomingRoadObjects[0]
        val upcomingRoadObjectSecond = routeProgress.upcomingRoadObjects[1]

        val expectedFirst = RestrictedAreaEntrance.Builder(
            RoadObjectGeometry.Builder(null, Point.fromLngLat(10.0, 20.0), null, null).build()
        ).distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE)
            .build()

        val expectedSecond = RestrictedAreaExit.Builder(
            RoadObjectGeometry.Builder(null, Point.fromLngLat(33.0, 44.0), null, null).build()
        ).distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE + LENGTH)
            .build()

        assertEquals(DISTANCE_TO_START, upcomingRoadObjectFirst.distanceToStart, .00001)
        assertEquals(DISTANCE_TO_START + LENGTH, upcomingRoadObjectSecond.distanceToStart, .00001)
        assertEquals(expectedFirst, upcomingRoadObjectFirst.roadObject)
        assertEquals(expectedFirst.hashCode(), upcomingRoadObjectFirst.roadObject.hashCode())
        assertEquals(expectedFirst.toString(), upcomingRoadObjectFirst.roadObject.toString())
        assertEquals(expectedFirst.objectType, RoadObjectType.RESTRICTED_AREA_ENTRANCE)
        assertEquals(expectedSecond, upcomingRoadObjectSecond.roadObject)
        assertEquals(expectedSecond.hashCode(), upcomingRoadObjectSecond.roadObject.hashCode())
        assertEquals(expectedSecond.toString(), upcomingRoadObjectSecond.roadObject.toString())
        assertEquals(expectedSecond.objectType, RoadObjectType.RESTRICTED_AREA_EXIT)
    }

    @Test
    fun `incident alert collection is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            incidentRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = getRouteProgressFrom(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRoadObject = routeProgress!!.upcomingRoadObjects[0]

        assertEquals(
            DISTANCE_TO_START,
            upcomingRoadObject.distanceToStart,
            .00001
        )
        val expected = Incident.Builder(
            RoadObjectGeometry.Builder(
                LENGTH,
                LineString.fromLngLats(
                    listOf(Point.fromLngLat(10.0, 20.0), Point.fromLngLat(33.0, 44.0))
                ),
                1,
                2
            ).build(),
            IncidentInfo.Builder("some_id")
                .type(CONSTRUCTION)
                .creationTime(Date(40))
                .startTime(Date(60))
                .endTime(Date(80))
                .isClosed(true)
                .congestion(IncidentCongestion.Builder().value(4).build())
                .impact(IncidentImpact.LOW)
                .description("incident description")
                .subType("incident sub-type")
                .subTypeDescription("incident sub-type description")
                .alertcCodes(listOf(10, 20, 30))
                .build()
        )
            .distanceFromStartOfRoute(DISTANCE_FROM_START_OF_ROUTE)
            .build()

        assertEquals(expected, upcomingRoadObject.roadObject)
        assertEquals(expected.hashCode(), upcomingRoadObject.roadObject.hashCode())
        assertEquals(expected.toString(), upcomingRoadObject.roadObject.toString())
        assertEquals(expected.objectType, RoadObjectType.INCIDENT)
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

    private val incidentRouteAlert = createRouteAlert(
        hasLength = true,
        type = com.mapbox.navigator.RouteAlertType.INCIDENT,
        incidentInfo = com.mapbox.navigator.IncidentInfo(
            "some_id",
            null,
            com.mapbox.navigator.IncidentType.CONSTRUCTION,
            Date(60),
            Date(80),
            Date(40),
            null,
            emptyList(),
            true,
            com.mapbox.navigator.IncidentCongestion(4, IncidentCongestionDescription.LIGHT),
            com.mapbox.navigator.IncidentImpact.LOW,
            "incident description",
            "incident sub-type",
            "incident sub-type description",
            listOf(10, 20, 30),
            null,
            null,
            null
        )
    )

    private val tunnelEntranceRouteAlert = createRouteAlert(
        hasLength = true,
        type = com.mapbox.navigator.RouteAlertType.TUNNEL_ENTRANCE,
        tunnelInfo = com.mapbox.navigator.TunnelInfo(
            "Ted Williams Tunnel"
        )
    )

    private val countryBorderCrossingRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.BORDER_CROSSING,
        countryBorderCrossingInfo = com.mapbox.navigator.BorderCrossingInfo(
            com.mapbox.navigator.AdminInfo("USA", "US"),
            com.mapbox.navigator.AdminInfo("CAN", "CA")
        )
    )

    private val tollCollectionGantryRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.TOLL_COLLECTION_POINT,
        tollCollectionInfo = com.mapbox.navigator.TollCollectionInfo(
            com.mapbox.navigator.TollCollectionType.TOLL_GANTRY
        )
    )

    private val tollCollectionBoothRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.TOLL_COLLECTION_POINT,
        tollCollectionInfo = com.mapbox.navigator.TollCollectionInfo(
            com.mapbox.navigator.TollCollectionType.TOLL_BOOTH
        )
    )

    private val restStopRestRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.SERVICE_AREA,
        serviceAreaInfo = com.mapbox.navigator.ServiceAreaInfo(
            com.mapbox.navigator.ServiceAreaType.REST_AREA
        )
    )

    private val restStopServiceRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.SERVICE_AREA,
        serviceAreaInfo = com.mapbox.navigator.ServiceAreaInfo(
            com.mapbox.navigator.ServiceAreaType.SERVICE_AREA
        )
    )

    private val restrictedAreaRouteAlert = createRouteAlert(
        hasLength = true,
        type = com.mapbox.navigator.RouteAlertType.RESTRICTED_AREA
    )

    private fun createRouteAlert(
        hasLength: Boolean,
        type: com.mapbox.navigator.RouteAlertType,
        incidentInfo: com.mapbox.navigator.IncidentInfo? = null,
        tunnelInfo: com.mapbox.navigator.TunnelInfo? = null,
        countryBorderCrossingInfo: com.mapbox.navigator.BorderCrossingInfo? = null,
        tollCollectionInfo: com.mapbox.navigator.TollCollectionInfo? = null,
        serviceAreaInfo: com.mapbox.navigator.ServiceAreaInfo? = null
    ) = RouteAlert(
        type,
        DISTANCE_FROM_START_OF_ROUTE,
        if (hasLength) LENGTH else null,
        Point.fromLngLat(10.0, 20.0),
        1,
        if (hasLength) Point.fromLngLat(33.0, 44.0) else Point.fromLngLat(10.0, 20.0),
        if (hasLength) 2 else 1,
        incidentInfo, tunnelInfo, countryBorderCrossingInfo, tollCollectionInfo, serviceAreaInfo
    )

    private fun RouteAlert.toUpcomingRouteAlert(
        distanceToStart: Double = DISTANCE_TO_START
    ) = UpcomingRouteAlert(this, distanceToStart)

    companion object {
        private const val DISTANCE_TO_START = 1234.0
        private const val DISTANCE_FROM_START_OF_ROUTE = 123.0
        private const val LENGTH = 456.0
    }
}
