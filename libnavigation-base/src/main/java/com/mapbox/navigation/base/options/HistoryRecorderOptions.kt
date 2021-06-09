package com.mapbox.navigation.base.options

/**
 * Defines options for recording history files.
 *
 * @param enabled saves the history files when true, does nothing when false. false by default.
 * @param fileDirectory used for saving history files. Null use a default directory.
 */
class HistoryRecorderOptions private constructor(
    val enabled: Boolean,
    val fileDirectory: String?
) {
    /**
     * @return the builder that created the [HistoryRecorderOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        enabled(enabled)
        fileDirectory(fileDirectory)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryRecorderOptions

        if (enabled != other.enabled) return false
        if (fileDirectory != other.fileDirectory) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + (fileDirectory?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "HistoryRecorderOptions(" +
            "enabled=$enabled, " +
            "fileDirectory=$fileDirectory" +
            ")"
    }

    /**
     * Builder for [HistoryRecorderOptions].
     */
    class Builder {

        private var enabled: Boolean = false
        private var fileDirectory: String? = null

        /**
         * Enables history recording.
         */
        fun enabled(enabled: Boolean): Builder =
            apply { this.enabled = enabled }

        /**
         * Creates a custom file path to store the history files.
         */
        fun fileDirectory(filePath: String?): Builder =
            apply { this.fileDirectory = filePath }

        /**
         * Build the [HistoryRecorderOptions]
         */
        fun build(): HistoryRecorderOptions {
            return HistoryRecorderOptions(
                enabled = enabled,
                fileDirectory = fileDirectory
            )
        }
    }
}
