package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.os.Build
import android.os.Environment
import android.util.Log
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.DownloadOptions
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.HttpServiceInterceptorInterface
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Date
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class TestRouteRequest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    private lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setUp() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .build()
            )
        }
    }

    @Test
    fun emptyTest() {

    }

    @Test
    fun set_navigation_routes_successfully() = sdkTest(timeout = 5 * 60 * 1000) {
        val testRunId = Date().time.toString()
        val testCases = listOf(
            // short
            "7.544358,51.50893;7.262347,51.450093",
            "-0.469474,39.620483;-0.287629,39.564798",
            "121.338932,24.999322;121.212432,25.014613",
            "2.078786,48.88777;2.03899,49.10212",
            "-121.86703,37.704912;-121.900812,37.688709",
            "4.500574,51.244239;4.471488,51.179944",
            "3.594571,50.966804;3.744523,51.038471",
            "18.035172,53.120348;17.980242,53.141947",
            "151.063636,-33.761515;150.924209,-33.676139",
            "8.282866,48.836644;8.342386,49.031469",
            // medium
            "11.068773,48.395769;11.78753,48.351929",
            "6.841077,50.641112;7.119309,50.879783",
            "10.053927,48.973696;9.94959,49.39326",
            "4.785689,51.166292;4.335537,51.172037",
            "151.930139,-28.644001;152.014111,-29.03781",
            "10.238754,49.72532;10.916919,49.892882",
            "-117.8544,34.0684;-118.242378,34.047664",
            "8.449441,49.661692;8.540599,49.340809",
            // large
            "7.497153,51.370885;9.923661,53.563794",
            "9.850301,52.323563;12.51728,49.195625",
            "15.854543,50.193145;13.235352,49.137469",
            "9.26678,48.68429;14.432992,50.079143",
            "15.640537,56.204498;13.00278,55.581761",
            "8.538636,49.396247;10.741578,47.559664",
            "6.315747,51.500173;10.157902,48.679648",
            "7.241153,43.690799;5.832344,43.174577",
            "9.462456,54.81521;8.70746,53.270071",
            // huge
            "11.5104917,48.1921736;-0.12719,51.50643",
            "7.790255,48.3072049;5.00308,43.58463",
            "13.569852,52.443647;8.423119,48.101636",
            "6.785334,52.728778;13.600185,45.177684",
            "-0.045608,46.441391;-6.942835,37.25444",
            "6.769568,52.286483;16.322807,48.177671",
            "7.228465,52.46652;13.600185,45.177684",
            "2.475978,48.94695;0.042546,38.653993",
            "0.87803,40.970268;6.44268,51.1955",
            "0.967296,41.047474;6.44268,51.1955",
        )
        for (coordinates in testCases) {
            val measurementFull = measureResponseTimeRetryable(coordinates) {
                this
            }
            val measurementNoAnnotation = measureResponseTimeRetryable(coordinates) {
                this.annotations(null)
            }
            writeResults(
                testRunId = testRunId,
                coordinates = coordinates,
                listOf(
                    "full" to measurementFull,
                    "no annotations" to measurementNoAnnotation
                )
            )
        }
    }

    private suspend fun measureResponseTimeRetryable(
        coordinates: String,
        requestModifier: RouteOptions.Builder.() -> RouteOptions.Builder
    ): Measurement.SuccessfulMeasurement {
        while (true) {
            val measurement = measureResponseTime(coordinates, requestModifier)
            if (measurement is Measurement.SuccessfulMeasurement) {
                return measurement
            }
        }
    }

    private suspend fun measureResponseTime(
        coordinates: String,
        requestModifier: RouteOptions.Builder.() -> RouteOptions.Builder
    ): Measurement {
        var requestTime: Long? = null
        var responseTime: Long? = null
        var responseSize: Int? = null
        val interceptor = object : HttpServiceInterceptorInterface {
            override fun onRequest(request: HttpRequest): HttpRequest {
                if (request.url.contains(coordinates)) {
                    requestTime = Date().time
                }
                return request
            }

            override fun onDownload(download: DownloadOptions): DownloadOptions {
                return download
            }

            override fun onResponse(response: HttpResponse): HttpResponse {
                if (response.request.url.contains(coordinates)) {
                    responseTime = Date().time
                    responseSize = response.result.value?.data?.size
                }
                return response
            }

        }
        HttpServiceFactory.getInstance().setInterceptor(interceptor)
        val response: RouteRequestResult
        val time = measureTime {
            val routeOptions = RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinates(coordinates)
                .alternatives(false)
                .enableRefresh(false)
                .requestModifier()
                .build()
            response = mapboxNavigation.requestRoutes(routeOptions)
        }
        return when (response) {
            is RouteRequestResult.Failure -> {
                val failure = response as RouteRequestResult.Failure
                Log.e("time-test", "error getting route for $coordinates: ${failure.reasons}")
                Measurement.FailedMeasurement
            }
            is RouteRequestResult.Success -> {
                Measurement.SuccessfulMeasurement(
                    timeFromRequestTillFullResponse = responseTime!! - requestTime!!,
                    deviceProcessingTime = time.inWholeMilliseconds - (responseTime!! - requestTime!!),
                    timeFromRequestTillParsedRoute = time.inWholeMilliseconds,
                    responseSize = responseSize!!,
                    distance = response.routes.first().directionsRoute.distance()
                )
            }
        }
    }

    private fun writeResults(
        testRunId: String,
        coordinates: String,
        measurements: List<Pair<String, Measurement.SuccessfulMeasurement>>,
    ) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "route-request-time-test-results-${testRunId}.csv"
        )
        val resultBuilder = StringBuilder()
        if (file.createNewFile()) {
            resultBuilder.append("coordinates,distance,")
            for (measurement in measurements) {
                resultBuilder.append(
                    "time from request to parsed response in milliseconds ${measurement.first},",
                    "network request time ${measurement.first},",
                    "device processing time ${measurement.first},",
                    "response size bytes ${measurement.first},"
                )
            }
            resultBuilder.append("device\n")
        }
        resultBuilder.append(
            "\"", coordinates, "\","
        )
        resultBuilder.append(measurements.first().second.distance, ",")
        for (measurement in measurements) {
            resultBuilder.append(
                measurement.second.timeFromRequestTillParsedRoute,
                ",",
                measurement.second.timeFromRequestTillFullResponse,
                ",",
                measurement.second.deviceProcessingTime,
                ",",
                measurement.second.responseSize,
                ",",
            )
        }
        resultBuilder.append("${Build.MODEL}\n")
        file.appendText(resultBuilder.toString())
        Log.d("time-test", "result for $coordinates" + "(written to ${file.absolutePath})")
    }
}

sealed class Measurement {
    object FailedMeasurement : Measurement()
    data class SuccessfulMeasurement(
        val timeFromRequestTillFullResponse: Long,
        val deviceProcessingTime: Long,
        val responseSize: Int,
        val distance: Double,
        val timeFromRequestTillParsedRoute: Long
    ) : Measurement()
}
