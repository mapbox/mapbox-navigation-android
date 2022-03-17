package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationView
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.backstack.BackStackComponent
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@ExperimentalPreviewMapboxNavigationAPI
internal class BackPressCoordinator(
    private val context: DropInNavigationViewContext,
    navigationView: DropInNavigationView,
) : UICoordinator<ViewGroup>(navigationView) {

    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<ViewGroup>> {
        val binder = object : UIBinder {
            override fun bind(navigationView: ViewGroup): MapboxNavigationObserver {
                val backPressManager = context.viewModel.backPressManager
                return BackStackComponent(navigationView, backPressManager)
            }
        }
        return flowOf(binder)
    }
}
