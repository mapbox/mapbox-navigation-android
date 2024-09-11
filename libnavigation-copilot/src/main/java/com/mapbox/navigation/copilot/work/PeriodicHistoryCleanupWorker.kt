package com.mapbox.navigation.copilot.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.delete
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.LOG_CATEGORY
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.internal.listCopilotRecordingFiles
import com.mapbox.navigation.copilot.internal.listCopilotSessionFiles
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Periodic task worker that scans HISTORY_FILES_DIR and:
 * - schedules [HistoryUploadWorker] task for each .pbf.gz recording file that has
 *   matching .metadata.json session file, expect the latest one
 * - deletes all residual .pbf.gz recording files that's missing .metadata.json session file
 *
 * IMPORTANT: This worker expects all files to be chronologically named.
 * It uses file name to identify latest recording file.
 */
internal class PeriodicHistoryCleanupWorker(
    context: Context,
    private val workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val historyFilesDir by lazy { workerParams.inputData.getString(HISTORY_FILES_DIR)!! }
    private val stats = object {
        var recUploadCount = 0
        var recDeleteCount = 0
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sessions = loadCopilotSessions()
        val processedRecordings = scheduleRecordingsUpload(sessions)
        deleteResidualRecordings(sessions, processedRecordings)

        logD(
            "Finished processing history files: " +
                "uploading=${stats.recUploadCount}; deleted=${stats.recDeleteCount}",
        )
        Result.success()
    }

    private fun loadCopilotSessions(): List<CopilotSession> {
        // get all .metadata.json files except the latest one
        return File(historyFilesDir)
            .listCopilotSessionFiles()
            .sortedBy { it.absolutePath }
            .dropLast(1)
            .mapNotNull { file ->
                logD("Processing ${file.name}")
                CopilotSession.fromJson(file.readText()).getOrNull()
            }
    }

    /**
     * @return List of recordings scheduled for upload
     */
    private fun scheduleRecordingsUpload(sessions: List<CopilotSession>): List<File> {
        return sessions.fold(mutableListOf<File>()) { acc, session ->
            val recordingFile = File(session.recording)
            if (recordingFile.exists()) {
                logD("Uploading recording ${session.recording}")
                HistoryUploadWorker.uploadHistory(applicationContext, session)
                acc.add(recordingFile)
                stats.recUploadCount++
            }
            acc
        }
    }

    private fun deleteResidualRecordings(
        sessions: List<CopilotSession>,
        processedRecordings: List<File>,
    ) {
        val excludedRecordings = sessions.map { it.recording }.toMutableSet()
        processedRecordings.forEach {
            excludedRecordings.add(it.absolutePath)
        }

        // we exclude latest (active) recording file,
        // all recordings referenced by the .metadata.json files
        // and all recordings already scheduled for upload
        File(historyFilesDir)
            .listCopilotRecordingFiles()
            .sortedBy { it.absolutePath }
            .dropLast(1)
            .filter { it.absolutePath !in excludedRecordings }
            .forEach {
                logD("Deleting recording ${it.name}")
                delete(it)
                stats.recDeleteCount++
            }
    }

    internal companion object {

        private const val HISTORY_FILES_DIR: String = "history_files_dir"

        fun scheduleWork(
            context: Context,
            historyFilesDir: String?,
        ) {
            if (historyFilesDir.isNullOrBlank()) {
                logE("Failed to schedule periodic upload work! Missing $HISTORY_FILES_DIR.")
                return
            }

            val workRequest = PeriodicWorkRequestBuilder<PeriodicHistoryCleanupWorker>(
                15,
                TimeUnit.MINUTES, // repeatInterval
                5,
                TimeUnit.MINUTES, // flexTimeInterval
            ).setInputData(
                Data.Builder()
                    .putString(HISTORY_FILES_DIR, historyFilesDir)
                    .build(),
            )
                .addTag("copilot-cleanup")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "copilot-cleanup.periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest,
                )
        }

        private fun logD(msg: String) = logD("[cleanup] $msg", LOG_CATEGORY)

        private fun logE(msg: String) = logE("[cleanup] $msg", LOG_CATEGORY)
    }
}
