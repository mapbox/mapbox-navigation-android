package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisConfigDataSendingTest :
    BuilderTest<AdasisConfigDataSending, AdasisConfigDataSending.Builder>() {

    override fun getImplementationClass() = AdasisConfigDataSending::class

    override fun getFilledUpBuilder(): AdasisConfigDataSending.Builder {
        return AdasisConfigDataSending.Builder(AdasisMessageBinaryFormat.AdasisV2LittleEndian)
            .messageIntervalMs(100)
            .messagesInPackage(200)
            .metadataCycleSeconds(300)
            .enableRetransmission(false)
            .retransmissionMeters(400)
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
