package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompositeSinglePointClosestRouteHandlerTest {

    private val map = mockk<MapboxMap>(relaxed = true)
    private val clickPoint = ScreenCoordinate(1.0, 2.0)
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

    @Test
    fun noHandlers() = runBlockingTest {
        val sut = CompositeClosestRouteHandler(emptyList())

        val result = sut.handle(map, clickPoint, features)

        Assert.assertTrue(result.isError)
    }

    @Test
    fun allHandlersReturnFalse() = runBlockingTest {
        val handler1 = mockk<ClosestRouteHandler> {
            coEvery { handle(any(), any(), any()) } returns ExpectedFactory.createError(Unit)
        }
        val handler2 = mockk<ClosestRouteHandler> {
            coEvery { handle(any(), any(), any()) } returns ExpectedFactory.createError(Unit)
        }
        val sut = CompositeClosestRouteHandler(listOf(handler1, handler2))

        val result = sut.handle(map, clickPoint, features)

        Assert.assertTrue(result.isError)
        coVerifyOrder {
            handler1.handle(any(), any(), any())
            handler2.handle(any(), any(), any())
        }
    }

    @Test
    fun firstHandlersReturnTrue() = runBlockingTest {
        val handler1 = mockk<ClosestRouteHandler> {
            coEvery { handle(any(), any(), any()) } returns ExpectedFactory.createValue(10)
        }
        val handler2 = mockk<ClosestRouteHandler> {
            coEvery { handle(any(), any(), any()) } returns ExpectedFactory.createError(Unit)
        }
        val sut = CompositeClosestRouteHandler(listOf(handler1, handler2))

        val result = sut.handle(map, clickPoint, features)

        Assert.assertEquals(10, result.value!!)
        coVerify(exactly = 0) {
            handler2.handle(any(), any(), any())
        }
    }

    @Test
    fun lastHandlersReturnTrue() = runBlockingTest {
        val handler1 = mockk<ClosestRouteHandler> {
            coEvery { handle(any(), any(), any()) } returns ExpectedFactory.createError(Unit)
        }
        val handler2 = mockk<ClosestRouteHandler> {
            coEvery { handle(any(), any(), any()) } returns ExpectedFactory.createValue(10)
        }
        val sut = CompositeClosestRouteHandler(listOf(handler1, handler2))

        val result = sut.handle(map, clickPoint, features)

        Assert.assertEquals(10, result.value!!)
        coVerify(exactly = 1) {
            handler1.handle(any(), any(), any())
        }
    }
}
