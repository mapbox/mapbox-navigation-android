package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Data sending configuration
 *
 * @param messageBinaryFormat binary format in which Adasis message will be sent
 * @param messageIntervalMs interval between sending messages in milliseconds
 * @param messagesInPackage number of messages in one package (one message is 8 bytes)
 * @param metadataCycleSeconds time in seconds between repetition of META-DATA message
 * @param enableRetransmission if true, retransmission will be enabled
 * (package will be appended with retransmission data, messages from previous cycles)
 * @param retransmissionMeters after passing this distance, messages will not be retransmitted
 * @param treeTrailingLength the trailing length of the path tree, relatively
 * to the map-matched position, in the adasis provider
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisDataSendingConfig private constructor(
    val messageBinaryFormat: AdasisMessageBinaryFormat,
    val messageIntervalMs: Int,
    val messagesInPackage: Int,
    val metadataCycleSeconds: Int,
    val enableRetransmission: Boolean,
    val retransmissionMeters: Int,
    val treeTrailingLength: Int,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder(messageBinaryFormat)
        .messageIntervalMs(messageIntervalMs)
        .messagesInPackage(messagesInPackage)
        .metadataCycleSeconds(metadataCycleSeconds)
        .enableRetransmission(enableRetransmission)
        .retransmissionMeters(retransmissionMeters)
        .treeTrailingLength(treeTrailingLength)

    @JvmSynthetic
    internal fun toNativeAdasisConfigDataSending(): com.mapbox.navigator.AdasisConfigDataSending {
        return com.mapbox.navigator.AdasisConfigDataSending(
            messageBinaryFormat.toNativeMessageBinaryFormat(),
            messageIntervalMs,
            messagesInPackage,
            metadataCycleSeconds,
            enableRetransmission,
            retransmissionMeters,
            treeTrailingLength,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisDataSendingConfig

        if (messageBinaryFormat != other.messageBinaryFormat) return false
        if (messageIntervalMs != other.messageIntervalMs) return false
        if (messagesInPackage != other.messagesInPackage) return false
        if (metadataCycleSeconds != other.metadataCycleSeconds) return false
        if (enableRetransmission != other.enableRetransmission) return false
        if (retransmissionMeters != other.retransmissionMeters) return false
        return treeTrailingLength == other.treeTrailingLength
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = messageIntervalMs
        result = 31 * result + messageBinaryFormat.hashCode()
        result = 31 * result + messagesInPackage
        result = 31 * result + metadataCycleSeconds.hashCode()
        result = 31 * result + enableRetransmission.hashCode()
        result = 31 * result + retransmissionMeters.hashCode()
        result = 31 * result + treeTrailingLength.hashCode()
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
            "messagesInPackage=$messagesInPackage, " +
            "metadataCycleSeconds=$metadataCycleSeconds, " +
            "enableRetransmission=$enableRetransmission, " +
            "retransmissionMeters=$retransmissionMeters, " +
            "treeTrailingLength=$treeTrailingLength" +
            ")"
    }

    /**
     * Builder for [AdasisDataSendingConfig].
     *
     * @param messageBinaryFormat Binary format in which Adasis message will be sent
     */
    class Builder(private val messageBinaryFormat: AdasisMessageBinaryFormat) {

        private var messageIntervalMs: Int = 80
        private var messagesInPackage: Int = 20
        private var metadataCycleSeconds: Int = 5
        private var enableRetransmission: Boolean = true
        private var retransmissionMeters: Int = 300
        private var treeTrailingLength: Int = 100

        /**
         * Interval between sending messages in milliseconds
         */
        fun messageIntervalMs(messageIntervalMs: Int) = apply {
            this.messageIntervalMs = messageIntervalMs
        }

        /**
         * Number of messages in one package (one message is 8 bytes)
         */
        fun messagesInPackage(messagesInPackage: Int) = apply {
            this.messagesInPackage = messagesInPackage
        }

        /**
         * Time in seconds between repetition of META-DATA message
         */
        fun metadataCycleSeconds(metadataCycleSeconds: Int) = apply {
            this.metadataCycleSeconds = metadataCycleSeconds
        }

        /**
         * If true, retransmission will be enabled.
         * (package will be appended with retransmission data, messages from previous cycles)
         */
        fun enableRetransmission(enableRetransmission: Boolean) = apply {
            this.enableRetransmission = enableRetransmission
        }

        /**
         * After passing this distance, messages will not be retransmitted
         */
        fun retransmissionMeters(retransmissionMeters: Int) = apply {
            this.retransmissionMeters = retransmissionMeters
        }

        /**
         * The trailing length of the path tree, relatively to the map-matched position,
         * in the adasis provider
         */
        fun treeTrailingLength(treeTrailingLength: Int) = apply {
            this.treeTrailingLength = treeTrailingLength
        }

        /**
         * Build the [AdasisDataSendingConfig]
         */
        fun build() = AdasisDataSendingConfig(
            messageBinaryFormat = messageBinaryFormat,
            messageIntervalMs = messageIntervalMs,
            messagesInPackage = messagesInPackage,
            metadataCycleSeconds = metadataCycleSeconds,
            enableRetransmission = enableRetransmission,
            retransmissionMeters = retransmissionMeters,
            treeTrailingLength = treeTrailingLength,
        )
    }
}
