package com.mapbox.navigation.base.internal.factory

import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.getUpdatedObjectsAhead
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.toUpcomingRoadObjects
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.SDKAmenity
import com.mapbox.navigation.base.trip.model.roadobject.SDKAmenityType
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossing
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingAdminInfo
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingInfo
import com.mapbox.navigation.base.trip.model.roadobject.ic.Interchange
import com.mapbox.navigation.base.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentCongestion
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentImpact
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentInfo
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentType
import com.mapbox.navigation.base.trip.model.roadobject.jct.Junction
import com.mapbox.navigation.base.trip.model.roadobject.merge.MergingArea
import com.mapbox.navigation.base.trip.model.roadobject.merge.MergingAreaInfo
import com.mapbox.navigation.base.trip.model.roadobject.merge.MergingAreaType
import com.mapbox.navigation.base.trip.model.roadobject.notification.Notification
import com.mapbox.navigation.base.trip.model.roadobject.railwaycrossing.RailwayCrossing
import com.mapbox.navigation.base.trip.model.roadobject.railwaycrossing.RailwayCrossingInfo
import com.mapbox.navigation.base.trip.model.roadobject.restrictedarea.RestrictedArea
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStopType
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollectionType
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.Tunnel
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.TunnelInfo
import com.mapbox.navigator.Amenity
import com.mapbox.navigator.IncidentCongestionDescription
import com.mapbox.navigator.LocalizedString
import com.mapbox.navigator.MatchedRoadObjectLocation
import com.mapbox.navigator.RoadObject
import com.mapbox.navigator.RoadObjectMetadata
import com.mapbox.navigator.RoadObjectProvider
import com.mapbox.navigator.RouteAlertLocation
import com.mapbox.navigator.UpcomingRouteAlert
import com.mapbox.navigator.UpcomingRouteAlertUpdate
import com.mapbox.navigator.match.openlr.Standard
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

private typealias SDKRouteAlertLocation =
    com.mapbox.navigation.base.trip.model.roadobject.location.RouteAlertLocation

private typealias SDKRoadObjectProvider =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider

class RoadObjectFactoryTest {

    private val shape: Geometry = Point.fromLngLat(LONGITUDE, LATITUDE)
    private val location = SDKRouteAlertLocation(shape)

    @Test
    fun `buildRoadObject - tunnel entrance alert is parsed correctly`() {
        val nativeObject = tunnel

        val expected = Tunnel(
            ID,
            TunnelInfo(TUNNEL_NAME),
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.TUNNEL, roadObject.objectType)
    }

    @Test
    fun `buildRoadObject - country border crossing alert is parsed correctly`() {
        val nativeObject = countryBorderCrossing

        val expected = CountryBorderCrossing(
            ID,
            CountryBorderCrossingInfo(
                CountryBorderCrossingAdminInfo(USA_CODE_2, USA_CODE_3),
                CountryBorderCrossingAdminInfo(CANADA_CODE_2, CANADA_CODE_3)
            ),
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.COUNTRY_BORDER_CROSSING, roadObject.objectType)
    }

