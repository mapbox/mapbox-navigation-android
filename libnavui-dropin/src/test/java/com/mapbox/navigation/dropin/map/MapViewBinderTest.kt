package com.mapbox.navigation.dropin.map

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.compass.CompassPlugin
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapViewBinderTest {

    private val mapViewOwner = mockk<MapViewOwner>(relaxed = true)
    private val context = mockk<NavigationViewContext>(relaxed = true) {
        every { mapViewOwner } returns this@MapViewBinderTest.mapViewOwner
        every { styles } returns mockk(relaxed = true) {
            every { mapScalebarParams } returns MutableStateFlow(mockk())
        }
    }
    private val navigationViewBinding: MapboxNavigationViewLayoutBinding =
        MapboxNavigationViewLayoutBinding.inflate(
            LayoutInflater.from(ApplicationProvider.getApplicationContext()),
            FrameLayout(ApplicationProvider.getApplicationContext())
        )
    private val viewGroup = FrameLayout(ApplicationProvider.getApplicationContext())
    private val compassMock = mockk<CompassPlugin>(relaxed = true)
    private val map = mockk<MapboxMap>()
    private val mapView = mockk<MapView>(relaxed = true) {
        every { compass } returns compassMock
        every { scalebar } returns mockk(relaxed = true)
        every { camera } returns mockk(relaxed = true)
        every { parent } returns null
        every { getMapboxMap() } returns map
    }

    private val sut = object : MapViewBinder() {
        override fun getMapView(viewGroup: ViewGroup): MapView = mapView
    }

    @Before
    fun setUp() {
        sut.context = context
        sut.navigationViewBinding = navigationViewBinding
    }

    @Test
    fun `bind adds mapView`() {
        sut.bind(viewGroup)
        assertEquals(1, viewGroup.childCount)
        assertTrue(viewGroup.getChildAt(0) === mapView)
    }

    @Test
    fun `bind notifies listeners`() {
        val listener = mockk<MapboxMapObserver>(relaxed = true)
        sut.registerMapboxMapObserver(listener)
        sut.bind(viewGroup)
        verify { listener.onMapboxMapReady(map) }
    }

    @Test
    fun `bind updates mapView`() {
        sut.bind(viewGroup)
        verify { mapViewOwner.updateMapView(mapView) }
    }

    @Test
    fun `getMapLoadStylePolicy returns NEVER`() {
        assertEquals(MapStyleLoadPolicy.NEVER, sut.getMapStyleLoadPolicy())
    }
}
