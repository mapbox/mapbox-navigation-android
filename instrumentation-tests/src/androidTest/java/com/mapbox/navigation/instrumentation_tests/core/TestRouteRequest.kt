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
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.RouteRequestResult
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import org.junit.Before
import org.junit.Rule
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

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()
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
    fun set_navigation_routes_successfully() = sdkTest {
        listOf(
            "7.544358,51.50893;7.262347,51.450093",
            "-0.469474,39.620483;-0.287629,39.564798",
            "121.338932,24.999322;121.212432,25.014613",
            "2.078786,48.88777;2.03899,49.10212",
            "-121.86703,37.704912;-121.900812,37.688709",
            "4.500574,51.244239;4.471488,51.179944",
            "3.594571,50.966804;3.744523,51.038471",
            "18.035172,53.120348;17.980242,53.141947",
            "151.063636,-33.761515;150.924209,-33.676139",
            "8.282866,48.836644;8.342386,49.031469"
        ).forEach { coordinates ->
            var response: RouteRequestResult.Success
            var requestTime: Long? = null
            var responseTime: Long? = null
            var responseSize: Int? = null
            HttpServiceFactory.getInstance()
                .setInterceptor(object : HttpServiceInterceptorInterface {
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
                            responseSize = response.result.value!!.data.size
                        }
                        return response
                    }

                })
            val time = measureTime {
                val routeOptions = RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .coordinates(coordinates)
                    .alternatives(true)
                    .build()
                val result = mapboxNavigation.requestRoutes(routeOptions)
                response = result as RouteRequestResult.Success
            }
            writeResults(
                coordinates = coordinates,
                distance = response.routes.first().directionsRoute.distance(),
                sdkResponseTime = time.inWholeMilliseconds,
                networkResponseTime = responseTime!! - requestTime!!,
                responseSize = responseSize!!
            )
        }
    }

    private fun writeResults(
        coordinates: String,
        distance: Double,
        sdkResponseTime: Long,
        networkResponseTime: Long,
        responseSize: Int
    ) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "route-request-time-test-results.csv"
        )
        val header = if (file.createNewFile()) {
            "coordinates,distance,time from request to parsed response in milliseconds,network request time,response size bytes,device\n"
        } else ""
        val result = "$coordinates,$distance,$sdkResponseTime,$networkResponseTime,${responseSize},${Build.MODEL}\n"
        file.appendText(header + result)
        Log.d("time-test", result + "(written to ${file.absolutePath})")
    }
}
