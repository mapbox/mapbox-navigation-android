@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.internalRefreshRoute
import com.mapbox.navigation.base.internal.route.parsing.ResponseToParse
import com.mapbox.navigation.base.internal.route.parsing.setupParsing
import com.mapbox.navigation.base.internal.route.testing.toDataRefJava
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.utils.withMapboxNavigation
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The device EV refresh of a route the server built sends only state_of_charge, the charge
 * notifications and the stop's charge numbers from the planner, and sends back the route's own
 * values the merge would drop. The route model kept in Kotlin and the native route model merge
 * that response separately, so both are run on the same route and response here and must agree
 * with each other and keep everything but the charge. The JVM unit test
 * JavaRouteOperationsOnlineBuiltEvRefreshTest and nav-native ev_refresh_online_built.test.cpp
 * use the same route and response.
 */
class OnlineBuiltEvRefreshMergeTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 48.1
        longitude = 11.5
    }

    @Test
    fun kotlin_route_model_merge_keeps_everything_but_the_charge() = sdkTest {
        withMapboxNavigation {
            checkMerge(nativeRoute = false)
        }
    }

    @Test
    fun native_route_model_merge_keeps_everything_but_the_charge() = sdkTest {
        withMapboxNavigation {
            checkMerge(nativeRoute = true)
        }
    }

    @Test
    fun kotlin_and_native_route_model_merges_agree() = sdkTest {
        withMapboxNavigation {
            val kotlin = JsonParser.parseString(refreshed(nativeRoute = false).toJson())
            val native = JsonParser.parseString(refreshed(nativeRoute = true).toJson())
            for (leg in 0..1) {
                val kotlinLeg = kotlin.asJsonObject.legs()[leg].asJsonObject
                val nativeLeg = native.asJsonObject.legs()[leg].asJsonObject
                for (key in ANNOTATION_KEYS + "state_of_charge") {
                    assertEquals(
                        "leg $leg $key",
                        normalized(kotlinLeg.annotation()[key]),
                        normalized(nativeLeg.annotation()[key]),
                    )
                }
                for (key in ROAD_OBJECT_KEYS) {
                    assertEquals("leg $leg $key", sorted(kotlinLeg[key]), sorted(nativeLeg[key]))
                }
            }
            assertEquals(
                normalized(kotlin.asJsonObject.waypoints()),
                normalized(native.asJsonObject.waypoints()),
            )
        }
    }

    private suspend fun checkMerge(nativeRoute: Boolean) {
        val route = parse(nativeRoute)
        val refreshed = route.refreshOnDevice()

        val before = JsonParser.parseString(route.toJson()).asJsonObject
        val after = JsonParser.parseString(refreshed.toJson()).asJsonObject
        val charge = listOf(
            listOf(80, 79, 70, 69, 68),
            listOf(67, 66, 65, 64, 63),
        )
        for (leg in 0..1) {
            val was = before.legs()[leg].asJsonObject
            val isNow = after.legs()[leg].asJsonObject
            for (key in ANNOTATION_KEYS) {
                assertEquals(
                    "leg $leg $key",
                    normalized(was.annotation()[key]),
                    normalized(isNow.annotation()[key]),
                )
            }
            assertEquals(
                "leg $leg",
                charge[leg].map { it.toDouble() },
                isNow.annotation()["state_of_charge"].asJsonArray.map { it.asDouble },
            )
            assertEquals("leg $leg", was["duration"].asDouble, isNow["duration"].asDouble, 0.0)
            for (key in ROAD_OBJECT_KEYS - "notifications") {
                assertEquals("leg $leg $key", sorted(was[key]), sorted(isNow[key]))
            }
            assertEquals(
                "leg $leg notifications",
                sorted(was["notifications"]) { !it.isChargeNotification() },
                sorted(isNow["notifications"]) { !it.isChargeNotification() },
            )
        }
        // The charge notifications are the response's: the route's own are gone.
        assertEquals(
            listOf("evMinChargeAtDestination"),
            after.legs().flatMap { leg ->
                leg.asJsonObject["notifications"]?.asJsonArray?.toList().orEmpty()
                    .filter { it.isChargeNotification() }
                    .map { it.asJsonObject["subtype"].asString }
            },
        )

        val expectedStation = before.waypoints()[1].deepCopy().asJsonObject
        expectedStation["metadata"].asJsonObject.apply {
            addProperty("charge_time", 720)
            addProperty("charge_to", 52000)
            addProperty("charge_at_arrival", 18000)
        }
        assertEquals(
            normalized(expectedStation.metadata()),
            normalized(after.waypoints()[1].asJsonObject.metadata()),
        )
        assertEquals(normalized(before.waypoints()[0]), normalized(after.waypoints()[0]))
        assertEquals(normalized(before.waypoints()[2]), normalized(after.waypoints()[2]))
        assertEquals(820.0, refreshed.directionsRoute.duration(), 0.0)
    }

    private suspend fun refreshed(nativeRoute: Boolean): NavigationRoute =
        parse(nativeRoute).refreshOnDevice()

    private suspend fun parse(nativeRoute: Boolean): NavigationRoute =
        setupParsing(nativeRoute = nativeRoute).parseDirectionsResponse(
            ResponseToParse.from(
                responseBody = ONLINE_EV_ROUTE.toDataRefJava(),
                routeRequest = ROUTE_OPTIONS.toUrl("***").toString(),
                routerOrigin = RouterOrigin.ONLINE,
            ),
        ).getOrThrow().routes.first()

    // The car 2 points into the first leg.
    private fun NavigationRoute.refreshOnDevice(): NavigationRoute = internalRefreshRoute(
        DEVICE_RESPONSE.toDataRefJava(),
        legIndex = 0,
        legGeometryIndex = 2,
        responseTimeElapsedSeconds = 0,
        chargeOnly = true,
    ).getOrThrow()

    private fun NavigationRoute.toJson(): String = directionsRoute.toJson()

    private fun JsonObject.legs(): JsonArray = this["legs"].asJsonArray

    private fun JsonObject.waypoints(): JsonArray = this["waypoints"].asJsonArray

    private fun JsonObject.annotation(): JsonObject = this["annotation"].asJsonObject

    private fun JsonObject.metadata(): JsonObject = this["metadata"].asJsonObject

    // Number values compared as numbers: a merge may write 3 as 3.0.
    private fun sorted(
        list: JsonElement?,
        keep: (JsonElement) -> Boolean = { true },
    ): List<String> =
        list?.takeIf { it.isJsonArray }?.asJsonArray
            ?.filter(keep)
            ?.map { normalized(it).toString() }
            ?.sorted()
            .orEmpty()

    private fun JsonElement.isChargeNotification(): Boolean =
        asJsonObject["subtype"]?.asString in CHARGE_SUBTYPES

    private fun normalized(element: JsonElement?): JsonElement? = when {
        element == null || element.isJsonNull -> null
        element.isJsonObject -> JsonObject().also { copy ->
            element.asJsonObject.entrySet().sortedBy { it.key }
                .forEach { (key, value) -> copy.add(key, normalized(value)) }
        }
        element.isJsonArray -> JsonArray().also { copy ->
            element.asJsonArray.forEach { copy.add(normalized(it)) }
        }
        element.isJsonPrimitive && element.asJsonPrimitive.isNumber ->
            JsonParser.parseString(element.asDouble.toString())
        else -> element
    }

    private companion object {
        val ANNOTATION_KEYS =
            listOf("distance", "duration", "congestion_numeric", "historical_speed")
        val ROAD_OBJECT_KEYS = listOf("incidents", "closures", "notifications")
        val CHARGE_SUBTYPES = setOf(
            "evInsufficientCharge",
            "evMinChargeAtChargingStation",
            "evMinChargeAtDestination",
        )

        val ROUTE_OPTIONS: RouteOptions = RouteOptions.builder()
            .profile("driving-traffic")
            .geometries("polyline6")
            .annotations("duration,distance,congestion_numeric,state_of_charge")
            .coordinatesList(
                listOf(
                    Point.fromLngLat(11.5, 48.1),
                    Point.fromLngLat(11.505, 48.1),
                    Point.fromLngLat(11.51, 48.1),
                ),
            )
            .build()

        // nav-native ev_refresh_online_built.test.cpp OnlineEvRouteJson, its geometry as polyline6:
        // legs 0..5 and 5..10, a
        // charging stop at point 5. Incident 3 and the second closure end before the car but are
        // listed after objects that overlap it: both merges keep them.
        val ONLINE_EV_ROUTE = """
            {"code": "Ok", "uuid": "server-uuid", "routes": [{
              "distance": 740, "duration": 700, "weight": 700, "weight_name": "auto",
              "geometry": "_ixvzA_}{|T?o}@?o}@?o}@?o}@?o}@?o}@?o}@?o}@?o}@?o}@",
              "legs": [
                {"distance": 370, "duration": 50, "summary": "a",
                 "steps": [{"distance": 370, "duration": 50, "weight": 50, "name": "a",
                   "mode": "driving",
                            "driving_side": "right",
                            "geometry": "_ixvzA_}{|T?o}@?o}@?o}@?o}@?o}@",
                            "maneuver": {"location": [11.5, 48.1], "bearing_before": 0,
                              "bearing_after": 90,
                                         "instruction": "Head east", "type": "depart"}},
                     {"distance": 0, "duration": 0, "weight": 0, "name": "", "mode": "driving",
                      "driving_side": "right",
                      "geometry": "_ixvzAoue}T??",
                      "maneuver": {"location": [11.505, 48.1], "bearing_before": 90,
                        "bearing_after": 0,
                                   "instruction": "Arrive", "type": "arrive"}}],
                 "annotation": {"distance": [74, 74, 74, 74, 74], "duration": [10, 10, 10, 10, 10],
                                "congestion_numeric": [10, 20, 30, 40, 50],
                                "state_of_charge": [80, 79, 78, 77, 76],
                                "historical_speed": [50, 51, 52, 53, 54]},
                 "incidents": [{"id": "1", "type": "construction", "geometry_index_start": 1,
                                "geometry_index_end": 3},
                               {"id": "3", "type": "construction", "geometry_index_start": 1,
                                "geometry_index_end": 1},
                               {"id": "2", "type": "accident", "geometry_index_start": 4,
                                "geometry_index_end": 4}],
                 "closures": [{"geometry_index_start": 1, "geometry_index_end": 4},
                              {"geometry_index_start": 1, "geometry_index_end": 1}],
                 "notifications": [
                   {"type": "alert", "refresh_type": "dynamic", "subtype": "evInsufficientCharge",
                    "geometry_index": 3},
                   {"type": "alert", "refresh_type": "dynamic", "subtype": "stationUnavailable",
                    "geometry_index": 1, "reason": "outOfOrder", "station_id": "id5"},
                   {"type": "violation", "refresh_type": "static", "subtype": "maxHeight",
                    "geometry_index_start": 0, "geometry_index_end": 1}]},
                {"distance": 370, "duration": 50, "summary": "b",
                 "steps": [{"distance": 370, "duration": 50, "weight": 50, "name": "b",
                   "mode": "driving",
                            "driving_side": "right",
                            "geometry": "_ixvzAoue}T?o}@?o}@?o}@?o}@?o}@",
                            "maneuver": {"location": [11.505, 48.1], "bearing_before": 90,
                                         "bearing_after": 90, "instruction": "Continue",
                                           "type": "depart"}},
                     {"distance": 0, "duration": 0, "weight": 0, "name": "", "mode": "driving",
                      "driving_side": "right",
                      "geometry": "_ixvzA_no}T??",
                      "maneuver": {"location": [11.51, 48.1], "bearing_before": 90,
                        "bearing_after": 0,
                                   "instruction": "Arrive", "type": "arrive"}}],
                 "annotation": {"distance": [74, 74, 74, 74, 74], "duration": [10, 10, 10, 10, 10],
                                "congestion_numeric": [5, 5, 5, 5, 5],
                                "state_of_charge": [75, 74, 73, 72, 71],
                                "historical_speed": [60, 61, 62, 63, 64]},
                 "notifications": [
                   {"type": "alert", "refresh_type": "dynamic",
                     "subtype": "evMinChargeAtDestination",
                    "geometry_index": 2}]}],
              "waypoints": [
                {"name": "origin", "location": [11.5, 48.1]},
                {"name": "station", "location": [11.505, 48.1],
                 "metadata": {"type": "charging-station", "name": "Station id5",
                   "station_id": "id5",
                              "charge_time": 600, "charge_to": 50000, "charge_at_arrival": 20000,
                              "plug_type": "ccs_combo_type2", "current_type": "dc",
                                "power_kw": 150}},
                {"name": "destination", "location": [11.51, 48.1]}]
            }]}
        """.trimIndent()

        // What the device refresh sends for that route with the car at point 2: the planner's
        // charge and charge notifications, the online historical_speed slice, the in-range road
        // objects and other dynamic notifications rebased to the car (none here:
        // stationUnavailable at 1 lies before the car), and the online stop with new charge
        // numbers.
        val DEVICE_RESPONSE = """
            {"code": "Ok", "route": {
              "legs": [
                {"annotation": {"state_of_charge": [70, 69, 68],
                                "historical_speed": [52, 53, 54]},
                 "incidents": [{"id": "1", "type": "construction",
                                "geometry_index_start": -1, "geometry_index_end": 1},
                               {"id": "2", "type": "accident",
                                "geometry_index_start": 2, "geometry_index_end": 2}],
                 "closures": [{"geometry_index_start": -1, "geometry_index_end": 2}]},
                {"annotation": {"state_of_charge": [67, 66, 65, 64, 63],
                                "historical_speed": [60, 61, 62, 63, 64]},
                 "notifications": [
                   {"type": "violation", "subtype": "evMinChargeAtDestination",
                    "refresh_type": "dynamic", "details": {"requested_value": 7700,
                    "actual_value": 4200, "unit": "Wh"}}]}],
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
}
