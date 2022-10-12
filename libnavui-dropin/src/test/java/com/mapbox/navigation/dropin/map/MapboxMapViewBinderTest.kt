package com.mapbox.navigation.dropin.map

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowMapView::class])
class MapboxMapViewBinderTest {

    private val binder = MapboxMapViewBinder()

    @Test
    fun `shouldLoadMapStyle should be true`() {
        assertTrue(binder.shouldLoadMapStyle)
    }

    @Test
    fun getMapView() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mapView = binder.getMapView(context)

        assertNotNull(mapView)
        verify { mapView.compass.enabled = false }
    }
}
