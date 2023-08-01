package com.mapbox.navigation.instrumentation_tests.utils.routes

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.http.BaseMockRequestHandler

data class MockedEvRoutes(
    val routeOptions: RouteOptions,
    val mockWebServerHandler: BaseMockRequestHandler
) {
    val origin get() = routeOptions.coordinatesList().first()
}

data class MockedEvRouteWithSingleUserProvidedChargingStation(
    val routeOptions: RouteOptions,
    val mockWebServerHandler: BaseMockRequestHandler,
    val chargingStationId: String,
    val chargingStationPower: Int,
    val currentType: String,
    val originBearing: Float,
    val origin: Point
) {
    val chargingStationPowerKw: Int = chargingStationPower / 1000
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
        val originalRouteOptions = berlinEvRouteOptions(baseUrl)
        val newRouteOptions = originalRouteOptions.toBuilder()
            .coordinatesList(
                originalRouteOptions.coordinatesList().apply {
                    set(0, Point.fromLngLat(13.36742058325467, 52.49745756017697))
                }
            )
            .build()
        val newRouteOnlineRouteRequestHandler = MockDirectionsRequestHandler(
            profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            jsonResponse = readRawFileText(context, R.raw.ev_routes_berlin_reroute),
            expectedCoordinates = newRouteOptions.coordinatesList()
        )
        return MockedEvRoutes(newRouteOptions, newRouteOnlineRouteRequestHandler)
    }

    fun getBerlinEvRouteWithUserProvidedChargingStation(
        context: Context,
        baseUrl: String?
    ): MockedEvRouteWithSingleUserProvidedChargingStation {
        val originBearing = Bearing.builder()
            .degrees(45.0)
            .angle(290.0)
            .build()
        val chargingStationId = "ocm-54453"
        val chargingStationCurrentType = "dc"
        val chargingStationPower = 50_000
        val chargingStationLocation = Point.fromLngLat(13.361342, 52.498064)
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(13.361378213031003, 52.49813341962201),
                    chargingStationLocation,
                    Point.fromLngLat(13.393450988895268, 52.50913924804004)
                )
            )
            .bearingsList(
                listOf(
                    null,
                    originBearing,
                    null
                )
            )
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
                    "ev_min_charge_at_charging_station" to "1",
                    "waypoints.charging_station_power" to ";$chargingStationPower;",
                    "waypoints.charging_station_current_type" to ";$chargingStationCurrentType;",
                    "waypoints.charging_station_id" to ";$chargingStationId;"
                )
            )
            .apply {
                if (baseUrl != null) {
                    baseUrl(baseUrl)
                }
            }
            .build()
        val origin = routeOptions.coordinatesList().first()
        return MockedEvRouteWithSingleUserProvidedChargingStation(
            routeOptions,
            //TODO: replace
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = "",
                expectedCoordinates = routeOptions.coordinatesList(),
            ),
            chargingStationId,
            chargingStationPower,
            chargingStationCurrentType,
            originBearing.angle().toFloat(),
            origin
        )
    }

    private fun berlinEvRouteOptions(
        baseUrl: String?
    ): RouteOptions = RouteOptions.builder()
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
