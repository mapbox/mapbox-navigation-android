package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Defines options for Copilot.
 *
 * @param shouldSendHistoryOnlyWithFeedback true if Copilot History files should be sent only when they include Feedback events. Default is false
 * @param maxHistoryFileLengthMillis if a session takes longer than this value, the history file will be split into several parts
 * @param maxHistoryFilesPerSession if there are more history files for a session than this value, only the last [maxHistoryFilesPerSession] files will be uploaded
 * @param maxTotalHistoryFilesSizePerSession if the total size of history files exceeds this value, some of the first files will not be uploaded
 * @param shouldRecordFreeDriveHistories true if Copilot should record history files in Free Drive state. Default is true
 */
@ExperimentalPreviewMapboxNavigationAPI
class CopilotOptions private constructor(
    val shouldSendHistoryOnlyWithFeedback: Boolean,
    val maxHistoryFileLengthMillis: Long,
    val maxHistoryFilesPerSession: Int,
    val maxTotalHistoryFilesSizePerSession: Long,
    val shouldRecordFreeDriveHistories: Boolean,
) {

    /**
     * @return the builder that created the [CopilotOptions]
     */
    fun toBuilder(): Builder = Builder()
        .shouldSendHistoryOnlyWithFeedback(shouldSendHistoryOnlyWithFeedback)
        .maxHistoryFileLengthMillis(maxHistoryFileLengthMillis)
        .maxHistoryFilesPerSession(maxHistoryFilesPerSession)
        .maxTotalHistoryFilesSizePerSession(maxTotalHistoryFilesSizePerSession)
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
        if (maxHistoryFileLengthMillis != other.maxHistoryFileLengthMillis) {
            return false
        }
        if (maxHistoryFilesPerSession != other.maxHistoryFilesPerSession) {
            return false
        }
        if (maxTotalHistoryFilesSizePerSession != other.maxTotalHistoryFilesSizePerSession) {
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
        result = 31 * result + maxHistoryFileLengthMillis.hashCode()
        result = 31 * result + maxHistoryFilesPerSession
        result = 31 * result + maxTotalHistoryFilesSizePerSession.hashCode()
        result = 31 * result + shouldRecordFreeDriveHistories.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "CopilotOptions(" +
            "shouldSendHistoryOnlyWithFeedback=$shouldSendHistoryOnlyWithFeedback, " +
            "maxHistoryFileLengthMillis=$maxHistoryFileLengthMillis, " +
            "maxHistoryFilesPerSession=$maxHistoryFilesPerSession, " +
            "maxTotalHistoryFilesSizePerSession=$maxTotalHistoryFilesSizePerSession, " +
            "shouldRecordFreeDriveHistories=$shouldRecordFreeDriveHistories" +
            ")"
    }

    /**
     * Builder for [CopilotOptions].
     */
    class Builder {

        private var shouldSendHistoryOnlyWithFeedback: Boolean = false
        private var maxHistoryFileLengthMillis: Long = Long.MAX_VALUE
        private var maxHistoryFilesPerSession: Int = Int.MAX_VALUE
        private var maxTotalHistoryFilesSizePerSession: Long = Long.MAX_VALUE
        private var shouldRecordFreeDriveHistories: Boolean = true

        /**
         * Defines if Copilot History files should be sent only when they include Feedback events. Default is false
         */
        fun shouldSendHistoryOnlyWithFeedback(flag: Boolean): Builder = apply {
            this.shouldSendHistoryOnlyWithFeedback = flag
        }

        /**
         * Limits duration of a history recording. If a trip takes longer than this value,
         * the history file will be split into several parts. Default is [Long.MAX_VALUE]
         */
        fun maxHistoryFileLengthMillis(millis: Long): Builder = apply {
            check(millis > 0) {
                "maxHistoryFileLengthMilliseconds must be > 0"
            }
            this.maxHistoryFileLengthMillis = millis
        }

        /**
         * Limits number of history files per session. If there are more history files for a session than this value,
         * only the last [maxHistoryFilesPerSession] files will be uploaded. Default is [Int.MAX_VALUE]
         */
        fun maxHistoryFilesPerSession(count: Int): Builder = apply {
            check(count > 0) {
                "maxHistoryFilesPerSession must be > 0"
            }
            maxHistoryFilesPerSession = count
        }

        /**
         * Limits total size of history files per session. If the total size of history files exceeds this value,
         * some of the first files will not be uploaded. Default is [Long.MAX_VALUE]
         */
        fun maxTotalHistoryFilesSizePerSession(bytes: Long): Builder = apply {
            check(bytes > 0) {
                "maxTotalHistoryFilesSizePerSession must be > 0"
            }
            maxTotalHistoryFilesSizePerSession = bytes
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
                maxHistoryFileLengthMillis = maxHistoryFileLengthMillis,
                maxHistoryFilesPerSession = maxHistoryFilesPerSession,
                maxTotalHistoryFilesSizePerSession = maxTotalHistoryFilesSizePerSession,
                shouldRecordFreeDriveHistories = shouldRecordFreeDriveHistories,
            )
        }
    }
}
