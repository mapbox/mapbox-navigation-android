package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.navigation.testing.FieldsAreDoubledTest
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData

internal class RouteLineViewRenderRouteLineClearDataValueTest :
    FieldsAreDoubledTest<RouteLineViewRenderRouteLineClearDataValue, RouteLineClearValue>() {

    override val fieldsTypesMap: Map<Class<*>, Class<*>> = mapOf(
        RouteLineDynamicData::class.java to RouteLineDynamicEventData::class.java,
    )

    override fun getDoublerClass(): Class<*> {
        return RouteLineViewRenderRouteLineClearDataValue::class.java
    }

    override fun getOriginalClass(): Class<*> {
        return RouteLineClearValue::class.java
    }
}
