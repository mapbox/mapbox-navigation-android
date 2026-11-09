@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route.operations

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData
import com.mapbox.navigation.base.internal.route.testing.toDataRefJava
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The device EV refresh of a route the server built sends only state_of_charge, the charge
 * notifications and the stop's charge numbers from the planner, and sends back the route's own
 * values the merge would drop (nav-native materializeOnlineBuiltResponse). The Kotlin merge must
 * then keep everything else; the route's charge notifications in the window give way to the
 * response's.
 * The NN merge of the same route and response is checked in nav-native
 * ev_refresh_online_built.test.cpp.
 */
class JavaRouteOperationsOnlineBuiltEvRefreshTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `the merge keeps everything but the charge and the charge numbers`() {
        val route = DirectionsResponse.fromJson(ONLINE_EV_ROUTE).routes().first()
        val refreshed = refreshOnDevice(route, DEVICE_RESPONSE)

        val before = JsonParser.parseString(route.toJson()).asJsonObject
        val after = JsonParser.parseString(refreshed.toJson()).asJsonObject
        val charge = listOf("[80,79,70,69,68]", "[67,66,65,64,63]")
        for (leg in 0..1) {
            val was = before.legs()[leg].asJsonObject
            val isNow = after.legs()[leg].asJsonObject
            for (key in listOf("distance", "duration", "congestion_numeric", "historical_speed")) {
                assertEquals(
                    "leg $leg $key",
                    was.annotation()[key],
                    isNow.annotation()[key],
                )
            }
            assertEquals("leg $leg", charge[leg], isNow.annotation()["state_of_charge"].toString())
            assertEquals("leg $leg", was["duration"].asDouble, isNow["duration"].asDouble, 0.0)
            for (key in listOf("incidents", "closures")) {
                assertEquals("leg $leg $key", sorted(was[key]), sorted(isNow[key]))
            }
            assertEquals(
                "leg $leg notifications",
                sorted(was["notifications"]) { !it.isChargeNotification() },
                sorted(isNow["notifications"]) { !it.isChargeNotification() },
            )
        }

