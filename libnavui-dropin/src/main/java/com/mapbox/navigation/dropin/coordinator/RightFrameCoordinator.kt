package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.EmptyBinder
import com.mapbox.navigation.ui.base.lifecycle.Binder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@ExperimentalPreviewMapboxNavigationAPI
internal class RightFrameCoordinator(
    private val context: NavigationViewContext,
    frameLayout: ViewGroup
) : UICoordinator<ViewGroup>(frameLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<ViewGroup>> {
        return context.uiBinders.rightFrameContentBinder.map {
            it ?: EmptyBinder()
        }
    }
}
