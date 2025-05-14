package com.mapbox.navigation.ui.maps.route.line.api

import androidx.annotation.AnyThread
import com.mapbox.bindgen.Value
import com.mapbox.maps.StylePropertyValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

internal fun interface RouteLineCommandProvider<T, R> {

    suspend fun generateCommand(input: R): T
}

internal abstract class RouteLineValueProvider :
    RouteLineCommandProvider<StylePropertyValue, RouteLineViewOptionsData>

internal class LightRouteLineValueProvider(
    private val anyThreadExpressionGenerator: (RouteLineViewOptionsData) -> StylePropertyValue,
) : RouteLineValueProvider() {

    @AnyThread
    override suspend fun generateCommand(input: RouteLineViewOptionsData): StylePropertyValue {
        return anyThreadExpressionGenerator(input)
    }
}

internal class HeavyRouteLineValueProvider(
    private val calculationScope: CoroutineScope,
    private val workerThreadExpressionGenerator: (RouteLineViewOptionsData) -> StylePropertyValue,
) : RouteLineValueProvider() {

    @AnyThread
    override suspend fun generateCommand(input: RouteLineViewOptionsData): StylePropertyValue {
        return withContext(calculationScope.coroutineContext) {
            workerThreadExpressionGenerator(input)
        }
    }
}
