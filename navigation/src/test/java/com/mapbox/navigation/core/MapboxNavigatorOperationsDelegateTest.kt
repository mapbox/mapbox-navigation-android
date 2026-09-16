package com.mapbox.navigation.core

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.route.parsing.models.nn.ContinuousAlternativesParsingSuccessfulResult
import com.mapbox.navigation.base.internal.route.parsing.models.nn.RouteInterfacesParser
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigator.NavigatorOperationsStartActiveGuidanceCallback
import com.mapbox.navigator.NavigatorOperationsStartActiveGuidanceResult
import com.mapbox.navigator.RouteInterface
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxNavigatorOperationsDelegateTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val routesParser = mockk<RouteInterfacesParser>()
    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    private val delegate = MapboxNavigatorOperationsDelegate(mapboxNavigation, routesParser, scope)

    @Test
    fun `startActiveGuidance fails immediately when no routes are provided`() {
        val callback = mockk<NavigatorOperationsStartActiveGuidanceCallback>(relaxed = true)

        delegate.startActiveGuidance(emptyList(), 0, callback)

        val resultSlot = slot<Expected<String, NavigatorOperationsStartActiveGuidanceResult>>()
        verify { callback.run(capture(resultSlot)) }
        assertTrue(resultSlot.captured.isError)
        coVerify(exactly = 0) { routesParser.parseRoutes(any()) }
    }

    @Test
    fun `startActiveGuidance parses routes without going through the skippable alternatives path`() {
        val nativeRoutes = listOf(mockk<RouteInterface>(), mockk<RouteInterface>())
        val parsedRoutes = listOf(
            mockk<NavigationRoute> { every { id } returns "route#0" },
            mockk<NavigationRoute> { every { id } returns "route#1" },
        )
        coEvery { routesParser.parseRoutes(nativeRoutes) } returns
            Result.success(ContinuousAlternativesParsingSuccessfulResult(parsedRoutes))
        every { mapboxNavigation.setNavigationRoutes(parsedRoutes, 2, any()) } answers {
            thirdArg<RoutesSetCallback>().onRoutesSet(
                ExpectedFactory.createValue(RoutesSetSuccess(emptyMap())),
            )
        }
        every { mapboxNavigation.getNavigationRoutes() } returns parsedRoutes
        val callback = mockk<NavigatorOperationsStartActiveGuidanceCallback>(relaxed = true)

        delegate.startActiveGuidance(nativeRoutes, 2, callback)

        val resultSlot = slot<Expected<String, NavigatorOperationsStartActiveGuidanceResult>>()
        verify { callback.run(capture(resultSlot)) }
        assertTrue(resultSlot.captured.isValue)
        assertEquals(listOf("route#0", "route#1"), resultSlot.captured.value!!.routeIds)
        coVerify(exactly = 0) { routesParser.parserContinuousAlternatives(any()) }
    }

    @Test
    fun `startActiveGuidance fails when route parsing fails`() {
        val nativeRoutes = listOf(mockk<RouteInterface>())
        coEvery { routesParser.parseRoutes(nativeRoutes) } returns
            Result.failure(IllegalStateException("bad json"))
        val callback = mockk<NavigatorOperationsStartActiveGuidanceCallback>(relaxed = true)

        delegate.startActiveGuidance(nativeRoutes, 0, callback)

        val resultSlot = slot<Expected<String, NavigatorOperationsStartActiveGuidanceResult>>()
        verify { callback.run(capture(resultSlot)) }
        assertTrue(resultSlot.captured.isError)
        verify(exactly = 0) { mapboxNavigation.setNavigationRoutes(any(), any(), any()) }
    }
}
