package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.navigation.testing.FieldsAreDoubledTest
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue

internal class RouteLineViewRenderRouteLineUpdateDataValueTest :
    FieldsAreDoubledTest<RouteLineViewRenderRouteLineUpdateDataValue, RouteLineUpdateValue>() {

    override val fieldsTypesMap: Map<Class<*>, Class<*>> = mapOf(
        RouteLineDynamicData::class.java to RouteLineDynamicEventData::class.java,
    )

    override fun getDoublerClass(): Class<*> {
        return RouteLineViewRenderRouteLineUpdateDataValue::class.java
    }

    override fun getOriginalClass(): Class<*> {
        return RouteLineUpdateValue::class.java
    }
}
