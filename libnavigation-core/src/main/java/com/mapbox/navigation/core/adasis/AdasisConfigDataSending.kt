package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Data sending configuration
 * * @param messageIntervalMs interval between sending messages in milliseconds
 * @param messagesInPackage number of messages in one package (one message is 8 bytes)
 * @param sortProfileshortsByOffset if true, profileshorts will be sorted by offset
 * @param sortProfilelongsByOffset if true, profilelongs will be sorted by offset
 * @param enableRetransmission if true, retransmission will be enabled
 * (package will be appended with retransmission data, messages from previous cycles)
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigDataSending(
    val messageIntervalMs: Int,
    val messagesInPackage: Int,
    val sortProfileshortsByOffset: Boolean,
    val sortProfilelongsByOffset: Boolean,
    val enableRetransmission: Boolean,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfigDataSending(): com.mapbox.navigator.AdasisConfigDataSending {
        return com.mapbox.navigator.AdasisConfigDataSending(
            messageIntervalMs,
            messagesInPackage,
            sortProfileshortsByOffset,
            sortProfilelongsByOffset,
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

        if (messageIntervalMs != other.messageIntervalMs) return false
        if (messagesInPackage != other.messagesInPackage) return false
        if (sortProfileshortsByOffset != other.sortProfileshortsByOffset) return false
        if (sortProfilelongsByOffset != other.sortProfilelongsByOffset) return false
        if (enableRetransmission != other.enableRetransmission) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = messageIntervalMs
        result = 31 * result + messagesInPackage
        result = 31 * result + sortProfileshortsByOffset.hashCode()
        result = 31 * result + sortProfilelongsByOffset.hashCode()
        result = 31 * result + enableRetransmission.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigDataSending(" +
            "messageIntervalMs=$messageIntervalMs, " +
            "messagesInPackage=$messagesInPackage, " +
            "sortProfileshortsByOffset=$sortProfileshortsByOffset, " +
            "sortProfilelongsByOffset=$sortProfilelongsByOffset, " +
            "enableRetransmission=$enableRetransmission" +
            ")"
    }
}
