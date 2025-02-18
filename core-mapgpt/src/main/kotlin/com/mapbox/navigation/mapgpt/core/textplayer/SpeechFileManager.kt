package com.mapbox.navigation.mapgpt.core.textplayer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Manages the lifecycle of speech files for Text-To-Speech operations, including their
 * creation, caching, and deletion. Links announcements with speech file progress, allowing
 * observation of file creation status through [Announcement.mediaCacheId] and
 * [SpeechFileProgress.mediaCacheId] matching.
 */
interface SpeechFileManager {

    /**
     * Observes the creation progress of a speech file identified by [mediaCacheId]. Emits the
     * current progress or null if the file is unrequested or deleted. Useful for real-time
     * progress tracking.
     *
     * @param mediaCacheId The unique identifier of the speech file to observe.
     * @return A flow emitting [SpeechFileProgress] or null.
     */
    fun observeProgress(mediaCacheId: String): Flow<SpeechFileProgress?>

    /**
     * Retrieves the current progress of a speech file creation synchronously. Returns null if the
     * file is either unrequested or has been deleted, indicating no active or completed process.
     *
     * @param mediaCacheId The unique identifier of the speech file.
     * @return [SpeechFileProgress] or null.
     */
    fun getProgress(mediaCacheId: String): SpeechFileProgress?

    /**
     * Initiates or retrieves the download of a speech file for the provided announcement. Waits
     * until the download is complete before returning the file's path, ensuring the file is ready
     * for use.
     *
     * @param remoteTTSApiClient Client used for speech file requests.
     * @param announcement The announcement requiring a speech file.
     * @return A [Result] containing the file path or an error.
     */
    suspend fun requestSpeechFile(
        remoteTTSApiClient: RemoteTTSApiClient,
        announcement: Announcement,
    ): Result<String>

    /**
     * Deletes the speech file linked to [mediaCacheId] from cache, freeing up storage space. Used
     * when a file is no longer needed.
     *
     * @param mediaCacheId The unique identifier of the speech file to delete.
     */
    fun delete(mediaCacheId: String)

    /**
     * Cleans up old or unused speech files. Should be called when speech files are definitively
     * no longer needed, such as at the end of a navigation session.
     */
    fun clear()
}

/**
 * [SpeechFileProgress] exists when a file is available or the download is in progress. This
 * function will return null if no file is downloaded or the download has not started. This
 * function will block while the download is in progress, and will return the progress once the
 * download has started writing bytes.
 *
 * To start a download, you must call [SpeechFileManager.requestSpeechFile].
 */
@Suppress("MagicNumber")
internal suspend fun SpeechFileManager.isDataAvailable(mediaCacheId: String): SpeechFileProgress? {
    return observeProgress(mediaCacheId).first { progress ->
        progress == null || progress.bytesRead >= 4L
    }
}
