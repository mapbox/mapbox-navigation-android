package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Callback which is getting called to report ADASISv2Message
 */
@ExperimentalPreviewMapboxNavigationAPI
interface ADASISv2MessageCallback {

    /**
     * Called when ADASIS message is available
     * @param message ADASIS message
     */
    fun onMessage(message: ADASISv2Message)
}
