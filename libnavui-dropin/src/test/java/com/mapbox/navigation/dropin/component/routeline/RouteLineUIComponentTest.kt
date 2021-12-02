package com.mapbox.navigation.dropin.component.routeline

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Test

class RouteLineUIComponentTest {

    private val mockStyle: Style by lazy {
        mockk()
    }

    private val mockMap: MapboxMap by lazy {
        mockk {
            every { getStyle() } returns mockStyle
        }
    }

    private val mapView: MapView by lazy {
        mockk(relaxed = true) {
            every { getMapboxMap() } returns mockMap
        }
    }

    private val mockViewModel: RouteLineViewModel by lazy {
        mockk(relaxed = true)
    }

    @Test
    fun onStyleLoaded() {
        MapboxRouteLineUIComponent(
            mapView,
            mockViewModel
        ).onStyleLoaded(mockk())

        verify { mockViewModel.mapStyleUpdated(mockStyle) }
    }

    @Test
    fun onRoutesChanged() {
        val arg = mockk<RoutesUpdatedResult>()

        MapboxRouteLineUIComponent(
            mapView,
            mockViewModel
        ).onRoutesChanged(arg)

        verify { mockViewModel.routesUpdated(arg, mockStyle) }
    }

    @Test
    fun onRouteProgressChanged() {
        val arg = mockk<RouteProgress>()

        MapboxRouteLineUIComponent(
            mapView,
            mockViewModel
        ).onRouteProgressChanged(arg)

        verify { mockViewModel.routeProgressUpdated(arg, mockStyle) }
    }

    @Test
    fun onIndicatorPositionChanged() {
        val point = Point.fromLngLat(-44.0, -33.0)

        MapboxRouteLineUIComponent(
            mapView,
            mockViewModel
        ).onIndicatorPositionChanged(point)

        verify { mockViewModel.positionChanged(point, mockStyle) }
    }

    @Test
    fun onMapClick() {
        val point = Point.fromLngLat(-44.0, -33.0)

        val result = MapboxRouteLineUIComponent(
            mapView,
            mockViewModel
        ).onMapClick(point)

        verify { mockViewModel.mapClick(point, mockMap) }
        assertFalse(result)
    }
}
