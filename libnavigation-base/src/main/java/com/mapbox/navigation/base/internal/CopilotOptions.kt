package com.mapbox.navigation.base.internal

/**
 * Defines options for Copilot.
 *
 * @param shouldSendHistoryOnlyWithFeedback true if Copilot History files should be sent only when they include Feedback events. Default is false
 */
class CopilotOptions private constructor(
    val shouldSendHistoryOnlyWithFeedback: Boolean
) {
    /**
     * @return the builder that created the [CopilotOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        shouldSendHistoryOnlyWithFeedback(shouldSendHistoryOnlyWithFeedback)
    }

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

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return shouldSendHistoryOnlyWithFeedback.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CopilotOptions(" +
            "shouldSendHistoryOnlyWithFeedback=$shouldSendHistoryOnlyWithFeedback" +
            ")"
    }

    /**
     * Builder for [CopilotOptions].
     */
    class Builder {

        private var shouldSendHistoryOnlyWithFeedback: Boolean = false

        /**
         * Defines if Copilot History files should be sent only when they include Feedback events. Default is false
         */
        fun shouldSendHistoryOnlyWithFeedback(flag: Boolean): Builder =
            apply { this.shouldSendHistoryOnlyWithFeedback = flag }

        /**
         * Build the [CopilotOptions]
         */
        fun build(): CopilotOptions {
            return CopilotOptions(
                shouldSendHistoryOnlyWithFeedback = shouldSendHistoryOnlyWithFeedback
            )
        }
    }
}
