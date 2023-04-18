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
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import android.Manifest

@OptIn(ExperimentalTime::class)
class TestRouteRequest {

    private lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setUp() {
        val instrumentation = getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        instrumentation.uiAutomation.grantRuntimePermission(packageName, Manifest.permission.READ_EXTERNAL_STORAGE)
        instrumentation.uiAutomation.grantRuntimePermission(packageName, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        instrumentation.uiAutomation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(instrumentation.targetContext)
                    .accessToken(getMapboxAccessTokenFromResources(instrumentation.targetContext))
                    .build()
            )
        }
    }

    @Test
    fun emptyTest() {

    }

    @Test
    fun set_navigation_routes_successfully() = sdkTest(timeout = 15 * 60 * 1000) {
        val testRunId = Date().time.toString()
        val testCases = testCoordinates.split(" \n")
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

val testCoordinates = "11.556629,48.17807;-1.17603,49.07621;27.42738,38.61291 \n" +
    "11.556629,48.17807;13.37691,52.51604 \n" +
    "11.5677064,48.1929694;99.81235,13.53424 \n" +
    "11.556609,48.1780642;13.37691,52.51604 \n" +
    "11.5668400251,48.1818997927;13.37691,52.51604 \n" +
    "11.4296636,47.9931385;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.5737369,48.189747;-9.14952,38.72633;16.36842,48.20263 \n" +
    "11.402489,47.9257878;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.4300383,47.994738;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.4289309,47.9941565;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.4024752,47.9257573;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.3783481,47.8481881;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.4030265,47.9268006;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.5840157356,48.1878;13.376918,52.516043 \n" +
    "11.556629,48.17807;13.37691,52.51604 \n" +
    "11.5663704212,48.1761101115;13.37691,52.51604 \n" +
    "11.403157,47.9267844;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.556629,48.17807;1.82466,50.45929;27.42738,38.61291 \n" +
    "11.3944659,47.9105577;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.3992346,47.8887374;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.556629,48.17807;24.75257,59.43642;27.42738,38.61291 \n" +
    "11.5739535,48.1937357;-0.12721,51.50643 \n" +
    "11.5739517,48.1937483;-0.12721,51.50643 \n" +
    "9.9716829,48.3958511;18.5234,4.32837 \n" +
    "11.3906892,47.8604079;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.4030265,47.9268007;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.556629,48.17807;3.10735,48.55617;27.42738,38.61291 \n" +
    "10.8024151,47.2154592;13.36112,38.12207;10.8728,47.80691 \n" +
    "11.556629,48.17807;29.97999,31.24675;27.42738,38.61291 \n" +
    "11.35997,47.8224;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "10.8024151,47.2154592;13.36112,38.12207;10.8728,47.80691 \n" +
    "11.556629,48.17807;3.90792,47.48976;27.42738,38.61291 \n" +
    "11.556629,48.17807;13.37691,52.51604 \n" +
    "11.556629,48.17807;-3.50005,50.54835 \n" +
    "11.556629,48.17807;7.06923,45.74704;27.42738,38.61291 \n" +
    "11.4029965,47.9276187;-2.91609,53.19512;1.4482,43.60525;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.556629,48.17807;-3.04254,53.60234;27.42738,38.61291 \n" +
    "11.8030624146,48.7129403445;9.98745,53.55562 \n" +
    "11.5789844,48.1951892;10.139132,47.2081063;-21.9424438,64.1459577 \n" +
    "11.556629,48.17807;8.24215,50.08466;27.42738,38.61291 \n" +
    "10.7865715,47.2135943;10.8728,47.80691;13.36112,38.12207 \n" +
    "11.556629,48.17807;13.37691,52.51604 \n" +
    "11.4092795,47.9457331;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "44.845853,48.697788;4.940725,51.657334 \n" +
    "11.556629,48.17807;-3.01709,53.20409;27.42738,38.61291 \n" +
    "10.776486499,47.2164195448;10.87284,47.806723;13.36112,38.12207 \n" +
    "-3.715614,48.576627;-4.099141,48.010914 \n" +
    "11.556629,48.17807;0.11173,51.46201;27.42738,38.61291 \n" +
    "11.556629,48.17807;-0.12719,51.50643 \n" +
    "11.3965493,47.9043193;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.556629,48.17807;9.54455,47.59918;27.42738,38.61291 \n" +
    "44.616753,48.888868;4.940725,51.657334 \n" +
    "44.781469,48.739324;4.940725,51.657334 \n" +
    "11.4260065,48.0294335;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.802416009,48.7193104081;9.98745,53.55562 \n" +
    "11.3598136,47.8223907;6.236919,46.394687;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.637671,48.251936;45.4642,49.4642 \n" +
    "11.7999541556,48.7113479323;9.98745,53.55562 \n" +
    "11.556629,48.17807;11.51966,48.04218;27.42738,38.61291 \n" +
    "44.86082,48.714028;4.940725,51.657334 \n" +
    "11.556629,48.17807;14.43355,50.09242;27.42738,38.61291 \n" +
    "44.779741,48.740219;4.940725,51.657334 \n" +
    "11.4275913,47.9975795;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "11.4260071,48.0294343;-6.24827,53.34807;-2.9784,53.41005 \n" +
    "44.85016,48.692611;4.940725,51.657334 \n" +
    "44.859935,48.719977;4.940725,51.657334 \n" +
    "44.832105,48.745531;4.940725,51.657334 \n" +
    "44.845853,48.697788;4.940725,51.657334 \n" +
    "44.781469,48.739324;4.940725,51.657334 \n" +
    "44.72408,48.807735;4.940725,51.657334 \n" +
    "44.862374,48.715382;4.940725,51.657334 \n" +
    "44.845853,48.697788;4.940725,51.657334 \n" +
    "44.845853,48.697788;4.940725,51.657334 \n" +
    "44.844962,48.727858;4.940725,51.657334 \n" +
    "44.844783,48.703681;4.940725,51.657334 \n" +
    "44.841714,48.752039;4.940725,51.657334 \n" +
    "11.5898499,48.5538424;-4.27561,55.78318;11.4250088,48.7630351 \n" +
    "11.7999358413,48.7153544454;9.98745,53.55562 \n" +
    "11.5679494,48.1911628;9.98745,53.55562 \n" +
    "44.854457,48.722867;4.940725,51.657334 \n" +
    "44.812579,48.778754;4.940725,51.657334 \n" +
    "8.520268,47.183087;37.02774,39.734052 \n" +
    "44.78958,48.756694;4.940725,51.657334 \n" +
    "44.723088,48.804567;4.940725,51.657334 \n" +
    "44.845853,48.697788;4.940725,51.657334 \n" +
    "44.734406,48.790446;4.940725,51.657334 \n" +
    "44.854457,48.722867;4.940725,51.657334 \n" +
    "44.844783,48.703681;4.940725,51.657334 \n" +
    "44.831926,48.734697;4.940725,51.657334 \n" +
    "44.845853,48.697788;4.940725,51.657334 \n" +
    "44.844962,48.727858;4.940725,51.657334 \n" +
    "44.862374,48.715382;4.940725,51.657334 \n" +
    "44.723088,48.804567;4.940725,51.657334 \n" +
    "44.859935,48.719977;4.940725,51.657334 \n" +
    "44.80811,48.777352;4.940725,51.657334 \n" +
    "15.56254,38.211048;11.57288,50.925545 \n" +
    "11.5540919,48.1862157;-2.9784,53.41005;11.5157603,48.1098886 \n" +
    "44.78958,48.756694;4.940725,51.657334 \n" +
    "44.790432,48.768936;4.940725,51.657334 \n" +
    "44.851576,48.724353;4.940725,51.657334"