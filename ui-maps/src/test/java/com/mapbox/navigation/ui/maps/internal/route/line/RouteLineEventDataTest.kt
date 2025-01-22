package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.navigation.testing.FieldsAreDoubledTest
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData

internal class RouteLineEventDataTest : FieldsAreDoubledTest<RouteLineEventData, RouteLineData>() {

    override val fieldsTypesMap: Map<Class<*>, Class<*>> = mapOf(
        RouteLineDynamicData::class.java to RouteLineDynamicEventData::class.java,
    )

    override fun getDoublerClass(): Class<*> {
        return RouteLineEventData::class.java
    }

    override fun getOriginalClass(): Class<*> {
        return RouteLineData::class.java
    }
}
