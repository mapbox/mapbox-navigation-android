package com.mapbox.navigation.base

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.EHorizonOptions
import com.mapbox.navigation.base.options.HistoryRecorderOptions
import com.mapbox.navigation.base.options.IncidentsOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.base.trip.model.eh.EHorizon
import com.mapbox.navigation.base.trip.model.eh.EHorizonEdge
import com.mapbox.navigation.base.trip.model.eh.EHorizonEdgeMetadata
import com.mapbox.navigation.base.trip.model.eh.EHorizonGraphPath
import com.mapbox.navigation.base.trip.model.eh.EHorizonGraphPosition
import com.mapbox.navigation.base.trip.model.eh.EHorizonPosition
import com.mapbox.navigation.base.trip.model.eh.RoadName
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectEdgeLocation
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectEnterExitInfo
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectMatcherError
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPassInfo
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectPosition
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossing
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingAdminInfo
import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossingInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.GantryDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.LineDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.PointDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.PolygonDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.RoadObjectDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.distanceinfo.SubGraphDistanceInfo
import com.mapbox.navigation.base.trip.model.roadobject.incident.Incident
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentCongestion
import com.mapbox.navigation.base.trip.model.roadobject.incident.IncidentInfo
import com.mapbox.navigation.base.trip.model.roadobject.location.GantryLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.OpenLRLineLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.OpenLRPointLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.PointLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.PolygonLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.PolylineLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.RoadObjectLocation
import com.mapbox.navigation.base.trip.model.roadobject.location.RouteAlertLocation
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.Tunnel
import com.mapbox.navigation.base.trip.model.roadobject.tunnel.TunnelInfo
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class EqualHashCodeTest {

    /**
     * com.mapbox.navigation.base.options
     */

    @Test
    fun `NavigationOptions hashCode and equals test`() {
        val clazz = NavigationOptions::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `RoutingTilesOptions hashCode and equals test`() {
        val clazz = RoutingTilesOptions::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `IncidentsOptions hashCode and equals test`() {
        val clazz = IncidentsOptions::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `PredictiveCacheLocationOptions hashCode and equals test`() {
        val clazz = PredictiveCacheLocationOptions::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `HistoryRecorderOptions hashCode and equals test`() {
        val clazz = HistoryRecorderOptions::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `DeviceProfile hashCode and equals test`() {
        val clazz = DeviceProfile::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `EHorizonOptions hashCode and equals test`() {
        val clazz = EHorizonOptions::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.route
     */

    @Test
    fun `RouteRefreshOptions hashCode and equals test`() {
        val clazz = RouteRefreshOptions::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `RouteAlternativesOptions hashCode and equals test`() {
        val clazz = RouteAlternativesOptions::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.formatter
     */

    @Test
    fun `DistanceFormatterOptions hashCode and equals test`() {
        val clazz = DistanceFormatterOptions::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model
     */

    @Test
    fun `RouteStepProgress hashCode and equals test`() {
        val clazz = RouteStepProgress::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                Point::class.java,
                Point.fromLngLat(3.0, 4.5),
                Point.fromLngLat(5.0, .5),
            )
            .verify()
    }

    @Test
    fun `RouteProgress hashCode and equals test`() {
        val clazz = RouteProgress::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                RouteLegProgress::class.java,
                RouteLegProgress.Builder().build(),
                RouteLegProgress.Builder().legIndex(4).build(),
            )
            .withPrefabValues(
                List::class.java,
                listOf(Point.fromLngLat(3.0, 4.5)),
                listOf(Point.fromLngLat(5.0, .5)),
            )
            .verify()
    }

    @Test
    fun `RouteLegProgress hashCode and equals test`() {
        val clazz = RouteLegProgress::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                Point::class.java,
                Point.fromLngLat(3.0, 4.5),
                Point.fromLngLat(5.0, .5),
            )
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model.roadobject
     */

    @Test
    fun `RoadObjectMatcherError hashCode and equals test`() {
        val clazz = RoadObjectMatcherError::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `RoadObjectPosition hashCode and equals test`() {
        val clazz = RoadObjectPosition::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                Point::class.java,
                Point.fromLngLat(3.0, 4.5),
                Point.fromLngLat(5.0, .5),
            )
            .verify()
    }

    @Test
    fun `RoadObjectEdgeLocation hashCode and equals test`() {
        val clazz = RoadObjectEdgeLocation::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `RoadObjectPassInfo hashCode and equals test`() {
        val clazz = RoadObjectPassInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `UpcomingRoadObject hashCode and equals test`() {
        val clazz = UpcomingRoadObject::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                RoadObjectDistanceInfo::class.java,
                GantryDistanceInfo("1", RoadObjectType.REST_STOP, 0.0),
                GantryDistanceInfo("2", RoadObjectType.REST_STOP, 1.0),
            )
            .verify()
    }

    @Test
    fun `RoadObject hashCode and equals test`() {
        val clazz = RoadObject::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `RoadObjectEnterExitInfo hashCode and equals test`() {
        val clazz = RoadObjectEnterExitInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model.roadobject.border
     */

    @Test
    fun `CountryBorderCrossingInfo hashCode and equals test`() {
        val clazz = CountryBorderCrossingInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `CountryBorderCrossingAdminInfo hashCode and equals test`() {
        val clazz = CountryBorderCrossingAdminInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `CountryBorderCrossing hashCode and equals test`() {
        val clazz = CountryBorderCrossing::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model.roadobject.location
     */

    @Test
    fun `OpenLRLineLocation hashCode and equals test`() {
        val clazz = OpenLRLineLocation::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `PointLocation hashCode and equals test`() {
        val clazz = PointLocation::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                RoadObjectPosition::class.java,
                RoadObjectPosition(EHorizonGraphPosition(0, 0.0), Point.fromLngLat(3.0, 4.5)),
                RoadObjectPosition(EHorizonGraphPosition(1, 4.0), Point.fromLngLat(5.0, .5)),
            )
            .verify()
    }

    @Test
    fun `RoadObjectLocation hashCode and equals test`() {
        val clazz = RoadObjectLocation::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `RouteAlertLocation hashCode and equals test`() {
        val clazz = RouteAlertLocation::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `PolylineLocation hashCode and equals test`() {
        val clazz = PolylineLocation::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `OpenLRPointLocation hashCode and equals test`() {
        val clazz = OpenLRPointLocation::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `GantryLocation hashCode and equals test`() {
        val clazz = GantryLocation::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                RoadObjectPosition::class.java,
                RoadObjectPosition(EHorizonGraphPosition(0, 0.0), Point.fromLngLat(3.0, 4.5)),
                RoadObjectPosition(EHorizonGraphPosition(1, 4.0), Point.fromLngLat(5.0, .5)),
            )
            .verify()
    }

    @Test
    fun `PolygonLocation hashCode and equals test`() {
        val clazz = PolygonLocation::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                RoadObjectPosition::class.java,
                RoadObjectPosition(EHorizonGraphPosition(0, 0.0), Point.fromLngLat(3.0, 4.5)),
                RoadObjectPosition(EHorizonGraphPosition(1, 4.0), Point.fromLngLat(5.0, .5)),
            )
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model.roadobject.tunnel
     */

    @Test
    fun `TunnelInfo hashCode and equals test`() {
        val clazz = TunnelInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `Tunnel hashCode and equals test`() {
        val clazz = Tunnel::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model.roadobject.distanceinfo
     */

    @Test
    fun `PolygonDistanceInfo hashCode and equals test`() {
        val clazz = PolygonDistanceInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `LineDistanceInfo hashCode and equals test`() {
        val clazz = LineDistanceInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `SubGraphDistanceInfo hashCode and equals test`() {
        val clazz = SubGraphDistanceInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `GantryDistanceInfo hashCode and equals test`() {
        val clazz = GantryDistanceInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `PointDistanceInfo hashCode and equals test`() {
        val clazz = PointDistanceInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model.roadobject.tollcollection
     */

    @Test
    fun `TollCollection hashCode and equals test`() {
        val clazz = TollCollection::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model.roadobject.incident
     */

    @Test
    fun `IncidentInfo hashCode and equals test`() {
        val clazz = IncidentInfo::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `IncidentCongestion hashCode and equals test`() {
        val clazz = IncidentCongestion::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `Incident hashCode and equals test`() {
        val clazz = Incident::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model.roadobject.reststop
     */

    @Test
    fun `RestStop hashCode and equals test`() {
        val clazz = RestStop::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    /**
     * com.mapbox.navigation.base.trip.model.eh
     */

    @Test
    fun `EHorizonEdge hashCode and equals test`() {
        val clazz = EHorizonEdge::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                EHorizonEdge::class.java,
                EHorizonEdge(1, 0, 0.0, emptyList()),
                EHorizonEdge(2, 1, 1.0, emptyList()),
            )
            .verify()
    }

    @Test
    fun `RoadName hashCode and equals test`() {
        val clazz = RoadName::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `EHorizonGraphPosition hashCode and equals test`() {
        val clazz = EHorizonGraphPosition::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `EHorizonPosition hashCode and equals test`() {
        val clazz = EHorizonPosition::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                EHorizonEdge::class.java,
                EHorizonEdge(1, 0, 0.0, emptyList()),
                EHorizonEdge(2, 1, 1.0, emptyList()),
            )
            .verify()
    }

    @Test
    fun `EHorizonEdgeMetadata hashCode and equals test`() {
        val clazz = EHorizonEdgeMetadata::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }

    @Test
    fun `EHorizon hashCode and equals test`() {
        val clazz = EHorizon::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .withPrefabValues(
                EHorizonEdge::class.java,
                EHorizonEdge(1, 0, 0.0, emptyList()),
                EHorizonEdge(2, 1, 1.0, emptyList()),
            )
            .verify()
    }

    @Test
    fun `EHorizonGraphPath hashCode and equals test`() {
        val clazz = EHorizonGraphPath::class.java
        EqualsVerifier.forClass(clazz)
            .usingGetClass()
            .verify()
    }
}
