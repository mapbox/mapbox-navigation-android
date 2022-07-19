package com.mapbox.navigation.dropin

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelBinder
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A class that is a central place to hold the definitions for custom view injections and view(s)
 * swapping.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class ViewBinder {

    private val _speedLimit: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _maneuver: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _roadName: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _infoPanelBinder: MutableStateFlow<InfoPanelBinder?> = MutableStateFlow(null)
    private val _infoPanelTripProgressBinder: MutableStateFlow<UIBinder?> =
        MutableStateFlow(null)
    private val _infoPanelHeaderBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _infoPanelContentBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _actionButtonsBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _leftFrameBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _rightFrameBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)

    private val _customActionButtons: MutableStateFlow<List<ActionButtonDescription>> =
        MutableStateFlow(emptyList())

    val speedLimit: StateFlow<UIBinder?> get() = _speedLimit.asStateFlow()
    val maneuver: StateFlow<UIBinder?> get() = _maneuver.asStateFlow()
    val roadName: StateFlow<UIBinder?> get() = _roadName.asStateFlow()
    val infoPanelBinder: StateFlow<InfoPanelBinder?> get() = _infoPanelBinder.asStateFlow()
    val infoPanelTripProgressBinder: StateFlow<UIBinder?>
        get() = _infoPanelTripProgressBinder.asStateFlow()
    val infoPanelHeaderBinder: StateFlow<UIBinder?> get() = _infoPanelHeaderBinder.asStateFlow()
    val infoPanelContentBinder: StateFlow<UIBinder?> get() = _infoPanelContentBinder.asStateFlow()
    val actionButtonsBinder: StateFlow<UIBinder?> get() = _actionButtonsBinder.asStateFlow()
    val leftFrameContentBinder: StateFlow<UIBinder?> get() = _leftFrameBinder.asStateFlow()
    val rightFrameContentBinder: StateFlow<UIBinder?> get() = _rightFrameBinder.asStateFlow()

    val customActionButtons: StateFlow<List<ActionButtonDescription>>
        get() = _customActionButtons.asStateFlow()

    fun applyCustomization(customization: ViewBinderCustomization) {
        customization.speedLimitBinder?.also { _speedLimit.emitOrNull(it) }
        customization.maneuverBinder?.also { _maneuver.emitOrNull(it) }
        customization.roadNameBinder?.also { _roadName.emitOrNull(it) }
        customization.infoPanelBinder?.also { _infoPanelBinder.emitOrNull(it) }
        customization.infoPanelTripProgressBinder?.also {
            _infoPanelTripProgressBinder.emitOrNull(it)
        }
        customization.infoPanelHeaderBinder?.also { _infoPanelHeaderBinder.emitOrNull(it) }
        customization.infoPanelContentBinder?.also { _infoPanelContentBinder.emitOrNull(it) }
        customization.actionButtonsBinder?.also { _actionButtonsBinder.emitOrNull(it) }
        customization.leftFrameBinder?.also { _leftFrameBinder.emitOrNull(it) }
        customization.rightFrameBinder?.also { _rightFrameBinder.emitOrNull(it) }

        customization.customActionButtons?.also { _customActionButtons.value = it }
    }

    private fun <T : UIBinder> MutableStateFlow<T?>.emitOrNull(v: T) {
        tryEmit(if (v != UIBinder.USE_DEFAULT) v else null)
    }
}
