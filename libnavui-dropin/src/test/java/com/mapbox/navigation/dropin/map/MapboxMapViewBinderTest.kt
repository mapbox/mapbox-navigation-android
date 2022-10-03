package com.mapbox.navigation.dropin.map

import android.view.ViewGroup
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.compass.CompassPlugin
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapboxMapViewBinderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mapStyleLoader = mockk<MapStyleLoader>(relaxed = true)
    private val context = mockk<NavigationViewContext>(relaxed = true) {
        every { mapStyleLoader } returns this@MapboxMapViewBinderTest.mapStyleLoader
    }
    private val compassMock = mockk<CompassPlugin>(relaxed = true)
    private val mapView = mockk<MapView>(relaxed = true) {
        every { id } returns R.id.mapView
        every { parent } returns null
        every { compass } returns compassMock
    }
    private val viewGroup = mockk<ViewGroup>(relaxed = true)
    private val binder = MapboxMapViewBinder()

    @Before
    fun setUp() {
        binder.context = context
    }

    @Test
    fun `addMapViewToLayout does not readd views`() {
        binder.addMapViewToLayout(mapView, viewGroup)
        verify(exactly = 0) {
            viewGroup.removeAllViews()
            viewGroup.addView(any())
        }
    }

    @Test
    fun `onMapViewReady disables compass`() {
        binder.onMapViewReady(mapView)
        verify { compassMock.enabled = false }
    }

    @Test
    fun `getMapLoadStylePolicy returns ON_CONFIGURATION_CHANGE`() {
        assertEquals(MapStyleLoadPolicy.ON_CONFIGURATION_CHANGE, binder.getMapStyleLoadPolicy())
    }
}
