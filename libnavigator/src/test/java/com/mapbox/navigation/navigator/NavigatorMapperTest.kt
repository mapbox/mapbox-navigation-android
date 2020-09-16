package com.mapbox.navigation.navigator

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.alert.RouteAlertGeometry
import com.mapbox.navigation.base.trip.model.alert.RouteAlertType
import com.mapbox.navigation.base.trip.model.alert.TunnelEntranceAlert
import com.mapbox.navigation.navigator.internal.NavigatorMapper
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.PassiveManeuver
import com.mapbox.navigator.PassiveManeuverBorderCrossingInfo
import com.mapbox.navigator.PassiveManeuverIncidentInfo
import com.mapbox.navigator.PassiveManeuverServiceAreaInfo
import com.mapbox.navigator.PassiveManeuverTollCollectionInfo
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
                incidentPassiveManeuver,
                borderCrossingPassiveManeuver,
                tollCollectionPassiveManeuver,
                serviceAreaPassiveManeuver
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
            TunnelEntranceAlert.Metadata(),
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
    fun `unsupported alert types are ignored in the progress update`() {
        every { navigationStatus.upcomingPassiveManeuvers } returns listOf(
            incidentPassiveManeuver.toUpcomingManeuver(),
            borderCrossingPassiveManeuver.toUpcomingManeuver(),
            tollCollectionPassiveManeuver.toUpcomingManeuver(),
            serviceAreaPassiveManeuver.toUpcomingManeuver()
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
        type = PassiveManeuverType.KINCIDENT
    )

    private val tunnelEntrancePassiveManeuver = createPassiveManeuver(
        type = PassiveManeuverType.KTUNNEL_ENTRANCE
    )

    private val borderCrossingPassiveManeuver = createPassiveManeuver(
        type = PassiveManeuverType.KBORDER_CROSSING
    )

    private val tollCollectionPassiveManeuver = createPassiveManeuver(
        type = PassiveManeuverType.KTOLL_COLLECTION_POINT
    )

    private val serviceAreaPassiveManeuver = createPassiveManeuver(
        type = PassiveManeuverType.KSERVICE_AREA
    )

    private fun createPassiveManeuver(
        type: PassiveManeuverType,
        incidentInfo: PassiveManeuverIncidentInfo? = null,
        tunnelInfo: PassiveManeuverTunnelInfo? = null,
        borderCrossingInfo: PassiveManeuverBorderCrossingInfo? = null,
        tollCollectionInfo: PassiveManeuverTollCollectionInfo? = null,
        serviceAreaInfo: PassiveManeuverServiceAreaInfo? = null
    ) = PassiveManeuver(
        type,
        123.0,
        456.0,
        Point.fromLngLat(10.0, 20.0),
        1,
        Point.fromLngLat(33.0, 44.0),
        2,
        incidentInfo, tunnelInfo, borderCrossingInfo, tollCollectionInfo, serviceAreaInfo
    )

    private val defaultDistanceToStart = 1234.0
    private fun PassiveManeuver.toUpcomingManeuver(
        distanceToStart: Double = defaultDistanceToStart
    ) = UpcomingPassiveManeuver(this, distanceToStart)
}
