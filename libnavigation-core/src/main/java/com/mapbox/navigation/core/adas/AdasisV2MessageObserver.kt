package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Callback which is getting called to report Adasis message
 */
@ExperimentalPreviewMapboxNavigationAPI
internal fun interface AdasisV2MessageObserver {

    /**
     * Called when new Adasis message is available
     * @param messageBuffer Message buffer in format specified via [AdasisDataSendingConfig.messageBinaryFormat]
     */
    fun onMessage(messageBuffer: List<Byte>)
}
