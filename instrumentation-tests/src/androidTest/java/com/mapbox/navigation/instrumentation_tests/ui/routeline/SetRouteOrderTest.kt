package com.mapbox.navigation.instrumentation_tests.ui.routeline

import android.content.Context
import android.location.Location
import android.os.CountDownTimer
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.internal.route.testing.createNavigationRouteForTest
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.utils.idling.MapStyleInitIdlingResource
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedCallback
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedResult
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Timer
import kotlin.concurrent.timerTask

class SetRouteOrderTest : BaseTest<BasicNavigationViewActivity>(
    BasicNavigationViewActivity::class.java,
) {

    private val initIdlingResource: MapStyleInitIdlingResource by lazy {
        MapStyleInitIdlingResource(activity.binding.mapView)
    }

    private val myResourceIdler =
        CountingIdlingResource("MultipleRouteSetTestResource")

    override fun setupMockLocation(): Location {
        val shortRoute = getRoute(context, R.raw.short_route)
        val origin = shortRoute.directionsRoute.routeOptions()!!.coordinatesList().first()
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
        val routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        val routeLineView = MapboxRouteLineView(
            MapboxRouteLineViewOptions.Builder(activity).build(),
        )
        val shortRoute = getRoute(activity, R.raw.short_route)
        val longRoute = getRoute(activity, R.raw.cross_country_route)

        val consumerLongRoute =
            MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { value ->
                logE("long", "SetRouteCancellationTest")
                throw RuntimeException("Previous set routes call wasn't cancelled as expected.")
            }

        myResourceIdler.increment()
        routeLineApi.setNavigationRoutes(listOf(longRoute), consumerLongRoute)
        Timer().schedule(
            timerTask {
                routeLineApi.setNavigationRoutes(listOf(shortRoute)) {
                    routeLineView.renderRouteDrawData(
                        activity.binding.mapView.mapboxMap.style!!,
                        it,
                        activity.binding.mapView.mapboxMap,
                        object : RoutesRenderedCallback {
                            override fun onRoutesRendered(result: RoutesRenderedResult) {
                                object : CountDownTimer(1000, 1000) {
                                    override fun onTick(p0: Long) {
                                        // no-op
                                    }

                                    override fun onFinish() {
                                        val property =
                                            activity.binding.mapView.mapboxMap.style!!
                                                .getStyleLayerProperty(
                                                    "mapbox-layerGroup-1-traffic",
                                                    "line-gradient",
                                                )
                                        assertNotNull(property.value.contents)
                                        assertTrue(property.value.contents is ArrayList<*>)
                                        assertEquals(
                                            9,
                                            (property.value.contents as ArrayList<*>).size,
                                        )
                                        myResourceIdler.decrement()
                                    }
                                }.start()
                            }
                        },
                    )
                }
            },
            5,
        )

        Espresso.onIdle()
    }

    @Test
    fun multipleSetRouteCall_longAndShortRouteSameConsumerTest() {
        var consumerCallCount = 0
        val routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        val routeLineView = MapboxRouteLineView(
            MapboxRouteLineViewOptions.Builder(activity).build(),
        )
        val shortRoute = getRoute(activity, R.raw.short_route)
        val longRoute = getRoute(activity, R.raw.cross_country_route)
        val longRoutes = listOf(longRoute)
        val shortRoutes = listOf(shortRoute)

        val consumer =
            MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { value ->
                // The first call to set routes with the long route should get cancelled
                // so the short route used for the second call to set routes should be the primary.
                runBlocking {
                    routeLineView.renderRouteDrawData(
                        activity.binding.mapView.mapboxMap.style!!,
                        value,
                        activity.binding.mapView.mapboxMap,
                        object : RoutesRenderedCallback {
                            override fun onRoutesRendered(result: RoutesRenderedResult) {
                                object : CountDownTimer(1000, 1000) {
                                    override fun onTick(p0: Long) {
                                        // no-op
                                    }

                                    override fun onFinish() {
                                        val property =
                                            activity.binding.mapView.mapboxMap.style!!
                                                .getStyleLayerProperty(
                                                    "mapbox-layerGroup-1-traffic",
                                                    "line-gradient",
                                                )
                                        assertNotNull(property.value.contents)
                                        assertTrue(property.value.contents is ArrayList<*>)
                                        assertEquals(
                                            9,
                                            (property.value.contents as ArrayList<*>).size,
                                        )
                                        myResourceIdler.decrement()
                                    }
                                }.start()
                            }
                        },
                    )
                }

                consumerCallCount += 1
            }

        myResourceIdler.increment()
        routeLineApi.setNavigationRoutes(longRoutes, consumer)
        Timer().schedule(
            timerTask {
                routeLineApi.setNavigationRoutes(shortRoutes, consumer)
            },
            5,
        )

        Espresso.onIdle()
    }

    @Test
    fun clearRoutesTest() {
        val routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        val routeLineView = MapboxRouteLineView(
            MapboxRouteLineViewOptions.Builder(activity).build(),
        )
        val route = getRoute(activity, R.raw.cross_country_route)
        var clearInvoked = false

        val consumer = MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>> { value ->
            check(!clearInvoked)
            routeLineView.renderRouteDrawData(
                activity.binding.mapView.mapboxMap.style!!,
                value,
                activity.binding.mapView.mapboxMap,
                object : RoutesRenderedCallback {
                    override fun onRoutesRendered(result: RoutesRenderedResult) {
                        object : CountDownTimer(1000, 1000) {
                            override fun onTick(p0: Long) {
                                // no-op
                            }

                            override fun onFinish() {
                                val property = activity.binding.mapView.mapboxMap.style!!
                                    .getStyleLayerProperty(
                                        "mapbox-layerGroup-1-traffic",
                                        "line-gradient",
                                    )
                                assertNotNull(property.value.contents)
                                assertTrue(property.value.contents is ArrayList<*>)
                                assertEquals(625, (property.value.contents as ArrayList<*>).size)
                                myResourceIdler.decrement()
                            }
                        }.start()
                    }
                },
            )
        }

        val clearConsumer =
            MapboxNavigationConsumer<Expected<RouteLineError, RouteLineClearValue>> { value ->
                clearInvoked = true
                routeLineView.renderClearRouteLineValue(
                    activity.binding.mapView.mapboxMap.style!!,
                    value,
                    activity.binding.mapView.mapboxMap,
                    object : RoutesRenderedCallback {
                        override fun onRoutesRendered(result: RoutesRenderedResult) {
                            // callback is invoked when rendering is finished
                            myResourceIdler.decrement()
                        }
                    },
                )
            }

        myResourceIdler.increment()
        myResourceIdler.increment()
        routeLineApi.setNavigationRoutes(listOf(route), consumer)
        Timer().schedule(
            timerTask {
                routeLineApi.clearRouteLine(clearConsumer)
            },
            5,
        )
        Espresso.onIdle()
    }

    private fun getRoute(context: Context, routeResourceId: Int): NavigationRoute {
        val routeAsString = readRawFileText(context, routeResourceId)
        val directionRoute = DirectionsRoute.fromJson(routeAsString)
        return createNavigationRouteForTest(
            DirectionsResponse.builder()
                .code("Ok")
                .routes(listOf(directionRoute))
                .build(),
            directionRoute.routeOptions()!!,
            RouterOrigin.ONLINE,
        ).first()
    }
}
