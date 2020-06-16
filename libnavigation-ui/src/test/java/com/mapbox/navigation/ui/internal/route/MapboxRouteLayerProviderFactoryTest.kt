package com.mapbox.navigation.ui.internal.route

import com.mapbox.navigation.ui.route.RouteStyleDescriptor
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxRouteLayerProviderFactoryTest {

    @Test
    fun getLayerProvider() {
        val descriptors = listOf<RouteStyleDescriptor>()

        val result = MapboxRouteLayerProviderFactory.getLayerProvider(descriptors)

        assertEquals(descriptors, result.routeStyleDescriptors)
    }
}
