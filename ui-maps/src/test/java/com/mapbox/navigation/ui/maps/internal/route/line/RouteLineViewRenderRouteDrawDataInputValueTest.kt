package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.navigation.testing.FieldsAreDoubledTest
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue

internal class RouteLineViewRenderRouteDrawDataInputValueTest :
    FieldsAreDoubledTest<RouteLineViewRenderRouteDrawDataInputValue, RouteSetValue>() {

    override val fieldsTypesMap: Map<Class<*>, Class<*>> = mapOf(
        RouteLineData::class.java to RouteLineEventData::class.java,
        RouteLineDynamicData::class.java to RouteLineDynamicEventData::class.java,
    )

    override fun getDoublerClass(): Class<*> {
        return RouteLineViewRenderRouteDrawDataInputValue::class.java
    }

    override fun getOriginalClass(): Class<*> {
        return RouteSetValue::class.java
    }
}
