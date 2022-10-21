package com.mapbox.navigation.dropin.actionbutton

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.EmptyBinder
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Coordinator for navigation actions.
 * This is the side panel for a portrait view.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class ActionButtonsCoordinator(
    private val context: NavigationViewContext,
    actionList: ViewGroup
) : UICoordinator<ViewGroup>(actionList) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return combine(
            context.options.showActionButtons,
            context.uiBinders.actionButtonsBinder,
            context.uiBinders.customActionButtons
        ) { show, uiBinder, customButtons ->
            if (show) {
                val binder = uiBinder ?: ActionButtonsBinder.defaultBinder()
                if (binder is ActionButtonsBinder) {
                    binder.context = context
                    binder.customButtons = customButtons
                }
                binder
            } else {
                EmptyBinder()
            }
        }
    }
}
