package com.mapbox.navigation.ui.maps.internal.route.line

import android.content.Context
import android.graphics.drawable.Drawable
import com.mapbox.navigation.testing.FieldsAreDoubledTest
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions

internal class RouteLineViewOptionsDataTest :
    FieldsAreDoubledTest<RouteLineViewOptionsData, MapboxRouteLineViewOptions>() {

    override val excludedFields: Set<Pair<String, Class<*>>> = setOf(
        "context" to Context::class.java,
        "originWaypointIcon" to Drawable::class.java,
        "destinationWaypointIcon" to Drawable::class.java,
    )

    override fun getDoublerClass(): Class<*> {
        return RouteLineViewOptionsData::class.java
    }

    override fun getOriginalClass(): Class<*> {
        return MapboxRouteLineViewOptions::class.java
    }
}
