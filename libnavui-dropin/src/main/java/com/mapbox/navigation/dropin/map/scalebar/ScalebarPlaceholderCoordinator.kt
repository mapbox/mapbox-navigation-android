package com.mapbox.navigation.dropin.map.scalebar

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.Binder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@ExperimentalPreviewMapboxNavigationAPI
internal class ScalebarPlaceholderCoordinator(
    private val context: NavigationViewContext,
    scalebarLayout: ViewGroup
) : UICoordinator<ViewGroup>(scalebarLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<ViewGroup>> {
        return flowOf(ScalebarPlaceholderBinder(context))
    }
}
