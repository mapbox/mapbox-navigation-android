package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.net.URL

class ChargingStationsMetadataUpdaterTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val nativeRoute = NativeRouteParserRule()

    @Test
    fun `offline EV route with user provided charging stations`() = runBlocking {
        val offlineRoutes = createTestOfflineRoutes()
        val result = restoreChargingStationsMetadataFromUrl(offlineRoutes)
        result.forEachIndexed { index, navigationRoute ->
            assertEquals(
                "route #$index doesn't have charging stations ids",
                listOf(null, "ocm-54453", null),
                navigationRoute.getChargingStationIds()
            )
            assertEquals(
                "route #$index doesn't have charging stations powers",
                listOf(null, "50", null),
                navigationRoute.getChargingStationPowersKW()
            )
            assertEquals(
                "route #$index doesn't have charging stations current type",
                listOf(null, "dc", null),
                navigationRoute.getChargingStationPowerCurrentTypes()
            )
        }
    }

    @Test
    fun `offline EV route has only required user provided charging stations parameters`() = runBlocking {
        val offlineRoutes = createTestOfflineRoutes {
            RouteOptions.fromUrl(URL(it)).let {
                it.toBuilder()
                    .unrecognizedJsonProperties(it.unrecognizedJsonProperties?.apply {
                        remove("waypoints.charging_station_id")
                        remove("waypoints.charging_station_current_type")
                    })
                    .build().toUrl("").toString()
            }
        }
        val result = restoreChargingStationsMetadataFromUrl(offlineRoutes)
        result.forEachIndexed { index, navigationRoute ->
            assertEquals(
                listOf(null, null, null),
                navigationRoute.getChargingStationIds()
            )
            assertEquals(
                listOf(null, null, null),
                navigationRoute.getChargingStationPowerCurrentTypes()
            )
            assertEquals(
                listOf(null, "50", null),
                navigationRoute.getChargingStationPowersKW()
            )
        }
    }

    private fun createTestOfflineRoutes(
        urlUpdate: (String) -> String = { it }
    ): List<NavigationRoute> {
        val url =
            "http://localhost:57725/directions/v5/mapbox/driving-traffic/13.3674044,52.4974382;13.377746,52.51224;13.393451,52.5091392?access_token=***&geometries=polyline6&alternatives=true&overview=full&steps=true&avoid_maneuver_radius=40.0&bearings=280%2C90%3B%3B&layers=%3B%3B&continue_straight=true&annotations=state_of_charge&roundabout_exits=true&voice_instructions=true&banner_instructions=true&enable_refresh=true&snapping_include_closures=true%3B%3B&snapping_include_static_closures=true%3B%3B&waypoints_per_route=true&waypoints.charging_station_power=%3B50000%3B&ev_max_charge=50000&ev_charging_curve=0%2C100000%3B40000%2C70000%3B60000%2C30000%3B80000%2C10000&engine=electric&energy_consumption_curve=0%2C300%3B20%2C160%3B80%2C140%3B120%2C180&ev_min_charge_at_charging_station=1&waypoints.charging_station_id=%3Bocm-54453%3B&waypoints.charging_station_current_type=%3Bdc%3B&ev_add_charging_stops=false&ev_initial_charge=1000&ev_connector_types=ccs_combo_type1%2Cccs_combo_type2"
        return NavigationRoute.create(
            resourceAsString("testOfflineRoute.json"),
            urlUpdate(url),
            RouterOrigin.Onboard
        )
    }

    private fun resourceAsString(
        name: String,
        packageName: String = "com.mapbox.navigation.core.reroute"
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        return IOUtils.toString(inputStream, "UTF-8")
    }
}

fun NavigationRoute.getChargingStationIds(): List<String?> {
    return getWaypointsMetadata("station_id")
}

fun NavigationRoute.getChargingStationPowersKW(): List<String?> {
    return getWaypointsMetadata("power_kw")
}

fun NavigationRoute.getChargingStationPowerCurrentTypes(): List<String?> {
    return getWaypointsMetadata("current_type")
}

fun NavigationRoute.getChargingStationTypes(): List<String?> {
    return getWaypointsMetadata("type")
}

fun NavigationRoute.getWaypointsMetadata(fieldName: String): List<String?> {
    return this.waypoints?.map {
        it.getUnrecognizedProperty("metadata")
            ?.asJsonObject
            ?.get(fieldName)
            ?.asString
    } ?: emptyList()
}