package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossing
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingAdminInfo
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingInfo
import com.mapbox.navigation.base.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentCongestion
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentImpact
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentInfo
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentType
import com.mapbox.navigation.base.trip.model.roadobject.restrictedarea.RestrictedArea
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStopType
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollectionType
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.Tunnel
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.TunnelInfo
import com.mapbox.navigator.IncidentCongestionDescription
import com.mapbox.navigator.MatchedRoadObjectLocation
import com.mapbox.navigator.RoadObject
import com.mapbox.navigator.RoadObjectMetadata
import com.mapbox.navigator.RoadObjectProvider
import com.mapbox.navigator.RouteAlertLocation
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.util.Date

private typealias SDKRouteAlertLocation =
    com.mapbox.navigation.base.trip.model.roadobject.location.RouteAlertLocation

private typealias SDKRoadObjectProvider =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider

// https://github.com/mapbox/mapbox-navigation-android/issues/4492
@Ignore
class RoadObjectMapperTest {

    private val shape: Geometry = Point.fromLngLat(LONGITUDE, LATITUDE)
    private val location = SDKRouteAlertLocation(shape)

    @Test
    fun `tunnel entrance alert is parsed correctly`() {
        val nativeObject = tunnel

        val expected = Tunnel(
            ID,
            TunnelInfo(TUNNEL_NAME),
            LENGTH,
            location,
            SDKRoadObjectProvider.MAPBOX,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.TUNNEL, roadObject.objectType)
    }

    @Test
    fun `country border crossing alert is parsed correctly`() {
        val nativeObject = countryBorderCrossing

        val expected = CountryBorderCrossing(
            ID,
            CountryBorderCrossingInfo(
                CountryBorderCrossingAdminInfo(USA_CODE_2, USA_CODE_3),
                CountryBorderCrossingAdminInfo(CANADA_CODE_2, CANADA_CODE_3)
            ),
            LENGTH,
            location,
            SDKRoadObjectProvider.MAPBOX,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.COUNTRY_BORDER_CROSSING, roadObject.objectType)
    }

