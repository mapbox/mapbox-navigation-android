package com.mapbox.navigation.ui.maps.route.line.api

import androidx.annotation.AnyThread
import com.mapbox.maps.StylePropertyValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal fun interface RouteLineCommandProvider<T, R> {

    /**
     * Generates a style expression for lines.
     * Please note that this can be a heavy task and [workerCoroutineContext] might be used to
     * generate the command.
     */
    @AnyThread
    suspend fun generateCommand(workerCoroutineContext: CoroutineContext, input: R): T
}

internal abstract class RouteLineValueProvider :
    RouteLineCommandProvider<StylePropertyValue, RouteLineViewOptionsData>

/**
 * Version of [RouteLineValueProvider] that does not use a worker context to generate the expression.
 */
internal class LightRouteLineValueProvider(
    private val anyThreadExpressionGenerator: (RouteLineViewOptionsData) -> StylePropertyValue,
) : RouteLineValueProvider() {

    @AnyThread
    override suspend fun generateCommand(
        workerCoroutineContext: CoroutineContext,
        input: RouteLineViewOptionsData,
    ): StylePropertyValue {
        return anyThreadExpressionGenerator(input)
    }
}

/**
 * Version of [RouteLineValueProvider] that uses a worker context to generate the expression.
 */
internal class HeavyRouteLineValueProvider(
    private val workerThreadExpressionGenerator: (RouteLineViewOptionsData) -> StylePropertyValue,
) : RouteLineValueProvider() {

    @AnyThread
    override suspend fun generateCommand(
        workerCoroutineContext: CoroutineContext,
        input: RouteLineViewOptionsData,
    ): StylePropertyValue {
        return withContext(workerCoroutineContext) {
            workerThreadExpressionGenerator(input)
        }
    }
}
