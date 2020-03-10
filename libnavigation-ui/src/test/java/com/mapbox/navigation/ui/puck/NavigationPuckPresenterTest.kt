package com.mapbox.navigation.ui.puck

import com.mapbox.libnavigation.ui.R
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationPuckPresenterTest {

    @Test
    fun puckDrawableUpdatesOnRouteProgress() {
        val mapboxMap = mockk<MapboxMap>(relaxUnitFun = true)
        val mapboxNavigation = mockk< MapboxNavigation>(relaxUnitFun = true)
        val puckDrawableSupplier = DefaultMapboxPuckDrawableSupplier()
        val presenter = NavigationPuckPresenter(mapboxMap, puckDrawableSupplier)
        val routeProgressObserverSlot = slot<RouteProgressObserver>()
        val routeProgress = mockk<RouteProgress>()
        val locationComponentOptions = mockk<LocationComponentOptions>(relaxUnitFun = true)
        val locationComponent = mockk<LocationComponent>(relaxUnitFun = true)
        val builder = mockk<LocationComponentOptions.Builder>()
        val drawableSlot = slot<Int>()
        every { mapboxMap.locationComponent } returns locationComponent
        every { mapboxMap.padding } returns intArrayOf()
        every { locationComponent.locationComponentOptions } returns locationComponentOptions
        every { routeProgress.currentState() } returns RouteProgressState.LOCATION_TRACKING
        every { locationComponentOptions.gpsDrawable() } returns 0
        every { locationComponentOptions.toBuilder() } returns builder
        every { builder.gpsDrawable(capture(drawableSlot)) } returns builder
        every { builder.padding(any()) } returns builder
        every { builder.build() } returns locationComponentOptions
        presenter.addProgressChangeListener(mapboxNavigation)
        verify { mapboxNavigation.registerRouteProgressObserver(capture(routeProgressObserverSlot)) }

        routeProgressObserverSlot.captured.onRouteProgressChanged(routeProgress)

        verify { locationComponent.applyStyle(locationComponentOptions) }
        assertEquals(R.drawable.user_puck_icon, drawableSlot.captured)
    }

    @Test
    fun onStop() {
        val mapboxMap = mockk<MapboxMap>(relaxUnitFun = true)
        val mapboxNavigation = mockk< MapboxNavigation>(relaxUnitFun = true)
        val puckDrawableSupplier = DefaultMapboxPuckDrawableSupplier()
        val presenter = NavigationPuckPresenter(mapboxMap, puckDrawableSupplier)
        presenter.addProgressChangeListener(mapboxNavigation)

        presenter.onStop()

        verify(exactly = 1) { mapboxNavigation.unregisterRouteProgressObserver(any()) }
    }

    @Test
    fun onStart() {
        val mapboxMap = mockk<MapboxMap>(relaxUnitFun = true)
        val mapboxNavigation = mockk< MapboxNavigation>(relaxUnitFun = true)
        val puckDrawableSupplier = DefaultMapboxPuckDrawableSupplier()
        val presenter = NavigationPuckPresenter(mapboxMap, puckDrawableSupplier)
        presenter.addProgressChangeListener(mapboxNavigation)
        presenter.onStop()

        presenter.onStart()

        verify(exactly = 2) { mapboxNavigation.registerRouteProgressObserver(any()) }
    }
}
