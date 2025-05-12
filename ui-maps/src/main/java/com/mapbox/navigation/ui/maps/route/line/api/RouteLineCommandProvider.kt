package com.mapbox.navigation.ui.maps.route.line.api

import androidx.annotation.AnyThread
import com.mapbox.bindgen.Value
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

internal fun interface RouteLineCommandProvider<T, R> {

    suspend fun generateCommand(input: R): T
}

internal abstract class RouteLineValueProvider :
    RouteLineCommandProvider<Value, RouteLineViewOptionsData>

internal class LightRouteLineValueProvider(
    private val anyThreadExpressionGenerator: (RouteLineViewOptionsData) -> Value,
) : RouteLineValueProvider() {

    @AnyThread
    override suspend fun generateCommand(input: RouteLineViewOptionsData): Value {
        return anyThreadExpressionGenerator(input)
    }
}

internal class HeavyRouteLineValueProvider(
    private val calculationScope: CoroutineScope,
    private val workerThreadExpressionGenerator: (RouteLineViewOptionsData) -> Value,
) : RouteLineValueProvider() {

    @AnyThread
    override suspend fun generateCommand(input: RouteLineViewOptionsData): Value {
        return withContext(calculationScope.coroutineContext) {
            workerThreadExpressionGenerator(input)
        }
    }
}
