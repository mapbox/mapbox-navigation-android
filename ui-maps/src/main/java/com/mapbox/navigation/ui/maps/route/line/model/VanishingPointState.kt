package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Describes the vanishing point update algorithm's state.
 */
enum class VanishingPointState {
    /**
     * Always try to take the most recently calculated distance and set the vanishing point.
     */
    ENABLED,

    /**
     * Try to take the most recently calculated distance and set the vanishing point.
     *
     * Accept the value only if the progress is greater than the last update. This avoids
     * the vanishing point from creeping backwards after the destination is passed.
     */
    ONLY_INCREASE_PROGRESS,

    /**
     * Ignore puck position updates and leave the vanishing point in the current position.
     */
    DISABLED,
}
