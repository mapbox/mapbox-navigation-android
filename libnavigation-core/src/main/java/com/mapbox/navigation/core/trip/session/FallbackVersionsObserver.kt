package com.mapbox.navigation.core.trip.session

interface FallbackVersionsObserver {

    /**
     * This callback is raised only if there are not enough tiles of current version.
     * This callback is called to notify that the specified `versions` can be used for switching into Fallback mode.
     * This means there are tiles downloaded (available offline) of the specified `versions` covering current location
     * so MapMatching should work (even offline) after switching to the one of the specified `versions`.
     */
    fun onFallbackVersionsFound(versions: List<String>)

    /**
     * Notifies a caller that it's OK to switch back to the latest version.
     * In order to use the latest version, it's sufficient to set "" in version configuration
     */
    fun onCanReturnToLatest()
}
