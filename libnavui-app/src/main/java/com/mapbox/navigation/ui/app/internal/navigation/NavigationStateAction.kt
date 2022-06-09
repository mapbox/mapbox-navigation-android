package com.mapbox.navigation.ui.app.internal.navigation

import com.mapbox.navigation.ui.app.internal.Action

/**
 * Defines actions responsible to mutate the [NavigationState].
 */
sealed class NavigationStateAction : Action {
    /**
     * The action update the [NavigationState] for the screen.
     * @property state to update the screen to
     */
    data class Update(val state: NavigationState) : NavigationStateAction()
}
