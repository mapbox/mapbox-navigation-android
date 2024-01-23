package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigator.MessageBinaryFormat

/**
 * Binary format in which Adasis message will be sent.
 *
 * @see [MapboxNavigation.setAdasisMessageObserver]
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class AdasisMessageBinaryFormat {

    /**
     * ADASISv2 standard 8 byte payload, CAN compatible. Big endian
     */
    object AdasisV2BigEndian : AdasisMessageBinaryFormat()

    /**
     * ADASISv2 standard 8 byte payload, CAN compatible. Little endian
     */
    object AdasisV2LittleEndian : AdasisMessageBinaryFormat()

    /**
     * ADASISv2 message structures serialised with FlatBuffers scheme
     */
    object FlatBuffers : AdasisMessageBinaryFormat()

    @JvmSynthetic
    internal fun toNativeMessageBinaryFormat(): MessageBinaryFormat {
        return when (this) {
            is AdasisV2BigEndian -> MessageBinaryFormat.ADASISV2_BE
            is AdasisV2LittleEndian -> MessageBinaryFormat.ADASISV2_LE
            is FlatBuffers -> MessageBinaryFormat.FLAT_BUFFERS
            else -> throw IllegalStateException("Unknown format: $this")
        }
    }
}
