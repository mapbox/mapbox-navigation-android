package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedRenderedFeature
import com.mapbox.maps.QueryRenderedFeaturesCallback
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SinglePointClosestRouteHandlerTest {

    private val layerIds = listOf("layerId1", "layerId2")
    private val clickPoint = ScreenCoordinate(1.0, 2.0)
    private val map = mockk<MapboxMap>(relaxed = true)
    private val features = listOf<FeatureCollection>(
        mockk {
            every { features() } returns listOf(
                mockk {
                    every { id() } returns "id#0"
                },
                mockk {
                    every { id() } returns "id#1"
                },
            )
        },
        mockk {
            every { features() } returns listOf(
                mockk {
                    every { id() } returns "id#1"
                },
                mockk {
                    every { id() } returns "id#2"
                },
            )
        },
    )
    private val sut = SinglePointClosestRouteHandler(layerIds)
    private val index = 1

    @Before
    fun setUp() {
        mockkObject(ClosestRouteUtils)
    }

    @After
    fun tearDown() {
        unmockkObject(ClosestRouteUtils)
    }

    @Test
    fun handle_error() = runTest {
        mockMapAnswer(ExpectedFactory.createError("some error"))
        every {
            ClosestRouteUtils.getIndexOfFirstFeature(any(), any())
        } returns ExpectedFactory.createError(Unit)

        val result = sut.handle(map, clickPoint, features)

        assertTrue(result.isError)
        verify { ClosestRouteUtils.getIndexOfFirstFeature(match { it.isEmpty() }, features) }
    }

    @Test
    fun handle_success_indexIsNotFound() = runTest {
        val queriedFeatures = listOf<QueriedRenderedFeature>(mockk(), mockk())
        mockMapAnswer(ExpectedFactory.createValue(queriedFeatures))
        every {
            ClosestRouteUtils.getIndexOfFirstFeature(any(), any())
        } returns ExpectedFactory.createError(Unit)

        val result = sut.handle(map, clickPoint, features)

        assertTrue(result.isError)
        verify { ClosestRouteUtils.getIndexOfFirstFeature(queriedFeatures, features) }
    }

    @Test
    fun handle_success_indexIsFound() = runTest {
        val queriedFeatures = listOf<QueriedRenderedFeature>(mockk(), mockk())
        mockMapAnswer(ExpectedFactory.createValue(queriedFeatures))
        every {
            ClosestRouteUtils.getIndexOfFirstFeature(queriedFeatures, features)
        } returns ExpectedFactory.createValue(index)

        val result = sut.handle(map, clickPoint, features)

        assertEquals(index, result.value!!)
    }

    private fun mockMapAnswer(result: Expected<String, List<QueriedRenderedFeature>>) {
        every {
            map.queryRenderedFeatures(
                match { it.isScreenCoordinate && it.screenCoordinate == clickPoint },
                match<RenderedQueryOptions> { it.layerIds == layerIds && it.filter == null },
                any(),
            )
        } answers {
            thirdArg<QueryRenderedFeaturesCallback>().run(result)
            Cancelable {}
        }
    }
}
