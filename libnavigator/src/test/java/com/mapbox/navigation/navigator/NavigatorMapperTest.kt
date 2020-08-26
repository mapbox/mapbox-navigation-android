package com.mapbox.navigation.navigator

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.trip.model.alert.CountryBorderCrossingAdminInfo
import com.mapbox.navigation.base.trip.model.alert.CountryBorderCrossingAlert
import com.mapbox.navigation.base.trip.model.alert.IncidentAlert
import com.mapbox.navigation.base.trip.model.alert.IncidentCongestion
import com.mapbox.navigation.base.trip.model.alert.IncidentImpact
import com.mapbox.navigation.base.trip.model.alert.IncidentInfo
import com.mapbox.navigation.base.trip.model.alert.IncidentType.CONSTRUCTION
import com.mapbox.navigation.base.trip.model.alert.RestStopAlert
import com.mapbox.navigation.base.trip.model.alert.RestStopType
import com.mapbox.navigation.base.trip.model.alert.RestrictedAreaAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlertGeometry
import com.mapbox.navigation.base.trip.model.alert.RouteAlertType
import com.mapbox.navigation.base.trip.model.alert.TollCollectionAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionType
import com.mapbox.navigation.base.trip.model.alert.TunnelEntranceAlert
import com.mapbox.navigation.base.trip.model.alert.TunnelInfo
import com.mapbox.navigation.navigator.internal.NavigatorMapper
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.RouteAlert
import com.mapbox.navigator.RouteAlertAdminInfo
import com.mapbox.navigator.RouteAlertBorderCrossingInfo
import com.mapbox.navigator.RouteAlertIncidentCongestionInfo
import com.mapbox.navigator.RouteAlertIncidentInfo
import com.mapbox.navigator.RouteAlertIncidentType
import com.mapbox.navigator.RouteAlertServiceAreaInfo
import com.mapbox.navigator.RouteAlertServiceAreaType
import com.mapbox.navigator.RouteAlertTollCollectionInfo
import com.mapbox.navigator.RouteAlertTollCollectionType
import com.mapbox.navigator.RouteAlertTunnelInfo
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.UpcomingRouteAlert
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class NavigatorMapperTest {

    private val navigatorMapper = NavigatorMapper()

    @Test
    fun `route progress is null when route is null`() {
        val navigationStatus: NavigationStatus = mockk()

        val routeProgress = navigatorMapper.getRouteProgress(
            null,
            null,
            navigationStatus,
            mockk(relaxed = true)
        )

        assertNull(routeProgress)
    }

    @Test
    fun `route progress minimum requirements`() {
        val routeProgress = navigatorMapper.getRouteProgress(
            directionsRoute,
            null,
            navigationStatus,
            mockk(relaxed = true)
        )

        assertNotNull(routeProgress)
    }

    @Test
    fun `route init info is null when route info is null`() {
        assertNull(navigatorMapper.getRouteInitInfo(null))
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

        val routeProgress = navigatorMapper.getRouteProgress(
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

        val result = navigatorMapper.getRouteInitInfo(routeInfo)!!

        assertEquals(1, result.routeAlerts.size)
        assertEquals(RouteAlertType.TunnelEntrance, result.routeAlerts[0].alertType)
    }

    @Test
    fun `tunnel entrance alert is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            tunnelEntranceRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = TunnelEntranceAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        ).alertGeometry(
            RouteAlertGeometry.Builder(
                456.0,
                Point.fromLngLat(10.0, 20.0),
                1,
                Point.fromLngLat(33.0, 44.0),
                2
            ).build()
        )
            .info(TunnelInfo.Builder("Ted Williams Tunnel").build())
            .build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.TunnelEntrance)
    }

    @Test
    fun `parsing multiple tunnel entrances returns multiple alerts`() {
        val firstEntrance = tunnelEntranceRouteAlert.toUpcomingRouteAlert(100.0)
        val secondEntrance = tunnelEntranceRouteAlert.toUpcomingRouteAlert(200.0)
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            firstEntrance,
            secondEntrance
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlerts = routeProgress!!.upcomingRouteAlerts

        assertEquals(firstEntrance.distanceToStart, upcomingRouteAlerts[0].distanceToStart, .0001)
        assertEquals(secondEntrance.distanceToStart, upcomingRouteAlerts[1].distanceToStart, .0001)
    }

    @Test
    fun `country border crossing alert is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            countryBorderCrossingRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = CountryBorderCrossingAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        )
            .from(CountryBorderCrossingAdminInfo.Builder("US", "USA").build())
            .to(CountryBorderCrossingAdminInfo.Builder("CA", "CAN").build())
            .build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.CountryBorderCrossing)
    }

    @Test
    fun `toll collection alert is parsed correctly (gantry)`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            tollCollectionGantryRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = TollCollectionAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        )
            .tollCollectionType(TollCollectionType.TollGantry)
            .build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.TollCollection)
    }

    @Test
    fun `toll collection alert is parsed correctly (booth)`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            tollCollectionBoothRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = TollCollectionAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        )
            .tollCollectionType(TollCollectionType.TollBooth)
            .build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.TollCollection)
    }

    @Test
    fun `unknown toll collection alert is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            unknownTollCollectionRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = TollCollectionAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        ).build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.TollCollection)
    }

    @Test
    fun `rest stop alert is parsed correctly (rest)`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            restStopRestRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = RestStopAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        )
            .restStopType(RestStopType.RestArea)
            .build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.RestStop)
    }

    @Test
    fun `rest stop alert is parsed correctly (service)`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            restStopServiceRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = RestStopAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        )
            .restStopType(RestStopType.ServiceArea)
            .build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.RestStop)
    }

    @Test
    fun `unknown rest stop alert is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            unknownRestStopRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = RestStopAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        ).build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.RestStop)
    }

    @Test
    fun `restricted area alert is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            restrictedAreaRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = RestrictedAreaAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        ).alertGeometry(
            RouteAlertGeometry.Builder(
                456.0,
                Point.fromLngLat(10.0, 20.0),
                1,
                Point.fromLngLat(33.0, 44.0),
                2
            ).build()
        ).build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.RestrictedArea)
    }

    @Test
    fun `incident alert collection is parsed correctly`() {
        every { navigationStatus.upcomingRouteAlerts } returns listOf(
            incidentRouteAlert.toUpcomingRouteAlert()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus,
            mockk(relaxed = true)
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = IncidentAlert.Builder(
            Point.fromLngLat(10.0, 20.0),
            123.0
        )
            .alertGeometry(
                RouteAlertGeometry.Builder(
                    456.0,
                    Point.fromLngLat(10.0, 20.0),
                    1,
                    Point.fromLngLat(33.0, 44.0),
                    2
                ).build()
            )
            .info(
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
            .build()

        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.alertType, RouteAlertType.Incident)
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
        type = com.mapbox.navigator.RouteAlertType.KINCIDENT,
        incidentInfo = RouteAlertIncidentInfo(
            "some_id",
            RouteAlertIncidentType.KCONSTRUCTION,
            Date(60),
            Date(80),
            Date(40),
            null,
            emptyList(),
            true,
            RouteAlertIncidentCongestionInfo(4, null),
            "low",
            "incident description",
            "incident sub-type",
            "incident sub-type description",
            listOf(10, 20, 30)
        )
    )

    private val tunnelEntranceRouteAlert = createRouteAlert(
        hasLength = true,
        type = com.mapbox.navigator.RouteAlertType.KTUNNEL_ENTRANCE,
        tunnelInfo = RouteAlertTunnelInfo(
            "Ted Williams Tunnel"
        )
    )

    private val countryBorderCrossingRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.KBORDER_CROSSING,
        countryBorderCrossingInfo = RouteAlertBorderCrossingInfo(
            RouteAlertAdminInfo("USA", "US"),
            RouteAlertAdminInfo("CAN", "CA")
        )
    )

    private val tollCollectionGantryRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.KTOLL_COLLECTION_POINT,
        tollCollectionInfo = RouteAlertTollCollectionInfo(
            RouteAlertTollCollectionType.KTOLL_GANTRY
        )
    )

    private val tollCollectionBoothRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.KTOLL_COLLECTION_POINT,
        tollCollectionInfo = RouteAlertTollCollectionInfo(
            RouteAlertTollCollectionType.KTOLL_BOOTH
        )
    )

    private val unknownTollCollectionRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.KTOLL_COLLECTION_POINT,
        tollCollectionInfo = null
    )

    private val restStopRestRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.KSERVICE_AREA,
        serviceAreaInfo = RouteAlertServiceAreaInfo(RouteAlertServiceAreaType.KREST_AREA)
    )

    private val restStopServiceRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.KSERVICE_AREA,
        serviceAreaInfo = RouteAlertServiceAreaInfo(
            RouteAlertServiceAreaType.KSERVICE_AREA
        )
    )

    private val unknownRestStopRouteAlert = createRouteAlert(
        hasLength = false,
        type = com.mapbox.navigator.RouteAlertType.KSERVICE_AREA,
        serviceAreaInfo = null
    )

    private val restrictedAreaRouteAlert = createRouteAlert(
        hasLength = true,
        type = com.mapbox.navigator.RouteAlertType.KRESTRICTED_AREA
    )

    private fun createRouteAlert(
        hasLength: Boolean,
        type: com.mapbox.navigator.RouteAlertType,
        incidentInfo: RouteAlertIncidentInfo? = null,
        tunnelInfo: RouteAlertTunnelInfo? = null,
        countryBorderCrossingInfo: RouteAlertBorderCrossingInfo? = null,
        tollCollectionInfo: RouteAlertTollCollectionInfo? = null,
        serviceAreaInfo: RouteAlertServiceAreaInfo? = null
    ) = RouteAlert(
        type,
        123.0,
        if (hasLength) 456.0 else null,
        Point.fromLngLat(10.0, 20.0),
        1,
        if (hasLength) Point.fromLngLat(33.0, 44.0) else Point.fromLngLat(10.0, 20.0),
        if (hasLength) 2 else 1,
        incidentInfo, tunnelInfo, countryBorderCrossingInfo, tollCollectionInfo, serviceAreaInfo
    )

    private val defaultDistanceToStart = 1234.0
    private fun RouteAlert.toUpcomingRouteAlert(
        distanceToStart: Double = defaultDistanceToStart
    ) = UpcomingRouteAlert(this, distanceToStart)
}
