package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasisConfigTest : BuilderTest<AdasisConfig, AdasisConfig.Builder>() {

    override fun getImplementationClass() = AdasisConfig::class

    override fun getFilledUpBuilder(): AdasisConfig.Builder {
        val dataSending = AdasisConfigDataSending.Builder(
            AdasisMessageBinaryFormat.AdasisV2BigEndian
        )
            .messageIntervalMs(100)
            .messagesInPackage(200)
            .metadataCycleSeconds(300)
            .enableRetransmission(false)
            .retransmissionMeters(400)
            .build()

        val messageOptions = AdasisConfigMessageOptions.Builder()
            .enable(false)
            .radiusMeters(12345)
            .build()

        val pathOptions = AdasisConfigPathOptions.Builder()
            .stub(Stub.Builder().options(messageOptions).build())
            .segment(Segment.Builder().options(messageOptions).build())
            .profileShort(ProfileShort.Builder().options(messageOptions).build())
            .profileLong(ProfileLong.Builder().options(messageOptions).build())
            .build()

        return AdasisConfig.Builder()
            .dataSending(dataSending)
            .pathOptions(pathOptions)
    }

    override fun trigger() {
        // trigger, see KDoc
    }
}
