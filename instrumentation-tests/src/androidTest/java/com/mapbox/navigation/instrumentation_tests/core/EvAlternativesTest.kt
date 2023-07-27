package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.location.stayOnPosition
import com.mapbox.navigation.instrumentation_tests.utils.routes.getChargingStationIds
import com.mapbox.navigation.instrumentation_tests.utils.withMapboxNavigation
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAsync
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.net.URL

class EvAlternativesTest : BaseCoreNoCleanUpTest() {

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            longitude = 13.361378213031003
            latitude = 52.49813341962201
        }
    }

    @Test
    @Ignore("used for semi manual testing")
    fun passForkPointToReceiveEvAlternatives() = sdkTest(
        timeout = INCREASED_TIMEOUT_BECAUSE_OF_REAL_ROUTING_TILES_USAGE
    ) {
        val testRouteOptions =
            RouteOptions.fromUrl(
                URL(
                    "https://api.mapbox.com/directions/v5/mapbox/" +
                        "driving-traffic/11.587428364032348,48.20148957377813" +
                        ";11.81872714026062,50.67773738599428" +
                        ";13.378818297105255,52.627628120089355" +
                        "?access_token=***&geometries=polyline6" +
                        "&alternatives=true&overview=full" +
                        "&steps=true&continue_straight=true&annotations=state_of_charge" +
                        "&roundabout_exits=true&voice_instructions=true" +
                        "&banner_instructions=true" +
                        "&enable_refresh=true&waypoints_per_route=true&engine=electric" +
                        "&ev_initial_charge=30000&ev_max_charge=50000" +
                        "&ev_connector_types=ccs_combo_type1%2Cccs_combo_type2" +
                        "&energy_consumption_curve=0%2C300%3B20%2C160%3B80%2C140%3B120%2C180" +
                        "&ev_charging_curve=" +
                        "0%2C100000%3B40000%2C70000%3B60000%2C30000%3B80000%2C10000" +
                        "&ev_min_charge_at_charging_station=7000&bearings=1,45;65,45;" +
                        "&radiuses=5;50;unlimited&waypoint_names=origin;test;destination" +
                        "&waypoint_targets=;;13.379077134850064,52.62734923825474" +
                        "&approaches=;curb;" +
                        "&layers=0;0;0&snapping_include_static_closures=true;false;true" +
                        "&snapping_include_closures=true;false;true&waypoints=0;1;2"
                )
            )
        val origin = testRouteOptions.coordinatesList().first()
        withMapboxNavigation(
            historyRecorderRule = mapboxHistoryTestRule
        ) { navigation ->
            navigation.registerRouteAlternativesObserver(
                AdvancedAlternativesObserverFromDocumentation(navigation)
            )
            val requestResult = navigation.requestRoutes(testRouteOptions)
                .getSuccessfulResultOrThrowException()
            assertEquals(RouterOrigin.Offboard, requestResult.routerOrigin)

            stayOnPosition(
                latitude = origin.latitude(),
                longitude = testRouteOptions.coordinatesList().first().longitude(),
                0.0f,
            ) {
                navigation.startTripSession()
                navigation.setNavigationRoutesAsync(requestResult.routes)
            }
            val forkPoint =
                navigation.getAlternativeMetadataFor(requestResult.routes[1])!!
                    .forkIntersectionOfPrimaryRoute.geometryIndexInRoute
            val points = requestResult.routes.first().directionsRoute.completeGeometryToPoints()
            val locationAfterAlternativeForkPoint = points[forkPoint + 10]

            stayOnPosition(
                latitude = locationAfterAlternativeForkPoint.latitude(),
                longitude = locationAfterAlternativeForkPoint.longitude(),
                bearing = 0f
            ) {
                val onlineRoutes = navigation.routesUpdates().filter {
                    it.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE &&
                        it.navigationRoutes.first().origin ==
                        RouterOrigin.Offboard && it.navigationRoutes.size > 1
                }.drop(1).first()
                val routeProgress = navigation.routeProgressUpdates().first()
                assertEquals(
                    onlineRoutes.navigationRoutes[1].getChargingStationIds()
                        .takeLast(routeProgress.remainingWaypoints),
                    requestResult.routes.first().getChargingStationIds()
                        .takeLast(routeProgress.remainingWaypoints)
                )
            }
        }
    }
}
