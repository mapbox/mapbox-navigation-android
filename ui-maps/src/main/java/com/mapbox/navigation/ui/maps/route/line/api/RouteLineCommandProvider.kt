package com.mapbox.navigation.ui.maps.route.line.api

import androidx.annotation.AnyThread
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

internal fun interface RouteLineCommandProvider<T, R> {

    suspend fun generateCommand(input: R): T
}

internal abstract class RouteLineExpressionProvider :
    RouteLineCommandProvider<Expression, RouteLineViewOptionsData>

internal class LightRouteLineExpressionProvider(
    private val anyThreadExpressionGenerator: (RouteLineViewOptionsData) -> Expression,
) : RouteLineExpressionProvider() {

    @AnyThread
    override suspend fun generateCommand(input: RouteLineViewOptionsData): Expression {
        return anyThreadExpressionGenerator(input)
    }
}

internal class HeavyRouteLineExpressionProvider(
    private val calculationScope: CoroutineScope,
    private val workerThreadExpressionGenerator: (RouteLineViewOptionsData) -> Expression,
) : RouteLineExpressionProvider() {

    @AnyThread
    override suspend fun generateCommand(input: RouteLineViewOptionsData): Expression {
        return withContext(calculationScope.coroutineContext) {
            workerThreadExpressionGenerator(input)
        }
    }
}
