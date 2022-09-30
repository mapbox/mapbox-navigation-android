@file:JvmName("NavigationViewContextEx")

package com.mapbox.navigation.dropin.internal.extensions

import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.arrival.ArrivalTextComponent
import com.mapbox.navigation.dropin.extendablebutton.RoutePreviewButtonComponent
import com.mapbox.navigation.dropin.extendablebutton.StartNavigationButtonComponent
import com.mapbox.navigation.dropin.infopanel.InfoPanelEndNavigationButtonBinder
import com.mapbox.navigation.dropin.map.geocoding.POINameComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.tripprogress.TripProgressBinder
import kotlinx.coroutines.flow.map

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.poiNameComponent(textView: AppCompatTextView) =
    POINameComponent(store, textView, styles.poiNameTextAppearance)

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.routePreviewButtonComponent(buttonContainer: ViewGroup) =
    RoutePreviewButtonComponent(
        store,
        buttonContainer,
        styles.routePreviewButtonParams,
        routeOptionsProvider,
    )

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.startNavigationButtonComponent(buttonContainer: ViewGroup) =
    StartNavigationButtonComponent(
        store,
        buttonContainer,
        styles.startNavigationButtonParams,
        routeOptionsProvider,
    )

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.endNavigationButtonComponent(
    endNavigationButtonLayout: ViewGroup
): MapboxNavigationObserver {
    val binderFlow = uiBinders.infoPanelEndNavigationButtonBinder.map {
        it ?: InfoPanelEndNavigationButtonBinder(this)
    }
    return reloadOnChange(binderFlow) { it.bind(endNavigationButtonLayout) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.arrivalTextComponent(textView: AppCompatTextView) =
    ArrivalTextComponent(textView, styles.arrivalTextAppearance)

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.tripProgressComponent(
    tripProgressLayout: ViewGroup
): MapboxNavigationObserver {
    val binderFlow = uiBinders.infoPanelTripProgressBinder.map {
        it ?: TripProgressBinder(this)
    }
    return reloadOnChange(binderFlow) { it.bind(tripProgressLayout) }
}
