package com.mapbox.navigation.ui.camera

/**
 * This class is passed to [NavigationCameraUpdate] to
 * determine the update's behavior when passed to [NavigationCamera].
 */
enum class CameraUpdateMode {
    /**
     * For a given [NavigationCameraUpdate], this default mode means the
     * [NavigationCamera] will ignore the update when tracking is already
     * enabled.
     *
     * If tracking is disabled, the update animation will execute.
     */
    DEFAULT,

    /**
     * For a given [NavigationCameraUpdate], this override mode means the
     * [NavigationCamera] will stop tracking (if tracking) and execute the
     * given update animation.
     */
    OVERRIDE
}
