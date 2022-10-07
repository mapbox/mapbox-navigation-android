package com.mapbox.navigation.dropin.map

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapboxMapViewBinderTest {

    private val binder = MapboxMapViewBinder()

    @Test
    fun `shouldLoadMapStyle should be true`() {
        assertTrue(binder.shouldLoadMapStyle)
    }
}
