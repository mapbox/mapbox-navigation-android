package com.mapbox.navigation.dropin.internal

import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.component.infopanel.EndNavigationButtonComponent
import com.mapbox.navigation.dropin.component.infopanel.POINameComponent
import com.mapbox.navigation.dropin.component.infopanel.RoutePreviewButtonComponent
import com.mapbox.navigation.dropin.component.infopanel.StartNavigationButtonComponent
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.poiNameComponent(textView: AppCompatTextView) =
    POINameComponent(store, textView)

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.routePreviewButtonComponent(button: MapboxExtendableButton) =
    RoutePreviewButtonComponent(store, button, styles.routePreviewButtonStyle)

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.startNavigationButtonComponent(button: MapboxExtendableButton) =
    StartNavigationButtonComponent(store, button, styles.startNavigationButtonStyle)

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.endNavigationButtonComponent(button: MapboxExtendableButton) =
    EndNavigationButtonComponent(store, button, styles.endNavigationButtonStyle)
