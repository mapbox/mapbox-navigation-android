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
import com.mapbox.navigator.RouteParser
import com.mapbox.navigator.RouterOrigin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleBenchmark {

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun log() {
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
        val response = readRawFileText(context, R.raw.long_route_7k)
        val route = RouteParser.parseDirectionsResponse(
            response,
            "http://localhost:44685/directions/v5/mapbox/driving-traffic/4.898473756907066,52.37373595766587;5.359980783143584,43.280050656855906;11.571179644010442,48.145540095763664;13.394784408007155,52.51274942160785;-9.143239539655042,38.70880224984026;9.21595128801522,45.4694220491258?access_token=test&geometries=polyline6&alternatives=true&overview=full&steps=true&continue_straight=true&annotations=congestion_numeric%2Cmaxspeed%2Cclosure%2Cspeed%2Cduration%2Cdistance&roundabout_exits=true&voice_instructions=true&banner_instructions=true&enable_refresh=true",
            RouterOrigin.ONLINE
        ).onError {
            Log.d("benchmark", "error parsing route $it")
            Log.d("benchmark", "response was $response")
        }
            .value!!.first()
        val alertsCount = route.routeInfo.alerts.size
        Log.d("benchmark", "alerts count is $alertsCount")
        benchmarkRule.measureRepeated {
            val alerts = route.routeInfo.alerts
            this.runWithTimingDisabled {
                System.gc()
            }
        }
    }
}

fun readRawFileText(context: Context, @IntegerRes res: Int): String =
    context.resources.openRawResource(res).bufferedReader().use { it.readText() }