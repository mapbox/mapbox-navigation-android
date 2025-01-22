package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineNoOpExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineProviderBasedExpressionEventData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData

internal class RouteLineExpressionCommandHolder(
    val provider: RouteLineCommandProvider<Expression, RouteLineViewOptionsData>,
    val applier: RouteLineCommandApplier<Expression>,
) {

    suspend fun toRouteLineExpressionEventData(
        data: RouteLineViewOptionsData,
    ): RouteLineExpressionEventData {
        return try {
            val exp = provider.generateCommand(data)
            val property = applier.getProperty()
            RouteLineProviderBasedExpressionEventData(property, exp)
        } catch (ex: Throwable) {
            RouteLineNoOpExpressionEventData()
        }
    }
}

internal fun unsupportedRouteLineCommandHolder(): RouteLineExpressionCommandHolder {
    return RouteLineExpressionCommandHolder(
        LightRouteLineExpressionProvider { throw UnsupportedOperationException() },
        object : RouteLineCommandApplier<Expression>() {
            override fun applyCommand(style: Style, layerId: String, command: Expression) {
                // no-op
            }

            override fun getProperty(): String {
                return ""
            }
        },
    )
}
