package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Defines options for Copilot.
 *
 * @param shouldSendHistoryOnlyWithFeedback true if Copilot History files should be sent only when they include Feedback events. Default is false
 * @param shouldRecordFreeDriveHistories true if Copilot should record history files in Free Drive state. Default is true
 */
@ExperimentalPreviewMapboxNavigationAPI
class CopilotOptions private constructor(
    val shouldSendHistoryOnlyWithFeedback: Boolean,
    val shouldRecordFreeDriveHistories: Boolean,
) {

    /**
     * @return the builder that created the [CopilotOptions]
     */
    fun toBuilder(): Builder = Builder()
        .shouldSendHistoryOnlyWithFeedback(shouldSendHistoryOnlyWithFeedback)
        .shouldRecordFreeDriveHistories(shouldRecordFreeDriveHistories)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CopilotOptions

        if (shouldSendHistoryOnlyWithFeedback != other.shouldSendHistoryOnlyWithFeedback) {
            return false
        }
        if (shouldRecordFreeDriveHistories != other.shouldRecordFreeDriveHistories) {
            return false
        }

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = shouldSendHistoryOnlyWithFeedback.hashCode()
        result = 31 * result + shouldRecordFreeDriveHistories.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CopilotOptions(" +
            "shouldSendHistoryOnlyWithFeedback=$shouldSendHistoryOnlyWithFeedback, " +
            "shouldRecordFreeDriveHistories=$shouldRecordFreeDriveHistories" +
            ")"
    }

    /**
     * Builder for [CopilotOptions].
     */
    class Builder {

        private var shouldSendHistoryOnlyWithFeedback: Boolean = false
        private var shouldRecordFreeDriveHistories: Boolean = true

        /**
         * Defines if Copilot History files should be sent only when they include Feedback events. Default is false
         */
        fun shouldSendHistoryOnlyWithFeedback(flag: Boolean): Builder = apply {
            this.shouldSendHistoryOnlyWithFeedback = flag
        }

        /**
         * Defines if Copilot should record history files in Free Drive state. Default is true
         */
        fun shouldRecordFreeDriveHistories(flag: Boolean): Builder = apply {
            shouldRecordFreeDriveHistories = flag
        }

        /**
         * Build the [CopilotOptions]
         */
        fun build(): CopilotOptions {
            return CopilotOptions(
                shouldSendHistoryOnlyWithFeedback = shouldSendHistoryOnlyWithFeedback,
                shouldRecordFreeDriveHistories = shouldRecordFreeDriveHistories,
            )
        }
    }
}
