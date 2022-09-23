package com.mapbox.navigation.dropin.component.map

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxMapScalebarParams
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScalebarPlaceholderComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val scalebarPlaceholderView = mockk<View>(relaxed = true)
    private val initialHeight = 7
    private val heightFlow = MutableStateFlow(initialHeight)
    private val mapScalebarParams = MutableStateFlow(
        MapboxMapScalebarParams.Builder(context).build()
    )
    private val component = ScalebarPlaceholderComponent(
        scalebarPlaceholderView,
        mapScalebarParams,
        heightFlow
    )

    @Test
    fun visibilityIsChangedOnOnAttachedHasHeight() {
        component.onAttached(mapboxNavigation)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun visibilityIsChangedOnOnAttachedNoHeight() {
        heightFlow.tryEmit(0)
        component.onAttached(mapboxNavigation)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun mapScalebarParamsChangeBeforeOnAttached() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }

    @Test
    fun heightChangeBeforeOnAttached() {
        heightFlow.tryEmit(8)
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnAttachedHasHeight() {
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnAttachedNoHeight() {
        heightFlow.tryEmit(0)
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        verify { scalebarPlaceholderView.visibility = View.VISIBLE }
    }

    @Test
    fun heightChangeAfterOnAttachedHasHeight() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        heightFlow.tryEmit(8)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun heightChangeAfterOnAttachedNoHeight() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        heightFlow.tryEmit(0)
        verify { scalebarPlaceholderView.visibility = View.VISIBLE }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnDetached() {
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        component.onDetached(mapboxNavigation)
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }

    @Test
    fun heightChangeAfterOnDetached() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        component.onDetached(mapboxNavigation)
        heightFlow.tryEmit(9)
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }
}
