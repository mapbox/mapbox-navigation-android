package com.mapbox.navigation.instrumentation_tests.ui.routeline

import android.content.Context
import android.location.Location
import androidx.annotation.IntegerRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.internal.route.RouteCompatibilityCache
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.instrumentation_tests.utils.idling.MapStyleInitIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.utils.internal.logE
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Timer
import kotlin.concurrent.timerTask

class SetRouteOrderTest : BaseTest<BasicNavigationViewActivity>(
    BasicNavigationViewActivity::class.java
) {

    private val initIdlingResource: MapStyleInitIdlingResource by lazy {
        MapStyleInitIdlingResource(activity.binding.mapView)
    }
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(MapboxRouteLineOptions.Builder(activity).build())
    }
    private val myResourceIdler =
        CountingIdlingResource("MultipleRouteSetTestResource")

    override fun setupMockLocation(): Location {
        val shortRoute = getRoute(activity, R.raw.short_route)
        val origin = shortRoute.routeOptions()!!.coordinatesList().first()
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = origin.latitude()
            longitude = origin.longitude()
        }
    }

    @Before
    fun setUp() {
        initIdlingResource.register()
        IdlingRegistry.getInstance().register(myResourceIdler)
        Espresso.onIdle()
    }

    @After
    fun tearDown() {
        initIdlingResource.unregister()
    }

    @Test
    fun multipleSetRouteCall_longAndShortRouteTest() {
        val shortRoute = getRoute(activity, R.raw.short_route).toNavigationRoute()
        val longRoute = getRoute(activity, R.raw.cross_country_route).toNavigationRoute()
        RouteCompatibilityCache.cacheCreationResult(listOf(shortRoute, longRoute))

        val consumerLongRoute =
            MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { value ->
                logE("long", "SetRouteCancellationTest")
                throw RuntimeException("Previous set routes call wasn't cancelled as expected.")
            }

        val consumerShortRoute =
            MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { value ->
                logE("short", "SetRouteCancellationTest")
                val primaryRoute = routeLineApi.getPrimaryNavigationRoute()
                val contents =
                    value.value!!.primaryRouteLineData.dynamicData.trafficExpressionProvider!!
                        .generateExpression().contents as ArrayList<*>
                assertEquals(shortRoute.directionsRoute, primaryRoute?.directionsRoute)
                assertEquals(
                    7,
                    contents.size
                )
                myResourceIdler.decrement()
            }

        myResourceIdler.increment()
        routeLineApi.setNavigationRoutes(listOf(longRoute), consumerLongRoute)
        Timer().schedule(
            timerTask {
                routeLineApi.setNavigationRoutes(listOf(shortRoute), consumerShortRoute)
            },
            5
        )

        Espresso.onIdle()
    }

    @Test
    fun multipleSetRouteCall_longAndShortRouteSameConsumerTest() {
        var consumerCallCount = 0
        val shortRoute = getRoute(activity, R.raw.short_route)
        val longRoute = getRoute(activity, R.raw.cross_country_route)
        val longRoutes = listOf(RouteLine(longRoute, null))
        val shortRoutes = listOf(RouteLine(shortRoute, null))

        val consumer =
            MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { value ->
                // The first call to set routes with the long route should get cancelled
                // so the short route used for the second call to set routes should be the primary.
                val primaryRoute = routeLineApi.getPrimaryRoute()
                val contents = value.value!!
                    .primaryRouteLineData
                    .dynamicData
                    .trafficExpressionProvider!!
                    .generateExpression()
                    .contents as ArrayList<*>
                assertEquals(shortRoute, primaryRoute)
                assertEquals(
                    7,
                    contents.size
                )

                consumerCallCount += 1
                myResourceIdler.decrement()
            }

        myResourceIdler.increment()
        routeLineApi.setRoutes(longRoutes, consumer)
        Timer().schedule(
            timerTask {
                routeLineApi.setRoutes(shortRoutes, consumer)
            },
            5
        )

        Espresso.onIdle()
    }

    @Test
    fun clearRoutesTest() {
        val route = getRoute(activity, R.raw.cross_country_route)
        val routeLines = listOf(RouteLine(route, null))
        var clearInvoked = false

        val consumer = MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { value ->
            check(!clearInvoked)
            val primaryRoute = routeLineApi.getPrimaryRoute()
            val contents = value.value!!
                .primaryRouteLineData
                .dynamicData
                .trafficExpressionProvider!!
                .generateExpression()
                .contents as ArrayList<*>
            assertEquals(route, primaryRoute)
            assertEquals(
                625,
                contents.size
            )
            myResourceIdler.decrement()
        }

        val clearConsumer =
            MapboxNavigationConsumer<Expected<RouteLineError, RouteLineClearValue>> { value ->
                clearInvoked = true
                assertEquals(
                    0,
                    value.value!!.primaryRouteSource.features()!!.size
                )
                myResourceIdler.decrement()
            }

        myResourceIdler.increment()
        myResourceIdler.increment()
        routeLineApi.setRoutes(routeLines, consumer)
        Timer().schedule(
            timerTask {
                routeLineApi.clearRouteLine(clearConsumer)
            },
            5
        )
    }

    private fun getRoute(context: Context, @IntegerRes routeFileResource: Int): DirectionsRoute {
        val routeAsString = readRawFileText(context, routeFileResource)
        return DirectionsRoute.fromJson(routeAsString)
    }
}