    @Test
    fun `buildRoadObject - toll collection alert is parsed correctly (gantry)`() {
        val nativeObject = tollCollectionGantry

        val expected = TollCollection(
            ID,
            TollCollectionType.TOLL_GANTRY,
            "toll_name_1",
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )
        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject) as TollCollection

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.TOLL_COLLECTION, roadObject.objectType)
        assertEquals("toll_name_1", roadObject.name)
    }

    @Test
    fun `buildRoadObject - toll collection alert is parsed correctly (booth)`() {
        val nativeObject = tollCollectionBooth

        val expected = TollCollection(
            ID,
            TollCollectionType.TOLL_BOOTH,
            "toll_name_2",
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject) as TollCollection

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.TOLL_COLLECTION, roadObject.objectType)
        assertEquals("toll_name_2", roadObject.name)
    }

    @Test
    fun `buildRoadObject - rest stop alert is parsed correctly (rest)`() {
        val nativeObject = restStopRest

        val expected = RestStop(
            id = ID,
            restStopType = RestStopType.REST_AREA,
            name = "rest_stop_name",
            amenities = listOf(
                SDKAmenity(
                    type = SDKAmenityType.ATM,
                    name = amenityATM.name,
                    brand = amenityATM.brand
                )
            ),
            guideMapUri = "some_uri",
            length = LENGTH,
            provider = SDKRoadObjectProvider.MAPBOX,
            isUrban = false,
            nativeRoadObject = nativeObject
        )
        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject) as RestStop

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.REST_STOP, roadObject.objectType)
        assertEquals("rest_stop_name", roadObject.name)
        assertEquals("some_uri", roadObject.guideMapUri)
    }

    @Test
    fun `buildRoadObject - rest stop alert is parsed correctly (service)`() {
        val nativeObject = restStopService

        val expected = RestStop(
            id = ID,
            restStopType = RestStopType.SERVICE_AREA,
            name = "rest_area_name",
            amenities = listOf(
                SDKAmenity(
                    type = SDKAmenityType.ATM,
                    name = amenityATM.name,
                    brand = amenityATM.brand
                )
            ),
            guideMapUri = "some_uri",
            length = LENGTH,
            provider = SDKRoadObjectProvider.MAPBOX,
            isUrban = false,
            nativeRoadObject = nativeObject
        )
        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject) as RestStop

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.REST_STOP, roadObject.objectType)
        assertEquals("rest_area_name", roadObject.name)
        assertEquals("some_uri", roadObject.guideMapUri)
    }

    @Test
    fun `buildRoadObject - restricted area alert is parsed correctly`() {
        val nativeObject = restrictedArea

        val expected = RestrictedArea(
            ID,
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.RESTRICTED_AREA, roadObject.objectType)
    }

    @Test
    fun `buildRoadObject - incident alert collection is parsed correctly`() {
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
                INCIDENT_NUM_LANES_BLOCKED,
                listOf(INCIDENT_AFFECTED_ROAD_NAME),
            ),
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertNotNull(roadObject as Incident)
        assertEquals(RoadObjectType.INCIDENT, roadObject.objectType)
        assertEquals(expected.info.toString(), roadObject.info.toString())
        assertEquals(expected.length, roadObject.length)
        assertEquals(expected.location, roadObject.location)
        assertEquals(expected.provider, roadObject.provider)
        assertEquals(expected.isUrban, roadObject.isUrban)
        assertEquals(expected.nativeRoadObject, nativeObject)
    }

    @Test
    fun `buildRoadObject - railway crossing alert is parsed correctly`() {
        val nativeObject = railwayCrossing

        val expected = RailwayCrossing(
            ID,
            RailwayCrossingInfo(),
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.RAILWAY_CROSSING, roadObject.objectType)
    }

    @Test
    fun `buildRoadObject - merging area from left is parsed correctly`() {
        val nativeObject = mergingAreaLeft

        val expected = MergingArea(
            ID,
            MergingAreaInfo(MergingAreaType.FROM_LEFT),
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.MERGING_AREA, roadObject.objectType)
    }

    @Test
    fun `buildRoadObject - merging area from right is parsed correctly`() {
        val nativeObject = mergingAreaRight

        val expected = MergingArea(
            ID,
            MergingAreaInfo(MergingAreaType.FROM_RIGHT),
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.MERGING_AREA, roadObject.objectType)
    }

    @Test
    fun `buildRoadObject - merging area from both is parsed correctly`() {
        val nativeObject = mergingAreaBothSides

        val expected = MergingArea(
            ID,
            MergingAreaInfo(MergingAreaType.FROM_BOTH_SIDES),
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
        assertEquals(expected.hashCode(), roadObject.hashCode())
        assertEquals(expected.toString(), roadObject.toString())
        assertEquals(RoadObjectType.MERGING_AREA, roadObject.objectType)
    }

    @Test
    fun `buildRoadObject - unsupported notification`() {
        val nativeObject = notification

        val expected = Notification(
            ID,
            LENGTH,
            SDKRoadObjectProvider.MAPBOX,
            false,
            nativeObject
        )

        val roadObject = RoadObjectFactory.buildRoadObject(nativeObject)

        assertEquals(expected, roadObject)
    }

    @Test
    fun `toUpcomingRoadObjects - should map native to SDK UpcomingRoadObjects`() {
        val nativeObjects: List<com.mapbox.navigator.UpcomingRouteAlert> = listOf(
            tunnel,
            countryBorderCrossing,
            tollCollectionGantry,
            tollCollectionBooth,
            restStopRest,
            restStopService,
            restrictedArea,
            incident,
            railwayCrossing,
            ic,
            jct,
            mergingAreaLeft,
            notification,
        ).mapIndexed { distanceToStart, roadObject ->
            UpcomingRouteAlert(roadObject, distanceToStart.toDouble())
        }
        // notification object isn't supported yet,
        // see https://mapbox.atlassian.net/browse/NAVAND-1311
        val unsupportedObjectsCount = 1

        val sdkObjects = nativeObjects.toUpcomingRoadObjects()

        assertEquals(nativeObjects.size - unsupportedObjectsCount, sdkObjects.size)
        assertTrue(sdkObjects[0].roadObject is Tunnel)
        assertTrue(sdkObjects[1].roadObject is CountryBorderCrossing)
        assertTrue(sdkObjects[2].roadObject is TollCollection)
        assertTrue(sdkObjects[3].roadObject is TollCollection)
        assertTrue(sdkObjects[4].roadObject is RestStop)
        assertTrue(sdkObjects[5].roadObject is RestStop)
        assertTrue(sdkObjects[6].roadObject is RestrictedArea)
        assertTrue(sdkObjects[7].roadObject is Incident)
        assertTrue(sdkObjects[8].roadObject is RailwayCrossing)
        assertTrue(sdkObjects[9].roadObject is Interchange)
        assertTrue(sdkObjects[10].roadObject is Junction)
        assertTrue(sdkObjects[11].roadObject is MergingArea)
        sdkObjects.dropLast(unsupportedObjectsCount).forEachIndexed { distanceToStart, obj ->
            assertEquals(distanceToStart.toDouble(), obj.distanceToStart)
        }
    }

    @Test
    fun `create upcoming route objects reusing existing`() {
        val firstUpdate: List<UpcomingRouteAlert> = listOf(
            UpcomingRouteAlert(
                createRoadObject(
                    type = com.mapbox.navigator.RoadObjectType.RESTRICTED_AREA,
                    location = matchedRoadObjectLocation(location.shape),
                    id = "0"
                ),
                5.0
            ),
            UpcomingRouteAlert(
                createRoadObject(
                    type = com.mapbox.navigator.RoadObjectType.RESTRICTED_AREA,
                    location = matchedRoadObjectLocation(location.shape),
                    id = "1"
                ),
                9.0
            ),
            UpcomingRouteAlert(
                createRoadObject(
                    type = com.mapbox.navigator.RoadObjectType.RESTRICTED_AREA,
                    location = matchedRoadObjectLocation(location.shape),
                    id = "2"
                ),
                10.0
            ),
        )
        val secondUpdate: List<UpcomingRouteAlertUpdate> = listOf(
            UpcomingRouteAlertUpdate("0", 4.0),
            UpcomingRouteAlertUpdate("2", 8.0),
        )
        val existingUpcomingRoadObjects = firstUpdate.toUpcomingRoadObjects()

        val roadObjects = existingUpcomingRoadObjects.getUpdatedObjectsAhead(secondUpdate)

        assertEquals(2, roadObjects.size)
        assertEquals(4.0, roadObjects[0].distanceToStart)
        assertEquals(8.0, roadObjects[1].distanceToStart)
        assertTrue(roadObjects[0].roadObject === existingUpcomingRoadObjects[0].roadObject)
        assertTrue(roadObjects[1].roadObject === existingUpcomingRoadObjects[2].roadObject)
    }

    private fun matchedRoadObjectLocation(geometry: Geometry): MatchedRoadObjectLocation {
        return MatchedRoadObjectLocation.valueOf(object : RouteAlertLocation(1) {
            override fun getShape(): Geometry = geometry
        })
    }

    private val incident = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.INCIDENT,
        location = matchedRoadObjectLocation(location.shape),
        incidentInfo = com.mapbox.navigator.IncidentInfo(
            INCIDENT_ID,
            com.mapbox.navigator.match.openlr.OpenLR(INCIDENT_OPEN_LR, Standard.TOM_TOM),
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
            HashMap(),
            INCIDENT_LONG_DESCRIPTION,
            INCIDENT_LANES_CLEAR_DESC,
            INCIDENT_NUM_LANES_BLOCKED,
            listOf(INCIDENT_AFFECTED_ROAD_NAME),
        )
    )

    private val tunnel = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.TUNNEL,
        location = matchedRoadObjectLocation(location.shape),
        tunnelInfo = com.mapbox.navigator.TunnelInfo("id#0", TUNNEL_NAME)
    )

    private val railwayCrossing = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.RAILWAY_CROSSING,
        location = matchedRoadObjectLocation(location.shape),
        railwayCrossingInfo = com.mapbox.navigator.RailwayCrossingInfo("id#1")
    )

    private val mergingAreaLeft = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.MERGING_AREA,
        location = matchedRoadObjectLocation(location.shape),
        mergingAreaInfo = com.mapbox.navigator.MergingAreaInfo(
            "id#0",
            com.mapbox.navigator.MergingAreaType.FROM_LEFT
        )
    )

    private val mergingAreaRight = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.MERGING_AREA,
        location = matchedRoadObjectLocation(location.shape),
        mergingAreaInfo = com.mapbox.navigator.MergingAreaInfo(
            "id#0",
            com.mapbox.navigator.MergingAreaType.FROM_RIGHT
        )
    )

    private val mergingAreaBothSides = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.MERGING_AREA,
        location = matchedRoadObjectLocation(location.shape),
        mergingAreaInfo = com.mapbox.navigator.MergingAreaInfo(
            "id#0",
            com.mapbox.navigator.MergingAreaType.FROM_BOTH
        )
    )

    private val ic = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.IC,
        location = matchedRoadObjectLocation(location.shape),
        icInfo = com.mapbox.navigator.IcInfo("id#2", listOf(LocalizedString("en", "name")))
    )

    private val jct = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.JCT,
        location = matchedRoadObjectLocation(location.shape),
        jctInfo = com.mapbox.navigator.JctInfo("id#3", listOf(LocalizedString("it", "nome")))
    )

    private val countryBorderCrossing = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.BORDER_CROSSING,
        location = matchedRoadObjectLocation(location.shape),
        countryBorderCrossingInfo = com.mapbox.navigator.BorderCrossingInfo(
            "id#4",
            com.mapbox.navigator.AdminInfo(USA_CODE_3, USA_CODE_2),
            com.mapbox.navigator.AdminInfo(CANADA_CODE_3, CANADA_CODE_2)
        )
    )

    private val tollCollectionGantry = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.TOLL_COLLECTION_POINT,
        location = matchedRoadObjectLocation(location.shape),
        tollCollectionInfo = com.mapbox.navigator.TollCollectionInfo(
            "id#5",
            com.mapbox.navigator.TollCollectionType.TOLL_GANTRY,
            "toll_name_1"
        )
    )

    private val tollCollectionBooth = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.TOLL_COLLECTION_POINT,
        location = matchedRoadObjectLocation(location.shape),
        tollCollectionInfo = com.mapbox.navigator.TollCollectionInfo(
            "id#6",
            com.mapbox.navigator.TollCollectionType.TOLL_BOOTH,
            "toll_name_2",
        )
    )

    private val amenityATM = Amenity(
        com.mapbox.navigator.AmenityType.ATM,
        "amenity_ATM",
        "brand_ATM"
    )

    private val restStopRest = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.SERVICE_AREA,
        location = matchedRoadObjectLocation(location.shape),
        serviceAreaInfo = com.mapbox.navigator.ServiceAreaInfo(
            "id#7",
            com.mapbox.navigator.ServiceAreaType.REST_AREA,
            "rest_stop_name",
            listOf(amenityATM),
            "some_uri",
        )
    )

    private val restStopService = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.SERVICE_AREA,
        location = matchedRoadObjectLocation(location.shape),
        serviceAreaInfo = com.mapbox.navigator.ServiceAreaInfo(
            "id#8",
            com.mapbox.navigator.ServiceAreaType.SERVICE_AREA,
            "rest_area_name",
            listOf(amenityATM),
            "some_uri",
        )
    )

    private val restrictedArea = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.RESTRICTED_AREA,
        location = matchedRoadObjectLocation(location.shape),
    )

    private val notification = createRoadObject(
        type = com.mapbox.navigator.RoadObjectType.NOTIFICATION,
        location = matchedRoadObjectLocation(location.shape)
    )

    private fun createRoadObject(
        type: com.mapbox.navigator.RoadObjectType,
        location: MatchedRoadObjectLocation,
        incidentInfo: com.mapbox.navigator.IncidentInfo? = null,
        tunnelInfo: com.mapbox.navigator.TunnelInfo? = null,
        countryBorderCrossingInfo: com.mapbox.navigator.BorderCrossingInfo? = null,
        tollCollectionInfo: com.mapbox.navigator.TollCollectionInfo? = null,
        serviceAreaInfo: com.mapbox.navigator.ServiceAreaInfo? = null,
        railwayCrossingInfo: com.mapbox.navigator.RailwayCrossingInfo? = null,
        icInfo: com.mapbox.navigator.IcInfo? = null,
        jctInfo: com.mapbox.navigator.JctInfo? = null,
        mergingAreaInfo: com.mapbox.navigator.MergingAreaInfo? = null,
        id: String = ID
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
            com.mapbox.navigator.RoadObjectType.RAILWAY_CROSSING ->
                RoadObjectMetadata.valueOf(railwayCrossingInfo!!)
            com.mapbox.navigator.RoadObjectType.IC ->
                RoadObjectMetadata.valueOf(icInfo!!)
            com.mapbox.navigator.RoadObjectType.JCT ->
                RoadObjectMetadata.valueOf(jctInfo!!)
            com.mapbox.navigator.RoadObjectType.MERGING_AREA ->
                RoadObjectMetadata.valueOf(mergingAreaInfo!!)
            else -> mockk()
        }

        return RoadObject(
            id,
            LENGTH,
            location,
            type,
            RoadObjectProvider.MAPBOX,
            metadata,
            false,
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
        private const val INCIDENT_AFFECTED_ROAD_NAME = "affected_road_name"
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
