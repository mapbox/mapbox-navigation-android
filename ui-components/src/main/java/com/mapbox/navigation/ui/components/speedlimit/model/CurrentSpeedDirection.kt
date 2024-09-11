package com.mapbox.navigation.ui.components.speedlimit.model

/**
 * Specify the direction of current speed relative to posted speed.
 */
enum class CurrentSpeedDirection {
    /**
     * This will render current speed to the top of posted speed.
     */
    TOP,

    /**
     * This will render current speed to the end of posted speed.
     */
    END,

    /**
     * This will render current speed to the start of posted speed.
     */
    START,

    /**
     * This will render current speed to the bottom of posted speed.
     */
    BOTTOM,
}
