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

private const val LOG_TAG = "vadzim-test"

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
        val response = readRawFileText(context, R.raw.test_route)
        val route = RouteParser.parseDirectionsResponse(
            response,
            "https://api.mapbox.com/directions/v5/mapbox/driving/18.047275378041377,54.56050010079204;17.952359140908555,54.57131787223446?access_token=***&geometries=polyline6&alternatives=true&overview=full&steps=true&continue_straight=true&annotations=congestion_numeric%2Cmaxspeed%2Cclosure%2Cspeed%2Cduration%2Cdistance&roundabout_exits=true&voice_instructions=true&banner_instructions=true&enable_refresh=true",
            RouterOrigin.ONLINE
        ).onError {
            Log.d(LOG_TAG, "error parsing route $it")
            Log.d(LOG_TAG, "response was $response")
        }
            .value!!.first()
        val alertsCount = route.routeInfo.alerts.size
        val location = route.routeInfo.alerts.map { it.roadObject.location }
        Log.d(LOG_TAG, "alerts count is $alertsCount")
        Log.d(LOG_TAG, "locations are $location")
        benchmarkRule.measureRepeated {
            val alerts = route.routeInfo.alerts
            this.runWithTimingDisabled {
                Runtime.getRuntime().gc();
            }
        }
    }
}

fun readRawFileText(context: Context, @IntegerRes res: Int): String =
    context.resources.openRawResource(res).bufferedReader().use { it.readText() }