package com.mapbox.navigation.ui.maps.route.line.api

import android.util.Log
import com.mapbox.maps.StylePropertyValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal fun interface RouteLineCommandProvider<T, R> {

    /**
     * Generates a style expression for lines.
     * Please note that this can be a heavy task and [workerCoroutineContext] might be used to
     * generate the command.
     */
    suspend fun generateCommand(workerCoroutineContext: CoroutineContext, input: R): T
}

internal abstract class RouteLineExpressionValueProvider : RouteLineCommandProvider<StylePropertyValue, RouteLineViewOptionsData>

internal class LightRouteLineExpressionValueProvider(
    private val anyThreadExpressionGenerator: (RouteLineViewOptionsData) -> StylePropertyValue,
) : RouteLineExpressionValueProvider() {

    override suspend fun generateCommand(
        workerCoroutineContext: CoroutineContext,
        input: RouteLineViewOptionsData,
    ): StylePropertyValue {
        return anyThreadExpressionGenerator(input)
    }
}

internal class HeavyRouteLineExpressionValueProvider(
    private val workerThreadExpressionGenerator: (RouteLineViewOptionsData) -> StylePropertyValue,
) : RouteLineExpressionValueProvider() {
    private val TAG = "MbxRouteLineView"
    override suspend fun generateCommand(
        workerCoroutineContext: CoroutineContext,
        input: RouteLineViewOptionsData,
    ): StylePropertyValue {
        return withContext(workerCoroutineContext) {
            Log.d(TAG, "generateCommand() called")
            workerThreadExpressionGenerator(input).also {
                Log.d(TAG, "generateCommand() delaying...")
                delay(1000L)
                Log.d(TAG, "generateCommand() done")
            }
        }
    }
}
