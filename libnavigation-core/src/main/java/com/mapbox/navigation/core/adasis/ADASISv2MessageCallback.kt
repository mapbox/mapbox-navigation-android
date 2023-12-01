package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Callback which is getting called to report ADASISv2Message
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface ADASISv2MessageCallback {

    /**
     * Called when ADASIS message is available
     * @param messageBuffer Message buffer in format specified via [AdasisConfigDataSending.messageBinaryFormat]
     */
    fun onMessage(messageBuffer: List<Byte>)
}
