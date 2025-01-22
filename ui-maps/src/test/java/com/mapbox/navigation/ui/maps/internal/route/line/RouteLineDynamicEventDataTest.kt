package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.navigation.testing.FieldsAreDoubledTest
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineExpressionCommandHolder
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData

internal class RouteLineDynamicEventDataTest :
    FieldsAreDoubledTest<RouteLineDynamicEventData, RouteLineDynamicData>() {

    override val fieldsTypesMap: Map<Class<*>, Class<*>> = mapOf(
        RouteLineExpressionCommandHolder::class.java to RouteLineExpressionEventData::class.java,
    )

    override val fieldsNamesMap: Map<String, String> = mapOf(
        "baseExpressionCommandHolder" to "baseExpressionData",
        "casingExpressionCommandHolder" to "casingExpressionData",
        "trafficExpressionCommandHolder" to "trafficExpressionData",
        "restrictedSectionExpressionCommandHolder" to "restrictedSectionExpressionData",
        "trailExpressionCommandHolder" to "trailExpressionData",
        "trailCasingExpressionCommandHolder" to "trailCasingExpressionData",
        "blurExpressionCommandHolder" to "blurExpressionCommandData",
    )

    override fun getDoublerClass(): Class<*> {
        return RouteLineDynamicEventData::class.java
    }

    override fun getOriginalClass(): Class<*> {
        return RouteLineDynamicData::class.java
    }
}
