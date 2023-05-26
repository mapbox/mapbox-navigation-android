package com.mapbox.navigation.core

import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.internal.factory.RouteIndicesFactory
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.internal.RoutesProgressData
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
class RoutesProgressDataProviderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val provider = RoutesProgressDataProvider()
    private val currentLegIndex = 9
    private val routeGeometryIndex = 44
    private val legGeometryIndex = 33
    private val altLegIndex1 = 3
    private val altRouteGeometryIndex1 = 67
    private val altLegGeometryIndex1 = 36
    private val altLegIndex2 = 7
    private val altRouteGeometryIndex2 = 59
    private val altLegGeometryIndex2 = 28
    private val altId1 = "altid#1"
    private val altId2 = "altid#2"
    private val routeProgress = mockk<RouteProgress> {
        every { currentRouteGeometryIndex } returns routeGeometryIndex
        every { currentLegProgress } returns mockk {
            every { legIndex } returns currentLegIndex
            every { geometryIndex } returns legGeometryIndex
        }
        every { internalAlternativeRouteIndices() } returns mapOf(
            altId1 to RouteIndicesFactory.buildRouteIndices(
                altLegIndex1,
                0,
                altRouteGeometryIndex1,
                altLegGeometryIndex1,
                1
            ),
            altId2 to RouteIndicesFactory.buildRouteIndices(
                altLegIndex2,
                1,
                altRouteGeometryIndex2,
                altLegGeometryIndex2,
                2
            ),
        )
    }
    private val expected = RoutesProgressData(
        RouteProgressData(
            currentLegIndex,
            routeGeometryIndex,
            legGeometryIndex
        ),
        mapOf(
            altId1 to RouteProgressData(altLegIndex1, altRouteGeometryIndex1, altLegGeometryIndex1),
            altId2 to RouteProgressData(altLegIndex2, altRouteGeometryIndex2, altLegGeometryIndex2),
        )
    )

    @Test
    fun stateAfterUpdate() = runBlocking {
        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider.getRouteRefreshRequestDataOrWait())
    }

    @Test
    fun updateDuringRetrieval() = runBlocking {
        launch {
            delay(100)
            provider.onRouteProgressChanged(routeProgress)
        }

        assertEquals(expected, provider.getRouteRefreshRequestDataOrWait())
    }

    @Test
    fun updateDuringRetrieval_async() = coroutineRule.runBlockingTest {
        pauseDispatcher {
            launch {
                delay(100)
                provider.onRouteProgressChanged(routeProgress)
            }
            var value: RoutesProgressData? = null
            val update = launch {
                value = provider.getRouteRefreshRequestDataOrWait()
                throw AssertionError()
            }
            advanceTimeBy(50)
            update.cancel()
            advanceTimeBy(50)
            assertNull(value)
            assertEquals(expected, provider.getRouteRefreshRequestDataOrWait())
        }
    }

    @Test
    fun stateAfterUpdateLegProgressIsNull() = runBlocking {
        val routeIndex = 50
        val routeProgress = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeIndex
            every { currentLegProgress } returns null
            every { internalAlternativeRouteIndices() } returns emptyMap()
        }
        val expected = RoutesProgressData(
            RouteProgressData(0, routeIndex, null),
            emptyMap()
        )

        provider.onRouteProgressChanged(routeProgress)

        assertEquals(expected, provider.getRouteRefreshRequestDataOrWait())
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
            every { internalAlternativeRouteIndices() } returns emptyMap()
        }
        val legIndex2 = 9
        val routeGeometryIndex2 = 44
        val legGeometryIndex2 = 33
        val routeProgress2 = mockk<RouteProgress> {
            every { currentRouteGeometryIndex } returns routeGeometryIndex2
            every { currentLegProgress } returns mockk {
                every { legIndex } returns legIndex2
                every { geometryIndex } returns legGeometryIndex2
                every { internalAlternativeRouteIndices() } returns emptyMap()
            }
        }
        val expected = RoutesProgressData(
            RouteProgressData(
                legIndex2,
                routeGeometryIndex2,
                legGeometryIndex2
            ),
            emptyMap()
        )

        provider.onRouteProgressChanged(routeProgress1)
        provider.onRouteProgressChanged(routeProgress2)

        assertEquals(expected, provider.getRouteRefreshRequestDataOrWait())
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
                every { internalAlternativeRouteIndices() } returns emptyMap()
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
                every { internalAlternativeRouteIndices() } returns emptyMap()
            }
        }
        val expected = RoutesProgressData(
            RouteProgressData(
                legIndex1,
                routeGeometryIndex1,
                legGeometryIndex1
            ),
            emptyMap()
        )
        provider.onRouteProgressChanged(routeProgress1)

        launch {
            delay(500)
            provider.onRouteProgressChanged(routeProgress2)
        }

        assertEquals(expected, provider.getRouteRefreshRequestDataOrWait())
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
                every { internalAlternativeRouteIndices() } returns emptyMap()
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
                every { internalAlternativeRouteIndices() } returns emptyMap()
            }
        }
        val expected = RoutesProgressData(
            RouteProgressData(
                legIndex2,
                routeGeometryIndex2,
                legGeometryIndex2
            ),
            emptyMap()
        )
        provider.onRouteProgressChanged(routeProgress1)

        provider.onNewRoutes()
        launch {
            delay(100)
            provider.onRouteProgressChanged(routeProgress2)
        }

        assertEquals(expected, provider.getRouteRefreshRequestDataOrWait())
    }
}
