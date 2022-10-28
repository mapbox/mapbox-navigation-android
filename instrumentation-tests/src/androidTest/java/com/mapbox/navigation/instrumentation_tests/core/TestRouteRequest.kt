package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.os.Environment
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.api.directions.v5.models.RouteOptions
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
            "2.078786,48.88777;2.03899,49.10212"
        ).forEach { coordinates ->
            var response: RouteRequestResult.Success
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
                timeFromRequestToResponseInMilliseconds = time.inWholeMilliseconds
            )
        }
    }
}

private fun writeResults(
    coordinates: String,
    distance: Double,
    timeFromRequestToResponseInMilliseconds: Long
) {
    val file = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "results.csv"
    )
    val header = if (file.createNewFile()) {
        "coordinates,distance,time from request to parsed response in milliseconds\n"
    } else ""
    val result = "$coordinates,$distance,$timeFromRequestToResponseInMilliseconds\n"
    file.appendText(header + result)
    Log.d("time-test", result + "(written to ${file.absolutePath})")
}
