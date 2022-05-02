package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.model.Action

/**
 * Defines actions responsible to mutate the [NavigationState].
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class NavigationStateAction : Action {
    /**
     * The action update the [NavigationState] for the screen.
     * @property state to update the screen to
     */
    data class Update(val state: NavigationState) : NavigationStateAction()
}
