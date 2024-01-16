package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Callback which is getting called to report Adasis message
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface AdasisV2MessageCallback {

    /**
     * Called when Adasis message is available
     * @param messageBuffer Message buffer in format specified via [AdasisConfigDataSending.messageBinaryFormat]
     * @param context Additional context with metadata related to the current messages package
     */
    fun onMessage(messageBuffer: List<Byte>, context: AdasisMessageContext)
}
