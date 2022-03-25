package com.mapbox.navigation.dropin

import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelTripProgressBinder
import com.mapbox.navigation.dropin.component.speedlimit.SpeedLimitViewBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NavigationUIBinders {

    private val _speedLimit: MutableStateFlow<UIBinder> = MutableStateFlow(SpeedLimitViewBinder())
    private val _maneuver: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _roadName: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _infoPanelTripProgressBinder: MutableStateFlow<UIBinder> =
        MutableStateFlow(InfoPanelTripProgressBinder())
    private val _infoPanelHeaderBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _infoPanelContentBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)
    private val _actionButtonsBinder: MutableStateFlow<UIBinder?> = MutableStateFlow(null)

    val speedLimit: StateFlow<UIBinder> get() = _speedLimit.asStateFlow()
    val maneuver: StateFlow<UIBinder?> get() = _maneuver.asStateFlow()
    val roadName: StateFlow<UIBinder?> get() = _roadName.asStateFlow()
    val infoPanelTripProgressBinder: StateFlow<UIBinder>
        get() = _infoPanelTripProgressBinder.asStateFlow()
    val infoPanelHeaderBinder: StateFlow<UIBinder?> get() = _infoPanelHeaderBinder.asStateFlow()
    val infoPanelContentBinder: StateFlow<UIBinder?> get() = _infoPanelContentBinder.asStateFlow()
    val actionButtonsBinder: StateFlow<UIBinder?> get() = _actionButtonsBinder.asStateFlow()

    fun applyCustomization(customization: ViewBinderCustomization) {
        customization.speedLimit?.also { _speedLimit.emitOrDefault(it, SpeedLimitViewBinder()) }
        customization.maneuver?.also { _maneuver.emitOrNull(it) }
        customization.roadName?.also { _roadName.emitOrNull(it) }
        customization.infoPanelTripProgressBinder?.also {
            _infoPanelTripProgressBinder.emitOrDefault(it, InfoPanelTripProgressBinder())
        }
        customization.infoPanelHeaderBinder?.also { _infoPanelHeaderBinder.emitOrNull(it) }
        customization.infoPanelContentBinder?.also { _infoPanelContentBinder.emitOrNull(it) }
        customization.actionButtonsBinder?.also { _actionButtonsBinder.emitOrNull(it) }
    }

    private fun MutableStateFlow<UIBinder>.emitOrDefault(v: UIBinder, default: UIBinder) {
        tryEmit(if (v != UIBinder.USE_DEFAULT) v else default)
    }

    private fun MutableStateFlow<UIBinder?>.emitOrNull(v: UIBinder) {
        tryEmit(if (v != UIBinder.USE_DEFAULT) v else null)
    }
}
