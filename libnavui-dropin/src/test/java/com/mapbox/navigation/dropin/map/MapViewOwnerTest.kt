package com.mapbox.navigation.dropin.map

import com.mapbox.maps.MapView
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class MapViewOwnerTest {

    @Test
    fun `when map view is null onAttached not called upon calling register`() {
        val sut = MapViewOwner()
        val observer = spyk<TestMapViewObserver>()

        sut.registerObserver(observer)

        verify(exactly = 0) {
            observer.onAttached(any())
        }
    }

    @Test
    fun `when map view exists onAttached is called upon calling register`() {
        val sut = MapViewOwner()
        val mapView = mockk<MapView>()
        val observer = spyk<TestMapViewObserver>()
        sut.updateMapView(mapView)

        sut.registerObserver(observer)

        verify { observer.onAttached(mapView) }
    }

    @Test
    fun `when map view is null onDetached not called upon calling unregister`() {
        val sut = MapViewOwner()
        val observer = spyk<TestMapViewObserver>()

        sut.unregisterObserver(observer)

        verify(exactly = 0) {
            observer.onDetached(any())
        }
    }

    @Test
    fun `when map view exists onDetached is called upon calling unregister`() {
        val sut = MapViewOwner()
        val mapView = mockk<MapView>()
        val observer = spyk<TestMapViewObserver>()
        sut.updateMapView(mapView)

        sut.registerObserver(observer)
        sut.unregisterObserver(observer)

        verify { observer.onDetached(mapView) }
    }

    @Test
    fun `multiple register calls should only call onAttached once`() {
        val sut = MapViewOwner()
        val mapView = mockk<MapView>()
        val observer = spyk<TestMapViewObserver>()
        sut.updateMapView(mapView)

        sut.registerObserver(observer)
        sut.registerObserver(observer)

        verify(exactly = 1) { observer.onAttached(mapView) }
    }

    @Test
    fun `multiple unregister calls should only call onDetached once`() {
        val sut = MapViewOwner()
        val mapView = mockk<MapView>()
        val observer = spyk<TestMapViewObserver>()
        sut.updateMapView(mapView)

        sut.registerObserver(observer)
        sut.unregisterObserver(observer)
        sut.unregisterObserver(observer)

        verify(exactly = 1) { observer.onDetached(mapView) }
    }

    @Test
    fun `when already registered and mapView is updated onAttached is called`() {
        val sut = MapViewOwner()
        val mapView = mockk<MapView>()
        val observer = spyk<TestMapViewObserver>()
        sut.registerObserver(observer)

        sut.updateMapView(mapView)

        verify(exactly = 1) { observer.onAttached(mapView) }
    }

    @Test
    fun `when update old mapView onDetach and onAttach should be called`() {
        val sut = MapViewOwner()
        val mapView1 = mockk<MapView>()
        val mapView2 = mockk<MapView>()
        val observer = spyk<TestMapViewObserver>()
        sut.registerObserver(observer)

        sut.updateMapView(mapView1)
        verify(exactly = 0) { observer.onDetached(any()) }
        verify(exactly = 1) { observer.onAttached(mapView1) }

        sut.updateMapView(mapView2)
        verify(exactly = 1) { observer.onDetached(mapView1) }
        verify(exactly = 1) { observer.onAttached(mapView2) }
    }

    @Test
    fun `flow value is null by default`() {
        val sut = MapViewOwner()
        assertNull(sut.mapViews.value)
    }

    @Test
    fun `flow value is chanhed when mapView is updated`() {
        val sut = MapViewOwner()
        val mapView1 = mockk<MapView>()
        val mapView2 = mockk<MapView>()

        sut.updateMapView(mapView1)

        assertEquals(mapView1, sut.mapViews.value)

        sut.updateMapView(mapView2)

        assertEquals(mapView2, sut.mapViews.value)
    }
}

internal open class TestMapViewObserver : MapViewObserver() {
    private var attachedTo: MapView? = null

    override fun onAttached(mapView: MapView) {
        attachedTo = mapView
    }

    override fun onDetached(mapView: MapView) {
        attachedTo = null
    }
}
