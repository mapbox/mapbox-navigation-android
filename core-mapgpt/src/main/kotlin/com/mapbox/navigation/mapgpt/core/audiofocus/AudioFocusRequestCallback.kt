package com.mapbox.navigation.mapgpt.core.audiofocus

/**
 * Interface definition for a callback to be invoked when a player requests audio focus
 */
fun interface AudioFocusRequestCallback {
    /**
     * Invoked when the AudioFocusDelegate processed the request.
     * @param result true on successful focus change request, false otherwise.
     */
    operator fun invoke(result: Boolean)
}
