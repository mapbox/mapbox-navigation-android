package com.mapbox.navigation.dropin

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.Binder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LeftFrameCoordinator(
    private val context: NavigationViewContext,
    frameLayout: ViewGroup
) : UICoordinator<ViewGroup>(frameLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<ViewGroup>> {
        return context.uiBinders.leftFrameContentBinder.map {
            it ?: EmptyBinder()
        }
    }
}
