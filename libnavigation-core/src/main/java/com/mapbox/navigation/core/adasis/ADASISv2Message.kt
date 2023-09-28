package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Describes a class holding methods for ADASISv2 messages.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ADASISv2Message internal constructor(
    private val nativeMessage: com.mapbox.navigator.ADASISv2Message
) {

    /**
     * Converts this message to list of bytes
     */
    fun toFlatBuffer(): List<Byte> = nativeMessage.toFlatBuffer()

    /**
     * Converts this message to JSON
     */
    fun toJson(): String = nativeMessage.toJson()

    /**
     * Converts this message to hex string
     */
    fun toHex(): String = nativeMessage.toHex()

    /**
     * Converts this message to big endian
     */
    fun toBigEndian(): Long = nativeMessage.toBigEndian()

    /**
     * Converts this message to little endian
     */
    fun toLittleEndian(): Long = nativeMessage.toLittleEndian()
}
