package com.mapbox.services.android.navigation.v5.navigation

/**
 * Listener that needs to be added to [MapboxOfflineRouter.configure] to know when
 * offline data is initialized an [MapboxOfflineRouter.findRoute] could be called.
 */
interface OnOfflineTilesConfiguredCallback {

    /**
     * Called whe the offline data is initialized
     */
    fun onConfigured()
}
