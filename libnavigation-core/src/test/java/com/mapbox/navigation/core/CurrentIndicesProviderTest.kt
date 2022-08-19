package com.mapbox.navigation.core

import com.mapbox.navigation.base.internal.CurrentIndices
import com.mapbox.navigation.base.internal.CurrentIndicesFactory
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentIndicesProviderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val provider = CurrentIndicesProvider()
    private val currentLegIndex = 9
    private val routeGeometryIndex = 44
    private val legGeometryIndex = 33
    private val routeProgress = mockk<RouteProgress> {
        every { currentRouteGeometryIndex } returns routeGeometryIndex
        every { currentLegProgress } returns mockk {
            every { legIndex } returns currentLegIndex
            every { geometryIndex } returns legGeometryIndex
        }
    }
    private val expected = CurrentIndicesFactory.createIndices(
        currentLegIndex,
        routeGeometryIndex,
        legGeometryIndex
    )

    @Test
    fun stateAfterUpdate() = runBlocking {
        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider.getFilledIndicesOrWait())
    }

    @Test
    fun updateDuringRetrieval() = runBlocking {
        launch {
            delay(100)
            provider.onRouteProgressChanged(routeProgress)
        }

        assertEquals(expected, provider.getFilledIndicesOrWait())
    }

    @Test
    fun updateDuringRetrieval_async() = coroutineRule.runBlockingTest {
        pauseDispatcher {
            launch {
                delay(100)
                provider.onRouteProgressChanged(routeProgress)
            }
            var value: CurrentIndices? = null
            val update = launch {
                value = provider.getFilledIndicesOrWait()
                throw AssertionError()
            }
            advanceTimeBy(50)
            update.cancel()
            advanceTimeBy(50)
            assertNull(value)
            assertEquals(expected, provider.getFilledIndicesOrWait())
        }
    }

    @Test
    fun stateAfterUpdateLegProgressIsNull() = runBlocking {
        val routeIndex = 50
        val routeProgress = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeIndex
            every { currentLegProgress } returns null
        }
        val expected = CurrentIndicesFactory.createIndices(0, routeIndex, null)

        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider.getFilledIndicesOrWait())
    }

    @Test
    fun stateAfterUpdateTwice() = runBlocking {
        val legIndex1 = 5
        val routeGeometryIndex1 = 78
        val legGeometryIndex1 = 61
        val routeProgress1 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeGeometryIndex1
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndex1
                every { geometryIndex } returns legGeometryIndex1
            }
        }
        val legIndex2 = 9
        val routeGeometryIndex2 = 44
        val legGeometryIndex2 = 33
        val routeProgress2 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeGeometryIndex2
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndex2
                every { geometryIndex } returns legGeometryIndex2
            }
        }
        val expected = CurrentIndicesFactory.createIndices(
            legIndex2,
            routeGeometryIndex2,
            legGeometryIndex2
        )

        provider.onRouteProgressChanged(routeProgress1)
        provider.onRouteProgressChanged(routeProgress2)

        assertEquals(expected, provider.getFilledIndicesOrWait())
    }

    @Test
    fun doesNotWaitForUpdateIfAlreadyHasValue() = runBlocking {
        val legIndex1 = 5
        val routeGeometryIndex1 = 78
        val legGeometryIndex1 = 61
        val routeProgress1 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeGeometryIndex1
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndex1
                every { geometryIndex } returns legGeometryIndex1
            }
        }
        val legIndex2 = 9
        val routeGeometryIndex2 = 44
        val legGeometryIndex2 = 33
        val routeProgress2 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeGeometryIndex2
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndex2
                every { geometryIndex } returns legGeometryIndex2
            }
        }
        val expected = CurrentIndicesFactory.createIndices(
            legIndex1,
            routeGeometryIndex1,
            legGeometryIndex1
        )
        provider.onRouteProgressChanged(routeProgress1)

        launch {
            delay(500)
            provider.onRouteProgressChanged(routeProgress2)
        }

        assertEquals(expected, provider.getFilledIndicesOrWait())
    }

    @Test
    fun waitsForUpdateIfValueIsCleared() = runBlocking {
        val legIndex1 = 5
        val routeGeometryIndex1 = 78
        val legGeometryIndex1 = 61
        val routeProgress1 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeGeometryIndex1
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndex1
                every { geometryIndex } returns legGeometryIndex1
            }
        }
        val legIndex2 = 9
        val routeGeometryIndex2 = 44
        val legGeometryIndex2 = 33
        val routeProgress2 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeGeometryIndex2
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndex2
                every { geometryIndex } returns legGeometryIndex2
            }
        }
        val expected = CurrentIndicesFactory.createIndices(
            legIndex2,
            routeGeometryIndex2,
            legGeometryIndex2
        )
        provider.onRouteProgressChanged(routeProgress1)

        provider.clear()
        launch {
            delay(100)
            provider.onRouteProgressChanged(routeProgress2)
        }

        assertEquals(expected, provider.getFilledIndicesOrWait())
    }
}
