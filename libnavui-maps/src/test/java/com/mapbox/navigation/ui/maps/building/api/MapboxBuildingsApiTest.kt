package com.mapbox.navigation.ui.maps.building.api

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedRenderedFeature
import com.mapbox.maps.QueryRenderedFeaturesCallback
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.building.BuildingAction
import com.mapbox.navigation.ui.maps.building.BuildingProcessor
import com.mapbox.navigation.ui.maps.building.BuildingResult
import com.mapbox.navigation.ui.maps.building.model.BuildingError
import com.mapbox.navigation.ui.maps.building.model.BuildingValue
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxBuildingsApiTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val consumer =
        mockk<MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(BuildingProcessor)
    }

    @After
    fun tearDown() {
        unmockkObject(BuildingProcessor)
    }

    @Test
    fun `map query rendered feature fails no op`() = coroutineRule.runBlockingTest {
        val queriedFeaturesExpected = mockk<Expected<String, List<QueriedRenderedFeature>>> {
            every { value } returns null
            every { error } returns "whatever"
        }
        val mapboxMap = mockk<MapboxMap> {
            every {
                queryRenderedFeatures(
                    any<RenderedQueryGeometry>(),
                    any<RenderedQueryOptions>(),
                    any(),
                )
            } answers {
                thirdArg<QueryRenderedFeaturesCallback>().run(queriedFeaturesExpected)
                Cancelable {}
            }
        }
        val mockPoint = Point.fromLngLat(123.3434, -37.4567)
        val mockAction = BuildingAction.QueryBuilding(mockPoint, mapboxMap)
        val mockResult = BuildingResult.QueriedBuildings(
            ExpectedFactory.createError(BuildingError(queriedFeaturesExpected.error)),
        )
        val messageSlot = slot<Expected<BuildingError, BuildingValue>>()
        coEvery { BuildingProcessor.queryBuilding(mockAction) } returns mockResult
        val buildingsApi = MapboxBuildingsApi(mapboxMap)

        buildingsApi.queryBuildingToHighlight(mockPoint, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals("whatever", messageSlot.captured.error?.errorMessage)
    }

    @Test
    fun `map query rendered feature returns list of queried features`() =
        coroutineRule.runBlockingTest {
            val queriedFeaturesExpected = mockSuccessQueriedFeature()
            val mapboxMap = mockk<MapboxMap> {
                every {
                    queryRenderedFeatures(
                        any<RenderedQueryGeometry>(),
                        any<RenderedQueryOptions>(),
                        any(),
                    )
                } answers {
                    thirdArg<QueryRenderedFeaturesCallback>().run(queriedFeaturesExpected)
                    Cancelable {}
                }
            }
            val mockPoint = Point.fromLngLat(123.3434, -37.4567)
            val mockAction = BuildingAction.QueryBuilding(mockPoint, mapboxMap)
            val mockResult = BuildingResult.QueriedBuildings(
                ExpectedFactory.createValue(
                    BuildingValue(queriedFeaturesExpected.value ?: emptyList()),
                ),
            )
            val messageSlot = slot<Expected<BuildingError, BuildingValue>>()
            coEvery { BuildingProcessor.queryBuilding(mockAction) } returns mockResult
            val buildingsApi = MapboxBuildingsApi(mapboxMap)

            buildingsApi.queryBuildingToHighlight(mockPoint, consumer)

            verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
            assertEquals(queriedFeaturesExpected.value, messageSlot.captured.value?.buildings)
        }

    @Test
    fun `query building on waypoint arrival with null point`() {
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true)
        val mapboxMap = mockk<MapboxMap>()
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)
        val mockResult = BuildingResult.GetDestination(null)
        val messageSlot = slot<Expected<BuildingError, BuildingValue>>()
        coEvery { BuildingProcessor.queryBuildingOnWaypoint(mockAction) } returns mockResult
        val errorMessage = "waypoint inside $mockRouteProgress is null"
        val buildingsApi = MapboxBuildingsApi(mapboxMap)

        buildingsApi.queryBuildingOnWaypoint(mockRouteProgress, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(errorMessage, messageSlot.captured.error?.errorMessage)
    }

    @Test
    fun `query building on waypoint arrival with some value`() = coroutineRule.runBlockingTest {
        val queriedFeaturesExpected = mockSuccessQueriedFeature()
        val mockPoint = Point.fromLngLat(-123.4567, 37.8765)
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true)
        val mockAction = BuildingAction.QueryBuildingOnWaypoint(mockRouteProgress)
        val mockResult = BuildingResult.GetDestination(mockPoint)
        coEvery { BuildingProcessor.queryBuildingOnWaypoint(mockAction) } returns mockResult
        val mapboxMap = mockk<MapboxMap> {
            every {
                queryRenderedFeatures(
                    any<RenderedQueryGeometry>(),
                    any<RenderedQueryOptions>(),
                    any(),
                )
            } answers {
                thirdArg<QueryRenderedFeaturesCallback>().run(queriedFeaturesExpected)
                Cancelable { }
            }
        }
        val mockPointAction = BuildingAction.QueryBuilding(mockPoint, mapboxMap)
        val mockPointResult = BuildingResult.QueriedBuildings(
            ExpectedFactory.createValue(
                BuildingValue(queriedFeaturesExpected.value ?: emptyList()),
            ),
        )
        coEvery { BuildingProcessor.queryBuilding(mockPointAction) } returns mockPointResult
        val messageSlot = slot<Expected<BuildingError, BuildingValue>>()
        val buildingsApi = MapboxBuildingsApi(mapboxMap)

        buildingsApi.queryBuildingOnWaypoint(mockRouteProgress, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(queriedFeaturesExpected.value, messageSlot.captured.value?.buildings)
    }

    @Test
    fun `query building on destination arrival with null point`() {
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true)
        val mapboxMap = mockk<MapboxMap>()
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)
        val mockResult = BuildingResult.GetDestination(null)
        val messageSlot = slot<Expected<BuildingError, BuildingValue>>()
        coEvery { BuildingProcessor.queryBuildingOnFinalDestination(mockAction) } returns mockResult
        val errorMessage = "final destination point inside $mockRouteProgress is null"
        val buildingsApi = MapboxBuildingsApi(mapboxMap)

        buildingsApi.queryBuildingOnFinalDestination(mockRouteProgress, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(errorMessage, messageSlot.captured.error?.errorMessage)
    }

    @Test
    fun `query building on destination arrival with some value`() = coroutineRule.runBlockingTest {
        val queriedFeaturesExpected = mockSuccessQueriedFeature()
        val mockPoint = Point.fromLngLat(-123.4567, 37.8765)
        val mockRouteProgress = mockk<RouteProgress>(relaxed = true)
        val mockAction = BuildingAction.QueryBuildingOnFinalDestination(mockRouteProgress)
        val mockResult = BuildingResult.GetDestination(mockPoint)
        coEvery { BuildingProcessor.queryBuildingOnFinalDestination(mockAction) } returns mockResult
        val mapboxMap = mockk<MapboxMap> {
            every {
                queryRenderedFeatures(
                    any<RenderedQueryGeometry>(),
                    any<RenderedQueryOptions>(),
                    any(),
                )
            } answers {
                thirdArg<QueryRenderedFeaturesCallback>().run(queriedFeaturesExpected)
                Cancelable {}
            }
        }
        val mockPointAction = BuildingAction.QueryBuilding(mockPoint, mapboxMap)
        val mockPointResult = BuildingResult.QueriedBuildings(
            ExpectedFactory.createValue(
                BuildingValue(queriedFeaturesExpected.value ?: emptyList()),
            ),
        )
        coEvery { BuildingProcessor.queryBuilding(mockPointAction) } returns mockPointResult
        val messageSlot = slot<Expected<BuildingError, BuildingValue>>()
        val buildingsApi = MapboxBuildingsApi(mapboxMap)

        buildingsApi.queryBuildingOnFinalDestination(mockRouteProgress, consumer)

        verify(exactly = 1) { consumer.accept(capture(messageSlot)) }
        assertEquals(queriedFeaturesExpected.value, messageSlot.captured.value?.buildings)
    }

    @Test
    fun cancel() {
        val mockParentJob = mockk<CompletableJob>(relaxed = true)
        val mockJobControl = mockk<JobControl> {
            every { job } returns mockParentJob
        }
        mockkObject(InternalJobControlFactory) {
            every { InternalJobControlFactory.createMainScopeJobControl() } returns mockJobControl

            MapboxBuildingsApi(mockk()).cancel()

            verify { mockParentJob.cancelChildren() }
        }
    }

    private fun mockSuccessQueriedFeature() =
        mockk<Expected<String, List<QueriedRenderedFeature>>> {
            mockk {
                every { value } returns listOf(
                    mockk {
                        every { queriedFeature } returns mockk {
                            every { source } returns "composite"
                            every { sourceLayer } returns "building"
                            every { feature } returns loadFeature()
                        }
                    },
                )
                every { error } returns null
            }
        }

    private fun loadFeature() = Feature.fromJson(
        FileUtils.loadJsonFixture("arrival-highlight-building-feature.json"),
    )
}
