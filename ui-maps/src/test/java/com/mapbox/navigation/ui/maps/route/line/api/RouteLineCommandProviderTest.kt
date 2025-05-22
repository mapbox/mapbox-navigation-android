package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.StylePropertyValueKind
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RouteLineCommandProviderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val expression = mockk<Expression>(relaxed = true)
    private val viewData = mockk<RouteLineViewOptionsData>(relaxed = true)

    @Test
    fun applyCommand_LightRouteLineExpressionProvider() = runBlocking {
        val testScope = coroutineRule.createTestScope()
        val deferred = testScope.async(Dispatchers.Main) {
            val mainThreadId = Thread.currentThread().id
            val invocationThreadId = AtomicLong()
            val block: (RouteLineViewOptionsData) -> StylePropertyValue = {
                invocationThreadId.set(Thread.currentThread().id)
                StylePropertyValue(expression, StylePropertyValueKind.EXPRESSION)
            }
            val provider = LightRouteLineExpressionValueProvider(block)

            val resultDeferred = async {
                provider.generateCommand(testScope.coroutineContext, viewData)
            }
            val result = resultDeferred.await()

            assertNotNull(invocationThreadId.get())
            assertEquals(mainThreadId, invocationThreadId.get())
            assertEquals(expression, result.toExpression())
        }

        deferred.await()
        testScope.cancel()
    }

    @Test
    fun applyCommand_HeavyRouteLineExpressionProvider() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Default)
        val deferred = testScope.async(Dispatchers.Main) {
            val mainThreadId = Thread.currentThread().id
            val invocationThreadId = AtomicLong()
            val block: (RouteLineViewOptionsData) -> StylePropertyValue = {
                invocationThreadId.set(Thread.currentThread().id)
                StylePropertyValue(expression, StylePropertyValueKind.EXPRESSION)
            }
            val provider = HeavyRouteLineExpressionValueProvider(block)

            val result = provider.generateCommand(testScope.coroutineContext, viewData)

            assertNotNull(invocationThreadId.get())
            assertNotEquals(mainThreadId, invocationThreadId.get())
            assertEquals(expression, result.toExpression())
        }

        deferred.await()
        testScope.cancel()
    }
}
