package com.mapbox.navigation.mapgpt.core.wakeword

abstract class WakeWordAction {
    class Unknown(val provider: WakeWordProvider, val action: String) : WakeWordAction() {
        override fun toString(): String {
            return "WakeWordAction.Unknown(provider=$provider, action=$action)"
        }
    }
    object ActivateMapGpt : WakeWordAction()
}