        val expectedStation = before.waypoints()[1].deepCopy().asJsonObject
        expectedStation["metadata"].asJsonObject.apply {
            addProperty("charge_time", 720)
            addProperty("charge_to", 52000)
            addProperty("charge_at_arrival", 18000)
        }
        assertEquals(expectedStation.metadata(), after.waypoints()[1].asJsonObject.metadata())
        assertEquals(before.waypoints()[0], after.waypoints()[0])
        assertEquals(before.waypoints()[2], after.waypoints()[2])
        assertEquals(820.0, refreshed.duration(), 0.0)
    }

    @Test
    fun `the merge takes the charge notifications from the device response`() {
        // The response's evInsufficientCharge at 0 counts from the car, so the merge puts it at
        // 2. The route's evInsufficientCharge at 3 and evMinChargeAtDestination at 2 are gone.
        val route = DirectionsResponse.fromJson(ONLINE_EV_ROUTE).routes().first()
        val refreshed = refreshOnDevice(
            route,
            deviceResponse(
                firstLegNotifications = DEVICE_INSUFFICIENT_CHARGE_AT_0,
                secondLegNotifications = DEVICE_MIN_CHARGE_AT_DESTINATION,
            ),
        )

        val legs = refreshed.legs()!!
        assertEquals(listOf<Int?>(2), legs[0].indexesOf("evInsufficientCharge"))
        assertEquals(listOf<Int?>(null), legs[1].indexesOf("evMinChargeAtDestination"))
    }

    @Test
    fun `the merge clears the charge notifications the device response leaves out`() {
        // The charge has recovered, so the planner sent no charge notification.
        val route = DirectionsResponse.fromJson(ONLINE_EV_ROUTE).routes().first()
        assertEquals(listOf<Int?>(3), route.legs()!![0].indexesOf("evInsufficientCharge"))
        assertEquals(listOf<Int?>(2), route.legs()!![1].indexesOf("evMinChargeAtDestination"))

        val refreshed = refreshOnDevice(route, deviceResponse())

        val legs = refreshed.legs()!!
        assertEquals(emptyList<Int?>(), legs[0].indexesOf("evInsufficientCharge"))
        assertEquals(emptyList<Int?>(), legs[1].indexesOf("evMinChargeAtDestination"))
        assertEquals(
            listOf("maxHeight", "stationUnavailable"),
            legs[0].notifications().orEmpty().mapNotNull { it.subtype() }.sorted(),
        )
    }

    // The merge of response onto route with the car 2 points into the first leg.
    private fun refreshOnDevice(route: DirectionsRoute, response: String): DirectionsRoute {
        val operations = JavaRouteOperations(
            DirectionsParsedRouteData(
                route = route,
                routesWaypoint = route.waypoints(),
                requestUUID = "server-uuid",
                routeOptions = ROUTE_OPTIONS,
                routeIndex = 0,
                routerOrigin = RouterOrigin.ONLINE,
                responseOriginAPI = ResponseOriginAPI.DIRECTIONS_API,
            ),
        )
        return operations.refresh(
            response.toDataRefJava(),
            legIndex = 0,
            legGeometryIndex = 2,
            responseTimeElapsedSeconds = 0,
            refreshedDirectionsRouteContext = mockk(relaxed = true),
        ).getOrThrow().routeModelsParsingResult.data.route
    }

    private fun RouteLeg.indexesOf(subtype: String): List<Int?> =
        notifications().orEmpty().filter { it.subtype() == subtype }.map { it.geometryIndex() }

    private fun JsonElement.isChargeNotification(): Boolean =
        asJsonObject["subtype"]?.asString in CHARGE_SUBTYPES

    private fun JsonObject.legs(): JsonArray = this["legs"].asJsonArray

    private fun JsonObject.waypoints(): JsonArray = this["waypoints"].asJsonArray

    private fun JsonObject.annotation(): JsonObject = this["annotation"].asJsonObject

    private fun JsonObject.metadata(): JsonObject = this["metadata"].asJsonObject

    // Number values compared as numbers: the merge may write 3 as 3.0.
    private fun sorted(
        list: JsonElement?,
        keep: (JsonElement) -> Boolean = { true },
    ): List<String> =
        list?.takeIf { it.isJsonArray }?.asJsonArray
            ?.filter(keep)
            ?.map { normalized(it).toString() }
            ?.sorted()
            .orEmpty()

    private fun normalized(element: JsonElement): JsonElement = when {
        element.isJsonObject -> JsonObject().also { copy ->
            element.asJsonObject.entrySet().sortedBy { it.key }
                .forEach { (key, value) -> copy.add(key, normalized(value)) }
        }
        element.isJsonPrimitive && element.asJsonPrimitive.isNumber ->
            JsonParser.parseString(element.asDouble.toString())
        else -> element
    }

    private companion object {
        val ROUTE_OPTIONS: RouteOptions = RouteOptions.builder()
            .profile("driving-traffic")
            .coordinatesList(
                listOf(
                    Point.fromLngLat(11.5, 48.1),
                    Point.fromLngLat(11.505, 48.1),
                    Point.fromLngLat(11.51, 48.1),
                ),
            )
            .build()

        // nav-native ev_refresh_online_built.test.cpp OnlineEvRouteJson. Incident 3 and the
        // second closure end before the car but are listed after objects that overlap it: both
        // merges keep them.
        val ONLINE_EV_ROUTE = """
            {"code": "Ok", "uuid": "server-uuid", "routes": [{
              "distance": 740, "duration": 700, "weight": 700, "weight_name": "auto",
              "geometry": "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
              "legs": [
                {"distance": 370, "duration": 50, "summary": "a", "steps": [],
                 "annotation": {"distance": [74, 74, 74, 74, 74],
                                "duration": [10, 10, 10, 10, 10],
                                "congestion_numeric": [10, 20, 30, 40, 50],
                                "state_of_charge": [80, 79, 78, 77, 76],
                                "historical_speed": [50, 51, 52, 53, 54]},
                 "incidents": [{"id": "1", "type": "construction",
                                "geometry_index_start": 1, "geometry_index_end": 3},
                               {"id": "3", "type": "construction",
                                "geometry_index_start": 1, "geometry_index_end": 1},
                               {"id": "2", "type": "accident",
                                "geometry_index_start": 4, "geometry_index_end": 4}],
                 "closures": [{"geometry_index_start": 1, "geometry_index_end": 4},
                              {"geometry_index_start": 1, "geometry_index_end": 1}],
                 "notifications": [
                   {"type": "alert", "refresh_type": "dynamic",
                    "subtype": "evInsufficientCharge", "geometry_index": 3},
                   {"type": "alert", "refresh_type": "dynamic", "subtype": "stationUnavailable",
                    "geometry_index": 1, "reason": "outOfOrder", "station_id": "id5"},
                   {"type": "violation", "refresh_type": "static", "subtype": "maxHeight",
                    "geometry_index_start": 0, "geometry_index_end": 1}]},
                {"distance": 370, "duration": 50, "summary": "b", "steps": [],
                 "annotation": {"distance": [74, 74, 74, 74, 74],
                                "duration": [10, 10, 10, 10, 10],
                                "congestion_numeric": [5, 5, 5, 5, 5],
                                "state_of_charge": [75, 74, 73, 72, 71],
                                "historical_speed": [60, 61, 62, 63, 64]},
                 "notifications": [
                   {"type": "alert", "refresh_type": "dynamic",
                    "subtype": "evMinChargeAtDestination", "geometry_index": 2}]}],
              "waypoints": [
                {"name": "origin", "location": [11.5, 48.1]},
                {"name": "station", "location": [11.505, 48.1],
                 "metadata": {"type": "charging-station", "name": "Station id5",
                              "station_id": "id5", "charge_time": 600, "charge_to": 50000,
                              "charge_at_arrival": 20000, "plug_type": "ccs_combo_type2",
                              "current_type": "dc", "power_kw": 150}},
                {"name": "destination", "location": [11.51, 48.1]}]
            }]}
        """.trimIndent()

        val CHARGE_SUBTYPES = setOf(
            "evInsufficientCharge",
            "evMinChargeAtChargingStation",
            "evMinChargeAtDestination",
        )

        // Charge notifications as the planner writes them. evMinChargeAtDestination has no
        // geometry_index.
        const val DEVICE_INSUFFICIENT_CHARGE_AT_0 = """{"type": "alert",
            "refresh_type": "dynamic", "subtype": "evInsufficientCharge", "geometry_index": 0}"""
        const val DEVICE_MIN_CHARGE_AT_DESTINATION = """{"type": "violation",
            "subtype": "evMinChargeAtDestination", "refresh_type": "dynamic",
            "details": {"requested_value": 7700, "actual_value": 4200, "unit": "Wh"}}"""

        // What materializeOnlineBuiltResponse sends for that route with the car at point 2: the
        // planner's charge and charge notifications, the online historical_speed slice, the
        // in-range road objects and other dynamic notifications rebased to the car (none here:
        // stationUnavailable at 1 lies before the car), and the online stop with new charge
        // numbers.
        fun deviceResponse(
            firstLegNotifications: String? = null,
            secondLegNotifications: String? = null,
        ): String {
            fun notifications(list: String?) =
                list?.let { ""","notifications": [$it]""" }.orEmpty()
            return """
            {"code": "Ok", "route": {
              "legs": [
                {"annotation": {"state_of_charge": [70, 69, 68],
                                "historical_speed": [52, 53, 54]},
                 "incidents": [{"id": "1", "type": "construction",
                                "geometry_index_start": -1, "geometry_index_end": 1},
                               {"id": "2", "type": "accident",
                                "geometry_index_start": 2, "geometry_index_end": 2}],
                 "closures": [{"geometry_index_start": -1, "geometry_index_end": 2}]
                 ${notifications(firstLegNotifications)}},
                {"annotation": {"state_of_charge": [67, 66, 65, 64, 63],
                                "historical_speed": [60, 61, 62, 63, 64]}
                 ${notifications(secondLegNotifications)}}],
              "waypoints": [
                {},
                {"name": "station", "location": [11.505, 48.1],
                 "metadata": {"type": "charging-station", "name": "Station id5",
                              "station_id": "id5", "charge_time": 720, "charge_to": 52000,
                              "charge_at_arrival": 18000, "plug_type": "ccs_combo_type2",
                              "current_type": "dc", "power_kw": 150}},
                {}]}}
            """.trimIndent()
        }

        // The same planner answer as nav-native plannerAnswerFor(params): evMinChargeAtDestination
        // on the last leg.
        val DEVICE_RESPONSE =
            deviceResponse(secondLegNotifications = DEVICE_MIN_CHARGE_AT_DESTINATION)
    }
}
