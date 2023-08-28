package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.QueryFeaturesCallback
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RectClosestRouteHandlerTest {

    private val layerIds = listOf("layerId1", "layerId2")
    private val clickPoint = ScreenCoordinate(15.0, 35.0)
    private val padding = 10f
    private val box = ScreenBox(
        ScreenCoordinate(5.0, 25.0),
        ScreenCoordinate(25.0, 45.0),
    )
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
    private val sut = RectClosestRouteHandler(layerIds, padding)
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
    fun handle_error() = runBlockingTest {
        mockMapAnswer(ExpectedFactory.createError("some error"))
        every {
            ClosestRouteUtils.getIndexOfFirstFeature(any(), any())
        } returns ExpectedFactory.createError(Unit)

        val result = sut.handle(map, clickPoint, features)

        assertTrue(result.isError)
        verify { ClosestRouteUtils.getIndexOfFirstFeature(match { it.isEmpty() }, features) }
    }

    @Test
    fun handle_success_indexIsNotFound() = runBlockingTest {
        val queriedFeatures = listOf<QueriedFeature>(mockk(), mockk())
        mockMapAnswer(ExpectedFactory.createValue(queriedFeatures))
        every {
            ClosestRouteUtils.getIndexOfFirstFeature(any(), any())
        } returns ExpectedFactory.createError(Unit)

        val result = sut.handle(map, clickPoint, features)

        assertTrue(result.isError)
        verify { ClosestRouteUtils.getIndexOfFirstFeature(queriedFeatures, features) }
    }

    @Test
    fun handle_success_indexIsFound() = runBlockingTest {
        val queriedFeatures = listOf<QueriedFeature>(mockk(), mockk())
        mockMapAnswer(ExpectedFactory.createValue(queriedFeatures))
        every {
            ClosestRouteUtils.getIndexOfFirstFeature(queriedFeatures, features)
        } returns ExpectedFactory.createValue(index)

        val result = sut.handle(map, clickPoint, features)

        assertEquals(index, result.value!!)
    }

    private fun mockMapAnswer(result: Expected<String, List<QueriedFeature>>) {
        every {
            map.queryRenderedFeatures(
                box,
                match { it.layerIds == layerIds && it.filter == null },
                any()
            )
        } answers {
            thirdArg<QueryFeaturesCallback>().run(result)
        }
    }
}
