package com.mapbox.navigation.core.routeoptions.ev

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.URL

class EvRouteOptionsUpdaterTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val nativeRoute = NativeRouteParserRule()

    @Test
    fun `server added charging stations are converted to user provided for charging stations stickiness`() {
        val navigationRoute = createTestEvRoute()
        val routeProgress = createRouteProgress(navigationRoute, remainingWaypointsValue = 5)
        val originalRouteOptions = navigationRoute.routeOptions

        val result = createUpdater().update(
            originalRouteOptions,
            routeProgress,
            mockLocationMatcher(
                point = originalRouteOptions.coordinatesList().first(),
                bearingValue = 33.3f,
            )
        )

        val updatedRouteOptions = (result as RouteOptionsUpdater.RouteOptionsResult.Success).routeOptions

        val waypointsIndexesWithoutOrigin = listOf(3, 5)
        val waypointsIndexes = listOf(0, 3, 5)
        assertEquals(6, updatedRouteOptions.coordinatesList().size)
        assertEquals(
            "user added waypoints shouldn't change",
            originalRouteOptions.coordinatesList().drop(1),
            updatedRouteOptions.coordinatesList().takeByIndexes(waypointsIndexesWithoutOrigin)
        )
        assertEquals(
            "charging station location is incorrect",
            navigationRoute.waypoints?.takeExceptIndexes(waypointsIndexes)?.map { it.location() },
            updatedRouteOptions.coordinatesList().takeExceptIndexes(waypointsIndexes)
        )
        assertEquals(6, updatedRouteOptions.bearingsList()?.size)
        assertEquals(
            "original bearings shouldn't change",
            originalRouteOptions.bearingsList()?.drop(1),
            updatedRouteOptions.bearingsList()?.takeByIndexes(waypointsIndexesWithoutOrigin)
        )
        assertEquals(6, updatedRouteOptions.radiusesList()?.size)
        assertEquals(
            "original radiuses shouldn't change",
            originalRouteOptions.radiusesList()?.drop(1),
            updatedRouteOptions.radiusesList()?.takeByIndexes(waypointsIndexesWithoutOrigin)
        )
        assertEquals(
            "charging stations doesn't have radiuses",
            listOf(null, null, null),
            updatedRouteOptions.radiusesList()?.takeExceptIndexes(waypointsIndexes)
        )
        assertEquals(6, updatedRouteOptions.waypointNamesList()?.size)
        assertEquals(
            "original waypoints names shouldn't change",
            originalRouteOptions.waypointNamesList()?.drop(1),
            updatedRouteOptions.waypointNamesList()?.takeByIndexes(waypointsIndexesWithoutOrigin)
        )
        assertEquals(
            listOf(null, null, null),
            updatedRouteOptions.waypointNamesList()
                ?.takeExceptIndexes(waypointsIndexesWithoutOrigin)
                ?.drop(1)
        )
        assertEquals(6, updatedRouteOptions.waypointTargetsList()?.size)
        assertEquals(
            "original waypoints target list shouldn't change",
            originalRouteOptions.waypointTargetsList()?.drop(1),
            updatedRouteOptions.waypointTargetsList()?.takeByIndexes(waypointsIndexesWithoutOrigin)
        )
        assertEquals(
            listOf(null, null, null),
            updatedRouteOptions.waypointTargetsList()
                ?.takeExceptIndexes(waypointsIndexesWithoutOrigin)
                ?.drop(1)
        )
        assertEquals(6, updatedRouteOptions.approachesList()?.size)
        assertEquals(
            "original waypoints approaches shouldn't change",
            originalRouteOptions.approachesList()?.drop(1),
            updatedRouteOptions.approachesList()?.takeByIndexes(waypointsIndexesWithoutOrigin)
        )
        assertEquals(
            listOf(null, null, null),
            updatedRouteOptions.approachesList()
                ?.takeExceptIndexes(waypointsIndexesWithoutOrigin)
                ?.drop(1)
        )
        assertEquals(6, updatedRouteOptions.layersList()?.size)
        assertEquals(
            "original waypoints layers shouldn't change",
            originalRouteOptions.layersList()?.drop(1),
            updatedRouteOptions.layersList()?.takeByIndexes(waypointsIndexesWithoutOrigin)
        )
        assertEquals(
            listOf(null, null, null),
            updatedRouteOptions.layersList()
                ?.takeExceptIndexes(waypointsIndexesWithoutOrigin)
                ?.drop(1)
        )
        val chargingStationsIdsRaw = updatedRouteOptions.getUnrecognizedProperty("waypoints.charging_station_id")
            ?.asString
        val chargingStationsIds = chargingStationsIdsRaw?.split(";")
        assertEquals(6, chargingStationsIds?.size)
        assertEquals(
            "$chargingStationsIdsRaw contains unexpected values for regular waypoints",
            listOf("", "", ""),
            chargingStationsIds?.takeByIndexes(waypointsIndexes)
        )
        assertEquals(
            listOf("ocm-176564", "ocm-195363", "ocm-134236"),
            chargingStationsIds?.takeExceptIndexes(waypointsIndexes)
        )
        val chargingStationsPowerRaw = updatedRouteOptions
            .getUnrecognizedProperty("waypoints.charging_station_power")
            ?.asString
        val chargingStationsPower = chargingStationsPowerRaw?.split(";")
        assertEquals(6, chargingStationsPower?.size)
        assertEquals(
            "$chargingStationsPowerRaw contains unexpected value for regular waypoints",
            listOf("", "", ""),
            chargingStationsPower?.takeByIndexes(waypointsIndexes)
        )
        assertEquals(
            listOf("150000", "300000", "350000"),
            chargingStationsPower?.takeExceptIndexes(waypointsIndexes)
        )
        val chargingStationsCurrentTypeRaw = updatedRouteOptions
            .getUnrecognizedProperty("waypoints.charging_station_current_type")
            ?.asString
        val chargingStationsCurrentType = chargingStationsCurrentTypeRaw?.split(";")
        assertEquals(6, chargingStationsPower?.size)
        assertEquals(
            "$chargingStationsCurrentTypeRaw contains unexpected value for regular waypoints",
            listOf("", "", ""),
            chargingStationsCurrentType?.takeByIndexes(waypointsIndexes)
        )
        assertEquals(
            listOf("dc", "dc", "dc"),
            chargingStationsCurrentType?.takeExceptIndexes(waypointsIndexes)
        )
        assertEquals(
            "Directions API shouldn't add new charging stations",
            false,
            updatedRouteOptions.getUnrecognizedProperty("ev_add_charging_stops")?.asBoolean
        )
        assertEquals(
            "snapping include static closures shouldn't change",
            originalRouteOptions.snappingIncludeStaticClosuresList()?.drop(1),
            updatedRouteOptions.snappingIncludeStaticClosuresList()
                ?.takeByIndexes(waypointsIndexesWithoutOrigin)
        )
        assertEquals(
            "charging stations aren't snapped to static closures",
            listOf(null, null, null),
            updatedRouteOptions.snappingIncludeStaticClosuresList()
                ?.takeExceptIndexes(waypointsIndexes)
        )
        assertEquals(
            "snapping include closures shouldn't change for original waypoits",
            originalRouteOptions.snappingIncludeClosuresList()?.drop(1),
            updatedRouteOptions.snappingIncludeClosuresList()
                ?.takeByIndexes(waypointsIndexesWithoutOrigin)
        )
        assertEquals(
            "charging stations aren't snapped to closures",
            listOf(null, null, null),
            updatedRouteOptions.snappingIncludeClosuresList()
                ?.takeExceptIndexes(waypointsIndexes)
        )
    }

    @Test
    fun `server added charging stations are converted to user provided for charging stations stickiness after passing the 3th waypoint`() {
        val navigationRoute = createTestEvRoute()
        val routeProgress = createRouteProgress(navigationRoute, remainingWaypointsValue = 2)
        val originalRouteOptions = navigationRoute.routeOptions
        val testBearing = 55.5f
        val testCurrentLocation = Point.fromLngLat(11.819678, 50.677717)
        val testZLevel = 0

        val result = createUpdater().update(
            originalRouteOptions,
            routeProgress,
            mockLocationMatcher(
                point = testCurrentLocation,
                bearingValue = testBearing,
                zLevelValue = testZLevel
            )
        )

        val updatedRouteOptions = (result as RouteOptionsUpdater.RouteOptionsResult.Success).routeOptions

        assertEquals(
            listOf(
                testCurrentLocation,
                // charging station location
                Point.fromLngLat(12.137587, 51.290247),
                originalRouteOptions.coordinatesList().last()
            ),
            updatedRouteOptions.coordinatesList()
        )
        assertEquals(
            listOf(
                testBearing.toDouble(),
                null,
                originalRouteOptions.bearingsList()?.last()?.angle()
            ),
            updatedRouteOptions.bearingsList()?.map { it?.angle() }
        )
        assertEquals(
            listOf(
                null,
                originalRouteOptions.radiusesList()?.last(),
            ),
            updatedRouteOptions.radiusesList()
                ?.drop(1) // new origin's radius is out of the scope of this test
        )
        assertEquals(
            listOf(
                null, // charging station
                originalRouteOptions.waypointNamesList()?.last()
            ),
            updatedRouteOptions.waypointNamesList()
                ?.drop(1) // new origin's name is out of the scope of this test
        )
        assertEquals(
            listOf(null, null, originalRouteOptions.waypointTargetsList()?.last()),
            updatedRouteOptions.waypointTargetsList()
        )
        assertEquals(
            listOf(
                null,
                null,
                originalRouteOptions.approachesList()?.last()
            ),
            updatedRouteOptions.approachesList()
        )
        assertEquals(
            listOf(
                testZLevel,
                null,
                originalRouteOptions.layersList()?.last()
            ),
            updatedRouteOptions.layersList()
        )
        val chargingStationsIdsRaw = updatedRouteOptions
            .getUnrecognizedProperty("waypoints.charging_station_id")
            ?.asString
        val chargingStationsIds = chargingStationsIdsRaw?.split(";")
        assertEquals(
            listOf("", "ocm-134236", ""),
            chargingStationsIds
        )
        val chargingStationsPowerRaw = updatedRouteOptions
            .getUnrecognizedProperty("waypoints.charging_station_power")
            ?.asString
        val chargingStationsPower = chargingStationsPowerRaw?.split(";")
        assertEquals(
            listOf("", "350000", ""),
            chargingStationsPower
        )
        val chargingStationsCurrentTypeRaw = updatedRouteOptions
            .getUnrecognizedProperty("waypoints.charging_station_current_type")
            ?.asString
        val chargingStationsCurrentType = chargingStationsCurrentTypeRaw?.split(";")
        assertEquals(
            listOf("", "dc", ""),
            chargingStationsCurrentType
        )
        assertEquals(
            listOf(true, null, false),
            updatedRouteOptions.snappingIncludeStaticClosuresList()
        )
        assertEquals(
            listOf(true, null, true),
            updatedRouteOptions.snappingIncludeClosuresList()
        )
    }

    @Test
    fun `no EV waypoints when destination is almost reached`() {
        val navigationRoute = createTestEvRoute()
        val routeProgress = createRouteProgress(navigationRoute, remainingWaypointsValue = 1)
        val originalRouteOptions = navigationRoute.routeOptions
        val testBearing = 225f
        val testCurrentLocation = Point.fromLngLat(13.373964, 52.654396)
        val testZLevel = 0

        val result = createUpdater().update(
            originalRouteOptions,
            routeProgress,
            mockLocationMatcher(
                point = testCurrentLocation,
                bearingValue = testBearing,
                zLevelValue = testZLevel
            )
        )

        val updatedRouteOptions = (result as RouteOptionsUpdater.RouteOptionsResult.Success)
            .routeOptions

        assertEquals(
            listOf(
                testCurrentLocation,
                originalRouteOptions.coordinatesList().last()
            ),
            updatedRouteOptions.coordinatesList()
        )
        assertEquals(
            listOf(
                testBearing.toDouble(),
                originalRouteOptions.bearingsList()?.last()?.angle()
            ),
            updatedRouteOptions.bearingsList()?.map { it?.angle() }
        )
        assertEquals(
            listOf(
                null,
                originalRouteOptions.radiusesList()?.last(),
            ),
            updatedRouteOptions.radiusesList()
        )
        assertEquals(
            listOf(
                null,
                originalRouteOptions.waypointNamesList()?.last()
            ),
            updatedRouteOptions.waypointNamesList()
        )
        assertEquals(
            listOf(null, originalRouteOptions.waypointTargetsList()?.last()),
            updatedRouteOptions.waypointTargetsList()
        )
        assertEquals(
            listOf(
                null,
                originalRouteOptions.approachesList()?.last()
            ),
            updatedRouteOptions.approachesList()
        )
        assertEquals(
            listOf(
                testZLevel,
                originalRouteOptions.layersList()?.last()
            ),
            updatedRouteOptions.layersList()
        )
        val chargingStationsIdsRaw = updatedRouteOptions.getUnrecognizedProperty("waypoints.charging_station_id")
        assertNull(chargingStationsIdsRaw)
        val chargingStationsPowerRaw = updatedRouteOptions
            .getUnrecognizedProperty("waypoints.charging_station_power")
            ?.asString
        assertNull(chargingStationsPowerRaw)
        val chargingStationsCurrentTypeRaw = updatedRouteOptions
            .getUnrecognizedProperty("waypoints.charging_station_current_type")
            ?.asString
        assertNull(chargingStationsCurrentTypeRaw)
    }

    @Test
    fun `convert server provided charging stations and append to existing user provided for charging stations stickiness`() {
        val navigationRoute = createTestEvRouteWithUserProvidedChargingStations()
        val routeProgress = createRouteProgress(navigationRoute, remainingWaypointsValue = 4)
        val originalRouteOptions = navigationRoute.routeOptions

        val result = createUpdater().update(
            originalRouteOptions,
            routeProgress,
            mockLocationMatcher(
                point = originalRouteOptions.coordinatesList().first(),
                bearingValue = 33.3f,
            )
        )

        val updatedRouteOptions = (result as RouteOptionsUpdater.RouteOptionsResult.Success).routeOptions

        val waypointsIndexes = listOf(0, 4)
        val waypointsCount = 5
        assertEquals(waypointsCount, updatedRouteOptions.coordinatesList().size)
        val chargingStationsIdsRaw = updatedRouteOptions.getUnrecognizedProperty("waypoints.charging_station_id")
            ?.asString
        val chargingStationsIds = chargingStationsIdsRaw?.split(";")
        assertEquals(waypointsCount, chargingStationsIds?.size)
        assertEquals(
            "$chargingStationsIdsRaw contains unexpected values for regular waypoints",
            listOf("", ""),
            chargingStationsIds?.takeByIndexes(waypointsIndexes)
        )
        assertEquals(
            listOf("opis-af2ce792-5f6e-11ed-9c8d-ac1f6be1d08e", "id_1", "opis-9e070a3a-5f6b-11ed-9c8d-ac1f6be1d08e"),
            chargingStationsIds?.takeExceptIndexes(waypointsIndexes)
        )
        val chargingStationsPowerRaw = updatedRouteOptions
            .getUnrecognizedProperty("waypoints.charging_station_power")
            ?.asString
        val chargingStationsPower = chargingStationsPowerRaw?.split(";")
        assertEquals(waypointsCount, chargingStationsPower?.size)
        assertEquals(
            "$chargingStationsPowerRaw contains unexpected value for regular waypoints",
            listOf("", ""),
            chargingStationsPower?.takeByIndexes(waypointsIndexes)
        )
        assertEquals(
            listOf("350000", "100000", "50000"),
            chargingStationsPower?.takeExceptIndexes(waypointsIndexes)
        )
        val chargingStationsCurrentTypeRaw = updatedRouteOptions
            .getUnrecognizedProperty("waypoints.charging_station_current_type")
            ?.asString
        val chargingStationsCurrentType = chargingStationsCurrentTypeRaw?.split(";")
        assertEquals(waypointsCount, chargingStationsPower?.size)
        assertEquals(
            "$chargingStationsCurrentTypeRaw contains unexpected value for regular waypoints",
            listOf("", ""),
            chargingStationsCurrentType?.takeByIndexes(waypointsIndexes)
        )
        assertEquals(
            listOf("dc", "dc", "dc"),
            chargingStationsCurrentType?.takeExceptIndexes(waypointsIndexes)
        )
        assertEquals(
            listOf(true, null, null, null, null),
            updatedRouteOptions.snappingIncludeStaticClosuresList()
        )
        assertEquals(
            listOf(true, null, null, null, null),
            updatedRouteOptions.snappingIncludeClosuresList()
        )
    }

    @Test
    fun `EV route with silent waypoints`() {
        val navigationRoute = createTestEvRoute()
        val routeProgress = createRouteProgress(navigationRoute, remainingWaypointsValue = 5)
        val originalRouteOptions = navigationRoute.routeOptions.toBuilder()
            .waypointIndices("0;2")
            .build()

        val result = createUpdater().update(
            originalRouteOptions,
            routeProgress,
            mockLocationMatcher(
                point = originalRouteOptions.coordinatesList().first(),
                bearingValue = 33.3f,
            )
        )

        assertTrue(result is RouteOptionsUpdater.RouteOptionsResult.Error)
    }

    private fun createTestEvRoute(): NavigationRoute {
        val rawUrl = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/11.587428364032348,48.20148957377813;11.81872714026062,50.67773738599428;13.378818297105255,52.627628120089355?geometries=polyline6&alternatives=false&overview=full&steps=true&continue_straight=true&annotations=state_of_charge&roundabout_exits=true&voice_instructions=true&banner_instructions=true&enable_refresh=true&waypoints_per_route=true&engine=electric&ev_initial_charge=30000&ev_max_charge=50000&ev_connector_types=ccs_combo_type1%2Cccs_combo_type2&energy_consumption_curve=0%2C300%3B20%2C160%3B80%2C140%3B120%2C180&ev_charging_curve=0%2C100000%3B40000%2C70000%3B60000%2C30000%3B80000%2C10000&ev_min_charge_at_charging_station=7000&bearings=1,45;65,45;&radiuses=5;50;unlimited&waypoint_names=origin;test;destination&waypoint_targets=;;13.379077134850064,52.62734923825474&approaches=;curb;&layers=0;0;0&snapping_include_static_closures=false;true;false&snapping_include_closures=true;false;true&waypoints=0;1;2"
        val url = RouteOptions.fromUrl(URL(rawUrl)).let {
            // normalizes accuracy of doubles
            it.toBuilder()
                .coordinatesList(it.coordinatesList())
                .waypointTargetsList(it.waypointTargetsList())
                .build()
        }.toUrl("***").toString()
        val navigationRoute = NavigationRoute.create(
            resourceAsString("testRoute.json"),
            url,
            RouterOrigin.Offboard
        ).first()
        return navigationRoute
    }

    private fun createTestEvRouteWithUserProvidedChargingStations(): NavigationRoute {
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/-122.42302878903037,37.780226502149986;-122.73225023858345,38.458230137668835;-123.2092146120633,39.147413952094894?overview=false&alternatives=true&waypoints_per_route=true&engine=electric&ev_initial_charge=600&ev_max_charge=50000&ev_connector_types=ccs_combo_type1,ccs_combo_type2&energy_consumption_curve=0,300;20,160;80,140;120,180&ev_charging_curve=0,100000;40000,70000;60000,30000;80000,10000&ev_min_charge_at_charging_station=1&access_token=***&waypoints.charging_station_power=;100000;&waypoints.charging_station_current_type=;dc;&waypoints.charging_station_id=;id_1;"
        val navigationRoute = NavigationRoute.create(
            resourceAsString("testRouteWithUserProvidedChargingStation.json"),
            url,
            RouterOrigin.Offboard
        ).first()
        return navigationRoute
    }

    private fun resourceAsString(
        name: String,
        packageName: String = "com.mapbox.navigation.core.routeoptions.ev"
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        return IOUtils.toString(inputStream, "UTF-8")
    }
}

private fun <E> List<E>.takeByIndexes(indexes: List<Int>): List<E> {
    val result = mutableListOf<E>()
    this.forEachIndexed { index, e ->
        if (indexes.contains(index)) {
            result.add(e)
        }
    }
    return result
}

private fun <E> List<E>.takeExceptIndexes(indexes: List<Int>): List<E> {
    val result = mutableListOf<E>()
    this.forEachIndexed { index, e ->
        if (!indexes.contains(index)) {
            result.add(e)
        }
    }
    return result
}

private fun createUpdater() = RouteOptionsUpdater()

private fun createRouteProgress(
    navigationRouteValue: NavigationRoute,
    remainingWaypointsValue: Int,
) = mockk<RouteProgress>(relaxed = true) {
    every { navigationRoute } returns navigationRouteValue
    every { remainingWaypoints } returns remainingWaypointsValue
}

private fun mockLocationMatcher(
    point: Point,
    bearingValue: Float = 3.3f,
    zLevelValue: Int = 0
): LocationMatcherResult = mockk {
    every { enhancedLocation } returns mockk {
        every { latitude } returns point.latitude()
        every { longitude } returns point.longitude()
        every { bearing } returns bearingValue
        every { zLevel } returns zLevelValue
    }
}
