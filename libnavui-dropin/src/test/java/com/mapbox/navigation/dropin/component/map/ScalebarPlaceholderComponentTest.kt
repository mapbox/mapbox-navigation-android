package com.mapbox.navigation.dropin.component.map

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxMapScalebarParams
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
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
    private val stateFlow = MutableStateFlow(
        State(
            navigation = NavigationState.FreeDrive,
            previewRoutes = RoutePreviewState.Ready(emptyList())
        )
    )
    private val mapScalebarParams = MutableStateFlow(MapboxMapScalebarParams.Builder(context).build())
    private val component = ScalebarPlaceholderComponent(scalebarPlaceholderView, mapScalebarParams, stateFlow)

    @Test
    fun visibilityIsChangedOnOnAttachedActiveGuidance() {
        stateFlow.tryEmit(State(navigation = NavigationState.FreeDrive))
        component.onAttached(mapboxNavigation)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun visibilityIsChangedOnOnAttachedFreeDrive() {
        stateFlow.tryEmit(State(navigation = NavigationState.FreeDrive))
        component.onAttached(mapboxNavigation)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun visibilityIsChangedOnOnAttachedRoutePreview() {
        stateFlow.tryEmit(State(navigation = NavigationState.RoutePreview))
        component.onAttached(mapboxNavigation)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun visibilityIsChangedOnOnAttachedDestinationPreview() {
        stateFlow.tryEmit(State(navigation = NavigationState.RoutePreview))
        component.onAttached(mapboxNavigation)
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun mapScalebarParamsChangeBeforeOnAttached() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }

    @Test
    fun stateChangeBeforeOnAttached() {
        stateFlow.tryEmit(State(navigation = NavigationState.RoutePreview))
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnAttachedActiveGuidance() {
        stateFlow.tryEmit(State(navigation = NavigationState.ActiveNavigation))
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnAttachedFreeDrive() {
        stateFlow.tryEmit(State(navigation = NavigationState.ActiveNavigation))
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnAttachedRoutePreview() {
        stateFlow.tryEmit(State(navigation = NavigationState.RoutePreview))
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        verify { scalebarPlaceholderView.visibility = View.VISIBLE }
    }

    @Test
    fun mapScalebarParamsChangeAfterOnAttachedDestinationPreview() {
        stateFlow.tryEmit(State(navigation = NavigationState.DestinationPreview))
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        verify { scalebarPlaceholderView.visibility = View.VISIBLE }
    }

    @Test
    fun stateChangeAfterOnAttachedActiveGuidance() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        stateFlow.tryEmit(State(navigation = NavigationState.ActiveNavigation))
        verify { scalebarPlaceholderView.visibility = View.GONE }
    }

    @Test
    fun stateChangeAfterOnAttachedFreeDrive() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        stateFlow.tryEmit(State(navigation = NavigationState.FreeDrive))
        verify { scalebarPlaceholderView.visibility = View.VISIBLE }
    }

    @Test
    fun stateChangeAfterOnAttachedRoutePreview() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        stateFlow.tryEmit(State(navigation = NavigationState.RoutePreview))
        verify { scalebarPlaceholderView.visibility = View.VISIBLE }
    }

    @Test
    fun stateChangeAfterOnAttachedDestinationPreview() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        stateFlow.tryEmit(State(navigation = NavigationState.DestinationPreview))
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
    fun stateChangeAfterOnDetached() {
        mapScalebarParams.tryEmit(MapboxMapScalebarParams.Builder(context).enabled(true).build())
        component.onAttached(mapboxNavigation)
        clearMocks(scalebarPlaceholderView)
        component.onDetached(mapboxNavigation)
        stateFlow.tryEmit(State(navigation = NavigationState.FreeDrive))
        verify(exactly = 0) { scalebarPlaceholderView.visibility = any() }
    }
}
