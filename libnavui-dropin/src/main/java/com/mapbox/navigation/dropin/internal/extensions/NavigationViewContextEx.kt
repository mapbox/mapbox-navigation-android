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
import kotlinx.coroutines.flow.map

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.poiNameComponent(textView: AppCompatTextView) =
    POINameComponent(store, textView, styles.poiNameTextAppearance)

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.routePreviewButtonComponent(
    buttonContainer: ViewGroup
): MapboxNavigationObserver {
    val binderFlow = uiBinders.infoPanelRoutePreviewButtonBinder.map {
        it ?: InfoPanelRoutePreviewButtonBinder(this)
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.startNavigationButtonComponent(
    buttonContainer: ViewGroup
): MapboxNavigationObserver {
    val binderFlow = uiBinders.infoPanelStartNavigationButtonBinder.map {
        it ?: InfoPanelStartNavigationButtonBinder(this)
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}

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
    val binderFlow = uiBinders.actionCameraModeButtonBinder.map {
        it ?: CameraModeButtonBinder(this, verticalSpacing)
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.audioGuidanceButtonComponent(
    buttonContainer: ViewGroup,
    @Px verticalSpacing: Int
): MapboxNavigationObserver {
    val binderFlow = uiBinders.actionToggleAudioButtonBinder.map {
        it ?: AudioGuidanceButtonBinder(this, verticalSpacing)
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}

@ExperimentalPreviewMapboxNavigationAPI
internal fun NavigationViewContext.recenterButtonComponent(
    buttonContainer: ViewGroup,
    @Px verticalSpacing: Int
): MapboxNavigationObserver {
    val binderFlow = uiBinders.actionRecenterButtonBinder.map {
        it ?: RecenterButtonBinder(this, verticalSpacing)
    }
    return reloadOnChange(binderFlow) { it.bind(buttonContainer) }
}
