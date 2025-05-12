package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.Value
import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineNoOpExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineProviderBasedExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData

internal class RouteLineValueCommandHolder(
    val provider: RouteLineCommandProvider<Value, RouteLineViewOptionsData>,
    val applier: RouteLineCommandApplier<Value>,
) {

    suspend fun toRouteLineExpressionEventData(
        data: RouteLineViewOptionsData,
    ): RouteLineExpressionEventData {
        return try {
            val value = provider.generateCommand(data)
            val property = applier.getProperty()
            RouteLineProviderBasedExpressionEventData(property, value = value)
        } catch (ex: Throwable) {
            RouteLineNoOpExpressionEventData()
        }
    }
}

internal fun unsupportedRouteLineCommandHolder(): RouteLineValueCommandHolder {
    return RouteLineValueCommandHolder(
        LightRouteLineValueProvider { throw UnsupportedOperationException() },
        object : RouteLineCommandApplier<Value>() {
            override fun applyCommand(style: Style, layerId: String, command: Value) {
                // no-op
            }

            override fun getProperty(): String {
                return ""
            }
        },
    )
}
