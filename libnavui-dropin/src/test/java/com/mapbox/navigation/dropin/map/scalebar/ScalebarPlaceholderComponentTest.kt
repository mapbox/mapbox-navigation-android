package com.mapbox.navigation.dropin.map.scalebar

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
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

    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val scalebarPlaceholderView = mockk<View>(relaxed = true)
    private val visibilityFlow = MutableStateFlow(false)
    private val isEnabledFlow = MutableStateFlow(false)
    private val component = ScalebarPlaceholderComponent(
        scalebarPlaceholderView,
        isEnabledFlow,
        visibilityFlow
    )

    @Test
    fun visibilityIsChangedOnOnAttachedVisible() {
        visibilityFlow.tryEmit(true)
        component.onAttached(mapboxNavigation)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun visibilityIsChangedOnOnAttachedNotVisible() {
        component.onAttached(mapboxNavigation)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun mapScalebarParamsChangeBeforeOnAttached() {
        isEnabledFlow.tryEmit(true)
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }

    @Test
    fun maneuverVisibilityChangeBeforeOnAttached() {
        visibilityFlow.tryEmit(true)
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnAttachedVisible() {
        visibilityFlow.tryEmit(true)
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        isEnabledFlow.tryEmit(true)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnAttachedNotVisible() {
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        isEnabledFlow.tryEmit(true)
        verify { scalebarPlaceholderView.visibility = View.VISIBLE }
    }

    @Test
    fun maneuverVisibilityChangeAfterOnAttachedVisible() {
        isEnabledFlow.tryEmit(true)
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        visibilityFlow.tryEmit(true)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun maneuverVisibilityChangeAfterOnAttachedNotVisible() {
        // so that it will <i>change</i> to false later
        visibilityFlow.tryEmit(true)
        isEnabledFlow.tryEmit(true)
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        visibilityFlow.tryEmit(false)
        verify { scalebarPlaceholderView.visibility = View.VISIBLE }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnDetached() {
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        component.onDetached(mapboxNavigation)
        isEnabledFlow.tryEmit(true)
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }

    @Test
    fun maneuverVisibilityChangeAfterOnDetached() {
        isEnabledFlow.tryEmit(true)
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        component.onDetached(mapboxNavigation)
        visibilityFlow.tryEmit(true)
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }
}
