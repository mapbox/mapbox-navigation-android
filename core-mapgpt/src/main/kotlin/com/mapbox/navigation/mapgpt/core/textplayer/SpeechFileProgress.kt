package com.mapbox.navigation.mapgpt.core.textplayer

import kotlinx.serialization.Serializable

/**
 * Tracks creation progress and state of speech files. It facilitates cache management by
 * enabling the eviction of obsolete files and the cleanup of files interrupted during creation.
 * This mechanism ensures efficient cache use and aids in maintaining a clean file storage by
 * removing incomplete or unused speech files upon the next app start.
 *
 * @property mediaCacheId Identifier for the speech file that is unique per announcement.
 * @property text Text content of the speech file.
 * @property filePath Absolute path to the associated speech file.
 * @property bytesRead Size of the file in bytes, used for cache management.
 * @property isDone Indicates completion of the speech file creation.
 */
@Serializable
data class SpeechFileProgress(
    val mediaCacheId: String,
    val text: String,
    val filePath: String,
    val bytesRead: Long,
    val isDone: Boolean,
)
