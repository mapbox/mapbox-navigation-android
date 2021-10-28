package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

class RouteLineClearValueTest {

    @Test
    fun toMutableValue() {
        val original = RouteLineClearValue(
            mockk(),
            listOf(),
            mockk()
        )

        val result = original.toMutableValue()

        assertEquals(original.primaryRouteSource, result.primaryRouteSource)
        assertEquals(original.alternativeRouteSourceSources, result.alternativeRouteSourceSources)
        assertEquals(original.waypointsSource, result.waypointsSource)
    }

    @Test
    fun toImmutableValue() {
        val original = RouteLineClearValue(
            mockk(),
            listOf(),
            mockk()
        )
        val replacementPrimary = mockk<FeatureCollection>()
        val replacementList = listOf<FeatureCollection>()
        val replacementWaypointSource = mockk<FeatureCollection>()
        val mutable = original.toMutableValue()

        mutable.primaryRouteSource = replacementPrimary
        mutable.alternativeRouteSourceSources = replacementList
        mutable.waypointsSource = replacementWaypointSource
        val result = mutable.toImmutableValue()

        assertEquals(replacementPrimary, result.primaryRouteSource)
        assertEquals(replacementList, result.alternativeRouteSourceSources)
        assertEquals(replacementWaypointSource, result.waypointsSource)
    }
}
