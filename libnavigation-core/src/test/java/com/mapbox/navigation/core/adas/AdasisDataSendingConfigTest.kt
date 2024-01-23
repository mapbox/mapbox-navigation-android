package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisDataSendingConfigTest :
    BuilderTest<AdasisDataSendingConfig, AdasisDataSendingConfig.Builder>() {

    override fun getImplementationClass() = AdasisDataSendingConfig::class

    override fun getFilledUpBuilder(): AdasisDataSendingConfig.Builder {
        return AdasisDataSendingConfig.Builder(AdasisMessageBinaryFormat.AdasisV2LittleEndian)
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
