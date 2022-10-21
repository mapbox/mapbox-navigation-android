@file:JvmName("NavigationViewContextEx")

package com.mapbox.navigation.dropin.internal.extensions

import android.view.ViewGroup
import androidx.annotation.Px
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.EmptyBinder
import com.mapbox.navigation.dropin.actionbutton.AudioGuidanceButtonBinder
import com.mapbox.navigation.dropin.actionbutton.CameraModeButtonBinder
import com.mapbox.navigation.dropin.actionbutton.CompassButtonBinder
import com.mapbox.navigation.dropin.actionbutton.RecenterButtonBinder
import com.mapbox.navigation.dropin.arrival.ArrivalTextComponent
import com.mapbox.navigation.dropin.infopanel.InfoPanelEndNavigationButtonBinder
import com.mapbox.navigation.dropin.infopanel.InfoPanelRoutePreviewButtonBinder
import com.mapbox.navigation.dropin.infopanel.InfoPanelStartNavigationButtonBinder
import com.mapbox.navigation.dropin.map.geocoding.POINameComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.dropin.tripprogress.TripProgressBinder
import kotlinx.coroutines.flow.combine

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.poiNameComponent(textView: AppCompatTextView) =
    POINameComponent(store, textView, styles.poiNameTextAppearance)

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.routePreviewButtonComponent(
    buttonContainer: ViewGroup
): MapboxNavigationObserver {
    val binderFlow = combine(
        options.showRoutePreviewButton,
        uiBinders.infoPanelRoutePreviewButtonBinder
    ) { show, binder ->
        if (show) {
            binder ?: InfoPanelRoutePreviewButtonBinder(this)
        } else {
            EmptyBinder()
        }
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.startNavigationButtonComponent(
    buttonContainer: ViewGroup
): MapboxNavigationObserver {
    val binderFlow = combine(
        options.showStartNavigationButton,
        uiBinders.infoPanelStartNavigationButtonBinder
    ) { show, binder ->
        if (show) {
            binder ?: InfoPanelStartNavigationButtonBinder(this)
        } else {
            EmptyBinder()
        }
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.endNavigationButtonComponent(
    endNavigationButtonLayout: ViewGroup
): MapboxNavigationObserver {
    val binderFlow = combine(
        options.showEndNavigationButton,
        uiBinders.infoPanelEndNavigationButtonBinder
    ) { show, binder ->
        if (show) {
            binder ?: InfoPanelEndNavigationButtonBinder(this)
        } else {
            EmptyBinder()
        }
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
    val binderFlow = combine(
        options.showTripProgress,
        uiBinders.infoPanelTripProgressBinder
    ) { show, binder ->
        if (show) {
            binder ?: TripProgressBinder(this)
        } else {
            EmptyBinder()
        }
    }
    return reloadOnChange(binderFlow) { it.bind(tripProgressLayout) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.compassButtonComponent(
    buttonContainer: ViewGroup,
    @Px verticalSpacing: Int
): MapboxNavigationObserver {
    val binderFlow = combine(
        options.showCompassActionButton,
        uiBinders.actionCompassButtonBinder
    ) { show, binder ->
        if (show) {
            binder ?: CompassButtonBinder(this, verticalSpacing)
        } else {
            EmptyBinder()
        }
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.cameraModeButtonComponent(
    buttonContainer: ViewGroup,
    @Px verticalSpacing: Int
): MapboxNavigationObserver {
    val binderFlow = combine(
        options.showCameraModeActionButton,
        uiBinders.actionCameraModeButtonBinder
    ) { show, binder ->
        if (show) {
            binder ?: CameraModeButtonBinder(this, verticalSpacing)
        } else {
            EmptyBinder()
        }
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.audioGuidanceButtonComponent(
    buttonContainer: ViewGroup,
    @Px verticalSpacing: Int
): MapboxNavigationObserver {
    val binderFlow = combine(
        options.showToggleAudioActionButton,
        uiBinders.actionToggleAudioButtonBinder
    ) { show, binder ->
        if (show) {
            binder ?: AudioGuidanceButtonBinder(this, verticalSpacing)
        } else {
            EmptyBinder()
        }
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.recenterButtonComponent(
    buttonContainer: ViewGroup,
    @Px verticalSpacing: Int
): MapboxNavigationObserver {
    val binderFlow = combine(
        options.showRecenterActionButton,
        uiBinders.actionRecenterButtonBinder
    ) { show, binder ->
        if (show) {
            binder ?: RecenterButtonBinder(this, verticalSpacing)
        } else {
            EmptyBinder()
        }
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}
