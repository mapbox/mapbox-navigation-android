package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisConfigTest : BuilderTest<AdasisConfig, AdasisConfig.Builder>() {

    override fun getImplementationClass() = AdasisConfig::class

    override fun getFilledUpBuilder(): AdasisConfig.Builder {
        val dataSending = AdasisDataSendingConfig.Builder(
            AdasisMessageBinaryFormat.AdasisV2BigEndian,
        )
            .messageIntervalMs(100)
            .messagesInPackage(200)
            .metadataCycleSeconds(300)
            .enableRetransmission(false)
            .retransmissionMeters(400)
            .treeTrailingLength(500)
            .build()

        val messageOptions = AdasisConfigMessageOptions.Builder()
            .enable(false)
            .radiusMeters(12345)
            .build()

        val pathOptions = AdasisConfigPathOptions.Builder()
            .stubOptions(
                AdasisStubOptions.Builder().options(messageOptions).build(),
            )
            .segmentOptions(
                AdasisSegmentOptions.Builder().options(messageOptions).build(),
            )
            .profileShortOptions(
                AdasisProfileShortOptions.Builder().options(messageOptions).build(),
            )
            .profileLongOptions(
                AdasisProfileLongOptions.Builder().options(messageOptions).build(),
            )
            .build()

        return AdasisConfig.Builder(dataSending)
            .pathOptions(pathOptions)
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
