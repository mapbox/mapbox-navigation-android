@file:JvmName("NavigationViewContextFlowable")

package com.mapbox.navigation.dropin.internal.extensions

import com.mapbox.navigation.dropin.infopanel.InfoPanelHeaderActiveGuidanceBinder
import com.mapbox.navigation.dropin.infopanel.InfoPanelHeaderArrivalBinder
import com.mapbox.navigation.dropin.infopanel.InfoPanelHeaderDestinationPreviewBinder
import com.mapbox.navigation.dropin.infopanel.InfoPanelHeaderRoutesPreviewBinder
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Helper extension that returns flowable that emits a Info Panel Header Content UIBinder
 * when NavigationState changes.
 */
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