    @Test
    fun `toll collection alert is parsed correctly (gantry)`() {
        val nativeObject = tollCollectionGantry

        val expected = TollCollection(
            ID,
            TollCollectionType.TOLL_GANTRY,
            LENGTH,
            location,
            SDKRoadObjectProvider.MAPBOX,
            nativeObject
        )
        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.TOLL_COLLECTION, roadObject.objectType)
    }

    @Test
    fun `toll collection alert is parsed correctly (booth)`() {
        val nativeObject = tollCollectionBooth

        val expected = TollCollection(
            ID,
            TollCollectionType.TOLL_BOOTH,
            LENGTH,
            location,
            SDKRoadObjectProvider.MAPBOX,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.TOLL_COLLECTION, roadObject.objectType)
    }

    @Test
    fun `rest stop alert is parsed correctly (rest)`() {
        val nativeObject = restStopRest

        val expected = RestStop(
            ID,
            RestStopType.REST_AREA,
            LENGTH,
            location,
            SDKRoadObjectProvider.MAPBOX,
            nativeObject
        )
        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.REST_STOP, roadObject.objectType)
    }

    @Test
    fun `rest stop alert is parsed correctly (service)`() {
        val nativeObject = restStopService

        val expected = RestStop(
            ID,
            RestStopType.SERVICE_AREA,
            LENGTH,
            location,
            SDKRoadObjectProvider.MAPBOX,
            nativeObject
        )
        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.REST_STOP, roadObject.objectType)
    }

    @Test
    fun `restricted area alert is parsed correctly`() {
        val nativeObject = restrictedArea

        val expected = RestrictedArea(
            ID,
            LENGTH,
            location,
            SDKRoadObjectProvider.MAPBOX,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.RESTRICTED_AREA, roadObject.objectType)
    }

    @Test
    fun `incident alert collection is parsed correctly`() {
        val nativeObject = incident

        val expected = Incident(
            ID,
            IncidentInfo(
                INCIDENT_ID,
                IncidentType.CONSTRUCTION,
                IncidentImpact.LOW,
                IncidentCongestion(4),
                INCIDENT_ROAD_CLOSED,
                INCIDENT_CREATION_TIME,
                INCIDENT_START_TIME,
                INCIDENT_END_TIME,
                INCIDENT_DESCRIPTION,
                INCIDENT_SUB_TYPE,
                INCIDENT_SUB_TYPE_DESCRIPTION,
                INCIDENT_ALERT_CODES,
                USA_CODE_2,
                USA_CODE_3,
                listOf(INCIDENT_LANES_BLOCKED),
                INCIDENT_LONG_DESCRIPTION,
                INCIDENT_LANES_CLEAR_DESC,
                INCIDENT_NUM_LANES_BLOCKED
            ),
            LENGTH,
            location,
            SDKRoadObjectProvider.MAPBOX,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.INCIDENT, roadObject.objectType)
    }

    private val incident = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.INCIDENT,
        incidentInfo = com.mapbox.navigator.IncidentInfo(
            INCIDENT_ID,
            INCIDENT_OPEN_LR,
            com.mapbox.navigator.IncidentType.CONSTRUCTION,
            INCIDENT_CREATION_TIME,
            INCIDENT_START_TIME,
            INCIDENT_END_TIME,
            USA_CODE_2,
            USA_CODE_3,
            listOf(INCIDENT_LANES_BLOCKED),
            INCIDENT_ROAD_CLOSED,
            com.mapbox.navigator.IncidentCongestion(4, IncidentCongestionDescription.LIGHT),
            com.mapbox.navigator.IncidentImpact.LOW,
            INCIDENT_DESCRIPTION,
            INCIDENT_SUB_TYPE,
            INCIDENT_SUB_TYPE_DESCRIPTION,
            INCIDENT_ALERT_CODES,
            INCIDENT_LONG_DESCRIPTION,
            INCIDENT_LANES_CLEAR_DESC,
            INCIDENT_NUM_LANES_BLOCKED
        )
    )

    private val tunnel = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.TUNNEL,
        tunnelInfo = com.mapbox.navigator.TunnelInfo(TUNNEL_NAME)
    )

    private val countryBorderCrossing = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.BORDER_CROSSING,
        countryBorderCrossingInfo = com.mapbox.navigator.BorderCrossingInfo(
            com.mapbox.navigator.AdminInfo(USA_CODE_3, USA_CODE_2),
            com.mapbox.navigator.AdminInfo(CANADA_CODE_3, CANADA_CODE_2)
        )
    )

    private val tollCollectionGantry = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.TOLL_COLLECTION_POINT,
        tollCollectionInfo = com.mapbox.navigator.TollCollectionInfo(
            com.mapbox.navigator.TollCollectionType.TOLL_GANTRY
        )
    )

    private val tollCollectionBooth = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.TOLL_COLLECTION_POINT,
        tollCollectionInfo = com.mapbox.navigator.TollCollectionInfo(
            com.mapbox.navigator.TollCollectionType.TOLL_BOOTH
        )
    )

    private val restStopRest = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.SERVICE_AREA,
        serviceAreaInfo = com.mapbox.navigator.ServiceAreaInfo(
            com.mapbox.navigator.ServiceAreaType.REST_AREA
        )
    )

    private val restStopService = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.SERVICE_AREA,
        serviceAreaInfo = com.mapbox.navigator.ServiceAreaInfo(
            com.mapbox.navigator.ServiceAreaType.SERVICE_AREA
        )
    )

    private val restrictedArea = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.RESTRICTED_AREA
    )

    private fun createRoadObject(
        type: com.mapbox.navigator.RoadObjectType,
        incidentInfo: com.mapbox.navigator.IncidentInfo? = null,
        tunnelInfo: com.mapbox.navigator.TunnelInfo? = null,
        countryBorderCrossingInfo: com.mapbox.navigator.BorderCrossingInfo? = null,
        tollCollectionInfo: com.mapbox.navigator.TollCollectionInfo? = null,
        serviceAreaInfo: com.mapbox.navigator.ServiceAreaInfo? = null
    ): RoadObject {
        val metadata = when (type) {
            com.mapbox.navigator.RoadObjectType.INCIDENT ->
                RoadObjectMetadata.valueOf(incidentInfo!!)
            com.mapbox.navigator.RoadObjectType.TUNNEL ->
                RoadObjectMetadata.valueOf(tunnelInfo!!)
            com.mapbox.navigator.RoadObjectType.BORDER_CROSSING ->
                RoadObjectMetadata.valueOf(countryBorderCrossingInfo!!)
            com.mapbox.navigator.RoadObjectType.TOLL_COLLECTION_POINT ->
                RoadObjectMetadata.valueOf(tollCollectionInfo!!)
            com.mapbox.navigator.RoadObjectType.SERVICE_AREA ->
                RoadObjectMetadata.valueOf(serviceAreaInfo!!)
            else -> mockk()
        }

        val routeAlertLocation: RouteAlertLocation = mockk()
        val location = MatchedRoadObjectLocation.valueOf(routeAlertLocation)

        return RoadObject(
            ID,
            LENGTH,
            location,
            type,
            RoadObjectProvider.MAPBOX,
            metadata
        )
    }

    companion object {
        private const val ID = "roadObjectId"
        private const val LENGTH = 456.0
        private const val LATITUDE = 5353.3
        private const val LONGITUDE = 2020.20
        private const val INCIDENT_ID = "incident_id"
        private const val INCIDENT_OPEN_LR = "incident_open_lr"
        private const val INCIDENT_LANES_BLOCKED = "incident_lanes_blocked"
        private const val INCIDENT_DESCRIPTION = "incident_description"
        private const val INCIDENT_LONG_DESCRIPTION = "incident_long_description"
        private const val INCIDENT_SUB_TYPE = "incident_sub_type"
        private const val INCIDENT_SUB_TYPE_DESCRIPTION = "incident_sub_type_description"
        private const val INCIDENT_LANES_CLEAR_DESC = "incident_lanes_clear_desc"
        private const val INCIDENT_NUM_LANES_BLOCKED = 10L
        private const val INCIDENT_ROAD_CLOSED = true
        private val INCIDENT_ALERT_CODES = listOf(10, 20, 30)
        private val INCIDENT_CREATION_TIME = Date(40)
        private val INCIDENT_START_TIME = Date(60)
        private val INCIDENT_END_TIME = Date(80)
        private const val USA_CODE_2 = "US"
        private const val USA_CODE_3 = "USA"
        private const val CANADA_CODE_2 = "CA"
        private const val CANADA_CODE_3 = "CAN"
        private const val TUNNEL_NAME = "tunnel name"
    }
}
