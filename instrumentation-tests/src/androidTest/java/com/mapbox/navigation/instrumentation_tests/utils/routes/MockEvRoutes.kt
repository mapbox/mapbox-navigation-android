package com.mapbox.navigation.instrumentation_tests.utils.routes

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.http.BaseMockRequestHandler
import java.net.URL

data class MockedEvRoutes(
    val routeOptions: RouteOptions,
    val mockWebServerHandler: BaseMockRequestHandler
) {
    val origin get() = routeOptions.coordinatesList().first()
}

object EvRoutesProvider {
    fun getBerlinEvRoute(context: Context, baseUrl: String? = null): MockedEvRoutes {
        val routeOptions = berlinEvRouteOptions(baseUrl)
        val jsonResponse = readRawFileText(context, R.raw.ev_routes_berlin)
        val evRouteRequestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = jsonResponse,
            expectedCoordinates = routeOptions.coordinatesList(),
        )
        return MockedEvRoutes(
            routeOptions,
            evRouteRequestHandler
        )
    }

    /***
     * has the same destination as [getBerlinEvRoute], but different origin,
     * could be use for reroute cases
     */
    fun getBerlinEvRouteReroute(context: Context, baseUrl: String? = null): MockedEvRoutes {
        val newRouteOptions = RouteOptions.fromUrl(
            URL(
                "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                    "13.3674044489%2C52.4974381849;13.377746%2C52.51224;13.393995%2C52.509175" +
                    "?access_token=***&alternatives=true&annotations=state_of_charge" +
                    "&avoid_maneuver_radius=40&banner_instructions=true" +
                    "&bearings=218.348%2C45%3B%3B&continue_straight=true&enable_refresh=true" +
                    "&energy_consumption_curve=0%2C300%3B20%2C160%3B80%2C140%3B120%2C180" +
                    "&engine=electric&ev_add_charging_stops=false" +
                    "&ev_charging_curve=" +
                    "0%2C100000%3B40000%2C70000%3B60000%2C30000%3B80000%2C10000" +
                    "&ev_connector_types=ccs_combo_type1%2Cccs_combo_type2" +
                    "&ev_initial_charge=1000" +
                    "&ev_max_charge=50000&ev_min_charge_at_charging_station=1" +
                    "&geometries=polyline6" +
                    "&layers=0%3B%3B&overview=full" +
                    "&roundabout_exits=true&snapping_include_closures=true%3B%3B" +
                    "&snapping_include_static_closures=true%3B%3B" +
                    "&steps=true&voice_instructions=true" +
                    "&waypoint_names=%3BIn%20den%20Ministerg%C3%A4rten%3BMarkgrafenstra%C3%9Fe" +
                    "&waypoints=0%3B1%3B2&waypoints.charging_station_current_type=%3Bdc%3B" +
                    "&waypoints.charging_station_id=%3Bocm-54453%3B" +
                    "&waypoints.charging_station_power=%3B50000%3B" +
                    "&waypoints_per_route=true"
            )
        ).toBuilder().apply {
            if (baseUrl != null) {
                baseUrl(baseUrl)
            }
        }.build()
        val newRouteOnlineRouteRequestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = readRawFileText(context, R.raw.ev_routes_berlin_reroute),
            expectedCoordinates = newRouteOptions.coordinatesList()
        )
        return MockedEvRoutes(newRouteOptions, newRouteOnlineRouteRequestHandler)
    }

    private fun berlinEvRouteOptions(baseUrl: String?): RouteOptions = RouteOptions.builder()
        .applyDefaultNavigationOptions()
        .coordinates("13.361378213031003,52.49813341962201;13.393450988895268,52.50913924804004")
        .annotations("state_of_charge")
        .alternatives(true)
        .waypointsPerRoute(true)
        .unrecognizedProperties(
            mapOf(
                "engine" to "electric",
                "ev_initial_charge" to "1000",
                "ev_max_charge" to "50000",
                "ev_connector_types" to "ccs_combo_type1,ccs_combo_type2",
                "energy_consumption_curve" to "0,300;20,160;80,140;120,180",
                "ev_charging_curve" to "0,100000;40000,70000;60000,30000;80000,10000",
                "ev_min_charge_at_charging_station" to "1"
            )
        )
        .apply {
            if (baseUrl != null) {
                baseUrl(baseUrl)
            }
        }
        .build()
}
