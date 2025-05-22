package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.Style
import com.mapbox.maps.StylePropertyValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineNoOpExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineProviderBasedExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import kotlin.coroutines.CoroutineContext

internal class RouteLineValueCommandHolder(
    val provider: RouteLineCommandProvider<StylePropertyValue, RouteLineViewOptionsData>,
    val applier: RouteLineCommandApplier<StylePropertyValue>,
) {

    suspend fun toRouteLineExpressionEventData(
        workerCoroutineContext: CoroutineContext,
        data: RouteLineViewOptionsData,
    ): RouteLineExpressionEventData {
        return try {
            val value = provider.generateCommand(workerCoroutineContext, data)
            val property = applier.getProperty()
            RouteLineProviderBasedExpressionEventData(property, value = value)
        } catch (ex: Throwable) {
            RouteLineNoOpExpressionEventData()
        }
    }
}

internal fun unsupportedRouteLineCommandHolder(): RouteLineValueCommandHolder {
    return RouteLineValueCommandHolder(
        LightRouteLineExpressionValueProvider { throw UnsupportedOperationException() },
        object : RouteLineCommandApplier<StylePropertyValue>() {
            override fun applyCommand(style: Style, layerId: String, command: StylePropertyValue) {
                // no-op
            }

            override fun getProperty(): String {
                return ""
            }
        },
    )
}
