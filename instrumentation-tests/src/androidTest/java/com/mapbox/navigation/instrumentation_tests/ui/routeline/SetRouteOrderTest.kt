package com.mapbox.navigation.instrumentation_tests.ui.routeline

import android.content.Context
import androidx.annotation.IntegerRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.bindgen.Expected
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
import com.mapbox.navigation.utils.internal.LoggerProvider
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
        val shortRoute = getRoute(activity, R.raw.short_route)
        val longRoute = getRoute(activity, R.raw.cross_country_route)
        val longRoutes = listOf(RouteLine(longRoute, null))
        val shortRoutes = listOf(RouteLine(shortRoute, null))

        val consumerLongRoute =
            MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { value ->
                LoggerProvider.logger.e(Tag("SetRouteCancellationTest"), Message("long"))
                val primaryRoute = routeLineApi.getPrimaryRoute()
                val contents =
                    value.value!!.trafficLineExpressionProvider!!.invoke().contents as ArrayList<*>
                assertEquals(
                    625,
                    contents.size
                )
                assertEquals(longRoute, primaryRoute)
                myResourceIdler.decrement()
            }

        val consumerShortRoute =
            MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { value ->
                LoggerProvider.logger.e(Tag("SetRouteCancellationTest"), Message("short"))
                val primaryRoute = routeLineApi.getPrimaryRoute()
                val contents =
                    value.value!!.trafficLineExpressionProvider!!.invoke().contents as ArrayList<*>
                assertEquals(shortRoute, primaryRoute)
                assertEquals(
                    7,
                    contents.size
                )
                myResourceIdler.decrement()
            }

        myResourceIdler.increment()
        myResourceIdler.increment()
        routeLineApi.setRoutes(longRoutes, consumerLongRoute)
        Timer().schedule(
            timerTask {
                routeLineApi.setRoutes(shortRoutes, consumerShortRoute)
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
                val primaryRoute = routeLineApi.getPrimaryRoute()
                // The long route result should come in first even though it takes longer.
                if (consumerCallCount == 0) {
                    val contents = value.value!!
                        .trafficLineExpressionProvider!!
                        .invoke()
                        .contents as ArrayList<*>
                    assertEquals(
                        625,
                        contents.size
                    )
                    assertEquals(longRoute, primaryRoute)
                } else {
                    val contents = value.value!!
                        .trafficLineExpressionProvider!!
                        .invoke()
                        .contents as ArrayList<*>
                    assertEquals(shortRoute, primaryRoute)
                    assertEquals(
                        7,
                        contents.size
                    )
                }

                consumerCallCount += 1
                myResourceIdler.decrement()
            }

        myResourceIdler.increment()
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
                .trafficLineExpressionProvider!!
                .invoke()
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
