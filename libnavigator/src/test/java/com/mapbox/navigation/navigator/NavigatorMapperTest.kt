package com.mapbox.navigation.navigator

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.alert.BorderCrossingAdminInfo
import com.mapbox.navigation.base.trip.model.alert.BorderCrossingAlert
import com.mapbox.navigation.base.trip.model.alert.RestStopAlert
import com.mapbox.navigation.base.trip.model.alert.RestStopType
import com.mapbox.navigation.base.trip.model.alert.RestrictedAreaAlert
import com.mapbox.navigation.base.trip.model.alert.RouteAlertGeometry
import com.mapbox.navigation.base.trip.model.alert.RouteAlertType
import com.mapbox.navigation.base.trip.model.alert.TollCollectionAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionType
import com.mapbox.navigation.base.trip.model.alert.TunnelEntranceAlert
import com.mapbox.navigation.navigator.internal.NavigatorMapper
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.PassiveManeuver
import com.mapbox.navigator.PassiveManeuverAdminInfo
import com.mapbox.navigator.PassiveManeuverBorderCrossingInfo
import com.mapbox.navigator.PassiveManeuverIncidentInfo
import com.mapbox.navigator.PassiveManeuverServiceAreaInfo
import com.mapbox.navigator.PassiveManeuverServiceAreaType
import com.mapbox.navigator.PassiveManeuverTollCollectionInfo
import com.mapbox.navigator.PassiveManeuverTollCollectionType
import com.mapbox.navigator.PassiveManeuverTunnelInfo
import com.mapbox.navigator.PassiveManeuverType
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.UpcomingPassiveManeuver
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorMapperTest {

    private val navigatorMapper = NavigatorMapper()

    @Test
    fun `route progress is null when route is null`() {
        val navigationStatus: NavigationStatus = mockk()

        val routeProgress = navigatorMapper.getRouteProgress(
            null,
            null,
            navigationStatus
        )

        assertNull(routeProgress)
    }

    @Test
    fun `route progress minimum requirements`() {
        val routeProgress = navigatorMapper.getRouteProgress(
            directionsRoute,
            null,
            navigationStatus
        )

        assertNotNull(routeProgress)
    }

    @Test
    fun `route init info is null when route info is null`() {
        assertNull(navigatorMapper.getRouteInitInfo(null))
    }

    @Test
    fun `alerts are present in the route init info is they are delivered from native`() {
        val routeInfo = RouteInfo(listOf(tunnelEntrancePassiveManeuver))

        val result = navigatorMapper.getRouteInitInfo(routeInfo)!!

        assertEquals(1, result.routeAlerts.size)
        assertEquals(RouteAlertType.TunnelEntrance, result.routeAlerts[0].type)
    }

    @Test
    fun `unsupported alert types are ignored in the route init info`() {
        val routeInfo = RouteInfo(
            listOf(
                incidentPassiveManeuver
            )
        )

        val result = navigatorMapper.getRouteInitInfo(routeInfo)!!

        assertEquals(0, result.routeAlerts.size)
    }

    @Test
    fun `tunnel entrance alert is parsed correctly`() {
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            tunnelEntrancePassiveManeuver.toUpcomingManeuver()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = TunnelEntranceAlert.Builder(
            TunnelEntranceAlert.Metadata.Builder().build(),
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
        assertEquals(expected.type, RouteAlertType.TunnelEntrance)
    }

    @Test
    fun `parsing multiple tunnel entrances returns multiple alerts`() {
        val firstEntrance = tunnelEntrancePassiveManeuver.toUpcomingManeuver(100.0)
        val secondEntrance = tunnelEntrancePassiveManeuver.toUpcomingManeuver(200.0)
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            firstEntrance,
            secondEntrance
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus
        )
        val upcomingRouteAlerts = routeProgress!!.upcomingRouteAlerts

        assertEquals(firstEntrance.distanceToStart, upcomingRouteAlerts[0].distanceToStart, .0001)
        assertEquals(secondEntrance.distanceToStart, upcomingRouteAlerts[1].distanceToStart, .0001)
    }

    @Test
    fun `border crossing alert is parsed correctly`() {
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            borderCrossingPassiveManeuver.toUpcomingManeuver()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = BorderCrossingAlert.Builder(
            BorderCrossingAlert.Metadata.Builder()
                .from(BorderCrossingAdminInfo.Builder("from", "fromAlpha").build())
                .to(BorderCrossingAdminInfo.Builder("to", "toAlpha").build())
                .build(),
            Point.fromLngLat(10.0, 20.0),
            123.0
        ).build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.type, RouteAlertType.BorderCrossing)
    }

    @Test
    fun `toll collection alert is parsed correctly`() {
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            tollCollectionPassiveManeuver.toUpcomingManeuver()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = TollCollectionAlert.Builder(
            TollCollectionAlert.Metadata.Builder()
                .type(TollCollectionType.TollGantry)
                .build(),
            Point.fromLngLat(10.0, 20.0),
            123.0
        ).build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.type, RouteAlertType.TollCollection)
    }

    @Test
    fun `unknown toll collection alert is parsed correctly`() {
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            unknownTollCollectionPassiveManeuver.toUpcomingManeuver()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = TollCollectionAlert.Builder(
            TollCollectionAlert.Metadata.Builder().build(),
            Point.fromLngLat(10.0, 20.0),
            123.0
        ).build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.type, RouteAlertType.TollCollection)
    }

    @Test
    fun `rest stop alert is parsed correctly`() {
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            restStopPassiveManeuver.toUpcomingManeuver()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = RestStopAlert.Builder(
            RestStopAlert.Metadata.Builder()
                .type(RestStopType.RestArea)
                .build(),
            Point.fromLngLat(10.0, 20.0),
            123.0
        ).build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.type, RouteAlertType.RestStop)
    }

    @Test
    fun `unknown rest stop alert is parsed correctly`() {
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            unknownRestStopPassiveManeuver.toUpcomingManeuver()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = RestStopAlert.Builder(
            RestStopAlert.Metadata.Builder().build(),
            Point.fromLngLat(10.0, 20.0),
            123.0
        ).build()
        assertEquals(expected, upcomingRouteAlert.routeAlert)
        assertEquals(expected.hashCode(), upcomingRouteAlert.routeAlert.hashCode())
        assertEquals(expected.toString(), upcomingRouteAlert.routeAlert.toString())
        assertEquals(expected.type, RouteAlertType.RestStop)
    }

    @Test
    fun `restricted area alert is parsed correctly`() {
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            restrictedAreaPassiveManeuver.toUpcomingManeuver()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus
        )
        val upcomingRouteAlert = routeProgress!!.upcomingRouteAlerts[0]

        assertEquals(
            defaultDistanceToStart,
            upcomingRouteAlert.distanceToStart,
            .00001
        )
        val expected = RestrictedAreaAlert.Builder(
            RestrictedAreaAlert.Metadata.Builder().build(),
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
        assertEquals(expected.type, RouteAlertType.RestrictedArea)
    }

    @Test
    fun `unsupported alert types are ignored in the progress update`() {
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            incidentPassiveManeuver.toUpcomingManeuver()
        )

        val routeProgress = navigatorMapper.getRouteProgress(
            mockk(relaxed = true),
            mockk(relaxed = true),
            navigationStatus
        )

        assertTrue(routeProgress!!.upcomingRouteAlerts.isEmpty())
    }

    private val directionsRoute: DirectionsRoute = mockk {
        every { legs() } returns listOf(
            mockk {
                every { distance() } returns 100.0
                every { steps() } returns listOf(
                    mockk {
                        every { distance() } returns 20.0
                        every { geometry() } returns "y{v|bA{}diiGOuDpBiMhM{k@~Syj@bLuZlEiM"
                    }
                )
            }
        )
    }

    private val navigationStatus: NavigationStatus = mockk {
        every { stepIndex } returns 0
        every { legIndex } returns 0
        every { remainingLegDistance } returns 80.0f
        every { remainingLegDuration } returns 10000
        every { remainingStepDistance } returns 15.0f
        every { remainingStepDuration } returns 300
        every { routeState } returns RouteState.TRACKING
        every { bannerInstruction } returns null
        every { voiceInstruction } returns null
        every { inTunnel } returns false
        every { upcomingPassiveManeuvers } returns emptyList()
    }

    private val incidentPassiveManeuver = createPassiveManeuver(
        hasLength = true,
        type = PassiveManeuverType.KINCIDENT
    )

    private val tunnelEntrancePassiveManeuver = createPassiveManeuver(
        hasLength = true,
        type = PassiveManeuverType.KTUNNEL_ENTRANCE
    )

    private val borderCrossingPassiveManeuver = createPassiveManeuver(
        hasLength = false,
        type = PassiveManeuverType.KBORDER_CROSSING,
        borderCrossingInfo = PassiveManeuverBorderCrossingInfo(
            PassiveManeuverAdminInfo("fromAlpha", "from"),
            PassiveManeuverAdminInfo("toAlpha", "to")
        )
    )

    private val tollCollectionPassiveManeuver = createPassiveManeuver(
        hasLength = false,
        type = PassiveManeuverType.KTOLL_COLLECTION_POINT,
        tollCollectionInfo = PassiveManeuverTollCollectionInfo(
            PassiveManeuverTollCollectionType.KTOLL_GANTRY
        )
    )

    private val unknownTollCollectionPassiveManeuver = createPassiveManeuver(
        hasLength = false,
        type = PassiveManeuverType.KTOLL_COLLECTION_POINT,
        tollCollectionInfo = null
    )

    private val restStopPassiveManeuver = createPassiveManeuver(
        hasLength = false,
        type = PassiveManeuverType.KSERVICE_AREA,
        serviceAreaInfo = PassiveManeuverServiceAreaInfo(PassiveManeuverServiceAreaType.KREST_AREA)
    )

    private val unknownRestStopPassiveManeuver = createPassiveManeuver(
        hasLength = false,
        type = PassiveManeuverType.KSERVICE_AREA,
        serviceAreaInfo = null
    )

    private val restrictedAreaPassiveManeuver = createPassiveManeuver(
        hasLength = true,
        type = PassiveManeuverType.KRESTRICTED_AREA
    )

    private fun createPassiveManeuver(
        hasLength: Boolean,
        type: PassiveManeuverType,
        incidentInfo: PassiveManeuverIncidentInfo? = null,
        tunnelInfo: PassiveManeuverTunnelInfo? = null,
        borderCrossingInfo: PassiveManeuverBorderCrossingInfo? = null,
        tollCollectionInfo: PassiveManeuverTollCollectionInfo? = null,
        serviceAreaInfo: PassiveManeuverServiceAreaInfo? = null
    ) = PassiveManeuver(
        type,
        123.0,
        if (hasLength) 456.0 else null,
        Point.fromLngLat(10.0, 20.0),
        1,
        if (hasLength) Point.fromLngLat(33.0, 44.0) else Point.fromLngLat(10.0, 20.0),
        if (hasLength) 2 else 1,
        incidentInfo, tunnelInfo, borderCrossingInfo, tollCollectionInfo, serviceAreaInfo
    )

    private val defaultDistanceToStart = 1234.0
    private fun PassiveManeuver.toUpcomingManeuver(
        distanceToStart: Double = defaultDistanceToStart
    ) = UpcomingPassiveManeuver(this, distanceToStart)
}
