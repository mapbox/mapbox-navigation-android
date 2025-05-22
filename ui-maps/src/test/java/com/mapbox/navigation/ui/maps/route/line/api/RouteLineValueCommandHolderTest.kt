package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.Style
import com.mapbox.maps.StylePropertyValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineNoOpExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineProviderBasedExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal class RouteLineValueCommandHolderTest {

    @Test
    fun toRouteLineExpressionEventData_ok() = runBlocking {
        val exp = mockk<StylePropertyValue>(relaxed = true)
        val viewData = mockk<RouteLineViewOptionsData>(relaxed = true)
        val provider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery { generateCommand(coroutineContext, viewData) } returns exp
        }
        val applier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true) {
            every { getProperty() } returns "some-property"
        }
        val data = RouteLineValueCommandHolder(provider, applier)

        val actual = data.toRouteLineExpressionEventData(coroutineContext, viewData)
        assertTrue(actual is RouteLineProviderBasedExpressionEventData)
        assertEquals(exp, (actual as RouteLineProviderBasedExpressionEventData).value)
        assertEquals("some-property", actual.property)
    }

    @Test
    fun toRouteLineExpressionEventData_throws() = runBlocking {
        val viewData = mockk<RouteLineViewOptionsData>(relaxed = true)
        val provider = mockk<RouteLineValueProvider>(relaxed = true) {
            coEvery { generateCommand(coroutineContext, viewData) } throws UnsupportedOperationException()
        }
        val applier = mockk<RouteLineCommandApplier<StylePropertyValue>>(relaxed = true) {
            every { getProperty() } returns "some-property"
        }
        val data = RouteLineValueCommandHolder(provider, applier)

        val actual = data.toRouteLineExpressionEventData(coroutineContext,  viewData)
        assertTrue(actual is RouteLineNoOpExpressionEventData)
    }

    @Test
    fun unsupportedRouteLineCommandHolderTest() = runBlocking {
        val actual = unsupportedRouteLineCommandHolder()
        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking {
                actual.provider.generateCommand(coroutineContext, mockk())
            }
        }
        val style = mockk<Style>()
        actual.applier.applyCommand(style, "some-layer-id", mockk())
        verify(exactly = 0) { style.setStyleLayerProperty(any(), any(), any()) }
    }
}
