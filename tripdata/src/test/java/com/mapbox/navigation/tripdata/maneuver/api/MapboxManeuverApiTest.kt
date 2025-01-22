package com.mapbox.navigation.tripdata.maneuver.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxOptions
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.tripdata.maneuver.ManeuverAction
import com.mapbox.navigation.tripdata.maneuver.ManeuverProcessor
import com.mapbox.navigation.tripdata.maneuver.ManeuverResult
import com.mapbox.navigation.tripdata.maneuver.ManeuverState
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverOptions
import com.mapbox.navigation.tripdata.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.tripdata.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.tripdata.shield.internal.api.getRouteShieldsFromModels
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.tripdata.shield.model.RouteShieldCallback
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxManeuverApiTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val maneuverOptions = mockk<ManeuverOptions>()
    private val distanceFormatter = mockk<DistanceFormatter>()
    private val routeShieldApi = mockk<MapboxRouteShieldApi>(relaxed = true)
    private val mapboxManeuverApi = MapboxManeuverApi(
        distanceFormatter,
        maneuverOptions,
        routeShieldApi,
    )

    private val testManeuvers = listOf<Maneuver>(
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_0"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/primary/url1"
                            every { mapboxShield } returns null
                            every { text } returns ""
                        }
                    },
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns "https://shield.mapbox.com/primary/url2"
                            every { mapboxShield } returns null
                            every { text } returns ""
                        }
                    },
                )
            }
            every { secondary } returns null
            every { sub } returns null
        },
        mockk {
            every { primary } returns mockk {
                every { id } returns "primary_1"
                every { componentList } returns listOf(
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns null
                            every { mapboxShield } returns MapboxShield.builder()
                                .baseUrl("base")
                                .name("name")
                                .textColor("color")
                                .displayRef("123")
                                .build()
                            every { text } returns ""
                        }
                    },
                    mockk {
                        every { node } returns mockk<RoadShieldComponentNode> {
                            every { shieldUrl } returns null
                            every { mapboxShield } returns MapboxShield.builder()
                                .baseUrl("base")
                                .name("name")
                                .textColor("color")
                                .displayRef("456")
                                .build()
                            every { text } returns ""
                        }
                    },
                )
            }
            every { secondary } returns null
            every { sub } returns null
        },
    )

    @Before
    fun setup() {
        mockkObject(ManeuverProcessor)
        mockkStatic(MapboxOptions::class)
        every { MapboxOptions.accessToken } returns "token"
    }

    @After
    fun tearDown() {
        unmockkObject(ManeuverProcessor)
        unmockkStatic(MapboxOptions::class)
    }

    @Test
    fun `when get maneuver invoked and processor returns failure`() {
        val dirRoute = mockk<DirectionsRoute>()
        val route = mockk<NavigationRoute> {
            every { directionsRoute } returns dirRoute
        }
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverListWithRoute(
            dirRoute,
            null,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverList.Failure("whatever")

        val result = mapboxManeuverApi.getManeuvers(route)

        assertTrue(result.isError)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when get maneuver invoked and processor returns inappropriate result`() {
        val dirRoute = mockk<DirectionsRoute>()
        val route = mockk<NavigationRoute> {
            every { directionsRoute } returns dirRoute
        }
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverListWithRoute(
            dirRoute,
            null,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverListWithProgress.Failure("whatever")

        mapboxManeuverApi.getManeuvers(route)
    }

    @Test
    fun `when get maneuver invoked and processor returns success`() {
        val dirRoute = mockk<DirectionsRoute>()
        val route = mockk<NavigationRoute> {
            every { directionsRoute } returns dirRoute
        }
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverListWithRoute(
            dirRoute,
            null,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverList.Success(listOf())

        val result = mapboxManeuverApi.getManeuvers(route)

        assertTrue(result.isValue)
    }

    @Test
    fun `when get maneuver with progress invoked and processor returns failure`() {
        val routeProgress = mockk<RouteProgress>()
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverListWithProgress.Failure("whatever")

        val result = mapboxManeuverApi.getManeuvers(routeProgress)

        assertTrue(result.isError)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when get maneuver with progress invoked and processor returns inappropriate result`() {
        val routeProgress = mockk<RouteProgress>()
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverList.Failure("whatever")

        mapboxManeuverApi.getManeuvers(routeProgress)
    }

    @Test
    fun `when get maneuver with progress invoked and processor returns success`() {
        val routeProgress = mockk<RouteProgress>()
        val maneuverState = ManeuverState()
        val action = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        every { ManeuverProcessor.process(action) } returns
            ManeuverResult.GetManeuverListWithProgress.Success(listOf())

        val result = mapboxManeuverApi.getManeuvers(routeProgress)

        assertTrue(result.isValue)
    }

    @Test
    fun `when legacy roadShields is invoked using new api returns success`() =
        coroutineRule.runBlockingTest {
            mockkStatic(MapboxRouteShieldApi::getRouteShieldsFromModels)
            coEvery { routeShieldApi.getRouteShieldsFromModels(any()) } returns listOf(
                ExpectedFactory.createError(
                    mockk {
                        every { url } returns "https://mapbox.com/url1"
                        every { errorMessage } returns "whatever1"
                    },
                ),
                ExpectedFactory.createError(
                    mockk {
                        every { url } returns "https://mapbox.com/url2"
                        every { errorMessage } returns "whatever2"
                    },
                ),
            )
            val maneuverList = listOf(testManeuvers[0])
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            mapboxManeuverApi.getRoadShields(maneuverList, callback)

            verify(exactly = 1) {
                callback.onRoadShields(
                    capture(shieldSlot),
                )
            }
            assertEquals(2, shieldSlot.captured.size)
            unmockkStatic(MapboxRouteShieldApi::getRouteShieldsFromModels)
        }

    @Test
    fun `when mapbox roadShields is invoked with new api returns success`() =
        coroutineRule.runBlockingTest {
            val userId = "userId"
            val styleId = "styleId"
            mockkStatic(MapboxRouteShieldApi::getRouteShieldsFromModels)
            val mockByteArray = byteArrayOf(1)
            coEvery { routeShieldApi.getRouteShieldsFromModels(any()) } returns listOf(
                ExpectedFactory.createValue(
                    mockk {
                        every { shield } returns mockk<RouteShield.MapboxDesignedShield> {
                            every { url } returns "https://shield.mapbox.com/url1"
                            every { byteArray } returns mockByteArray
                            every { mapboxShield } returns mockk()
                            every { shieldSprite } returns mockk()
                        }
                        every { origin } returns mockk {
                            every { isFallback } returns false
                            every { originalUrl } returns "https://shield.mapbox.com/url1"
                            every { originalErrorMessage } returns ""
                        }
                    },
                ),
                ExpectedFactory.createValue(
                    mockk {
                        every { shield } returns mockk<RouteShield.MapboxDesignedShield> {
                            every { url } returns "https://shield.mapbox.com/url2"
                            every { byteArray } returns mockByteArray
                            every { mapboxShield } returns mockk()
                            every { shieldSprite } returns mockk()
                        }
                        every { origin } returns mockk {
                            every { isFallback } returns false
                            every { originalUrl } returns "https://shield.mapbox.com/url2"
                            every { originalErrorMessage } returns ""
                        }
                    },
                ),
            )
            val maneuverList = listOf(testManeuvers[1])
            val callback: RouteShieldCallback = mockk(relaxed = true)
            val shieldSlot = slot<List<Expected<RouteShieldError, RouteShieldResult>>>()

            mapboxManeuverApi.getRoadShields(userId, styleId, maneuverList, callback)

            verify(exactly = 1) {
                callback.onRoadShields(
                    capture(shieldSlot),
                )
            }
            assertEquals(2, shieldSlot.captured.size)
            assertEquals(mockByteArray, shieldSlot.captured[0].value?.shield?.byteArray)
            assertEquals(
                "https://shield.mapbox.com/url2",
                shieldSlot.captured[1].value?.shield?.url,
            )
            unmockkStatic(MapboxRouteShieldApi::getRouteShieldsFromModels)
        }
}
