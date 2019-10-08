package com.mapbox.services.android.navigation.v5.navigation

/**
 * Listener for receiving updates about a route tile download.
 */
interface RouteTileDownloadListener {

    /**
     * Called if there is an error with the downloading.
     *
     * @param error with message description
     */
    fun onError(error: OfflineError)

    /**
     * Called with percentage progress updates of the download.
     *
     * @param percent completed
     */
    fun onProgressUpdate(percent: Int)

    /**
     * Called when download was completed.
     */
    fun onCompletion()
}
