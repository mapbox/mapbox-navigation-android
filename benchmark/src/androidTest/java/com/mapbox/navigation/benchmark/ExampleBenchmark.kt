package com.mapbox.navigation.benchmark

import android.Manifest
import android.content.Context
import androidx.annotation.IntegerRes
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
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

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun measureTimeTo_ReadJson() {
        val routeAsJson = loadJsonFixture(context,R.raw.multileg_route)

        benchmarkRule.measureRepeated {
            DirectionsRoute.fromJson(routeAsJson)
        }
    }

    @Test
    fun measureTimeTo_MapDirectionsRouteGeometry() {
        val routeAsJson = loadJsonFixture(context,R.raw.multileg_route)
        val directionsRoute = DirectionsRoute.fromJson(routeAsJson)
        val replayRouteMapper = ReplayRouteMapper()
        benchmarkRule.measureRepeated {
            replayRouteMapper.mapDirectionsRouteGeometry(directionsRoute)
        }
    }

    @Test
    fun measureTimeTo_doNothing() {
        benchmarkRule.measureRepeated {
            // Do nothing
        }
    }

    private fun loadJsonFixture(context: Context, @IntegerRes res: Int): String =
        context.resources.openRawResource(res).bufferedReader().use { it.readText() }

    /**
     * This is failing
     *
    MapboxInvalidModuleException(type=NavigationRouter)
    at com.mapbox.common.module.provider.MapboxModuleProvider.createModule(MapboxModuleProvider.kt:88)
    at com.mapbox.navigation.core.MapboxNavigation.<init>(MapboxNavigation.kt:204)
    at com.mapbox.navigation.benchmark.ExampleBenchmark.measureTimeToStartTripSession(ExampleBenchmark.kt:41)
    at java.lang.reflect.Method.invoke(Native Method)
    at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
    at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
    at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
     */
//    @Test
//    fun measureTimeToStartTripSession() {
//        benchmarkRule.measureRepeated {
//            val mapboxNavigation = MapboxNavigation(
//                NavigationOptions.Builder(context)
//                    .accessToken(context.getString(R.string.mapbox_access_token))
//                    .build()
//            )
//            mapboxNavigation.startTripSession()
//        }
//    }
}
