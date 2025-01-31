package com.mapbox.navigation.base.options

/**
 * Defines options for recording history files.
 *
 * @param fileDirectory used for saving history files. Null use a default directory.
 */
class HistoryRecorderOptions private constructor(
    val fileDirectory: String?,
) {
    /**
     * @return the builder that created the [HistoryRecorderOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        fileDirectory(fileDirectory)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryRecorderOptions

        if (fileDirectory != other.fileDirectory) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        return fileDirectory.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "HistoryRecorderOptions(" +
            "fileDirectory=$fileDirectory" +
            ")"
    }

    /**
     * Builder for [HistoryRecorderOptions].
     */
    class Builder {

        private var fileDirectory: String? = null

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
                fileDirectory = fileDirectory,
            )
        }
    }
}
