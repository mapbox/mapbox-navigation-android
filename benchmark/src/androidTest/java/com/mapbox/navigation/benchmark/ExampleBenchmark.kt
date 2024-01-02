package com.mapbox.navigation.benchmark

import android.content.Context
import android.util.Log
import androidx.annotation.IntegerRes
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LoggingLevel
import com.mapbox.geojson.LineString
import com.mapbox.navigator.RoadObjectType
import com.mapbox.navigator.RouteInterface
import com.mapbox.navigator.RouteParser
import com.mapbox.navigator.RouterOrigin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val LOG_TAG = "vadzim-test"

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleBenchmark {

    init {
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
    }

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun timeToRetrieveRouteAlerts() {
        val route = routeWithLongIncident()
        logShapeOfFirstRouteAlertLocation(route)
        benchmarkRule.measureRepeated {
            val alerts = route.routeInfo.alerts
            this.runWithTimingDisabled {
                Runtime.getRuntime().gc();
            }
        }
    }

    @Test
    fun timeToRetrieveRouteAlertsWithShapes() {
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
        val route = routeWithLongIncident()
        logShapeOfFirstRouteAlertLocation(route)
        benchmarkRule.measureRepeated {
            val alerts = route.routeInfo.alerts
            alerts.forEach {
                val location = it.roadObject.location
                if (location.isRouteAlertLocation) {
                    location.routeAlertLocation.shape
                }
            }
            this.runWithTimingDisabled {
                Runtime.getRuntime().gc();
            }
        }
    }

    private fun logShapeOfFirstRouteAlertLocation(route: RouteInterface) {
        val alertsCount = route.routeInfo.alerts.size
        val shape = route.routeInfo.alerts
            .filter { it.roadObject.type == RoadObjectType.INCIDENT }
            .map { it.roadObject.location }
            .first()
            .routeAlertLocation
            .shape
        Log.d(LOG_TAG, "alerts count is $alertsCount")
        Log.d(LOG_TAG, "shape is $shape")
        val lineStringCoordinates = (shape as? LineString)?.coordinates()?.size
        Log.d(LOG_TAG, "Line string coordinates count is $lineStringCoordinates")
    }

    private fun routeWithLongIncident(): RouteInterface {
        val response = readRawFileText(context, R.raw.test_route)
        val route = RouteParser.parseDirectionsResponse(
            response,
            "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/14.958055640969633,51.2002127680808;5.2869509774857875,51.058961031629536?access_token=***&geometries=polyline6&overview=full&steps=true&continue_straight=true&annotations=congestion_numeric%2Cmaxspeed%2Cclosure%2Cspeed%2Cduration%2Cdistance&language=en&roundabout_exits=true&voice_instructions=true&banner_instructions=true&voice_units=imperial&enable_refresh=true",
            RouterOrigin.ONLINE
        ).onError {
            Log.d(LOG_TAG, "error parsing route $it")
            Log.d(LOG_TAG, "response was $response")
        }.value!!.first()
        return route
    }
}

fun readRawFileText(context: Context, @IntegerRes res: Int): String =
    context.resources.openRawResource(res).bufferedReader().use { it.readText() }