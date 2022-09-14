@file:JvmName("NavigationViewContextFlowable")

package com.mapbox.navigation.dropin.internal.extensions

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.ViewBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelHeaderActiveGuidanceBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelHeaderArrivalBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelHeaderDestinationPreviewBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelHeaderRoutesPreviewBinder
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Helper extension to map [UIBinder] inside a [UICoordinator].
 * Uses a distinct by class to prevent refreshing views of the same type of [UIBinder].
 */
@ExperimentalPreviewMapboxNavigationAPI
internal fun <T : UIBinder> NavigationViewContext.flowUiBinder(
    selector: (value: ViewBinder) -> StateFlow<T>,
    mapper: suspend (value: T) -> T = { it }
): Flow<T> {
    return selector(this.uiBinders).map(mapper)
}

/**
 * Helper extension that returns flowable that emits a Info Panel Header Content UIBinder
 * when NavigationState changes.
 */
@ExperimentalPreviewMapboxNavigationAPI
@OptIn(ExperimentalCoroutinesApi::class)
internal fun NavigationViewContext.headerContentBinder(): Flow<UIBinder> =
    store.select { it.navigation }
        .flatMapLatest { navigationState ->
            when (navigationState) {
                NavigationState.FreeDrive ->
                    uiBinders.infoPanelHeaderFreeDriveBinder.map { binder ->
                        binder ?: UIBinder {
                            it.removeAllViews()
                            UIComponent()
                        }
                    }
                NavigationState.DestinationPreview ->
                    uiBinders.infoPanelHeaderDestinationPreviewBinder.map {
                        it ?: InfoPanelHeaderDestinationPreviewBinder(this)
                    }
                NavigationState.RoutePreview ->
                    uiBinders.infoPanelHeaderRoutesPreviewBinder.map {
                        it ?: InfoPanelHeaderRoutesPreviewBinder(this)
                    }
                NavigationState.ActiveNavigation ->
                    uiBinders.infoPanelHeaderActiveGuidanceBinder.map {
                        it ?: InfoPanelHeaderActiveGuidanceBinder(this)
                    }
                NavigationState.Arrival ->
                    uiBinders.infoPanelHeaderArrivalBinder.map {
                        it ?: InfoPanelHeaderArrivalBinder(this)
                    }
            }
        }
