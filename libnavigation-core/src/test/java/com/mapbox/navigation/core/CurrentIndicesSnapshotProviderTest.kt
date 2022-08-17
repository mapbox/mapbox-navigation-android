package com.mapbox.navigation.core

import com.mapbox.navigation.base.internal.CurrentIndicesSnapshot
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
class CurrentIndicesSnapshotProviderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val provider = CurrentIndicesSnapshotProvider()
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
    private val expected = CurrentIndicesSnapshot(
        currentLegIndex,
        routeGeometryIndex,
        legGeometryIndex
    )

    @Test
    fun initialStateIsNull() {
        assertNull(provider())
    }

    @Test
    fun stateIsNullAfterClear() {
        provider.onRouteProgressChanged(routeProgress)

        provider.clear()

        assertNull(provider())
    }

    @Test
    fun stateAfterUpdate() = runBlocking {
        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider())
        assertEquals(expected, provider.getFilledIndicesAndFreeze())
    }

    @Test
    fun updateDuringRetrieval() = runBlocking {
        launch {
            delay(100)
            provider.onRouteProgressChanged(routeProgress)
        }

        assertEquals(expected, provider.getFilledIndicesAndFreeze())
        assertEquals(expected, provider())
    }

    @Test
    fun updateDuringRetrieval_async() = coroutineRule.runBlockingTest {
        pauseDispatcher {
            launch {
                delay(100)
                provider.onRouteProgressChanged(routeProgress)
            }
            var value: CurrentIndicesSnapshot? = null
            val update = launch {
                value = provider.getFilledIndicesAndFreeze()
                throw AssertionError()
            }
            advanceTimeBy(50)
            update.cancel()
            advanceTimeBy(50)
            assertNull(value)
            assertEquals(expected, provider())
        }
    }

    @Test
    fun stateAfterUpdateLegProgressIsNull() = runBlocking {
        val routeIndex = 50
        val routeProgress = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeIndex
            every { currentLegProgress } returns null
        }
        val expected = CurrentIndicesSnapshot(0, routeIndex, null)

        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider())
        assertEquals(expected, provider.getFilledIndicesAndFreeze())
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
        val expected = CurrentIndicesSnapshot(legIndex2, routeGeometryIndex2, legGeometryIndex2)

        provider.onRouteProgressChanged(routeProgress1)
        provider.onRouteProgressChanged(routeProgress2)

        assertEquals(expected, provider())
        assertEquals(expected, provider.getFilledIndicesAndFreeze())
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
        val expected = CurrentIndicesSnapshot(legIndex1, routeGeometryIndex1, legGeometryIndex1)
        provider.onRouteProgressChanged(routeProgress1)

        launch {
            delay(500)
            provider.onRouteProgressChanged(routeProgress2)
        }

        assertEquals(expected, provider.getFilledIndicesAndFreeze())
        assertEquals(expected, provider())
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
        val expected = CurrentIndicesSnapshot(legIndex2, routeGeometryIndex2, legGeometryIndex2)
        provider.onRouteProgressChanged(routeProgress1)

        provider.clear()
        launch {
            delay(100)
            provider.onRouteProgressChanged(routeProgress2)
        }

        assertEquals(expected, provider.getFilledIndicesAndFreeze())
        assertEquals(expected, provider())
    }

    @Test
    fun getFilledIndicesAndFreezeForbidsUpdates() = runBlocking {
        provider.onRouteProgressChanged(routeProgress)
        val expected = provider.getFilledIndicesAndFreeze()

        provider.onRouteProgressChanged(
            mockk {
                every { currentRouteGeometryIndex } returns routeGeometryIndex
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 89
                    every { geometryIndex } returns 12
                }
            }
        )

        assertEquals(expected, provider())
        assertEquals(expected, provider.getFilledIndicesAndFreeze())
    }

    @Test
    fun invokeDoesNotForbidUpdates() = runBlocking {
        provider()
        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider())
        assertEquals(expected, provider.getFilledIndicesAndFreeze())
    }

    @Test
    fun unfreezeAllowsUpdates() = runBlocking {
        provider.onRouteProgressChanged(
            mockk {
                every { currentRouteGeometryIndex } returns routeGeometryIndex
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 89
                    every { geometryIndex } returns 12
                }
            }
        )
        provider.getFilledIndicesAndFreeze()

        provider.unfreeze()
        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider())
        assertEquals(expected, provider.getFilledIndicesAndFreeze())
    }

    @Test
    fun cancelDuringWaitAllowsUpdates_async() = coroutineRule.runBlockingTest {
        pauseDispatcher {
            val freeze = launch {
                provider.getFilledIndicesAndFreeze()
            }
            delay(50)

            freeze.cancel()
            provider.onRouteProgressChanged(routeProgress)

            assertEquals(expected, provider())
            assertEquals(expected, provider.getFilledIndicesAndFreeze())
        }
    }
}
