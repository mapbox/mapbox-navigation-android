package com.mapbox.navigation.ui.maps

/**
 * Listener that gets notified whenever there's an error with Predictive Cache.
 *
 * @see PredictiveCacheController
 */
fun interface PredictiveCacheControllerErrorHandler {

    /**
     * Called whenever there's an error with Predictive Cache.
     *
     * @param message error message
     */
    fun onError(message: String?)
}
