package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Data sending configuration
 *
 * @param messageBinaryFormat binary format in which Adasis message will be sent
 * @param messageIntervalMs interval between sending messages in milliseconds
 * @param messagesInPackage number of messages in one package (one message is 8 bytes)
 * @param sortProfileShortsByOffset if true, profile shorts will be sorted by offset
 * @param sortProfileLongsByOffset if true, profile longs will be sorted by offset
 * @param enableRetransmission if true, retransmission will be enabled
 * (package will be appended with retransmission data, messages from previous cycles)
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigDataSending(
    val messageBinaryFormat: AdasisMessageBinaryFormat,
    val messageIntervalMs: Int = 80,
    val messagesInPackage: Int = 20,
    val sortProfileShortsByOffset: Boolean = true,
    val sortProfileLongsByOffset: Boolean = true,
    val enableRetransmission: Boolean = true,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfigDataSending(): com.mapbox.navigator.AdasisConfigDataSending {
        return com.mapbox.navigator.AdasisConfigDataSending(
            messageBinaryFormat.toNativeMessageBinaryFormat(),
            messageIntervalMs,
            messagesInPackage,
            sortProfileShortsByOffset,
            sortProfileLongsByOffset,
            enableRetransmission,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfigDataSending

        if (messageBinaryFormat != other.messageBinaryFormat) return false
        if (messageIntervalMs != other.messageIntervalMs) return false
        if (messagesInPackage != other.messagesInPackage) return false
        if (sortProfileShortsByOffset != other.sortProfileShortsByOffset) return false
        if (sortProfileLongsByOffset != other.sortProfileLongsByOffset) return false
        if (enableRetransmission != other.enableRetransmission) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = messageIntervalMs
        result = 31 * result + messageBinaryFormat.hashCode()
        result = 31 * result + messagesInPackage
        result = 31 * result + sortProfileShortsByOffset.hashCode()
        result = 31 * result + sortProfileLongsByOffset.hashCode()
        result = 31 * result + enableRetransmission.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigDataSending(" +
            "messageBinaryFormat=$messageBinaryFormat, " +
            "messageIntervalMs=$messageIntervalMs, " +
            "messagesInPackage=$messagesInPackage, " +
            "sortProfileShortsByOffset=$sortProfileShortsByOffset, " +
            "sortProfileLongsByOffset=$sortProfileLongsByOffset, " +
            "enableRetransmission=$enableRetransmission" +
            ")"
    }
}
