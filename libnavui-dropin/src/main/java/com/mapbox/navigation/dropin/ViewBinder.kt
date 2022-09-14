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
    val speedLimit: StateFlow<UIBinder?> get() = _speedLimit.asStateFlow()

    private val _maneuver: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    val maneuver: StateFlow<UIBinder?> get() = _maneuver.asStateFlow()

    private val _roadName: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    val roadName: StateFlow<UIBinder?> get() = _roadName.asStateFlow()

    private val _actionButtonsBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    val actionButtonsBinder: StateFlow<UIBinder?> get() = _actionButtonsBinder.asStateFlow()

    // Additional frames

    private val _leftFrameBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    val leftFrameContentBinder: StateFlow<UIBinder?> get() = _leftFrameBinder.asStateFlow()

    private val _rightFrameBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    val rightFrameContentBinder: StateFlow<UIBinder?> get() = _rightFrameBinder.asStateFlow()

    private val _customActionButtons: MutableStateFlow<List<ActionButtonDescription>> =
        MutableStateFlow(emptyList())
    val customActionButtons: StateFlow<List<ActionButtonDescription>>
        get() = _customActionButtons.asStateFlow()

    // Info panel

    private val _infoPanelBinder: MutableStateFlow<InfoPanelBinder?> = MutableStateFlow(null)
    val infoPanelBinder: StateFlow<InfoPanelBinder?> get() = _infoPanelBinder.asStateFlow()

    private val _infoPanelHeaderBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    val infoPanelHeaderBinder: StateFlow<UIBinder?> get() = _infoPanelHeaderBinder.asStateFlow()

    private val _infoPanelHeaderFreeDriveBinder: MutableStateFlow<UIBinder?> =
        MutableStateFlow(null)
    val infoPanelHeaderFreeDriveBinder: StateFlow<UIBinder?> =
        _infoPanelHeaderFreeDriveBinder.asStateFlow()

    private val _infoPanelHeaderDestinationPreviewBinder: MutableStateFlow<UIBinder?> =
        MutableStateFlow(null)
    val infoPanelHeaderDestinationPreviewBinder: StateFlow<UIBinder?> =
        _infoPanelHeaderDestinationPreviewBinder.asStateFlow()

    private val _infoPanelHeaderRoutesPreviewBinder: MutableStateFlow<UIBinder?> =
        MutableStateFlow(null)
    val infoPanelHeaderRoutesPreviewBinder: StateFlow<UIBinder?> =
        _infoPanelHeaderRoutesPreviewBinder.asStateFlow()

    private val _infoPanelHeaderActiveGuidanceBinder: MutableStateFlow<UIBinder?> =
        MutableStateFlow(null)
    val infoPanelHeaderActiveGuidanceBinder: StateFlow<UIBinder?> =
        _infoPanelHeaderActiveGuidanceBinder.asStateFlow()

    private val _infoPanelHeaderArrivalBinder: MutableStateFlow<UIBinder?> =
        MutableStateFlow(null)
    val infoPanelHeaderArrivalBinder: StateFlow<UIBinder?> =
        _infoPanelHeaderArrivalBinder.asStateFlow()

    private val _infoPanelTripProgressBinder: MutableStateFlow<UIBinder?> =
        MutableStateFlow(null)
    val infoPanelTripProgressBinder: StateFlow<UIBinder?>
        get() = _infoPanelTripProgressBinder.asStateFlow()

    private val _infoPanelEndNavigationButtonBinder: MutableStateFlow<UIBinder?> =
        MutableStateFlow(null)
    val infoPanelEndNavigationButtonBinder: StateFlow<UIBinder?>
        get() = _infoPanelEndNavigationButtonBinder.asStateFlow()

    private val _infoPanelContentBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    val infoPanelContentBinder: StateFlow<UIBinder?> get() = _infoPanelContentBinder.asStateFlow()

    fun applyCustomization(customization: ViewBinderCustomization) {
        customization.speedLimitBinder?.also { _speedLimit.emitOrNull(it) }
        customization.maneuverBinder?.also { _maneuver.emitOrNull(it) }
        customization.roadNameBinder?.also { _roadName.emitOrNull(it) }
        customization.actionButtonsBinder?.also { _actionButtonsBinder.emitOrNull(it) }

        customization.leftFrameBinder?.also { _leftFrameBinder.emitOrNull(it) }
        customization.rightFrameBinder?.also { _rightFrameBinder.emitOrNull(it) }
        customization.customActionButtons?.also { _customActionButtons.value = it }

        customization.infoPanelBinder?.also { _infoPanelBinder.emitOrNull(it) }
        customization.infoPanelHeaderBinder?.also { _infoPanelHeaderBinder.emitOrNull(it) }
        customization.infoPanelHeaderFreeDriveBinder?.also {
            _infoPanelHeaderFreeDriveBinder.emitOrNull(it)
        }
        customization.infoPanelHeaderDestinationPreviewBinder?.also {
            _infoPanelHeaderDestinationPreviewBinder.emitOrNull(it)
        }
        customization.infoPanelHeaderRoutesPreviewBinder?.also {
            _infoPanelHeaderRoutesPreviewBinder.emitOrNull(it)
        }
        customization.infoPanelHeaderActiveGuidanceBinder?.also {
            _infoPanelHeaderActiveGuidanceBinder.emitOrNull(it)
        }
        customization.infoPanelHeaderArrivalBinder?.also {
            _infoPanelHeaderArrivalBinder.emitOrNull(it)
        }
        customization.infoPanelTripProgressBinder?.also {
            _infoPanelTripProgressBinder.emitOrNull(it)
        }
        customization.infoPanelContentBinder?.also { _infoPanelContentBinder.emitOrNull(it) }
        customization.infoPanelEndNavigationButtonBinder?.also {
            _infoPanelEndNavigationButtonBinder.emitOrNull(it)
        }
    }

    private fun <T : UIBinder> MutableStateFlow<T?>.emitOrNull(v: T) {
        tryEmit(if (v != UIBinder.USE_DEFAULT) v else null)
    }
}
