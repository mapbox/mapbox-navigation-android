package com.mapbox.navigation.ui.base.model

import com.mapbox.navigation.ui.base.State

/**
 * Immutable object which contains the required [State] to render [AlertView]
 * @property avText String text to show in the alert view
 * @property durationToDismiss Long specifies the time in ms for AlertView to dismiss automatically. It cannot be greater than 5000ms
 * @property error Holds erroneous throwable messages
 */
data class AlertState(
    val avText: String,
    val durationToDismiss: Long,
    val error: Throwable?
) : State {
    companion object {
        /**
         * Returns the initial state of [AlertView]
         * @return [AlertState]
         */
        fun idle(): AlertState = AlertState("", 3000L, null)
    }
}
