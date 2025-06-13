package com.mapbox.navigation.base.options

/**
 * Defines options for recording history files.
 *
 * @param fileDirectory used for saving history files. Null use a default directory.
 * @param shouldRecordRouteLineEvents true if recorder should record route line related events that can be used for debugging purposes. Default is false
 */
class HistoryRecorderOptions private constructor(
    val fileDirectory: String?,
    val shouldRecordRouteLineEvents: Boolean,
) {
    /**
     * @return the builder that created the [HistoryRecorderOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        fileDirectory(fileDirectory)
        shouldRecordRouteLineEvents(shouldRecordRouteLineEvents)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryRecorderOptions

        if (fileDirectory != other.fileDirectory) return false
        if (shouldRecordRouteLineEvents != other.shouldRecordRouteLineEvents) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = fileDirectory?.hashCode() ?: 0
        result = 31 * result + shouldRecordRouteLineEvents.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "HistoryRecorderOptions(" +
            "fileDirectory=$fileDirectory, " +
            "shouldRecordRouteLineEvents=$shouldRecordRouteLineEvents" +
            ")"
    }

    /**
     * Builder for [HistoryRecorderOptions].
     */
    class Builder {

        private var fileDirectory: String? = null
        private var shouldRecordRouteLineEvents: Boolean = false

        /**
         * Creates a custom file path to store the history files.
         */
        fun fileDirectory(filePath: String?): Builder =
            apply { this.fileDirectory = filePath }

        /**
         * Enables/disables route line events recording.
         * These events can further be used for debugging purposes.
         */
        fun shouldRecordRouteLineEvents(value: Boolean): Builder =
            apply { this.shouldRecordRouteLineEvents = value }

        /**
         * Build the [HistoryRecorderOptions]
         */
        fun build(): HistoryRecorderOptions {
            return HistoryRecorderOptions(
                fileDirectory = fileDirectory,
                shouldRecordRouteLineEvents = shouldRecordRouteLineEvents,
            )
        }
    }
}
