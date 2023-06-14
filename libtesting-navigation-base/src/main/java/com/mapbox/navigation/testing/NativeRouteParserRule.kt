package com.mapbox.navigation.testing

import android.annotation.SuppressLint
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.internal.utils.mapToNativeRouteOrigin
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.Waypoint
import com.mapbox.navigator.WaypointType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.json.JSONObject
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.URL
import java.util.UUID

class NativeRouteParserRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @SuppressLint("VisibleForTests")
            override fun evaluate() {
                mockkObject(NativeRouteParserWrapper)
                every {
                    NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
                } answers {
                    val origin = this.thirdArg<RouterOrigin>()
                    val response = JSONObject(this.firstArg<String>())
                    val routesCount = response.getJSONArray("routes").length()
                    val idBase = if (response.has("uuid")) {
                        response.getString("uuid")
                    } else {
                        "local@${UUID.randomUUID()}"
                    }
                    val routeOptions = RouteOptions.fromUrl(URL(secondArg()))
                    val parsedWaypoints = if (response.has("waypoints")) {
                        val directionsWaypoints = mutableListOf<DirectionsWaypoint>()
                        val array = response.getJSONArray("waypoints")
                        for (i in 0 until array.length()) {
                            directionsWaypoints.add(
                                DirectionsWaypoint.fromJson(array[i].toString())
                            )
                        }
                        directionsWaypoints.mapIndexed { index, directionsWaypoint ->
                            Waypoint(
                                directionsWaypoint.name(),
                                directionsWaypoint.location(),
                                directionsWaypoint.distance(),
                                null,
                                routeOptions.waypointTargetsList()?.get(index),
                                when {
                                    directionsWaypoint.getUnrecognizedProperty("metadata")
                                        ?.asJsonObject?.get("type")
                                        ?.asString == "charging-station" -> WaypointType.EV_CHARGING
                                    routeOptions.waypointIndicesList()
                                        ?.contains(index)?.not() == true -> WaypointType.SILENT
                                    else -> WaypointType.REGULAR
                                }
                            )
                        }
                    } else {
                        emptyList()
                    }
                    val nativeRoutes = mutableListOf<RouteInterface>().apply {
                        repeat(routesCount) {
                            add(
                                mockk {
                                    every { routeInfo } returns mockk(relaxed = true)
                                    every { routeId } returns "$idBase#$it"
                                    every {
                                        routerOrigin
                                    } returns origin.mapToNativeRouteOrigin()
                                    every { waypoints } returns parsedWaypoints
                                }
                            )
                        }
                    }
                    ExpectedFactory.createValue(nativeRoutes)
                }
                try {
                    base.evaluate()
                } finally {
                    unmockkObject(NativeRouteParserWrapper)
                }
            }
        }
    }
}
