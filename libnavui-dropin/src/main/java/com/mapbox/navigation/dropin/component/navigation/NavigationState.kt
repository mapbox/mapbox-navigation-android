package com.mapbox.navigation.dropin.component.navigation

/**
 * Defines the current screen state.
 */
sealed class NavigationState {
    /**
     * Sets the screen to FreeDrive state.
     */
    object FreeDrive : NavigationState()
    /**
     * Sets the screen to DestinationPreview state.
     */
    object DestinationPreview : NavigationState()
    /**
     * Sets the screen to RoutePreview state.
     */
    object RoutePreview : NavigationState()
    /**
     * Sets the screen to ActiveNavigation state.
     */
    object ActiveNavigation : NavigationState()
    /**
     * Sets the screen to Arrival state.
     */
    object Arrival : NavigationState()

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String = "NavigationState.${this::class.java.simpleName}"
}
